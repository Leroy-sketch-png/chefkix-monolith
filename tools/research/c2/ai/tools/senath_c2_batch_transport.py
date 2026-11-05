#!/usr/bin/env python3
"""Default-off, one-phase-at-a-time C2 Batch transport state machine.

Local validation is read-only. Remote phases require an independently lead-
reviewed release whose digest is pinned in this source; no release is currently
pinned. Tests inject an in-memory fake transport and synthetic evidence only.
"""

from __future__ import annotations

import argparse
import contextlib
import hashlib
import importlib.util
import io
import json
import math
import os
import re
import sys
import tempfile
import time
import uuid
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Iterator

PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
SAVED_RESULT_RECONCILER_V2_SHA256 = "97c07940c8d9b41a34bcfbcfa6b78760ac50ee41da7ba540f1fe7143691d5169"
OFFLINE_ATTEMPT_LEDGER_V1_SHA256 = "20a51bbfdc82f43cb1655657f8b02e7660cb1648f77de34610c40cb0f095e431"
T253_SOURCE_SHA256 = "24091fe506bb91c4225de4c3d9052e2e00e8518b5810666d78afeb0f151742ac"
T222_BRIDGE_SHA256 = "993f4718b138ebcfcf8a66d0a66b4a509d6b0bbd8a3af6d4b3ea3e20e7d58ac4"
T219_SOURCE_SHA256 = "d2a0a1c06f3a0f12c74c0bf3a766367ee93a2d0e5b3d26b4f35c2d2b1a71c36b"
T249_SOURCE_SHA256 = "5f894e1b2d50120781ef7aaad9bcfeb0fa70e976787586bca00367f83686b2ae"
PARSER_SOURCE_SHA256 = "342dfc1f547bebb539267648c6b7dca6fd2f749d1f4e18e0b57e089d73f34656"
T314_SOURCE_SHA256 = "392f9d082c65346dbf11b25772a129760329da0c9eb7f4f4562cab980d361c3a"
EXPECTED_MODEL = "gpt-4o-2024-08-06"
EXPECTED_ENDPOINT = "/v1/responses"
EXPECTED_PAYLOAD_FIELDS = ["recipe_title", "ingredient_list", "ingredient_to_replace"]
MAX_AUTHORIZED_USD = 100.0
MAX_AUTHORIZED_CALLS = 25_000
COMPLETION_WINDOW = "24h"
# Deliberately unset. The lead must review real receipts and pin their exact
# release digest in a separate reviewed change before any CLI remote phase.
PINNED_LEAD_RELEASE_SHA256: str | None = None
SHA256_RE = re.compile(r"[0-9a-f]{64}\Z")
ALLOWED_BATCH_STATES = {"validating", "in_progress", "finalizing", "completed", "failed", "expired", "cancelling", "cancelled"}


class DriverError(ValueError):
    """Sanitized failure; messages must never contain payloads or SDK text."""


