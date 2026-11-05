#!/usr/bin/env python3
"""Fail-closed, local-only C2 request and budget preflight.

This tool never contacts a provider and never authorizes dispatch. Input JSONL
records are {"case_id": str, "source_fields": {recipe_title, ingredients,
target_ingredient}, "payload": {recipe_title, ingredient_list,
ingredient_to_replace}}. It verifies an exact mapping from source_fields to
payload; ingredient lists preserve each alias group as one nested slot
(`list[list[str]]`); `gold_substitute` is not allowed. case_id remains local and is not
serialized into the provider request. The manifest pins every input/evidence
file by SHA-256.

The tool records exact UTF-8 bytes of the serialized HTTP request body, but
does not infer model input-token counts or request framing from those bytes.
The official Responses input-token-count endpoint is documented, but its
billing/call-cap treatment and transfer authorization are not established.
Without a separately authorized, request-bound exact-count receipt, input
token cost is UNKNOWN and no cost-based cohort scenario is emitted. The output
is a fail-closed evidence/readiness report only; a lead must inspect evidence,
choose/freeze the cohort, install a reservation, and separately authorize a
run.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any
from urllib.parse import urlparse


PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
MODEL = "gpt-4o-2024-08-06"
MONEY_CAP_USD = 100.0
CALL_CAP = 25_000
FALLBACK_CASES = 2_000
FULL_CASES = 10_747
FALLBACK_SEED = 20261021
T203_MANIFEST_SHA256 = "c84493e76240f041b4dccb01650be3f0aaa2033ad0b35b1ab9c88b135fa90fcb"
T203_PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
T203_CASE_COUNT = 10_747
T203_READY_COUNT = 10_499
T203_ERROR_COUNT = 248
T203_ALLOWED_ERROR_REASONS = {"TARGET_SLOT_MATCH_MULTIPLE"}
FRESHNESS_SECONDS = 24 * 60 * 60
INPUT_RATE = Decimal("2.50")
OUTPUT_RATE = Decimal("10.00")
PAYLOAD_KEYS = {"recipe_title", "ingredient_list", "ingredient_to_replace"}
OFFICIAL_HOSTS = {"platform.openai.com", "developers.openai.com"}
SCHEMA = {
    "type": "object",
    "properties": {
        "substitutions": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 20,
            "maxItems": 20,
        }
    },
    "required": ["substitutions"],
    "additionalProperties": False,
}


class PreflightError(ValueError):
    pass


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_json(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise PreflightError(f"expected a JSON object: {path}")
    return value


def pinned_file(base: Path, ref: Any, label: str) -> tuple[Path, dict[str, Any]]:
    if not isinstance(ref, dict) or set(ref) != {"path", "sha256"}:
        raise PreflightError(f"{label} must contain only path and sha256")
    rel = ref["path"]
    expected = ref["sha256"]
    if not isinstance(rel, str) or not rel or not isinstance(expected, str) or not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise PreflightError(f"invalid pinned file reference: {label}")
    path = (base / rel).resolve()
    try:
        path.relative_to(base.resolve())
    except ValueError as exc:
        raise PreflightError(f"{label} path escapes the manifest directory") from exc
    if not path.is_file():
        raise PreflightError(f"missing {label}: {path}")
    actual = sha256(path)
    if actual != expected:
        raise PreflightError(f"SHA-256 mismatch for {label}: expected {expected}, got {actual}")
    return path, load_json(path) if path.suffix.lower() == ".json" else {}


def parse_time(value: Any, label: str) -> datetime:
    if not isinstance(value, str):
        raise PreflightError(f"{label}.observed_at must be an ISO-8601 UTC timestamp")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise PreflightError(f"invalid {label}.observed_at") from exc
    if parsed.tzinfo is None or parsed.utcoffset() is None:
        raise PreflightError(f"{label}.observed_at must include a timezone")
    return parsed.astimezone(timezone.utc)


def ensure_fresh(receipt: dict[str, Any], label: str, now: datetime) -> None:
    observed = parse_time(receipt.get("observed_at"), label)
    age = (now - observed).total_seconds()
    if age < 0 or age > FRESHNESS_SECONDS:
        raise PreflightError(f"{label} is future-dated or older than the 24-hour preflight window")


def decimal_number(record: dict[str, Any], key: str, label: str, *, positive: bool = False) -> Decimal:
    value = record.get(key)
    if isinstance(value, bool) or not isinstance(value, (int, float, str)):
        raise PreflightError(f"{label}.{key} must be a finite decimal number")
    try:
        number = Decimal(str(value))
    except InvalidOperation as exc:
        raise PreflightError(f"{label}.{key} must be a finite decimal number") from exc
    if not number.is_finite() or number < 0 or (positive and number <= 0):
        qualifier = "positive " if positive else "nonnegative "
        raise PreflightError(f"{label}.{key} must be a finite {qualifier}decimal number")
    return number


def validate_capture(base: Path, receipt: dict[str, Any], label: str, *, official: bool = False) -> str:
    url = receipt.get("source_url")
    if official:
        parsed = urlparse(url) if isinstance(url, str) else None
        if parsed is None or parsed.scheme != "https" or parsed.hostname not in OFFICIAL_HOSTS:
            raise PreflightError(f"{label} source_url must use an official OpenAI documentation/account host")
    elif not isinstance(receipt.get("evidence_kind"), str) or not receipt["evidence_kind"].strip():
        raise PreflightError(f"{label} must identify the kind of captured evidence")
    capture_path, _ = pinned_file(base, receipt.get("capture_evidence"), f"{label} capture")
    if capture_path.stat().st_size == 0:
        raise PreflightError(f"{label} captured evidence is empty")
    return sha256(capture_path)


def ordered_ids_sha256(records: list[dict[str, Any]]) -> str:
    data = ("\n".join(case_identity(rec) for rec in records) + "\n").encode("utf-8")
    return hashlib.sha256(data).hexdigest()


def case_identity(record: dict[str, Any]) -> str:
    """Return the stable local identity shared by READY and error rows."""
    value = record.get("case_ref", record.get("case_id"))
    if not isinstance(value, str) or not value:
        raise PreflightError("case identity must be a nonempty local string")
    return value


def validate_ledger(ledger: dict[str, Any], base: Path, max_call_cost: Decimal | None) -> None:
    validate_capture(base, ledger, "budget ledger")
    if ledger.get("reconciliation_status") != "reconciled" or not isinstance(ledger.get("account_alias"), str) or not ledger["account_alias"].strip():
        raise PreflightError("ledger needs an attributable account alias and reconciled status")
    entries = ledger.get("entries")
    if not isinstance(entries, list) or not entries:
        raise PreflightError("ledger needs itemized spend/reservation/uncertain-call entries")
    totals = {"spent": Decimal("0"), "reserved": Decimal("0"), "uncertain": Decimal("0")}
    calls = {"spent": 0, "reserved": 0, "uncertain": 0}
    seen: set[str] = set()
    for index, entry in enumerate(entries):
        if not isinstance(entry, dict) or set(entry) - {"entry_id", "state", "usd", "calls", "worst_case_usd_per_call"}:
            raise PreflightError(f"ledger entry {index} has an invalid shape")
        entry_id, state = entry.get("entry_id"), entry.get("state")
        if not isinstance(entry_id, str) or not entry_id or entry_id in seen or state not in totals:
            raise PreflightError(f"ledger entry {index} has invalid identity/state")
        seen.add(entry_id)
        amount = decimal_number(entry, "usd", f"ledger.entries[{index}]")
        count = entry.get("calls")
        if isinstance(count, bool) or not isinstance(count, int) or count < 0:
            raise PreflightError(f"ledger entry {index} calls must be a nonnegative integer")
        if count and state in {"reserved", "uncertain"}:
            per_call = decimal_number(entry, "worst_case_usd_per_call", f"ledger.entries[{index}]", positive=True)
            if max_call_cost is None:
                raise PreflightError("input token cost is unknown; reservation sufficiency cannot be verified")
            if per_call < max_call_cost or amount < per_call * count:
                raise PreflightError(f"ledger entry {index} under-reserves worst-case call cost")
        elif "worst_case_usd_per_call" in entry:
            decimal_number(entry, "worst_case_usd_per_call", f"ledger.entries[{index}]")
        totals[state] += amount
        calls[state] += count
    expected_amounts = {"spent": "spent_usd", "reserved": "active_reservations_usd", "uncertain": "uncertain_calls_usd"}
    expected_calls = {"spent": "calls_spent", "reserved": "calls_reserved", "uncertain": "uncertain_calls"}
    for state in totals:
        if decimal_number(ledger, expected_amounts[state], "ledger") != totals[state]:
            raise PreflightError(f"ledger aggregate {expected_amounts[state]} does not match itemized entries")
        value = ledger.get(expected_calls[state])
        if isinstance(value, bool) or not isinstance(value, int) or value != calls[state]:
            raise PreflightError(f"ledger aggregate {expected_calls[state]} does not match itemized entries")


def validate_fallback_selection(receipt: dict[str, Any], full: list[dict[str, Any]], fallback: list[dict[str, Any]], full_sha: str, fallback_sha: str, base: Path) -> str:
    validate_capture(base, receipt, "fallback selection")
    if receipt.get("seed") != FALLBACK_SEED or receipt.get("method") != "stratified_without_replacement_partition1_x_oracle_v1":
        raise PreflightError("fallback selection must use the protocol's frozen seed and stratum design")
    if receipt.get("full_cohort_sha256") != full_sha or receipt.get("fallback_cohort_sha256") != fallback_sha:
        raise PreflightError("fallback selection receipt does not bind the exact cohort files")
    if receipt.get("full_case_ids_sha256") != ordered_ids_sha256(full) or receipt.get("selected_case_ids_sha256") != ordered_ids_sha256(fallback):
        raise PreflightError("fallback selection receipt does not bind the ordered case IDs")
    if not isinstance(receipt.get("selector_code_sha256"), str) or not re.fullmatch(r"[0-9a-f]{64}", receipt["selector_code_sha256"]):
        raise PreflightError("fallback selection needs a pinned selector implementation hash")
    stratum_counts = receipt.get("stratum_counts")
    if not isinstance(stratum_counts, dict) or not stratum_counts or any(isinstance(v, bool) or not isinstance(v, int) or v < 0 for v in stratum_counts.values()) or sum(stratum_counts.values()) != FALLBACK_CASES:
        raise PreflightError("fallback selection stratum counts must be nonnegative and sum to 2,000")
    return sha256(pinned_file(base, receipt["selector_code"], "fallback selector code")[0])


def validate_cohort(path: Path, expected_count: int, label: str) -> tuple[list[dict[str, Any]], str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    if len(lines) != expected_count:
        raise PreflightError(f"{label} expected {expected_count} records, found {len(lines)}")
    records: list[dict[str, Any]] = []
    case_ids: set[str] = set()
    for line_no, line in enumerate(lines, start=1):
        try:
            rec = json.loads(line)
        except json.JSONDecodeError as exc:
            raise PreflightError(f"{label}:{line_no} is invalid JSON") from exc
        if not isinstance(rec, dict):
            raise PreflightError(f"{label}:{line_no} must be a JSON object")
        legacy_ready = set(rec) == {"case_id", "source_fields", "payload"}
        generated_ready = set(rec) == {"case_id", "case_ref", "source_fields", "payload", "status"} and rec.get("status") == "READY"
        generated_error = set(rec) == {"case_ref", "reason", "status"} and rec.get("status") == "PREPARATION_ERROR"
        if generated_error:
            identity = case_identity(rec)
            reason = rec.get("reason")
            if identity in case_ids:
                raise PreflightError(f"{label}:{line_no} has a duplicate local case identity")
            if not isinstance(reason, str) or reason not in T203_ALLOWED_ERROR_REASONS:
                raise PreflightError(f"{label}:{line_no} has an unknown preparation-error reason")
            case_ids.add(identity)
            records.append(rec)
            continue
        if not (legacy_ready or generated_ready):
            raise PreflightError(f"{label}:{line_no} has an unsupported cohort record shape/status")
        identity = case_identity(rec)
        if identity in case_ids:
            raise PreflightError(f"{label}:{line_no} has a duplicate local case identity")
        if generated_ready:
            case_id = rec.get("case_id")
            if not isinstance(case_id, str) or not case_id:
                raise PreflightError(f"{label}:{line_no} has an invalid local case_id")
        case_ids.add(identity)
        source, payload = rec["source_fields"], rec["payload"]
        if not isinstance(source, dict) or set(source) != {"recipe_title", "ingredients", "target_ingredient"}:
            raise PreflightError(f"{label}:{line_no} source_fields must be exactly the three approved SSoT source fields")
        if not isinstance(payload, dict) or set(payload) != PAYLOAD_KEYS:
            raise PreflightError(f"{label}:{line_no} payload keys must be exactly {sorted(PAYLOAD_KEYS)}")
        if payload["recipe_title"] != source["recipe_title"] or payload["ingredient_list"] != source["ingredients"] or payload["ingredient_to_replace"] != source["target_ingredient"]:
            raise PreflightError(f"{label}:{line_no} payload is not an exact mapping of the approved source fields")
        if any(not isinstance(payload[k], str) or not payload[k].strip() for k in ("recipe_title", "ingredient_to_replace")):
            raise PreflightError(f"{label}:{line_no} title and ingredient_to_replace must be nonempty strings")
        ingredients = payload["ingredient_list"]
        if not isinstance(ingredients, list) or not ingredients or not all(
            isinstance(group, list) and group and all(isinstance(alias, str) and alias.strip() for alias in group)
            for group in ingredients
        ):
            raise PreflightError(f"{label}:{line_no} ingredient_list must preserve nested alias groups (list[list[str]])")
        target = payload["ingredient_to_replace"]
        matching_slots = [group for group in ingredients if any(alias == target for alias in group)]
        if len(matching_slots) != 1:
            raise PreflightError(f"{label}:{line_no} exact target spelling must occur in exactly one alias group")
        records.append(rec)
    return records, sha256(path)


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")


def response_schema() -> dict[str, Any]:
    return {"type": "json_schema", "name": "senath_top20", "strict": True, "schema": SCHEMA}


def request_body(manifest: dict[str, Any], payload: dict[str, Any]) -> dict[str, Any]:
    return {
        "model": MODEL,
        "input": [
            {"role": "system", "content": manifest["system_message"]},
            {"role": "user", "content": canonical_json(payload).decode("utf-8")},
        ],
        "temperature": 0,
        "max_output_tokens": manifest["max_output_tokens"],
        "store": False,
        "text": {"format": response_schema()},
    }


def request_size_report(cases: list[dict[str, Any]], manifest: dict[str, Any]) -> dict[str, Any]:
    ready = [rec for rec in cases if rec.get("status", "READY") == "READY"]
    errors = [rec for rec in cases if rec.get("status") == "PREPARATION_ERROR"]
    reason_counts: dict[str, int] = {}
    for rec in errors:
        reason = rec["reason"]
        reason_counts[reason] = reason_counts.get(reason, 0) + 1
    body_sizes = [len(canonical_json(request_body(manifest, rec["payload"]))) for rec in ready]
    return {
        "cases": len(cases),
        "denominator_cases": len(cases),
        "requestable_cases": len(ready),
        "preparation_error_cases": len(errors),
        "preparation_error_reasons": reason_counts,
        "canonical_request_body_utf8_bytes_total": sum(body_sizes),
        "canonical_request_body_utf8_bytes_max_per_case": max(body_sizes, default=0),
        "model_input_tokens": None,
        "model_input_token_cost_usd": None,
        "max_output_tokens_per_case": manifest["max_output_tokens"],
        "output_cost_usd": None,
    }


def preflight(manifest_path: Path, now: datetime | None = None) -> dict[str, Any]:
    now = now or datetime.now(timezone.utc)
    manifest_path = manifest_path.resolve()
    manifest = load_json(manifest_path)
    required = {
        "protocol_sha256", "model", "system_message", "max_output_tokens",
        "pricing_evidence", "model_availability", "budget_ledger", "account_availability",
        "full_cohort", "fallback_cohort", "fallback_selection_receipt",
    }
    optional: set[str] = set()
    if required - set(manifest) or set(manifest) - required - optional:
        raise PreflightError(f"manifest keys differ; missing={sorted(required-set(manifest))}, extra={sorted(set(manifest)-required-optional)}")
    if manifest["protocol_sha256"] != PROTOCOL_SHA256 or manifest["model"] != MODEL:
        raise PreflightError("protocol or exact pinned model does not match the authorized mission")
    if isinstance(manifest["max_output_tokens"], bool) or manifest["max_output_tokens"] != 256:
        raise PreflightError("candidate output cap must remain exactly 256 until separately frozen")
    if not isinstance(manifest["system_message"], str) or not manifest["system_message"].strip():
        raise PreflightError("system_message must be nonempty")
    if manifest["system_message"] != (
        "You generate ranked ingredient substitutions for a recipe. Given only the recipe title, its ingredient list, "
        "and the ingredient to replace, return exactly 20 distinct ingredient names that could replace that ingredient "
        "in this recipe, ordered from most to least suitable in this recipe context. The ingredient list is an array of slots: "
        "each inner list is one ingredient slot, and its strings are aliases for the same ingredient, not separate ingredients. "
        "Do not infer that the first alias is canonical. The exact ingredient-to-replace spelling identifies one slot; do not return any alias from that entire slot. Do not include the ingredient to "
        "replace itself. Do not include explanations, scores, citations, or any text outside the required JSON object."
    ):
        raise PreflightError("system_message does not match the reviewed candidate bytes")

    base = manifest_path.parent
    pricing_path, prices = pinned_file(base, manifest["pricing_evidence"], "pricing evidence")
    ledger_path, ledger = pinned_file(base, manifest["budget_ledger"], "budget ledger")
    account_path, account = pinned_file(base, manifest["account_availability"], "account availability")
    availability_path, availability = pinned_file(base, manifest["model_availability"], "model availability")
    full_path, _ = pinned_file(base, manifest["full_cohort"], "full cohort")
    fallback_path, _ = pinned_file(base, manifest["fallback_cohort"], "fallback cohort")
    selection_path, selection = pinned_file(base, manifest["fallback_selection_receipt"], "fallback selection receipt")
    for path, label in ((pricing_path, "pricing"), (ledger_path, "budget ledger"), (account_path, "account availability"), (availability_path, "model availability"), (selection_path, "fallback selection")):
        ensure_fresh(load_json(path), label, now)
    pricing_capture_sha = validate_capture(base, prices, "pricing", official=True)
    availability_capture_sha = validate_capture(base, availability, "model availability", official=True)
    account_capture_sha = validate_capture(base, account, "account availability", official=True)
    if prices.get("model") != MODEL or prices.get("currency") != "USD" or decimal_number(prices, "input_usd_per_million", "pricing", positive=True) != INPUT_RATE or decimal_number(prices, "output_usd_per_million", "pricing", positive=True) != OUTPUT_RATE:
        raise PreflightError("pricing receipt must match the frozen snapshot's reviewed positive USD rates")
    if availability.get("model") != MODEL or availability.get("status") != "verified_callable":
        raise PreflightError("the exact pinned model snapshot must have a fresh callable-availability receipt")
    if not isinstance(account.get("account_alias"), str) or not account["account_alias"].strip() or account.get("scope") != "project_available_balance" or "available_usd" not in account:
        raise PreflightError("account receipt must identify the account and project-available balance scope")

    full, full_sha = validate_cohort(full_path, FULL_CASES, "full cohort")
    fallback, fallback_sha = validate_cohort(fallback_path, FALLBACK_CASES, "fallback cohort")
    full_ids = {case_identity(r) for r in full}
    fallback_ids = {case_identity(r) for r in fallback}
    if not fallback_ids <= full_ids:
        raise PreflightError("fallback case IDs must be a subset of the frozen full cohort")

    selector_code_sha = validate_fallback_selection(selection, full, fallback, full_sha, fallback_sha, base)

    ledger_capture_sha = validate_capture(base, ledger, "budget ledger")
    available = decimal_number(account, "available_usd", "account")
    full_request = request_size_report(full, manifest)
    fallback_request = request_size_report(fallback, manifest)
    max_call_cost = None
    validate_ledger(ledger, base, max_call_cost)

    spent = decimal_number(ledger, "spent_usd", "ledger")
    reserved = decimal_number(ledger, "active_reservations_usd", "ledger")
    uncertain = decimal_number(ledger, "uncertain_calls_usd", "ledger")
    spent_calls, reserved_calls, uncertain_calls = (ledger[k] for k in ("calls_spent", "calls_reserved", "uncertain_calls"))
    money_residual = Decimal("100") - spent - reserved - uncertain
    call_residual = CALL_CAP - spent_calls - reserved_calls - uncertain_calls
    if money_residual < 0 or call_residual < 0:
        raise PreflightError("reconciled spend/reservations exceed a hard mission cap")
    effective_residual = min(money_residual, available)
    blocked = [
        "exact model input-token counts are unavailable; serialized request bytes are not a token-count receipt",
        "the documented input-token-count endpoint has no verified billing/call-cap treatment or transfer authorization",
        "no cost-based cohort scenario or reservation sufficiency decision can be produced",
    ]

    return {
        "status": "BLOCKED",
        "dispatch_authorized": False,
        "cohort_selected": False,
        "selected_cohort": None,
        "conditional_scenario_if_receipts_are_true": None,
        "protocol_sha256": PROTOCOL_SHA256,
        "model": MODEL,
        "evidence": {
            "manifest_sha256": sha256(manifest_path),
            "full_cohort_sha256": full_sha,
            "fallback_cohort_sha256": fallback_sha,
            "pricing_receipt_sha256": sha256(pricing_path),
            "pricing_capture_sha256": pricing_capture_sha,
            "model_availability_receipt_sha256": sha256(availability_path),
            "model_availability_capture_sha256": availability_capture_sha,
            "budget_ledger_sha256": sha256(ledger_path),
            "budget_ledger_capture_sha256": ledger_capture_sha,
            "account_availability_sha256": sha256(account_path),
            "account_capture_sha256": account_capture_sha,
            "fallback_selection_receipt_sha256": sha256(selection_path),
            "fallback_selector_code_sha256": selector_code_sha,
        },
        "residuals": {
            "mission_money_usd": str(money_residual.quantize(Decimal("0.000001"))),
            "observed_account_available_usd": str(available.quantize(Decimal("0.000001"))),
            "effective_money_usd": str(effective_residual.quantize(Decimal("0.000001"))),
            "calls": int(call_residual),
        },
        "full_test_request_size": full_request,
        "fallback_2000_request_size": fallback_request,
        "request_body_sha256_by_case": {
            case_identity(rec): hashlib.sha256(canonical_json(request_body(manifest, rec["payload"]))).hexdigest()
            for rec in full if rec.get("status", "READY") == "READY"
        },
        "serializer_source_sha256": sha256(Path(__file__).resolve()),
        "blocked_reasons": blocked,
        "input_token_evidence_boundary": "canonical request-body UTF-8 byte sizes are local serialization measurements, not model input-token counts or proof of actual transport bytes; exact model tokens and cost are UNKNOWN",
        "evidence_truth_boundary": "receipt hashes establish byte integrity only; a lead must independently inspect captured source/account/ledger evidence; this blocked preflight does not prove receipt truth or dispatch readiness",
        "required_next_action": "establish billing/call-cap treatment and transfer authorization for the documented input-token-count endpoint, then obtain a separately authorized request-bound exact-count receipt; until then do not select a cost-based cohort, reserve, or dispatch",
    }


def validate_generated_package(manifest_path: Path) -> dict[str, Any]:
    """Validate only pinned T203 case records and emit aggregate counts."""
    manifest_path = manifest_path.resolve()
    if sha256(manifest_path) != T203_MANIFEST_SHA256:
        raise PreflightError("generated package manifest pin mismatch")
    manifest = load_json(manifest_path)
    if (
        manifest.get("protocol_sha256") != T203_PROTOCOL_SHA256
        or manifest.get("state") != "LOCAL_ONLY_C2_API_SOURCE_CANDIDATE_NOT_SELECTED_OR_UPLOAD_AUTHORIZED"
        or manifest.get("cohort_count") != T203_CASE_COUNT
        or manifest.get("ready_count") != T203_READY_COUNT
        or manifest.get("preparation_error_count") != T203_ERROR_COUNT
        or manifest.get("cohort_selected") is not False
        or manifest.get("provider_called") is not False
        or manifest.get("upload_authorized") is not False
    ):
        raise PreflightError("generated package state/counts differ from the pinned candidate contract")
    partitions = manifest.get("partitions")
    if not isinstance(partitions, list) or len(partitions) != 54:
        raise PreflightError("generated package partition inventory is invalid")
    base = manifest_path.parent
    all_records: list[dict[str, Any]] = []
    ready_count = error_count = 0
    reason_counts: dict[str, int] = {}
    for expected_index, part in enumerate(partitions):
        if not isinstance(part, dict) or part.get("partition_index") != expected_index:
            raise PreflightError("generated package partition order is invalid")
        relative = Path("partitions") / f"test-{expected_index:03d}" / "case_records.jsonl"
        record_path = (base / relative).resolve()
        try:
            record_path.relative_to(base.resolve())
        except ValueError as exc:
            raise PreflightError("generated record path escapes package root") from exc
        if not record_path.is_file() or sha256(record_path) != part.get("case_records_sha256"):
            raise PreflightError("generated case-record partition hash mismatch")
        records, _ = validate_cohort(record_path, part.get("case_count"), f"generated partition {expected_index}")
        part_ready = sum(1 for rec in records if rec.get("status", "READY") == "READY")
        part_errors = len(records) - part_ready
        if part_ready != part.get("ready_count") or part_errors != part.get("preparation_error_count"):
            raise PreflightError("generated partition counts differ from manifest")
        for rec in records:
            if rec.get("status") == "PREPARATION_ERROR":
                code = rec["reason"]
                reason_counts[code] = reason_counts.get(code, 0) + 1
        all_records.extend(records)
        ready_count += part_ready
        error_count += part_errors
    if len(all_records) != T203_CASE_COUNT or ready_count != T203_READY_COUNT or error_count != T203_ERROR_COUNT:
        raise PreflightError("generated package denominator or disposition totals mismatch")
    identities = [case_identity(rec) for rec in all_records]
    if len(set(identities)) != T203_CASE_COUNT:
        raise PreflightError("generated package local case identities are not unique")
    if hashlib.sha256(("\n".join(identities) + "\n").encode("utf-8")).hexdigest() != manifest.get("case_refs_sha256"):
        raise PreflightError("generated package case-reference order hash mismatch")
    if reason_counts != manifest.get("preparation_error_reasons"):
        raise PreflightError("generated preparation-error reason totals mismatch")
    return {
        "status": "GENERATED_PACKAGE_VALIDATED_LOCAL_ONLY",
        "manifest_sha256": T203_MANIFEST_SHA256,
        "denominator_cases": T203_CASE_COUNT,
        "requestable_cases": ready_count,
        "preparation_error_cases": error_count,
        "preparation_error_reasons": reason_counts,
        "unique_local_case_references": len(set(identities)),
        "case_references_sha256": manifest["case_refs_sha256"],
        "request_body_hashes_include_preparation_errors": False,
        "cohort_selected": False,
        "dispatch_authorized": False,
        "model_input_tokens": None,
        "model_input_token_cost_usd": None,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--manifest", type=Path)
    mode.add_argument("--validate-generated-package", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        if args.validate_generated_package:
            report = validate_generated_package(args.validate_generated_package)
        else:
            report = preflight(args.manifest)
    except (OSError, json.JSONDecodeError, PreflightError, TypeError, KeyError, AttributeError) as exc:
        report = {"status": "BLOCKED", "dispatch_authorized": False, "error": str(exc)}
    serialized = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(serialized, encoding="utf-8", newline="\n")
    else:
        sys.stdout.write(serialized)
    return 0 if report.get("status") in {"SCENARIO_ESTIMATE_ONLY", "GENERATED_PACKAGE_VALIDATED_LOCAL_ONLY"} else 2


if __name__ == "__main__":
    raise SystemExit(main())