def sha256_bytes(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _reconciliation_id(facts: dict[str, Any], release_hash: str,
                       operation_nonce: str | None = None) -> str:
    binding = {
        "schema": "SENATH_C2_RECONCILIATION_ID_V1",
        "package_manifest_sha256": facts["package_manifest_sha256"],
        "batch_requests_sha256": facts["batch_requests_sha256"],
        "release_sha256": release_hash,
    }
    if operation_nonce is not None:
        binding["operation_nonce"] = operation_nonce
    return sha256_bytes(canonical_json(binding))


def _json_load(raw: bytes) -> Any:
    def no_duplicates(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                raise DriverError("duplicate JSON property")
            result[key] = value
        return result

    try:
        return json.loads(raw.decode("utf-8", errors="strict"), object_pairs_hook=no_duplicates,
                          parse_constant=lambda _: (_ for _ in ()).throw(DriverError("non-standard JSON constant")))
    except (UnicodeError, json.JSONDecodeError, DriverError):
        raise DriverError("malformed UTF-8 JSON") from None


def _jsonl(raw: bytes, *, empty_ok: bool = False) -> list[dict[str, Any]]:
    if not raw:
        if empty_ok:
            return []
        raise DriverError("required JSONL artifact is empty")
    if not raw.endswith(b"\n") or b"\r" in raw:
        raise DriverError("JSONL must be complete LF-terminated records")
    rows = [_json_load(line) for line in raw[:-1].split(b"\n")]
    if any(not isinstance(row, dict) for row in rows):
        raise DriverError("JSONL records must be objects")
    return rows


def _load_pinned(name: str, path: Path, expected_sha256: str):
    try:
        raw = path.read_bytes()
    except OSError:
        raise DriverError(f"pinned {name} source unavailable") from None
    if sha256_bytes(raw) != expected_sha256:
        raise DriverError(f"pinned {name} source digest mismatch")
    spec = importlib.util.spec_from_file_location(f"senath_t316_{name}", path)
    if spec is None or spec.loader is None:
        raise DriverError(f"pinned {name} source could not load")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def load_contracts(root: Path) -> dict[str, Any]:
    tools = root / "ai" / "tools"
    return {
        "t253": _load_pinned("t253", tools / "build_senath_c2_batch_transport_candidate.py", T253_SOURCE_SHA256),
        "t222": _load_pinned("t222", tools / "bridge_c2_offline_package_to_attempt_journal.py", T222_BRIDGE_SHA256),
        "t219": _load_pinned("t219", tools / "c2_attempt_resume.py", T219_SOURCE_SHA256),
        "t249": _load_pinned("t249", tools / "c2_attempt_journal_v2.py", T249_SOURCE_SHA256),
        "parser": _load_pinned("parser", tools / "validate_c2_openai_response.py", PARSER_SOURCE_SHA256),
        "t314": _load_pinned("t314", tools / "bridge_senath_saved_batch_results_to_journal.py", T314_SOURCE_SHA256),
    }


def _file_hash(path: Path) -> str:
    try:
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()
    except OSError:
        raise DriverError("required local artifact unavailable") from None


def validate_local_inputs(root: Path, package_dir: Path, cases_path: Path, events_path: Path,
                          contracts: dict[str, Any]) -> dict[str, Any]:
    """Read-only package/journal verification. Does not read credentials or call a client."""
    t253, t219, t249 = contracts["t253"], contracts["t219"], contracts["t249"]
    try:
        package_result = t253.validate_saved_package(root, package_dir)
        manifest = t253.read_json(package_dir / "manifest.json")
        cases, cases_raw = t219.read_jsonl(cases_path)
        events, events_raw = t219.read_jsonl(events_path) if events_path.exists() else ([], b"")
    except (OSError, ValueError, KeyError, TypeError):
        raise DriverError("local package or journal did not satisfy pinned contracts") from None
    if cases_raw != t219.jsonl_bytes(cases) or (events_raw and events_raw != t219.jsonl_bytes(events)):
        raise DriverError("case inventory or journal is noncanonical")
    package_hash = manifest.get("parent_t208_manifest_sha256")
    case_hash = sha256_bytes(cases_raw)
    journal_hash = sha256_bytes(events_raw)
    if not isinstance(package_hash, str) or SHA256_RE.fullmatch(package_hash) is None:
        raise DriverError("parent package binding is invalid")
    if not cases:
        raise DriverError("case inventory is empty")
    try:
        state = t249.reconcile_attempts_v2(
            cases, events, package_hash, expected_case_inventory_sha256=case_hash,
            event_ledger_sha256=journal_hash, v1_path=root / "ai/tools/c2_attempt_resume.py",
        )
    except ValueError:
        raise DriverError("case inventory or journal fails pinned reconciliation") from None
    ready_ids = [row["case_id"] for row in cases if row.get("disposition") == "READY"]
    error_ids = [row["case_id"] for row in cases if row.get("disposition") == "PREPARATION_ERROR"]
    if (len(ready_ids) + len(error_ids) != len(cases) or len(set(ready_ids + error_ids)) != len(cases)
            or state.get("case_count") != len(cases)):
        raise DriverError("case inventory counts or identities are inconsistent")
    # These are the fixed T253 package counts, not a driver-selected cohort.
    counts = manifest.get("counts")
    if (len(cases) != 10_747 or len(ready_ids) != 10_499 or len(error_ids) != 248
            or not isinstance(counts, dict)
            or counts.get("denominator_cases") != 10_747
            or counts.get("ready_requests") != 10_499
            or counts.get("retained_preparation_errors") != 248
            or package_result.get("denominator_cases") != 10_747
            or package_result.get("ready_requests") != 10_499
            or package_result.get("retained_preparation_errors") != 248):
        raise DriverError("T253 case counts differ from the frozen package inventory")
    batch_contract = manifest.get("batch_contract")
    if (not isinstance(batch_contract, dict) or batch_contract.get("url") != EXPECTED_ENDPOINT
            or batch_contract.get("method") != "POST"):
        raise DriverError("T253 model or endpoint differs from the frozen protocol")
    body = package_result.get("batch_requests_sha256")
    package_manifest_sha = package_result.get("manifest_sha256")
    if not SHA256_RE.fullmatch(body or "") or not SHA256_RE.fullmatch(package_manifest_sha or ""):
        raise DriverError("T253 package receipt lacks pinned output digests")
    return {
        "root": root.resolve(), "package_dir": package_dir.resolve(), "cases_path": cases_path.resolve(),
        "events_path": events_path.resolve(), "package_manifest": manifest,
        "package_manifest_sha256": package_manifest_sha, "parent_t208_manifest_sha256": package_hash,
        "request_bodies_sha256": manifest.get("parent_request_bodies_sha256"),
        "dispositions_sha256": manifest.get("parent_dispositions_sha256"),
        "batch_requests_sha256": body, "case_inventory_sha256": case_hash,
        "local_mapping_sha256": package_result.get("local_mapping_sha256"),
        "journal_sha256": journal_hash, "cases": cases, "events": events,
        "state_counts": state.get("state_counts", {}), "ready_ids": ready_ids,
        "preparation_error_count": len(error_ids), "denominator": len(cases),
        "request_count": len(ready_ids), "provider_called": False,
        "dispatch_authorized": False, "cohort_selected": False,
    }


def _iso_utc(value: Any) -> datetime:
    if not isinstance(value, str):
        raise DriverError("release timestamps must be ISO-8601 strings")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        raise DriverError("release timestamp is malformed") from None
    if parsed.tzinfo is None:
        raise DriverError("release timestamp must include timezone")
    return parsed.astimezone(timezone.utc)


def validate_release(raw: bytes, facts: dict[str, Any], *, now: datetime | None = None,
                     trusted_release_sha256: str | None = None) -> tuple[dict[str, Any], str]:
    """Validate the fixed release schema; production caller supplies only the source pin."""
    if trusted_release_sha256 is None or not SHA256_RE.fullmatch(trusted_release_sha256):
        raise DriverError("no independently lead-pinned release exists")
    digest = sha256_bytes(raw)
    if digest != trusted_release_sha256:
        raise DriverError("lead release digest is not pinned")
    release = _json_load(raw)
    fields = {
        "schema", "release_id", "issued_at", "expires_at", "protocol_sha256",
        "t253_manifest_sha256", "t208_manifest_sha256", "request_bodies_sha256",
        "dispositions_sha256", "case_inventory_sha256", "ready_case_set_sha256",
        "denominator", "request_count", "preparation_error_count", "payload_fields",
        "model", "endpoint", "completion_window", "max_attempts_per_case",
        "reserved_attempts", "reserved_cost_usd", "cost_scenario", "rights_receipt",
        "capacity_receipt", "cost_receipt",
    }
    if not isinstance(release, dict) or set(release) != fields:
        raise DriverError("lead release has an unexpected schema")
    try:
        current = (now or datetime.now(timezone.utc)).astimezone(timezone.utc)
        issued = _iso_utc(release["issued_at"])
        expires = _iso_utc(release["expires_at"])
        if issued > current or expires <= current or expires <= issued:
            raise DriverError("lead release is stale, future-dated or expired")
        if (release.get("schema") != "SENATH_C2_BATCH_RELEASE_V2"
                or not isinstance(release.get("release_id"), str) or not release["release_id"].strip()
                or release.get("protocol_sha256") != PROTOCOL_SHA256
                or release.get("t253_manifest_sha256") != facts["package_manifest_sha256"]
                or release.get("t208_manifest_sha256") != facts["parent_t208_manifest_sha256"]
                or release.get("request_bodies_sha256") != facts["request_bodies_sha256"]
                or release.get("dispositions_sha256") != facts["dispositions_sha256"]
                or release.get("case_inventory_sha256") != facts["case_inventory_sha256"]
                or release.get("ready_case_set_sha256") != sha256_bytes(canonical_json(facts["ready_ids"]))
                or release.get("denominator") != facts["denominator"]
                or release.get("request_count") != facts["request_count"]
                or release.get("preparation_error_count") != facts["preparation_error_count"]
                or release.get("payload_fields") != EXPECTED_PAYLOAD_FIELDS
                or release.get("model") != EXPECTED_MODEL
                or release.get("endpoint") != EXPECTED_ENDPOINT
                or release.get("completion_window") != COMPLETION_WINDOW
                or release.get("max_attempts_per_case") != 1
                or release.get("reserved_attempts") != facts["request_count"]):
            raise DriverError("release does not bind the frozen protocol/package/case contract")
        if facts["request_count"] < 1 or facts["request_count"] > MAX_AUTHORIZED_CALLS:
            raise DriverError("request count is outside Protocol v1 authority")
        receipts = []
        receipt_fields = {"sha256", "observed_at", "valid_through", "attributed_to", "scope"}
        for name, required_scope, extra_fields in (
                ("rights_receipt", "provider_batch_upload", set()),
                ("capacity_receipt", "openai_batch_capacity", {"remaining_authorized_calls"}),
                ("cost_receipt", "openai_batch_cost", {"remaining_authorized_budget_usd"})):
            receipt = release.get(name)
            if (not isinstance(receipt, dict)
                    or set(receipt) != receipt_fields | extra_fields
                    or not SHA256_RE.fullmatch(receipt.get("sha256", ""))
                    or not isinstance(receipt.get("attributed_to"), str) or not receipt["attributed_to"].strip()
                    or receipt.get("scope") != required_scope):
                raise DriverError("release receipt metadata is missing or malformed")
            observed = _iso_utc(receipt["observed_at"])
            valid_through = _iso_utc(receipt["valid_through"])
            if observed > current or valid_through < current or valid_through < expires:
                raise DriverError("a required rights/capacity/cost receipt is stale")
            receipts.append(receipt)
        cost = release.get("cost_receipt")
        remaining = cost.get("remaining_authorized_budget_usd") if isinstance(cost, dict) else None
        reserved = release.get("reserved_cost_usd")
        scenario = release.get("cost_scenario")
        scenario_fields = {
            "schema", "scenario_sha256", "evidence_bundle_sha256", "preflight_report_sha256",
            "token_receipt_report_sha256", "scenario_id", "request_count", "maximum_total_cost_usd",
        }
        if (not isinstance(scenario, dict) or set(scenario) != scenario_fields
                or scenario.get("schema") != "SENATH_C2_PINNED_COST_SCENARIO_V1"
                or scenario.get("scenario_id") != "FULL_TEST_CANDIDATE"
                or scenario.get("request_count") != facts["request_count"]
                or any(not isinstance(scenario.get(key), str) or not SHA256_RE.fullmatch(scenario[key])
                       for key in ("scenario_sha256", "evidence_bundle_sha256", "preflight_report_sha256", "token_receipt_report_sha256"))):
            raise DriverError("release cost scenario does not bind the full frozen request set")
        if (isinstance(remaining, bool) or not isinstance(remaining, (int, float))
                or isinstance(reserved, bool) or not isinstance(reserved, (int, float))):
            raise DriverError("worst-case reservation exceeds the 80%-of-remaining or Protocol v1 budget gate")
        try:
            remaining_decimal = Decimal(str(remaining))
            reserved_cost_decimal = Decimal(str(reserved))
        except (InvalidOperation, TypeError, ValueError):
            raise DriverError("worst-case reservation is malformed") from None
        if (not remaining_decimal.is_finite() or not reserved_cost_decimal.is_finite()
                or remaining_decimal < 0 or remaining_decimal > Decimal(str(MAX_AUTHORIZED_USD))
                or reserved_cost_decimal <= 0 or reserved_cost_decimal > Decimal("0.8") * remaining_decimal):
            raise DriverError("worst-case reservation exceeds the 80%-of-remaining or Protocol v1 budget gate")
        try:
            maximum_scenario_cost = Decimal(str(scenario["maximum_total_cost_usd"]))
        except (InvalidOperation, TypeError, ValueError):
            raise DriverError("release cost scenario maximum is malformed") from None
        if (not maximum_scenario_cost.is_finite() or not reserved_cost_decimal.is_finite()
                or maximum_scenario_cost <= 0 or reserved_cost_decimal < maximum_scenario_cost):
            raise DriverError("reservation is below the pinned scenario maximum")
        capacity = release["capacity_receipt"]
        remaining_calls = capacity.get("remaining_authorized_calls")
        if (isinstance(remaining_calls, bool) or not isinstance(remaining_calls, int)
                or remaining_calls < facts["request_count"] or remaining_calls > MAX_AUTHORIZED_CALLS):
            raise DriverError("capacity receipt does not reserve one attempt for every requested case")
    except (KeyError, TypeError):
        raise DriverError("lead release contains missing or invalid gate fields") from None
    return release, digest


def _get(value: Any, name: str, default: Any = None) -> Any:
    if isinstance(value, dict):
        return value.get(name, default)
    return getattr(value, name, default)


@contextlib.contextmanager
def _state_lock(path: Path) -> Iterator[None]:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a+b") as stream:
        if os.name == "nt":
            import msvcrt
            stream.seek(0, os.SEEK_END)
            if stream.tell() == 0:
                stream.write(b"\0")
                stream.flush()
            stream.seek(0)
            msvcrt.locking(stream.fileno(), msvcrt.LK_LOCK, 1)
            try:
                yield
            finally:
                stream.seek(0)
                msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)
        else:
            import fcntl
            fcntl.flock(stream.fileno(), fcntl.LOCK_EX)
            try:
                yield
            finally:
                fcntl.flock(stream.fileno(), fcntl.LOCK_UN)


def _read_state(path: Path, facts: dict[str, Any], release_hash: str) -> tuple[dict[str, Any], bytes]:
    if not path.exists():
        operation_nonce = uuid.uuid4().hex
        reconciliation_id = _reconciliation_id(facts, release_hash, operation_nonce)
        state = {
            "schema": "SENATH_C2_BATCH_DRIVER_STATE_V1", "revision": 0,
            "package_manifest_sha256": facts["package_manifest_sha256"],
            "case_inventory_sha256": facts["case_inventory_sha256"],
            "release_sha256": release_hash, "operation_nonce": operation_nonce,
            "reconciliation_id": reconciliation_id,
            "upload_phase": "not_started",
            "submit_phase": "not_started", "recovery_phase": "not_started",
            "recovery_binding": None,
            "intent_journal_sha256": None,
            "file_id": None, "batch_id": None, "batch_status": None,
            "output_file_id": None, "error_file_id": None, "request_counts": None,
            "history": [], "dispatch_authorized": False, "provider_called": False,
        }
        return state, b""
    try:
        raw = path.read_bytes()
    except OSError:
        raise DriverError("driver state unavailable") from None
    state = _json_load(raw)
    base_fields = {
        "schema", "revision", "package_manifest_sha256", "case_inventory_sha256",
        "release_sha256", "reconciliation_id", "upload_phase", "submit_phase", "recovery_phase",
        "intent_journal_sha256", "file_id", "batch_id", "batch_status",
        "output_file_id", "error_file_id", "request_counts", "history",
        "dispatch_authorized", "provider_called",
    }
    fields = set(state) if isinstance(state, dict) else set()
    has_operation_nonce = "operation_nonce" in fields
    has_recovery_binding = "recovery_binding" in fields
    expected_fields = (base_fields | ({"operation_nonce"} if has_operation_nonce else set())
                       | ({"recovery_binding"} if has_recovery_binding else set()))
    operation_nonce = state.get("operation_nonce") if isinstance(state, dict) else None
    if has_operation_nonce and (not isinstance(operation_nonce, str)
                                or re.fullmatch(r"[0-9a-f]{32}", operation_nonce) is None):
        raise DriverError("driver state operation identity is malformed")
    reconciliation_id = _reconciliation_id(facts, release_hash, operation_nonce)
    if (not isinstance(state, dict) or canonical_json(state) + b"\n" != raw
            or fields != expected_fields
            or state.get("schema") != "SENATH_C2_BATCH_DRIVER_STATE_V1"
            or state.get("package_manifest_sha256") != facts["package_manifest_sha256"]
            or state.get("case_inventory_sha256") != facts["case_inventory_sha256"]
            or state.get("release_sha256") != release_hash
            or state.get("reconciliation_id") != reconciliation_id
            or isinstance(state.get("revision"), bool) or not isinstance(state.get("revision"), int)
            or state["revision"] < 0
            or state.get("upload_phase") not in {"not_started", "in_flight", "unknown", "complete"}
            or state.get("submit_phase") not in {"not_started", "in_flight", "unknown", "complete"}
            or state.get("recovery_phase") not in {"not_started", "in_flight", "unknown", "complete"}
            or not isinstance(state.get("history"), list)
            or state.get("dispatch_authorized") is not False
            or state.get("provider_called") is not False):
        raise DriverError("driver state is malformed or bound to different inputs/release")
    if has_recovery_binding:
        _validate_recovery_binding(state, facts)
    elif state.get("recovery_phase") != "not_started":
        raise DriverError("legacy uncertain recovery has no bound operation; preserve for manual review")
    return state, raw


def _validate_recovery_binding(state: dict[str, Any], facts: dict[str, Any]) -> None:
    """Validate the durable recovery identity before touching pending evidence."""
    binding = state.get("recovery_binding")
    if binding is None:
        if state.get("recovery_phase") != "not_started":
            raise DriverError("recovery phase has no durable operation binding")
        return
    expected = {
        "schema", "operation_id", "destination", "output_file_id", "error_file_id",
        "expected_counts", "intent_journal_sha256", "attempts",
    }
    if not isinstance(binding, dict) or set(binding) != expected:
        raise DriverError("recovery operation binding is malformed")
    destination = binding.get("destination")
    if (binding.get("schema") != "C2_OUTPUT_RECOVERY_BINDING_V1"
            or not isinstance(binding.get("operation_id"), str)
            or re.fullmatch(r"[0-9a-f]{32}", binding["operation_id"]) is None
            or not isinstance(destination, str) or not destination
            or not Path(destination).is_absolute()
            or str(Path(destination).resolve()) != destination
            or binding.get("output_file_id") != state.get("output_file_id")
            or binding.get("error_file_id") != state.get("error_file_id")
            or binding.get("expected_counts") != state.get("request_counts")
            or binding.get("intent_journal_sha256") != state.get("intent_journal_sha256")
            or not isinstance(binding.get("attempts"), list)
            or not binding["attempts"]):
        raise DriverError("recovery operation binding differs from frozen terminal state")
    if (not isinstance(binding["expected_counts"], dict)
            or set(binding["expected_counts"]) != {"total", "completed", "failed"}
            or any(isinstance(value, bool) or not isinstance(value, int) or value < 0
                   for value in binding["expected_counts"].values())
            or binding["expected_counts"].get("total") != facts["request_count"]
            or binding["expected_counts"].get("completed", -1) + binding["expected_counts"].get("failed", -1)
            != facts["request_count"]):
        raise DriverError("recovery operation count binding is invalid")
    if (not isinstance(binding["intent_journal_sha256"], str)
            or SHA256_RE.fullmatch(binding["intent_journal_sha256"]) is None):
        raise DriverError("recovery operation journal binding is invalid")
    seen_attempts: set[str] = set()
    destination_path = Path(destination)
    for attempt in binding["attempts"]:
        fields = {"attempt_id", "pending_dir", "status", "partial_sha256"}
        if not isinstance(attempt, dict) or set(attempt) != fields:
            raise DriverError("recovery attempt record is malformed")
        attempt_id = attempt.get("attempt_id")
        pending_dir = attempt.get("pending_dir")
        if (not isinstance(attempt_id, str) or re.fullmatch(r"[0-9a-f]{32}", attempt_id) is None
                or attempt_id in seen_attempts
                or not isinstance(pending_dir, str)
                or str(Path(pending_dir).resolve()) != pending_dir
                or Path(pending_dir).parent != destination_path.parent
                or Path(pending_dir).name != f".{destination_path.name}.pending-{attempt_id}"
                or attempt.get("status") not in {"in_flight", "unknown", "complete"}):
            raise DriverError("recovery attempt identity/path/status is invalid")
        seen_attempts.add(attempt_id)
        partials = attempt.get("partial_sha256")
        if not isinstance(partials, dict) or set(partials) != {"output", "error"}:
            raise DriverError("recovery partial digest record is malformed")
        if any(value is not None and (not isinstance(value, str) or SHA256_RE.fullmatch(value) is None)
               for value in partials.values()):
            raise DriverError("recovery partial digest is malformed")
    if state.get("recovery_phase") == "not_started":
        raise DriverError("unstarted recovery unexpectedly has a bound operation")


def _write_state(path: Path, new_state: dict[str, Any], expected_raw: bytes) -> bytes:
    current = path.read_bytes() if path.exists() else b""
    if current != expected_raw:
        raise DriverError("driver state changed; refresh and reconcile before continuing")
    raw = canonical_json(new_state) + b"\n"
    temp = path.with_name(f".{path.name}.{uuid.uuid4().hex}.tmp")
    try:
        with temp.open("xb") as stream:
            stream.write(raw)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp, path)
    except OSError:
        try:
            temp.unlink(missing_ok=True)
        except OSError:
            pass
        raise DriverError("atomic driver-state persistence failed; phase may be uncertain") from None
    return raw


def _ensure_operation_identity(state: dict[str, Any], facts: dict[str, Any], release_hash: str,
                              state_path: Path, raw: bytes) -> tuple[dict[str, Any], bytes]:
    """Upgrade only a legacy state that has no unresolved remote mutation."""
    needs_nonce = "operation_nonce" not in state
    needs_recovery_binding = "recovery_binding" not in state
    if not needs_nonce and not needs_recovery_binding:
        return state, raw
    if (state["upload_phase"] not in {"not_started", "complete"}
            or state["submit_phase"] != "not_started" or state.get("batch_id")):
        raise DriverError("legacy state has no unique operation identity; preserve and reconcile manually")
    if needs_nonce:
        operation_nonce = uuid.uuid4().hex
        state["operation_nonce"] = operation_nonce
        state["reconciliation_id"] = _reconciliation_id(facts, release_hash, operation_nonce)
    if needs_recovery_binding:
        state["recovery_binding"] = None
    _history(state, "identity_upgrade", "complete")
    return state, _write_state(state_path, state, raw)


def _history(state: dict[str, Any], phase: str, result: str, at: datetime | None = None) -> None:
    state["history"].append({"sequence": len(state["history"]) + 1, "phase": phase,
                             "result": result,
                             "at": (at or datetime.now(timezone.utc)).isoformat(timespec="seconds")})
    state["revision"] += 1


def _ready_case_bytes(facts: dict[str, Any], contracts: dict[str, Any]) -> tuple[bytes, bytes]:
    v1 = contracts["t219"]
    ids = facts["ready_ids"]
    return canonical_json(ids), v1.jsonl_bytes(facts["cases"])


def _ensure_intents(facts: dict[str, Any], contracts: dict[str, Any], events_path: Path) -> str:
    """Resume only the local intent-write boundary; never appends a second intent."""
    v1, v2 = contracts["t219"], contracts["t249"]
    current_state = v2.reconcile_attempts_v2(
        facts["cases"], facts["events"], facts["parent_t208_manifest_sha256"],
        expected_case_inventory_sha256=facts["case_inventory_sha256"],
        event_ledger_sha256=facts["journal_sha256"],
        v1_path=facts["root"] / "ai/tools/c2_attempt_resume.py",
    )
    states = {row["case_id"]: row["state"] for row in current_state["cases"]}
    if any(states.get(case_id) not in {"NOT_STARTED", "UNCERTAIN_NO_RETRY"} for case_id in facts["ready_ids"]):
        raise DriverError("prior result or terminal state exists; batch creation is prohibited")
    raw = events_path.read_bytes() if events_path.exists() else b""
    digest = sha256_bytes(raw)
    for index, case_id in enumerate(facts["ready_ids"], start=1):
        if states[case_id] == "NOT_STARTED":
            event_id = f"intent:{facts['package_manifest_sha256']}:{index:05d}"
            result = v1.append_attempt_event(
                facts["cases_path"], events_path, facts["parent_t208_manifest_sha256"],
                facts["case_inventory_sha256"], expected_journal_sha256=digest,
                case_id=case_id, event_id=event_id, event_type="DISPATCH_INTENT",
            )
            digest = result["event_ledger_sha256"]
    return digest


def _create_sdk_client():
    """Import SDK only after release validation. Secret comes only from SDK env config."""
    try:
        from openai import OpenAI  # type: ignore[import-not-found]
    except ImportError:
        raise DriverError("official OpenAI SDK is unavailable") from None
    try:
        return OpenAI(max_retries=0, timeout=60.0)
    except Exception:
        raise DriverError("official OpenAI client construction failed") from None


def _validate_upload_response(response: Any, expected_bytes: int, expected_filename: str) -> str:
    file_id = _get(response, "id")
    purpose = _get(response, "purpose")
    object_type = _get(response, "object")
    size = _get(response, "bytes")
    if (not isinstance(file_id, str) or not file_id or purpose != "batch"
            or _get(response, "filename") != expected_filename
            or object_type not in {None, "file"}
            or (size is not None and (isinstance(size, bool) or size != expected_bytes))):
        raise DriverError("file-upload response does not match the approved artifact")
    return file_id


def _upload_filename(reconciliation_id: str) -> str:
    if SHA256_RE.fullmatch(reconciliation_id) is None:
        raise DriverError("upload operation identity is malformed")
    return f"senath-c2-{reconciliation_id}.jsonl"


def _validate_batch_response(response: Any, expected_file_id: str,
                             expected_reconciliation_id: str | None = None) -> dict[str, Any]:
    batch_id = _get(response, "id")
    if (not isinstance(batch_id, str) or not batch_id
            or _get(response, "endpoint") != EXPECTED_ENDPOINT
            or _get(response, "input_file_id") != expected_file_id
            or _get(response, "completion_window") != COMPLETION_WINDOW):
        raise DriverError("batch-create response differs from frozen endpoint/input/window")
    status = _get(response, "status")
    counts = _normalize_counts(_get(response, "request_counts"))
    if status not in ALLOWED_BATCH_STATES or counts is None:
        raise DriverError("batch-create response omitted allowlisted status/count metadata")
    if expected_reconciliation_id is not None:
        metadata = _get(response, "metadata")
        if (not isinstance(metadata, dict)
                or metadata.get("senath_reconciliation_id") != expected_reconciliation_id):
            raise DriverError("batch-create response omitted the frozen reconciliation binding")
    return {"batch_id": batch_id, "batch_status": status,
            "output_file_id": _get(response, "output_file_id"),
            "error_file_id": _get(response, "error_file_id"),
            "request_counts": counts}


def _normalize_counts(value: Any) -> dict[str, int] | None:
    if value is None:
        return None
    result = {name: _get(value, name) for name in ("total", "completed", "failed")}
    if (any(isinstance(item, bool) or not isinstance(item, int) or item < 0 for item in result.values())
            or result["completed"] + result["failed"] > result["total"]):
        raise DriverError("provider request counts are invalid")
    return result


def _normalize_batch_usage(value: Any) -> dict[str, Any]:
    """Keep Batch-level usage distinct from billed cost and per-case usage."""
    if value is None:
        return {
            "availability": "OMITTED",
            "completeness": "UNKNOWN",
            "input_tokens": None,
            "cached_input_tokens": None,
            "output_tokens": None,
            "reasoning_output_tokens": None,
            "total_tokens": None,
        }
    if not _has_provider_field(value, "input_tokens") and not _has_provider_field(value, "output_tokens") \
            and not _has_provider_field(value, "total_tokens"):
        raise DriverError("Batch usage object has no recognized token-count fields")
    input_tokens = _usage_count(value, "input_tokens")
    output_tokens = _usage_count(value, "output_tokens")
    total_tokens = _usage_count(value, "total_tokens")
    input_details = _get(value, "input_tokens_details")
    output_details = _get(value, "output_tokens_details")
    if (input_details is not None and not _has_provider_field(input_details, "cached_tokens")
            or output_details is not None and not _has_provider_field(output_details, "reasoning_tokens")):
        raise DriverError("Batch usage breakdown has an invalid shape")
    cached_tokens = _usage_count(input_details, "cached_tokens")
    reasoning_tokens = _usage_count(output_details, "reasoning_tokens")
    if input_tokens is not None and cached_tokens is not None and cached_tokens > input_tokens:
        raise DriverError("Batch cached-token usage exceeds input-token usage")
    if output_tokens is not None and reasoning_tokens is not None and reasoning_tokens > output_tokens:
        raise DriverError("Batch reasoning-token usage exceeds output-token usage")
    if (input_tokens is not None and output_tokens is not None and total_tokens is not None
            and input_tokens + output_tokens != total_tokens):
        raise DriverError("Batch total-token usage is inconsistent")
    required = (input_tokens, output_tokens, total_tokens)
    return {
        "availability": "PRESENT",
        "completeness": "COMPLETE" if all(item is not None for item in required) else "PARTIAL",
        "input_tokens": input_tokens,
        "cached_input_tokens": cached_tokens,
        "output_tokens": output_tokens,
        "reasoning_output_tokens": reasoning_tokens,
        "total_tokens": total_tokens,
    }


def _has_provider_field(value: Any, name: str) -> bool:
    if isinstance(value, dict):
        return name in value
    if value is None or isinstance(value, (str, bytes, int, float, bool, list, tuple)):
        return False
    return hasattr(value, name)


def _usage_count(value: Any, name: str) -> int | None:
    raw = _get(value, name)
    if raw is None:
        return None
    if isinstance(raw, bool) or not isinstance(raw, int) or raw < 0:
        raise DriverError("Batch usage contains an invalid token count")
    return raw


def _persist_batch_usage_observation(*, state_path: Path, state: dict[str, Any],
                                     facts: dict[str, Any], response: Any, status: str,
                                     counts: dict[str, int],
                                     observed_at: datetime | None = None) -> dict[str, Any]:
    """Append one minimized, operation-bound observation without replacing prior evidence."""
    usage = _normalize_batch_usage(_get(response, "usage"))
    reconciliation_id = state["reconciliation_id"]
    captured_at = (observed_at or datetime.now(timezone.utc)).astimezone(timezone.utc).isoformat(
        timespec="microseconds").replace("+00:00", "Z")
    snapshot = {
        "batch_id": state["batch_id"],
        "input_file_id": state["file_id"],
        "endpoint": EXPECTED_ENDPOINT,
        "completion_window": COMPLETION_WINDOW,
        "reconciliation_id": reconciliation_id,
        "status": status,
        "request_counts": counts,
        "model": _get(response, "model"),
        "usage": usage,
    }
    snapshot_sha256 = sha256_bytes(canonical_json(snapshot))
    receipt = {
        "schema": "SENATH_C2_BATCH_USAGE_COST_OBSERVATION_V1",
        "captured_at": captured_at,
        "binding": {
            "batch_id": state["batch_id"],
            "reconciliation_id": reconciliation_id,
            "package_manifest_sha256": facts["package_manifest_sha256"],
            "case_inventory_sha256": facts["case_inventory_sha256"],
            "request_jsonl_sha256": facts["batch_requests_sha256"],
            "input_file_id": state["file_id"],
            "endpoint": EXPECTED_ENDPOINT,
            "model": _get(response, "model"),
            "completion_window": COMPLETION_WINDOW,
            "status": status,
            "request_counts": counts,
        },
        "provider_snapshot_sha256": snapshot_sha256,
        "usage": usage,
        "cost_evidence": {
            "status": "NOT_OBSERVED",
            "amount": None,
            "currency": None,
            "operation_attribution": "UNAVAILABLE_FROM_BATCH_USAGE",
        },
        "rate_derived_estimate": {
            "status": "NOT_COMPUTED",
            "amount": None,
            "currency": None,
            "classification": "NOT_BILLED_COST",
        },
        "closeout": {
            "usage_bound_to_batch": any(usage[key] is not None for key in (
                "input_tokens", "cached_input_tokens", "output_tokens",
                "reasoning_output_tokens", "total_tokens",
            )),
            "aggregate_cost_observed": False,
            "operation_cost_attributable": False,
            "invoice_reconciled": False,
        },
    }
    receipt_bytes = canonical_json(receipt) + b"\n"
    receipt_sha256 = sha256_bytes(receipt_bytes)
    observation_id = sha256_bytes(canonical_json({
        "schema": receipt["schema"],
        "captured_at": captured_at,
        "batch_id": state["batch_id"],
        "reconciliation_id": reconciliation_id,
        "provider_snapshot_sha256": snapshot_sha256,
    }))
    record_dir = state_path.parent / (state_path.name + ".accounting") / reconciliation_id
    record_dir.mkdir(parents=True, exist_ok=True)
    destination = record_dir / f"usage-{observation_id}.json"
    temporary = record_dir / f".{observation_id}-{uuid.uuid4().hex}.pending"
    try:
        fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(receipt_bytes)
                stream.flush()
                os.fsync(stream.fileno())
        except Exception:
            try:
                os.close(fd)
            except OSError:
                pass
            raise
        try:
            # Hard-link promotion is atomic and fails rather than replacing an
            # observation already written under the same content-derived name.
            os.link(temporary, destination)
        except FileExistsError:
            if destination.read_bytes() != receipt_bytes:
                raise DriverError("existing Batch accounting observation conflicts") from None
        finally:
            temporary.unlink(missing_ok=True)
    except DriverError:
        raise
    except OSError:
        temporary.unlink(missing_ok=True)
        raise DriverError("Batch accounting observation could not be persisted") from None
    return {
        "path": destination.relative_to(state_path.parent).as_posix(),
        "sha256": receipt_sha256,
        "usage_availability": usage["availability"],
        "usage_completeness": usage["completeness"],
    }


def _validate_saved_usage_observation(raw: bytes, *, facts: dict[str, Any],
                                     state: dict[str, Any], filename: str) -> dict[str, Any]:
    """Verify a standalone observation before it is linked into mutable state."""
    receipt = _json_load(raw)
    fields = {"schema", "captured_at", "binding", "provider_snapshot_sha256", "usage",
              "cost_evidence", "rate_derived_estimate", "closeout"}
    if (not isinstance(receipt, dict) or set(receipt) != fields
            or canonical_json(receipt) + b"\n" != raw
            or receipt.get("schema") != "SENATH_C2_BATCH_USAGE_COST_OBSERVATION_V1"):
        raise DriverError("saved Batch accounting observation is malformed")
    captured_at = receipt.get("captured_at")
    _iso_utc(captured_at)
    binding = receipt.get("binding")
    binding_fields = {"batch_id", "reconciliation_id", "package_manifest_sha256", "case_inventory_sha256",
                      "request_jsonl_sha256", "input_file_id", "endpoint", "model", "completion_window",
                      "status", "request_counts"}
    if not isinstance(binding, dict) or set(binding) != binding_fields:
        raise DriverError("saved Batch accounting binding is malformed")
    counts = binding.get("request_counts")
    if (not isinstance(counts, dict) or set(counts) != {"total", "completed", "failed"}
            or any(isinstance(value, bool) or not isinstance(value, int) or value < 0
                   for value in counts.values())
            or counts["completed"] + counts["failed"] > counts["total"]
            or counts["total"] != facts["request_count"]):
        raise DriverError("saved Batch accounting counts are invalid")
    status = binding.get("status")
    if status not in ALLOWED_BATCH_STATES or (status == "completed"
            and counts["completed"] + counts["failed"] != counts["total"]):
        raise DriverError("saved Batch accounting status is invalid")
    expected_binding = {
        "batch_id": state.get("batch_id"),
        "reconciliation_id": state.get("reconciliation_id"),
        "package_manifest_sha256": facts["package_manifest_sha256"],
        "case_inventory_sha256": facts["case_inventory_sha256"],
        "request_jsonl_sha256": facts["batch_requests_sha256"],
        "input_file_id": state.get("file_id"),
        "endpoint": EXPECTED_ENDPOINT,
        "model": EXPECTED_MODEL,
        "completion_window": COMPLETION_WINDOW,
    }
    if any(binding.get(key) != value for key, value in expected_binding.items()):
        raise DriverError("saved Batch accounting observation belongs to different inputs or operation")
    usage = receipt.get("usage")
    usage_fields = {"availability", "completeness", "input_tokens", "cached_input_tokens",
                    "output_tokens", "reasoning_output_tokens", "total_tokens"}
    if not isinstance(usage, dict) or set(usage) != usage_fields:
        raise DriverError("saved Batch usage shape is invalid")
    numeric = ("input_tokens", "cached_input_tokens", "output_tokens", "reasoning_output_tokens", "total_tokens")
    for name in numeric:
        value = usage.get(name)
        if value is not None and (isinstance(value, bool) or not isinstance(value, int) or value < 0):
            raise DriverError("saved Batch usage count is invalid")
    present = any(usage[name] is not None for name in numeric)
    availability = usage.get("availability")
    completeness = ("UNKNOWN" if availability == "OMITTED"
                    else "COMPLETE" if all(usage[name] is not None for name in
                                           ("input_tokens", "output_tokens", "total_tokens"))
                    else "PARTIAL")
    if (usage.get("availability") not in {"OMITTED", "PRESENT"}
            or (availability == "OMITTED" and present)
            or usage.get("completeness") != completeness
            or (usage["input_tokens"] is not None and usage["cached_input_tokens"] is not None
                and usage["cached_input_tokens"] > usage["input_tokens"])
            or (usage["output_tokens"] is not None and usage["reasoning_output_tokens"] is not None
                and usage["reasoning_output_tokens"] > usage["output_tokens"])
            or (usage["input_tokens"] is not None and usage["output_tokens"] is not None
                and usage["total_tokens"] is not None
                and usage["input_tokens"] + usage["output_tokens"] != usage["total_tokens"])):
        raise DriverError("saved Batch usage completeness or totals are inconsistent")
    snapshot = {
        "batch_id": binding["batch_id"], "input_file_id": binding["input_file_id"],
        "endpoint": binding["endpoint"], "completion_window": binding["completion_window"],
        "reconciliation_id": binding["reconciliation_id"], "status": status,
        "request_counts": counts, "model": binding["model"], "usage": usage,
    }
    snapshot_digest = receipt.get("provider_snapshot_sha256")
    if not isinstance(snapshot_digest, str) or snapshot_digest != sha256_bytes(canonical_json(snapshot)):
        raise DriverError("saved Batch provider snapshot digest is invalid")
    if (receipt.get("cost_evidence") != {
            "status": "NOT_OBSERVED", "amount": None, "currency": None,
            "operation_attribution": "UNAVAILABLE_FROM_BATCH_USAGE"}
            or receipt.get("rate_derived_estimate") != {
                "status": "NOT_COMPUTED", "amount": None, "currency": None,
                "classification": "NOT_BILLED_COST"}
            or receipt.get("closeout") != {
                "usage_bound_to_batch": present, "aggregate_cost_observed": False,
                "operation_cost_attributable": False, "invoice_reconciled": False}):
        raise DriverError("saved Batch cost semantics are invalid")
    observation_id = sha256_bytes(canonical_json({
        "schema": receipt["schema"], "captured_at": captured_at,
        "batch_id": binding["batch_id"], "reconciliation_id": binding["reconciliation_id"],
        "provider_snapshot_sha256": snapshot_digest,
    }))
    if filename != f"usage-{observation_id}.json":
        raise DriverError("saved Batch observation filename does not match its content identity")
    return {"path": filename, "sha256": sha256_bytes(raw), "observation_id": observation_id,
            "captured_at": captured_at}


def reconcile_usage_observations(*, state_path: Path, facts: dict[str, Any]) -> dict[str, Any]:
    """Link complete, verified local receipts left orphaned by a state-write failure.

    This is an offline repair only: it never constructs a client, updates status,
    deletes receipts, or retries a provider operation.
    """
    state_path = state_path.resolve()
    with _with_phase_lock(state_path):
        try:
            raw = state_path.read_bytes()
            untrusted = _json_load(raw)
        except (OSError, DriverError):
            raise DriverError("existing driver state is unavailable or malformed") from None
        release_hash = untrusted.get("release_sha256") if isinstance(untrusted, dict) else None
        if not isinstance(release_hash, str) or SHA256_RE.fullmatch(release_hash) is None:
            raise DriverError("existing state has no valid release binding")
        state, raw = _read_state(state_path, facts, release_hash)
        if (state.get("submit_phase") != "complete" or not isinstance(state.get("batch_id"), str)
                or not state.get("batch_id") or not isinstance(state.get("file_id"), str)
                or not state.get("file_id")):
            raise DriverError("accounting reconciliation requires a bound completed Batch submission")
        accounting_root = state_path.parent / (state_path.name + ".accounting")
        observation_dir = accounting_root / state["reconciliation_id"]
        if accounting_root.is_symlink() or observation_dir.is_symlink():
            raise DriverError("accounting evidence path must not be a symbolic link")
        if not observation_dir.exists():
            return {"phase": "reconcile-accounting", "linked": 0, "already_linked": 0,
                    "incomplete_pending": 0, "pending_entries": [], "unrecognized_entries": [],
                    "provider_called": False, "dispatch_authorized": False}
        if not observation_dir.is_dir():
            raise DriverError("accounting evidence path is not a directory")
        entries = sorted(observation_dir.iterdir(), key=lambda item: item.name)
        pending_entries = [path.name for path in entries if path.name.endswith(".pending")]
        unrecognized_entries = [
            path.name for path in entries
            if not path.name.endswith(".pending")
            and re.fullmatch(r"usage-[0-9a-f]{64}\.json", path.name) is None
        ]
        linked_by_path: dict[str, str] = {}
        for event in state["history"]:
            if not isinstance(event, dict):
                raise DriverError("driver history is malformed")
            path, digest = event.get("usage_observation_path"), event.get("usage_observation_sha256")
            if path is None and digest is None:
                continue
            if (not isinstance(path, str) or not isinstance(digest, str)
                    or SHA256_RE.fullmatch(digest) is None or path in linked_by_path):
                raise DriverError("driver history contains an invalid accounting link")
            linked_by_path[path] = digest
        orphan_records = []
        already_linked = 0
        for path in entries:
            if not re.fullmatch(r"usage-[0-9a-f]{64}\.json", path.name):
                continue
            if path.is_symlink() or not path.is_file():
                raise DriverError("accounting observation must be a regular local file")
            try:
                observation_raw = path.read_bytes()
            except OSError:
                raise DriverError("accounting observation is unavailable") from None
            record = _validate_saved_usage_observation(
                observation_raw, facts=facts, state=state, filename=path.name)
            relative_path = (accounting_root.name + "/" + state["reconciliation_id"] + "/" + path.name)
            if relative_path in linked_by_path:
                if linked_by_path[relative_path] != record["sha256"]:
                    raise DriverError("linked accounting receipt digest changed")
                already_linked += 1
            else:
                orphan_records.append({**record, "path": relative_path})
        if orphan_records:
            for record in orphan_records:
                _history(state, "reconcile-accounting", "linked")
                state["history"][-1].update({
                    "usage_observation_path": record["path"],
                    "usage_observation_sha256": record["sha256"],
                    "usage_observation_id": record["observation_id"],
                    "usage_observation_captured_at": record["captured_at"],
                })
            _write_state(state_path, state, raw)
        return {"phase": "reconcile-accounting", "linked": len(orphan_records),
                "already_linked": already_linked, "incomplete_pending": len(pending_entries),
                "pending_entries": pending_entries, "unrecognized_entries": unrecognized_entries,
                "provider_called": False, "dispatch_authorized": False}
def _with_phase_lock(state_path: Path):
    return _state_lock(state_path.with_suffix(state_path.suffix + ".lock"))


def _phase_started_at(state: dict[str, Any], phase: str) -> datetime:
    for event in reversed(state["history"]):
        if event.get("phase") == phase and event.get("result") == "in_flight":
            try:
                value = datetime.fromisoformat(event["at"])
            except (KeyError, TypeError, ValueError):
                break
            if value.tzinfo is not None:
                return value.astimezone(timezone.utc)
            break
    raise DriverError("uncertain phase lacks a valid durable start time")


def _list_remote_objects(resource: Any, *, limit: int, **filters: Any) -> list[Any]:
    """Read a bounded, fully paginated listing; malformed/truncated pages fail closed."""
    rows: list[Any] = []
    cursor: str | None = None
    seen_ids: set[str] = set()
    for _ in range(100):
        params = {"limit": limit, **filters}
        if cursor is not None:
            params["after"] = cursor
        try:
            page = resource.list(**params)
        except Exception:
            raise DriverError("remote reconciliation listing failed") from None
        data = _get(page, "data")
        has_more = _get(page, "has_more")
        if not isinstance(data, list) or not isinstance(has_more, bool):
            raise DriverError("remote reconciliation listing is malformed")
        if len(data) > limit:
            raise DriverError("remote reconciliation page exceeded its requested limit")
        for row in data:
            object_id = _get(row, "id")
            if not isinstance(object_id, str) or not object_id or object_id in seen_ids:
                raise DriverError("remote reconciliation listing has missing or duplicate identities")
            seen_ids.add(object_id)
            rows.append(row)
        if not has_more:
            return rows
        next_cursor = _get(page, "last_id")
        if (not data or not isinstance(next_cursor, str) or not next_cursor
                or next_cursor != _get(data[-1], "id") or next_cursor == cursor):
            raise DriverError("remote reconciliation pagination cursor is invalid")
        cursor = next_cursor
    raise DriverError("remote reconciliation exceeded its page limit; outcome remains uncertain")


def _created_at_near(row: Any, started: datetime, *, clock_skew_seconds: int = 300) -> bool:
    value = _get(row, "created_at")
    if (isinstance(value, bool) or not isinstance(value, (int, float))
            or not math.isfinite(value)):
        raise DriverError("listed provider object has invalid creation time")
    # A lower bound alone can bind an unrelated object created long after an
    # uncertain mutation. Keep both sides bounded to the persisted intent time.
    return abs(value - started.timestamp()) <= clock_skew_seconds


def _reconcile_upload(state: dict[str, Any], facts: dict[str, Any], get_client: Any) -> dict[str, Any]:
    if state["upload_phase"] == "complete" and state.get("file_id"):
        return {"status": "already_bound", "file_id": state["file_id"]}
    if state["upload_phase"] not in {"in_flight", "unknown"}:
        raise DriverError("upload reconciliation requires an uncertain upload")
    if "operation_nonce" not in state:
        raise DriverError("legacy uncertain upload lacks a unique operation identity; manual review required")
    started = _phase_started_at(state, "upload")
    request_path = facts["package_dir"] / "batch_requests.jsonl"
    request_bytes = request_path.read_bytes()
    if sha256_bytes(request_bytes) != facts["batch_requests_sha256"]:
        raise DriverError("frozen upload bytes changed before reconciliation")
    expected_filename = _upload_filename(state["reconciliation_id"])
    listed = _list_remote_objects(get_client().files, limit=10_000, purpose="batch", order="desc")
    candidates = []
    for row in listed:
        if (_get(row, "purpose") == "batch"
                and _get(row, "filename") == expected_filename
                and _get(row, "bytes") == len(request_bytes)
                and _created_at_near(row, started)):
            candidates.append(row)
    matching: list[str] = []
    for row in candidates:
        file_id = _get(row, "id")
        if sha256_bytes(_download_bytes(get_client(), file_id)) == facts["batch_requests_sha256"]:
            matching.append(file_id)
    _history(state, "reconcile_upload", "matched" if len(matching) == 1 else
             "not_found" if not matching else "ambiguous")
    if len(matching) != 1:
        return {"status": "not_found" if not matching else "ambiguous", "candidate_count": len(matching)}
    state.update({"file_id": matching[0], "upload_phase": "complete"})
    return {"status": "reconciled", "file_id": matching[0]}


def _reconcile_submit(state: dict[str, Any], facts: dict[str, Any], get_client: Any) -> dict[str, Any]:
    if state["submit_phase"] == "complete" and state.get("batch_id"):
        return {"status": "already_bound", "batch_id": state["batch_id"]}
    if state["submit_phase"] not in {"in_flight", "unknown"} or not state.get("file_id"):
        raise DriverError("batch reconciliation requires an uncertain create and known input file")
    if "operation_nonce" not in state:
        raise DriverError("legacy uncertain Batch lacks a unique operation identity; manual review required")
    started = _phase_started_at(state, "submit")
    listed = _list_remote_objects(get_client().batches, limit=100)
    candidates = []
    for row in listed:
        metadata = _get(row, "metadata")
        if (_get(row, "input_file_id") == state["file_id"]
                and _get(row, "endpoint") == EXPECTED_ENDPOINT
                and _get(row, "completion_window") == COMPLETION_WINDOW
                and isinstance(metadata, dict)
                and metadata.get("senath_reconciliation_id") == state["reconciliation_id"]
                and _created_at_near(row, started)):
            candidates.append(row)
    if len(candidates) != 1:
        _history(state, "reconcile_submit", "not_found" if not candidates else "ambiguous")
        return {"status": "not_found" if not candidates else "ambiguous",
                "candidate_count": len(candidates)}
    result = _validate_batch_response(candidates[0], state["file_id"], state["reconciliation_id"])
    if result["request_counts"]["total"] != facts["request_count"]:
        _history(state, "reconcile_submit", "mismatched")
        return {"status": "mismatched", "candidate_count": 1}
    state.update(result)
    state["submit_phase"] = "complete"
    _history(state, "reconcile_submit", "matched")
    return {"status": "reconciled", "batch_id": state["batch_id"]}


def execute_phase(*, phase: str, facts: dict[str, Any], contracts: dict[str, Any],
                  state_path: Path, release: dict[str, Any], release_hash: str,
                  transport: Any | None = None, recovery_dir: Path | None = None) -> dict[str, Any]:
    """Run exactly one phase. Injected transport is for synthetic tests only."""
    if phase not in {"upload", "submit", "status", "recover", "reconcile-upload", "reconcile-submit",
                     "reconcile-recovery"}:
        raise DriverError("unknown remote phase")
    client = transport

    def get_client() -> Any:
        nonlocal client
        if client is None:
            client = _create_sdk_client()
        return client

    state_path = state_path.resolve()
    with _with_phase_lock(state_path):
        state, raw = _read_state(state_path, facts, release_hash)
        if phase in {"upload", "submit"}:
            state, raw = _ensure_operation_identity(state, facts, release_hash, state_path, raw)
        if phase in {"reconcile-upload", "reconcile-submit"}:
            result = (_reconcile_upload(state, facts, get_client) if phase == "reconcile-upload"
                      else _reconcile_submit(state, facts, get_client))
            _history(state, phase, result["status"])
            _write_state(state_path, state, raw)
            return {"phase": phase, **result, "provider_called": False, "dispatch_authorized": False}
        if phase == "upload":
            if state["upload_phase"] != "not_started":
                raise DriverError("upload was already attempted or is uncertain; never retry")
            request_path = facts["package_dir"] / "batch_requests.jsonl"
            request_bytes = request_path.read_bytes()
            if sha256_bytes(request_bytes) != facts["batch_requests_sha256"]:
                raise DriverError("provider-facing payload changed after local validation")
            active_client = get_client()
            state["upload_phase"] = "in_flight"
            _history(state, phase, "in_flight")
            raw = _write_state(state_path, state, raw)
            try:
                filename = _upload_filename(state["reconciliation_id"])
                response = active_client.files.create(file=(filename, io.BytesIO(request_bytes)), purpose="batch")
                file_id = _validate_upload_response(response, len(request_bytes), filename)
            except Exception:
                state["upload_phase"] = "unknown"
                _history(state, phase, "unknown")
                _write_state(state_path, state, raw)
                raise DriverError("upload outcome is uncertain; stop and reconcile, do not retry") from None
            state["file_id"] = file_id
            state["upload_phase"] = "complete"
            _history(state, phase, "complete")
            raw = _write_state(state_path, state, raw)
            return {"phase": phase, "status": "complete", "file_id": file_id, "provider_called": True}

        if phase == "submit":
            if state["upload_phase"] != "complete" or not state.get("file_id"):
                raise DriverError("batch submit requires one verified prior upload")
            if state["submit_phase"] != "not_started":
                raise DriverError("batch creation was attempted or is uncertain; never retry")
            if state.get("batch_id"):
                raise DriverError("batch already exists; duplicate creation prohibited")
            # Rebind the request artifact immediately before the remote create
            # phase; validation may have happened in an earlier process.
            request_path = facts["package_dir"] / "batch_requests.jsonl"
            if _file_hash(request_path) != facts["batch_requests_sha256"]:
                raise DriverError("provider-facing payload changed after local validation")
            active_client = get_client()
            # This is a purely local, resumable phase. If interrupted, inspect
            # the reconciler and append only missing intents before any create call.
            intent_digest = _ensure_intents(facts, contracts, facts["events_path"])
            state["submit_phase"] = "in_flight"
            state["intent_journal_sha256"] = intent_digest
            _history(state, phase, "in_flight")
            raw = _write_state(state_path, state, raw)
            try:
                response = active_client.batches.create(
                    input_file_id=state["file_id"], endpoint=EXPECTED_ENDPOINT,
                    completion_window=COMPLETION_WINDOW,
                    metadata={"senath_reconciliation_id": state["reconciliation_id"]},
                )
                result = _validate_batch_response(response, state["file_id"], state["reconciliation_id"])
                if result["request_counts"]["total"] != facts["request_count"]:
                    raise DriverError("batch-create count differs from frozen READY request count")
            except Exception:
                state["submit_phase"] = "unknown"
                _history(state, phase, "unknown")
                _write_state(state_path, state, raw)
                raise DriverError("batch-create outcome is uncertain; reconcile, never retry") from None
            state.update(result)
            state["submit_phase"] = "complete"
            _history(state, phase, "complete")
            raw = _write_state(state_path, state, raw)
            return {"phase": phase, "status": "complete", "batch_id": state["batch_id"],
                    "request_count": facts["request_count"], "provider_called": True,
                    "dispatch_authorized": False}

        if phase == "status":
            if not isinstance(state.get("batch_id"), str) or not state["batch_id"]:
                raise DriverError("status retrieval requires a reconciled known batch ID")
            if state["submit_phase"] != "complete":
                raise DriverError("status retrieval is blocked by an uncertain create phase")
            active_client = get_client()
            try:
                response = active_client.batches.retrieve(state["batch_id"])
                if (_get(response, "id") != state["batch_id"]
                        or _get(response, "input_file_id") != state.get("file_id")
                        or _get(response, "endpoint") != EXPECTED_ENDPOINT
                        or _get(response, "completion_window") != COMPLETION_WINDOW):
                    raise DriverError("retrieved Batch identity/input/endpoint mismatch")
                metadata = _get(response, "metadata")
                if (not isinstance(metadata, dict)
                        or metadata.get("senath_reconciliation_id") != state.get("reconciliation_id")):
                    raise DriverError("retrieved Batch operation binding mismatch")
                status = _get(response, "status")
                counts = _normalize_counts(_get(response, "request_counts"))
                if (status not in ALLOWED_BATCH_STATES or counts is None
                        or counts["total"] != facts["request_count"]
                        or (status == "completed" and counts["completed"] + counts["failed"] != counts["total"])):
                    raise DriverError("retrieved Batch status metadata is incomplete")
                usage_observation = _persist_batch_usage_observation(
                    state_path=state_path, state=state, facts=facts, response=response,
                    status=status, counts=counts,
                )
                state.update({"batch_status": status,
                              "output_file_id": _get(response, "output_file_id"),
                              "error_file_id": _get(response, "error_file_id"),
                              "request_counts": counts})
                _history(state, phase, status)
                state["history"][-1]["usage_observation_path"] = usage_observation["path"]
                state["history"][-1]["usage_observation_sha256"] = usage_observation["sha256"]
                raw = _write_state(state_path, state, raw)
            except Exception:
                raise DriverError("status retrieval failed or returned mismatched metadata") from None
            return {"phase": phase, "status": status, "request_counts": counts,
                    "usage_observation": usage_observation,
                    "provider_called": False, "dispatch_authorized": False}

        # A retry is only a read of the same completed Batch output/error IDs.
        # It starts in a new local directory and leaves every failed directory
        # intact for comparison and diagnosis.
        _validate_recovery_inputs(state, facts)
        if recovery_dir is None:
            raise DriverError("recovery output directory is required")
        recovery_dir = recovery_dir.resolve()
        if not recovery_dir.parent.is_dir():
            raise DriverError("recovery output parent is absent")
        if recovery_dir.exists():
            raise DriverError("recovery target exists; preserve it and stop")
        if phase == "recover":
            if state["recovery_phase"] != "not_started" or state.get("recovery_binding") is not None:
                raise DriverError("recovery already started; use reconcile-recovery or preserve evidence")
            binding = _new_recovery_binding(state, recovery_dir)
        else:
            binding = state.get("recovery_binding")
            if state["recovery_phase"] not in {"in_flight", "unknown"} or not isinstance(binding, dict):
                raise DriverError("reconcile-recovery requires an interrupted bound recovery")
            if binding.get("destination") != str(recovery_dir):
                raise DriverError("recovery destination differs from the original bound target")
            binding = json.loads(json.dumps(binding))
            _collect_prior_partial_digests(binding)
        attempt_id = uuid.uuid4().hex
        pending = recovery_dir.parent / f".{recovery_dir.name}.pending-{attempt_id}"
        binding["attempts"].append({
            "attempt_id": attempt_id, "pending_dir": str(pending), "status": "in_flight",
            "partial_sha256": {"output": None, "error": None},
        })
        state["recovery_binding"] = binding
        state["recovery_phase"] = "in_flight"
        _history(state, phase, "in_flight")
        raw = _write_state(state_path, state, raw)
        try:
            pending.mkdir()
            active_client = get_client()
            counts = binding["expected_counts"]
            output_bytes = _download_to_path(
                active_client, binding.get("output_file_id"), pending / "output.partial",
                expected_count=counts["completed"],
            )
            binding["attempts"][-1]["partial_sha256"]["output"] = sha256_bytes(output_bytes)
            raw = _write_state(state_path, state, raw)
            error_bytes = _download_to_path(
                active_client, binding.get("error_file_id"), pending / "errors.partial",
                expected_count=counts["failed"],
            )
            binding["attempts"][-1]["partial_sha256"]["error"] = sha256_bytes(error_bytes)
            raw = _write_state(state_path, state, raw)
            previous = _read_prior_partial_bytes(binding, exclude_attempt=attempt_id)
            _compare_prior_partials(previous, output_bytes, error_bytes)
            output_rows, error_rows, receipt = _validate_recovered_bundle(
                state, facts, contracts, output_bytes, error_bytes, counts,
            )
            _write_new(pending / "output.jsonl", output_bytes)
            _write_new(pending / "errors.jsonl", error_bytes)
            _write_new(pending / "receipt.json", canonical_json(receipt) + b"\n")
            if recovery_dir.exists():
                raise DriverError("recovery target appeared during save; refusing overwrite")
            _promote_directory_noreplace(pending, recovery_dir)
        except Exception:
            binding["attempts"][-1]["status"] = "unknown"
            _refresh_attempt_partial_digests(binding["attempts"][-1])
            state["recovery_binding"] = binding
            state["recovery_phase"] = "unknown"
            _history(state, phase, "unknown")
            _write_state(state_path, state, raw)
            raise DriverError("recovery transfer/save is uncertain; preserve evidence and reconcile") from None
        binding["attempts"][-1]["status"] = "complete"
        state["recovery_binding"] = binding
        state["recovery_phase"] = "complete"
        _history(state, phase, "complete")
        _write_state(state_path, state, raw)
        return {"phase": phase, "status": "complete", "recovery_dir": str(recovery_dir),
                "output_sha256": sha256_bytes(output_bytes), "error_sha256": sha256_bytes(error_bytes),
                "intent_journal_sha256": binding["intent_journal_sha256"],
                "result_count": len(output_rows) + len(error_rows), "provider_called": False,
                "dispatch_authorized": False}


def _download_bytes(client: Any, file_id: str | None) -> bytes:
    if not isinstance(file_id, str) or not file_id:
        raise DriverError("known Batch output/error file ID required")
    try:
        response = client.files.content(file_id)
        try:
            data = response.read()
        except Exception:
            data = getattr(response, "content", None)
    except DriverError:
        raise
    except Exception:
        # Provider SDK exception text can contain response details. Keep the
        # CLI boundary sanitized for failures during both acquisition and read.
        raise DriverError("remote file download failed") from None
    if not isinstance(data, bytes):
        raise DriverError("download did not return byte content")
    return data


def _validate_recovery_inputs(state: dict[str, Any], facts: dict[str, Any]) -> None:
    if state.get("batch_status") != "completed" or state.get("submit_phase") != "complete":
        raise DriverError("recovery requires known, terminal-complete Batch metadata")
    counts = state.get("request_counts")
    if (not isinstance(counts, dict) or set(counts) != {"total", "completed", "failed"}
            or any(isinstance(value, bool) or not isinstance(value, int) or value < 0 for value in counts.values())
            or counts["total"] != facts["request_count"]
            or counts["completed"] + counts["failed"] != counts["total"]):
        raise DriverError("terminal Batch counts do not match frozen request intents")
    if ((counts["completed"] > 0 and not state.get("output_file_id"))
            or (counts["failed"] > 0 and not state.get("error_file_id"))):
        raise DriverError("terminal-complete result file metadata/counts are incomplete")
    intent_journal_hash = state.get("intent_journal_sha256")
    request_path = facts["package_dir"] / "batch_requests.jsonl"
    mapping_path = facts["package_dir"] / "local_only_mapping.jsonl"
    manifest_path = facts["package_dir"] / "manifest.json"
    if (_file_hash(manifest_path) != facts["package_manifest_sha256"]
            or _file_hash(request_path) != facts["batch_requests_sha256"]
            or _file_hash(mapping_path) != facts["local_mapping_sha256"]):
        raise DriverError("frozen local package input changed since validation")
    if (_file_hash(facts["cases_path"]) != facts["case_inventory_sha256"]
            or not isinstance(intent_journal_hash, str)
            or SHA256_RE.fullmatch(intent_journal_hash) is None
            or not facts["events_path"].exists()
            or _file_hash(facts["events_path"]) != intent_journal_hash):
        raise DriverError("frozen case/journal input changed since local validation")


def _new_recovery_binding(state: dict[str, Any], destination: Path) -> dict[str, Any]:
    operation_id = uuid.uuid4().hex
    return {
        "schema": "C2_OUTPUT_RECOVERY_BINDING_V1",
        "operation_id": operation_id,
        "destination": str(destination),
        "output_file_id": state.get("output_file_id"),
        "error_file_id": state.get("error_file_id"),
        "expected_counts": dict(state["request_counts"]),
        "intent_journal_sha256": state["intent_journal_sha256"],
        "attempts": [],
    }


def _recovery_partial_path(attempt: dict[str, Any], kind: str) -> Path:
    pending = Path(attempt["pending_dir"])
    name = "output.partial" if kind == "output" else "errors.partial"
    path = pending / name
    final_name = "output.jsonl" if kind == "output" else "errors.jsonl"
    final = pending / final_name
    if path.exists() and final.exists():
        if path.is_symlink() or final.is_symlink() or _file_hash(path) != _file_hash(final):
            raise DriverError("recovery attempt contains competing partial and final evidence")
        # A validated copy is written under its final bundle name before the
        # atomic directory promotion. If that promotion races, retain both
        # byte-identical files and continue binding the original partial.
        return path
    # A successful atomic promotion removes the pending directory. During a
    # failed attempt only .partial is expected, but accepting the complete
    # intermediate file preserves older on-disk evidence without renaming it.
    return path if path.exists() else final


def _collect_prior_partial_digests(binding: dict[str, Any]) -> None:
    """Discover bytes left by a process crash and persist their digests in memory."""
    for attempt in binding["attempts"]:
        partials = attempt["partial_sha256"]
        pending_dir = Path(attempt["pending_dir"])
        if pending_dir.is_symlink():
            raise DriverError("prior recovery directory is a symbolic link; preserve evidence and stop")
        if attempt["status"] == "unknown" and not pending_dir.is_dir():
            raise DriverError("prior failed recovery directory is missing; preserve evidence and stop")
        for kind in ("output", "error"):
            path = _recovery_partial_path(attempt, kind)
            expected = partials[kind]
            if path.is_symlink():
                raise DriverError("previous recovery partial is a symbolic link; preserve evidence and stop")
            if not path.exists():
                if expected is not None:
                    raise DriverError("previous recovery partial disappeared; preserve evidence and stop")
                continue
            actual = _file_hash(path)
            if expected is not None and actual != expected:
                raise DriverError("previous recovery partial digest changed; preserve evidence and stop")
            partials[kind] = actual


def _refresh_attempt_partial_digests(attempt: dict[str, Any]) -> None:
    pending_dir = Path(attempt["pending_dir"])
    if pending_dir.is_symlink():
        return
    for kind in ("output", "error"):
        path = _recovery_partial_path(attempt, kind)
        if path.is_symlink():
            continue
        if path.is_file():
            attempt["partial_sha256"][kind] = _file_hash(path)


def _read_prior_partial_bytes(binding: dict[str, Any], *, exclude_attempt: str) -> dict[str, list[bytes]]:
    collected: dict[str, list[bytes]] = {"output": [], "error": []}
    for attempt in binding["attempts"]:
        if attempt["attempt_id"] == exclude_attempt:
            continue
        for kind in ("output", "error"):
            path = _recovery_partial_path(attempt, kind)
            expected = attempt["partial_sha256"][kind]
            if path.is_symlink():
                raise DriverError("previous recovery partial is a symbolic link; preserve evidence and stop")
            if not path.exists():
                if expected is not None:
                    raise DriverError("previous recovery partial is missing; preserve evidence and stop")
                continue
            try:
                data = path.read_bytes()
            except OSError:
                raise DriverError("previous recovery partial is unreadable; preserve evidence and stop") from None
            if expected is None or sha256_bytes(data) != expected:
                raise DriverError("previous recovery partial digest is unbound or changed")
            collected[kind].append(data)
    return collected


def _compare_prior_partials(previous: dict[str, list[bytes]], output: bytes, error: bytes) -> None:
    for kind, current in (("output", output), ("error", error)):
        for partial in previous[kind]:
            if not current.startswith(partial):
                raise DriverError("previous partial bytes differ from the immutable Batch result file")


def _download_to_path(client: Any, file_id: str | None, path: Path, *, expected_count: int) -> bytes:
    """Stream one immutable Batch artifact to an exclusive, durable partial file."""
    if expected_count == 0:
        if file_id is not None:
            raise DriverError("zero-count result unexpectedly has a provider file ID")
        _write_new(path, b"")
        return b""
    if not isinstance(file_id, str) or not file_id:
        raise DriverError("known Batch result file ID required")
    try:
        response = client.files.content(file_id)
    except Exception:
        raise DriverError("Batch result download failed") from None
    try:
        with path.open("xb") as stream:
            try:
                reader = getattr(response, "read", None)
                if callable(reader):
                    while True:
                        chunk = reader(1024 * 1024)
                        if chunk == b"":
                            break
                        if not isinstance(chunk, bytes):
                            raise DriverError("Batch result stream returned non-byte content")
                        stream.write(chunk)
                else:
                    content = getattr(response, "content", None)
                    if not isinstance(content, bytes):
                        raise DriverError("Batch result download did not return byte content")
                    stream.write(content)
            finally:
                stream.flush()
                os.fsync(stream.fileno())
        data = path.read_bytes()
    except DriverError:
        raise
    except Exception:
        raise DriverError("Batch result stream interrupted; partial bytes preserved") from None
    return data


def _validate_recovered_bundle(state: dict[str, Any], facts: dict[str, Any], contracts: dict[str, Any],
                               output_bytes: bytes, error_bytes: bytes,
                               counts: dict[str, int]) -> tuple[list[dict[str, Any]], list[dict[str, Any]], dict[str, Any]]:
    output_rows = _validate_result_file(output_bytes, expected_count=counts["completed"])
    error_rows = _validate_result_file(error_bytes, expected_count=counts["failed"])
    custom_ids = [row.get("custom_id") for row in output_rows + error_rows]
    if (any(not isinstance(value, str) or not value for value in custom_ids)
            or len(set(custom_ids)) != len(custom_ids)):
        raise DriverError("recovered result IDs are missing or duplicated")
    try:
        mapping_path = facts["package_dir"] / "local_only_mapping.jsonl"
        if _file_hash(mapping_path) != facts["local_mapping_sha256"]:
            raise DriverError("local-only result map changed after package validation")
        mapping, _ = contracts["t253"].read_jsonl(mapping_path)
    except (OSError, ValueError, KeyError, TypeError):
        raise DriverError("local-only result map is unavailable or invalid") from None
    ready_ids = set(facts["ready_ids"])
    mapped_tokens: set[str] = set()
    mapped_cases: set[str] = set()
    for row in mapping:
        if (not isinstance(row, dict) or set(row) != {"custom_id", "case_ref"}
                or not isinstance(row.get("custom_id"), str) or not row["custom_id"]
                or not isinstance(row.get("case_ref"), str)
                or row["case_ref"] not in ready_ids
                or row["custom_id"] in mapped_tokens or row["case_ref"] in mapped_cases):
            raise DriverError("local-only result map is ambiguous")
        mapped_tokens.add(row["custom_id"])
        mapped_cases.add(row["case_ref"])
    if mapped_cases != ready_ids or set(custom_ids) != mapped_tokens:
        raise DriverError("recovered result IDs do not cover the frozen READY map")
    receipt = {
        "schema": "C2_SAVED_BATCH_RECEIPT_V1", "status": "completed",
        "batch_id": state["batch_id"], "input_file_id": state["file_id"],
        "input_file_sha256": facts["batch_requests_sha256"],
        "endpoint": EXPECTED_ENDPOINT, "model": EXPECTED_MODEL,
        "output_file_sha256": sha256_bytes(output_bytes),
        "error_file_sha256": sha256_bytes(error_bytes),
        "request_counts": counts,
    }
    return output_rows, error_rows, receipt


def _validate_result_file(raw: bytes, *, expected_count: int) -> list[dict[str, Any]]:
    if expected_count == 0:
        if raw:
            raise DriverError("unexpected content in zero-count Batch result file")
        return []
    rows = _jsonl(raw)
    if len(rows) != expected_count:
        raise DriverError("saved Batch output line count differs from terminal counts")
    return rows


def _write_new(path: Path, raw: bytes) -> None:
    with path.open("xb") as stream:
        stream.write(raw)
        stream.flush()
        os.fsync(stream.fileno())


def _promote_directory_noreplace(source: Path, destination: Path) -> None:
    """Atomically install a complete recovery directory without replacing a target.

    The caller places source and destination beside one another on the same
    filesystem. Windows ``os.rename`` fails when the destination exists;
    Linux uses ``renameat2(RENAME_NOREPLACE)``. Refuse promotion on other
    platforms rather than emulate atomic no-replace with a racy exists check.
    """
    # Normalize lexically, not with Path.resolve(): the final destination may
    # have appeared as a symlink in the race window and must not be followed.
    source = Path(os.path.abspath(os.fspath(source)))
    destination = Path(os.path.abspath(os.fspath(destination)))
    if source.parent != destination.parent:
        raise DriverError("atomic recovery promotion requires sibling paths")
    if sys.platform == "win32":
        # Python maps this to the Windows rename operation, which does not
        # replace an existing destination (unlike POSIX rename).
        os.rename(source, destination)
        return
    if sys.platform.startswith("linux"):
        import ctypes

        try:
            libc = ctypes.CDLL(None, use_errno=True)
            renameat2 = libc.renameat2
        except (AttributeError, OSError):
            raise DriverError("atomic no-replace directory promotion is unavailable") from None
        renameat2.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p, ctypes.c_uint]
        renameat2.restype = ctypes.c_int
        result = renameat2(-100, os.fsencode(source), -100, os.fsencode(destination), 1)
        if result != 0:
            error_number = ctypes.get_errno()
            raise OSError(error_number, os.strerror(error_number), str(destination))
        return
    raise DriverError("atomic no-replace directory promotion is unavailable")


def _load_release(path: Path, facts: dict[str, Any], *, pin: str | None = None) -> tuple[dict[str, Any], str]:
    try:
        raw = path.read_bytes()
    except OSError:
        raise DriverError("lead release artifact unavailable") from None
    return validate_release(raw, facts, trusted_release_sha256=PINNED_LEAD_RELEASE_SHA256 if pin is None else pin)


def _load_saved_result_reconciler_v2(root: Path):
    path = root / "tools/research/c2/ai/tools/c2_saved_batch_reconcile_v2.py"
    try:
        raw = path.read_bytes()
    except OSError:
        raise DriverError("versioned offline result reconciler unavailable") from None
    if b"\r" in raw.replace(b"\r\n", b"") or hashlib.sha256(raw.replace(b"\r\n", b"\n")).hexdigest() != SAVED_RESULT_RECONCILER_V2_SHA256:
        raise DriverError("versioned offline result reconciler digest mismatch")
    spec = importlib.util.spec_from_file_location("senath_saved_result_reconciler_v2", path)
    if spec is None or spec.loader is None:
        raise DriverError("versioned offline result reconciler cannot load")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _load_offline_attempt_ledger_v1(root: Path):
    path = root / "tools/research/c2/ai/tools/c2_offline_attempt_ledger_v1.py"
    try:
        raw = path.read_bytes()
    except OSError:
        raise DriverError("versioned offline attempt-ledger builder unavailable") from None
    if b"\r" in raw.replace(b"\r\n", b""):
        raise DriverError("versioned offline attempt-ledger source has noncanonical line endings")
    source = raw.replace(b"\r\n", b"\n")
    if hashlib.sha256(source).hexdigest() != OFFLINE_ATTEMPT_LEDGER_V1_SHA256:
        raise DriverError("versioned offline attempt-ledger builder digest mismatch")
    spec = importlib.util.spec_from_file_location("senath_offline_attempt_ledger_v1", path)
    if spec is None or spec.loader is None:
        raise DriverError("versioned offline attempt-ledger builder cannot load")
    module = importlib.util.module_from_spec(spec)
    exec(compile(source, str(path), "exec"), module.__dict__)
    return module


def _run_cli(args: argparse.Namespace) -> int:
    root = args.root.resolve()
    if args.phase == "offline-attempt-ledger-v1":
        if args.execute or args.release or args.apply_saved_results:
            raise DriverError("offline attempt-ledger phase rejects execution and result-write flags")
        unrelated_paths = ("cases", "events", "state", "recovery_dir", "provider_receipt",
                           "output_jsonl", "error_jsonl", "expected_batch_id",
                           "case_inventory_sha256", "journal_sha256")
        if any(getattr(args, name, None) is not None for name in unrelated_paths):
            raise DriverError("offline attempt-ledger phase accepts only its package and output paths")
        if args.ledger_dir is None or args.ledger_action not in {"prepare", "validate"}:
            raise DriverError("offline attempt-ledger phase requires an action and ledger directory")
        builder = _load_offline_attempt_ledger_v1(root)
        result = builder.run_action(root=root, package_dir=args.package_dir,
                                    ledger_dir=args.ledger_dir, action=args.ledger_action)
        print(json.dumps(result, sort_keys=True))
        return 0
    if args.phase == "offline-saved-results-v2":
        if args.execute or args.release:
            raise DriverError("offline saved-result phase rejects remote execution flags")
        required = ("provider_receipt", "output_jsonl", "error_jsonl",
                    "expected_batch_id", "case_inventory_sha256", "journal_sha256", "cases", "events")
        if any(getattr(args, name, None) is None for name in required):
            raise DriverError("offline saved-result phase requires all local bundle paths and pins")
        reconciler = _load_saved_result_reconciler_v2(root)
        result = reconciler.run_saved_bundle(
            root=root, package_dir=args.package_dir, cases_path=args.cases,
            events_path=args.events, receipt_path=args.provider_receipt,
            output_path=args.output_jsonl, error_path=args.error_jsonl,
            expected_batch_id=args.expected_batch_id,
            case_inventory_sha256=args.case_inventory_sha256,
            expected_journal_sha256=args.journal_sha256,
            apply=bool(getattr(args, "apply_saved_results", False)))
        print(json.dumps(result, sort_keys=True))
        return 0
    if args.cases is None or args.events is None:
        raise DriverError("historical transport phases require case inventory and attempt journal paths")
    if args.state is None:
        raise DriverError("state path is required for historical transport phases")
    contracts = load_contracts(root)
    facts = validate_local_inputs(root, args.package_dir, args.cases, args.events, contracts)
    if args.phase == "validate":
        print(json.dumps({"status": "LOCAL_INPUTS_VALIDATED", "denominator": facts["denominator"],
                          "ready": facts["request_count"], "preparation_errors": facts["preparation_error_count"],
                          "case_inventory_sha256": facts["case_inventory_sha256"],
                          "package_manifest_sha256": facts["package_manifest_sha256"],
                          "provider_called": False, "dispatch_authorized": False, "cohort_selected": False}, sort_keys=True))
        return 0
    if args.phase == "reconcile-accounting":
        result = reconcile_usage_observations(state_path=args.state, facts=facts)
        print(json.dumps(result, sort_keys=True))
        return 0
    if not args.execute or not args.release:
        raise DriverError("remote phase requires explicit --execute and a lead release")
    release, release_hash = _load_release(args.release, facts)
    if PINNED_LEAD_RELEASE_SHA256 is None:
        raise DriverError("no independently lead-pinned release exists; SDK construction is blocked")
    result = execute_phase(
        phase=args.phase, facts=facts, contracts=contracts, state_path=args.state,
        release=release, release_hash=release_hash,
        recovery_dir=args.recovery_dir,
    )
    print(json.dumps(result, sort_keys=True))
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--package-dir", type=Path, required=True)
    parser.add_argument("--cases", type=Path)
    parser.add_argument("--events", type=Path)
    parser.add_argument("--state", type=Path)
    parser.add_argument("--phase", choices=["validate", "upload", "submit", "status", "recover",
                                             "reconcile-upload", "reconcile-submit", "reconcile-recovery",
                                             "reconcile-accounting", "offline-saved-results-v2",
                                             "offline-attempt-ledger-v1"], required=True)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--release", type=Path)
    parser.add_argument("--recovery-dir", type=Path)
    parser.add_argument("--provider-receipt", type=Path)
    parser.add_argument("--output-jsonl", type=Path)
    parser.add_argument("--error-jsonl", type=Path)
    parser.add_argument("--expected-batch-id")
    parser.add_argument("--case-inventory-sha256")
    parser.add_argument("--journal-sha256")
    parser.add_argument("--apply-saved-results", action="store_true",
                        help="explicitly mutate the local journal in offline-saved-results-v2 mode")
    parser.add_argument("--ledger-action", choices=["prepare", "validate"],
                        help="offline attempt-ledger operation; required for offline-attempt-ledger-v1")
    parser.add_argument("--ledger-dir", type=Path,
                        help="exclusive local output directory for offline-attempt-ledger-v1")
    args = parser.parse_args(argv)
    try:
        return _run_cli(args)
    except (DriverError, OSError, ValueError, KeyError, TypeError):
        print(json.dumps({"status": "BLOCKED", "reason": "preflight, release or phase validation failed; no retry"}, sort_keys=True))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
