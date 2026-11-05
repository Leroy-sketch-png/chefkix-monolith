#!/usr/bin/env python3
"""Versioned, offline-only C2 saved-Batch validator and WAL reconciler.

This is a portable replacement contract, not a byte replacement for T222,
T249, or T246. It keeps their historical digests untouched and reuses the
reviewed T219 compare-and-append journal. It never creates a provider client,
contacts a service, retries a request, or authorizes dispatch.
"""

from __future__ import annotations

import hashlib
import importlib.util
import json
import os
import re
import tempfile
from pathlib import Path
from typing import Any

CONTRACT = "C2_SAVED_BATCH_RECONCILE_V2"
PROTOCOL_SHA256 = "b2952599099d4bd94e3a117b4711af829b84949a60004ce21c963721b6d5c12"
T219_SHA256 = "d2a0a1c06f3a0f12c74c0bf3a766367ee93a2d0e5b3d26b4f35c2d2b1a71c36b"
T253_SHA256 = "24091fe506bb91c4225de4c3d9052e2e00e8518b5810666d78afeb0f151742ac"
EXPECTED_MODEL = "gpt-4o-2024-08-06"
EXPECTED_ENDPOINT = "/v1/responses"
EXPECTED_FIELDS = ("recipe_title", "ingredient_list", "ingredient_to_replace")
SHA256_RE = re.compile(r"[0-9a-f]{64}\Z")
TOKEN_RE = re.compile(r"c2_[A-Za-z0-9_-]{43}\Z")
FROZEN_COUNTS = {"denominator_cases": 10_747, "ready_requests": 10_499,
                 "retained_preparation_errors": 248}


class ReconcileError(ValueError):
    """Sanitized local validation or reconciliation failure."""


def sha256(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def _source_pin(raw: bytes) -> str:
    """Hash Git-canonical LF bytes while accepting only CRLF checkout conversion."""
    if b"\r" in raw.replace(b"\r\n", b""):
        raise ReconcileError("pinned source has noncanonical line endings")
    return sha256(raw.replace(b"\r\n", b"\n"))


def canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"),
                      allow_nan=False).encode("utf-8", errors="strict")


def _pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for key, value in pairs:
        if key in out:
            raise ReconcileError("duplicate JSON property")
        out[key] = value
    return out


def _json(raw: bytes) -> Any:
    try:
        return json.loads(raw.decode("utf-8", errors="strict"), object_pairs_hook=_pairs,
                          parse_constant=lambda _: (_ for _ in ()).throw(ReconcileError("invalid constant")))
    except (UnicodeError, json.JSONDecodeError, ReconcileError):
        raise ReconcileError("malformed UTF-8 JSON") from None


def _jsonl(raw: bytes, *, empty_ok: bool = False) -> list[dict[str, Any]]:
    if not raw:
        if empty_ok:
            return []
        raise ReconcileError("required JSONL is empty")
    if not raw.endswith(b"\n") or b"\r" in raw:
        raise ReconcileError("JSONL is incomplete or noncanonical")
    rows = [_json(line) for line in raw[:-1].split(b"\n")]
    if any(not isinstance(row, dict) for row in rows):
        raise ReconcileError("JSONL rows must be objects")
    if b"".join(canonical(row) + b"\n" for row in rows) != raw:
        raise ReconcileError("JSONL rows are not canonical")
    return rows


def validate_response(body: dict[str, Any], *, case_id: str, request_body_sha256: str) -> dict[str, Any]:
    """Return only a terminal disposition and digest; never return candidate text."""
    if not isinstance(body, dict) or not isinstance(case_id, str) or not case_id:
        raise ReconcileError("response or case identity is invalid")
    if not SHA256_RE.fullmatch(request_body_sha256 or ""):
        raise ReconcileError("request digest is invalid")
    response_digest = sha256(canonical(body))
    if body.get("status") != "completed":
        return {"case_id": case_id, "disposition": "TERMINAL_FAILURE",
                "failure_code": "response_truncated", "response_sha256": response_digest,
                "request_body_sha256": request_body_sha256}
    output = body.get("output")
    messages = [item for item in output if isinstance(item, dict) and item.get("type") == "message"] if isinstance(output, list) else []
    if len(messages) != 1 or messages[0].get("role") != "assistant" or messages[0].get("status") != "completed":
        return {"case_id": case_id, "disposition": "TERMINAL_FAILURE",
                "failure_code": "response_invalid", "response_sha256": response_digest,
                "request_body_sha256": request_body_sha256}
    content = messages[0].get("content")
    texts = [item.get("text") for item in content if isinstance(item, dict) and item.get("type") == "output_text"] if isinstance(content, list) else []
    if len(texts) != 1 or not isinstance(texts[0], str):
        return {"case_id": case_id, "disposition": "TERMINAL_FAILURE",
                "failure_code": "response_invalid", "response_sha256": response_digest,
                "request_body_sha256": request_body_sha256}
    try:
        result = _json(texts[0].encode("utf-8"))
    except ReconcileError:
        return {"case_id": case_id, "disposition": "TERMINAL_FAILURE",
                "failure_code": "response_invalid", "response_sha256": response_digest,
                "request_body_sha256": request_body_sha256}
    values = result.get("substitutions") if isinstance(result, dict) and set(result) == {"substitutions"} else None
    if (not isinstance(values, list) or len(values) != 20
            or any(not isinstance(item, str) or not item.strip() for item in values)):
        return {"case_id": case_id, "disposition": "TERMINAL_FAILURE",
                "failure_code": "response_invalid", "response_sha256": response_digest,
                "request_body_sha256": request_body_sha256}
    return {"case_id": case_id, "disposition": "RESPONSE_VALID",
            "response_sha256": response_digest, "request_body_sha256": request_body_sha256}


def _verify_dependencies(root: Path) -> None:
    for rel, expected in (("tools/research/c2/ai/tools/c2_attempt_resume.py", T219_SHA256),
                          ("tools/research/c2/ai/tools/build_senath_c2_batch_transport_candidate.py", T253_SHA256)):
        try:
            raw = (root / rel).read_bytes()
        except OSError:
            raise ReconcileError("pinned offline dependency unavailable") from None
        if _source_pin(raw) != expected:
            raise ReconcileError("pinned offline dependency digest mismatch")


def _load_verified_module(root: Path, rel: str, expected: str, name: str):
    path = root / rel
    try:
        raw = path.read_bytes()
    except OSError:
        raise ReconcileError("pinned offline dependency unavailable") from None
    if _source_pin(raw) != expected:
        raise ReconcileError("pinned offline dependency digest mismatch")
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise ReconcileError("pinned offline dependency cannot load")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _case_counts(cases: list[dict[str, Any]]) -> dict[str, int]:
    ready = sum(row.get("disposition") == "READY" for row in cases)
    errors = sum(row.get("disposition") == "PREPARATION_ERROR" for row in cases)
    return {"denominator_cases": len(cases), "ready_requests": ready,
            "retained_preparation_errors": errors}


def _receipt_bytes(record: dict[str, Any]) -> bytes:
    return canonical(record) + b"\n"


def _read_ingestion_receipt(path: Path) -> tuple[dict[str, Any], bytes] | None:
    if not path.exists():
        return None
    try:
        raw = path.read_bytes()
    except OSError:
        raise ReconcileError("ingestion receipt unreadable; preserve and stop") from None
    value = _json(raw[:-1]) if raw.endswith(b"\n") else None
    fields = {"schema", "state", "bundle_sha256", "package_manifest_sha256",
              "case_inventory_sha256", "base_journal_sha256", "final_journal_sha256",
              "result_count"}
    if (not isinstance(value, dict) or set(value) != fields
            or value.get("schema") != CONTRACT or value.get("state") not in {"STARTED", "COMPLETE"}
            or not SHA256_RE.fullmatch(value.get("bundle_sha256", ""))
            or not SHA256_RE.fullmatch(value.get("package_manifest_sha256", ""))
            or not SHA256_RE.fullmatch(value.get("case_inventory_sha256", ""))
            or not SHA256_RE.fullmatch(value.get("base_journal_sha256", ""))
            or (value.get("final_journal_sha256") is not None
                and not SHA256_RE.fullmatch(value["final_journal_sha256"]))
            or isinstance(value.get("result_count"), bool) or not isinstance(value.get("result_count"), int)
            or value["result_count"] < 0 or raw != _receipt_bytes(value)):
        raise ReconcileError("ingestion receipt malformed; preserve and stop")
    return value, raw


def _create_receipt_no_overwrite(path: Path, raw: bytes) -> None:
    try:
        descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(raw)
            stream.flush()
            os.fsync(stream.fileno())
    except FileExistsError:
        current = _read_ingestion_receipt(path)
        if current is None or current[1] != raw:
            raise ReconcileError("concurrent ingestion receipt differs; preserve and stop") from None
    except OSError:
        raise ReconcileError("ingestion receipt creation uncertain; preserve and stop") from None


def _replace_receipt_cas(path: Path, old: bytes, new: bytes) -> None:
    temporary: Path | None = None
    try:
        if path.read_bytes() != old:
            raise ReconcileError("ingestion receipt changed concurrently; preserve and stop")
        descriptor, name = tempfile.mkstemp(prefix=f".{path.name}.pending-", dir=path.parent)
        temporary = Path(name)
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(new)
            stream.flush()
            os.fsync(stream.fileno())
        if path.read_bytes() != old:
            raise ReconcileError("ingestion receipt changed concurrently; preserve and stop")
        os.replace(temporary, path)
        temporary = None
    except ReconcileError:
        raise
    except OSError:
        raise ReconcileError("ingestion receipt update uncertain; preserve and stop") from None
    finally:
        if temporary is not None:
            try:
                temporary.unlink()
            except OSError:
                pass


def _append_events_once(*, t219: Any, cases_path: Path, events_path: Path,
                        package_manifest_sha256: str, case_inventory_sha256: str,
                        expected_journal_sha256: str,
                        dispositions: list[dict[str, Any]]) -> tuple[int, str]:
    """Validate and append a complete local result delta with one WAL sync."""
    lock_path = events_path.with_name(events_path.name + ".lock")
    with t219._cooperative_file_lock(lock_path):
        cases, case_raw = t219.read_jsonl(cases_path)
        if sha256(case_raw) != case_inventory_sha256:
            raise ReconcileError("case inventory changed; preserve and stop")
        existing, prefix = t219.read_jsonl(events_path) if events_path.exists() else ([], b"")
        current_digest = sha256(prefix)
        if current_digest != expected_journal_sha256:
            raise ReconcileError("journal changed while waiting for lock; refresh and stop")
        try:
            prior = t219.reconcile_attempts(cases, existing, package_manifest_sha256,
                expected_case_inventory_sha256=case_inventory_sha256,
                event_ledger_sha256=current_digest)
        except (ValueError, KeyError, TypeError):
            raise ReconcileError("attempt journal fails frozen reconciliation") from None
        case_index = {row["case_id"]: row for row in cases}
        states = {row["case_id"]: row["state"] for row in prior["cases"]}
        next_events = list(existing)
        new_events: list[dict[str, Any]] = []
        for item in dispositions:
            case_id = item["case_id"]
            case = case_index.get(case_id)
            if case is None or case.get("disposition") != "READY" or states.get(case_id) != "UNCERTAIN_NO_RETRY":
                raise ReconcileError("result does not correspond to one prior uncertain attempt")
            event = {"sequence": len(next_events) + 1,
                     "event_id": item["event_id"], "case_id": case_id,
                     "package_manifest_sha256": package_manifest_sha256,
                     "event_type": item["event_type"],
                     "request_body_sha256": case["request_body_sha256"]}
            if item["event_type"] == "RESPONSE_RECEIVED":
                event["response_sha256"] = item["response_sha256"]
            elif item["event_type"] == "TERMINAL_FAILURE":
                event["failure_code"] = item["failure_code"]
            else:
                raise ReconcileError("result event type is not terminal")
            new_events.append(event)
            next_events.append(event)
            states[case_id] = item["event_type"]
        try:
            next_state = t219.reconcile_attempts(cases, next_events, package_manifest_sha256,
                expected_case_inventory_sha256=case_inventory_sha256)
        except (ValueError, KeyError, TypeError):
            raise ReconcileError("result event set violates one-attempt journal transitions") from None
        if next_state.get("dispatch_authorized") is not False:
            raise ReconcileError("journal cannot grant dispatch authorization")
        if not new_events:
            return 0, current_digest
        append_raw = t219.jsonl_bytes(new_events)
        try:
            descriptor = os.open(events_path, os.O_WRONLY | os.O_CREAT | os.O_APPEND | getattr(os, "O_BINARY", 0), 0o600)
        except OSError:
            raise ReconcileError("journal could not be opened for append") from None
        try:
            offset = 0
            while offset < len(append_raw):
                written = os.write(descriptor, append_raw[offset:])
                if written <= 0:
                    raise ReconcileError("journal append partial; outcome uncertain, stop")
                offset += written
            os.fsync(descriptor)
        except OSError:
            raise ReconcileError("journal append uncertain; reconcile and do not retry") from None
        finally:
            try:
                os.close(descriptor)
            except OSError:
                raise ReconcileError("journal close outcome uncertain; stop") from None
        actual = events_path.read_bytes()
        if actual != prefix + append_raw:
            raise ReconcileError("journal read-back mismatch; preserve and stop")
        final_rows, final_raw = t219.read_jsonl(events_path)
        verified = t219.reconcile_attempts(cases, final_rows, package_manifest_sha256,
            expected_case_inventory_sha256=case_inventory_sha256,
            event_ledger_sha256=sha256(final_raw))
        if (final_raw != prefix + append_raw or verified["event_count"] != prior["event_count"] + len(new_events)
                or verified.get("dispatch_authorized") is not False):
            raise ReconcileError("journal post-append verification failed; preserve and stop")
        return len(new_events), sha256(actual)


def reconcile_saved_bundle(*, root: Path, package_dir: Path, cases_path: Path,
                           events_path: Path, receipt_path: Path, output_path: Path,
                           error_path: Path, expected_batch_id: str,
                           expected_case_inventory_sha256: str,
                           expected_journal_sha256: str) -> dict[str, Any]:
    """Validate a saved bundle against prior intents; no mutation or network."""
    _verify_dependencies(root)
    t219 = _load_verified_module(root, "tools/research/c2/ai/tools/c2_attempt_resume.py", T219_SHA256,
                                 "c2_replacement_t219")
    t253 = _load_verified_module(root, "tools/research/c2/ai/tools/build_senath_c2_batch_transport_candidate.py",
                                 T253_SHA256, "c2_replacement_t253")
    try:
        package_result = t253.validate_saved_package(root, package_dir)
        manifest_raw = (package_dir / "manifest.json").read_bytes()
        manifest = _json(manifest_raw)
        cases_raw = cases_path.read_bytes()
        events_raw = events_path.read_bytes() if events_path.exists() else b""
        receipt_raw, output_raw, error_raw = receipt_path.read_bytes(), output_path.read_bytes(), error_path.read_bytes()
        mapping_raw = (package_dir / "local_only_mapping.jsonl").read_bytes()
        requests_raw = (package_dir / "batch_requests.jsonl").read_bytes()
    except (OSError, ValueError):
        raise ReconcileError("required local package or saved artifact unavailable") from None
    if not isinstance(manifest, dict) or not SHA256_RE.fullmatch(expected_case_inventory_sha256 or ""):
        raise ReconcileError("package manifest or case digest invalid")
    package_digest = manifest.get("parent_t208_manifest_sha256")
    if not SHA256_RE.fullmatch(package_digest or ""):
        raise ReconcileError("parent package digest invalid")
    if sha256(cases_raw) != expected_case_inventory_sha256:
        raise ReconcileError("frozen case inventory digest mismatch")
    cases = _jsonl(cases_raw)
    events = _jsonl(events_raw, empty_ok=True)
    if sha256(events_raw) != expected_journal_sha256:
        raise ReconcileError("journal digest changed; refresh and stop")
    try:
        state = t219.reconcile_attempts(cases, events, package_digest,
                                        expected_case_inventory_sha256=expected_case_inventory_sha256,
                                        event_ledger_sha256=expected_journal_sha256)
    except (ValueError, KeyError, TypeError):
        raise ReconcileError("attempt journal does not satisfy the pinned one-attempt contract") from None
    case_index = {row["case_id"]: row for row in cases}
    if len(case_index) != len(cases):
        raise ReconcileError("duplicate case identity")
    ready = {key: row for key, row in case_index.items() if row["disposition"] == "READY"}
    prep_errors = {key: row for key, row in case_index.items() if row["disposition"] == "PREPARATION_ERROR"}
    counts = _case_counts(cases)
    if counts != FROZEN_COUNTS or manifest.get("counts") != FROZEN_COUNTS:
        raise ReconcileError("case/manifest counts differ from frozen 10,747 = 10,499 + 248 denominator")
    mapping, requests = _jsonl(mapping_raw), _jsonl(requests_raw)
    token_to_case: dict[str, str] = {}
    for row in mapping:
        if set(row) != {"custom_id", "case_ref"}:
            raise ReconcileError("local correlation map schema invalid")
        token, case_ref = row["custom_id"], row["case_ref"]
        if (not isinstance(token, str) or not TOKEN_RE.fullmatch(token) or token in token_to_case
                or not isinstance(case_ref, str) or case_ref not in ready or case_ref in token_to_case.values()):
            raise ReconcileError("local correlation map invalid or ambiguous")
        token_to_case[token] = case_ref
    if set(token_to_case.values()) != set(ready):
        raise ReconcileError("correlation map does not cover READY denominator")
    request_tokens: set[str] = set()
    for row in requests:
        if (set(row) != {"custom_id", "method", "url", "body"} or row["method"] != "POST"
                or row["url"] != EXPECTED_ENDPOINT or not isinstance(row.get("custom_id"), str)):
            raise ReconcileError("Batch request envelope invalid")
        body = row["body"]
        if (row["custom_id"] not in token_to_case or row["custom_id"] in request_tokens
                or not isinstance(body, dict) or body.get("model") != EXPECTED_MODEL):
            raise ReconcileError("Batch request token or model invalid")
        request_tokens.add(row["custom_id"])
    if request_tokens != set(token_to_case):
        raise ReconcileError("request inventory differs from READY correlation map")
    outputs, errors = _jsonl(output_raw, empty_ok=True), _jsonl(error_raw, empty_ok=True)
    receipt = _json(receipt_raw)
    receipt_fields = {"schema", "status", "batch_id", "input_file_id", "input_file_sha256",
                      "endpoint", "model", "output_file_sha256", "error_file_sha256", "request_counts"}
    if (not isinstance(receipt, dict) or set(receipt) != receipt_fields
            or receipt.get("schema") != "C2_SAVED_BATCH_RECEIPT_V1" or receipt.get("status") != "completed"):
        raise ReconcileError("saved Batch receipt invalid")
    if (receipt.get("batch_id") != expected_batch_id or receipt.get("endpoint") != EXPECTED_ENDPOINT
            or receipt.get("model") != EXPECTED_MODEL or receipt.get("input_file_sha256") != package_result.get("batch_requests_sha256")
            or receipt.get("output_file_sha256") != sha256(output_raw)
            or receipt.get("error_file_sha256") != sha256(error_raw)):
        raise ReconcileError("saved Batch receipt does not bind exact files and job")
    expected_counts = {"total": len(ready), "completed": len(outputs), "failed": len(errors)}
    if (not isinstance(receipt.get("input_file_id"), str) or not receipt["input_file_id"]
            or receipt.get("request_counts") != expected_counts or len(outputs) + len(errors) != len(ready)):
        raise ReconcileError("saved result files do not cover READY denominator")
    by_token: dict[str, tuple[str, dict[str, Any]]] = {}
    result_ids: set[str] = set()
    for row, is_error in [*((r, False) for r in outputs), *((r, True) for r in errors)]:
        if not isinstance(row.get("custom_id"), str) or row["custom_id"] not in token_to_case or row["custom_id"] in by_token:
            raise ReconcileError("result token is missing, duplicated, or unknown")
        if not isinstance(row.get("id"), str) or not row["id"]:
            raise ReconcileError("result identity invalid")
        if row["id"] in result_ids:
            raise ReconcileError("duplicate provider result identity")
        result_ids.add(row["id"])
        by_token[row["custom_id"]] = ("error" if is_error else "response", row)
    if set(by_token) != set(token_to_case):
        raise ReconcileError("result token set differs from READY denominator")
    states = {r["case_id"]: r["state"] for r in state["cases"]}
    if {k for k,v in states.items() if v in {"UNCERTAIN_NO_RETRY", "RESPONSE_RECEIVED", "TERMINAL_FAILURE"}} != set(ready):
        raise ReconcileError("every READY case must have exactly one prior uncertain intent")
    results: list[dict[str, Any]] = []
    for token, case_id in token_to_case.items():
        kind, row = by_token[token]
        case = ready[case_id]
        if kind == "error":
            if set(row) != {"id", "custom_id", "error"} or not isinstance(row["error"], dict):
                raise ReconcileError("Batch error envelope invalid")
            record = {"case_id": case_id, "event_type": "TERMINAL_FAILURE", "failure_code": "provider_error"}
        else:
            response = row.get("response")
            if (set(row) != {"id", "custom_id", "response"} or not isinstance(response, dict)
                    or set(response) != {"status_code", "request_id", "body"}
                    or isinstance(response.get("status_code"), bool) or not isinstance(response.get("status_code"), int)
                    or response["status_code"] < 100 or response["status_code"] > 599
                    or not isinstance(response.get("request_id"), str) or not response["request_id"]
                    or not isinstance(response.get("body"), dict)):
                raise ReconcileError("saved response envelope invalid")
            if response["status_code"] != 200:
                parsed = {"disposition": "TERMINAL_FAILURE", "failure_code": "provider_error"}
            else:
                parsed = validate_response(response["body"], case_id=case_id,
                                           request_body_sha256=case["request_body_sha256"])
            record = {"case_id": case_id, "event_type": "RESPONSE_RECEIVED",
                      "response_sha256": parsed["response_sha256"]} if parsed["disposition"] == "RESPONSE_VALID" else {
                      "case_id": case_id, "event_type": "TERMINAL_FAILURE", "failure_code": parsed["failure_code"]}
        results.append(record)
    # Preparation errors remain in the fixed denominator; no result may map to one.
    bundle_binding = {
        "contract": CONTRACT, "batch_id": expected_batch_id,
        "package_manifest_sha256": sha256(manifest_raw),
        "case_inventory_sha256": expected_case_inventory_sha256,
        "batch_input_sha256": package_result.get("batch_requests_sha256"),
        "provider_receipt_sha256": sha256(receipt_raw),
        "output_sha256": sha256(output_raw), "error_sha256": sha256(error_raw),
        "result_count": len(results),
    }
    return {"contract": CONTRACT, "protocol_sha256": PROTOCOL_SHA256,
            "denominator": len(cases), "ready": len(ready), "preparation_errors": len(prep_errors),
            "result_count": len(results), "results": sorted(results, key=lambda row: row["case_id"]),
            "bundle_sha256": sha256(canonical(bundle_binding)),
            "package_manifest_sha256": package_digest,
            "case_inventory_sha256": expected_case_inventory_sha256,
            "journal_sha256": expected_journal_sha256, "dispatch_authorized": False,
            "provider_called": False, "retry_authorized": False}


def append_result_events(*, root: Path, cases_path: Path, events_path: Path,
                         package_manifest_sha256: str, case_inventory_sha256: str,
                         expected_journal_sha256: str, validation: dict[str, Any],
                         bundle_sha256: str, ingestion_receipt_path: Path,
                         source_artifact_paths: list[Path]) -> dict[str, Any]:
    """Append already-validated local result dispositions; uncertainty stops, never retries."""
    if (validation.get("contract") != CONTRACT or validation.get("dispatch_authorized") is not False
            or validation.get("retry_authorized") is not False or validation.get("bundle_sha256") != bundle_sha256
            or validation.get("package_manifest_sha256") != package_manifest_sha256
            or validation.get("case_inventory_sha256") != case_inventory_sha256
            or validation.get("journal_sha256") != expected_journal_sha256):
        raise ReconcileError("validation receipt is not from this offline contract")
    if not SHA256_RE.fullmatch(bundle_sha256 or ""):
        raise ReconcileError("saved bundle digest invalid")
    t219 = _load_verified_module(root, "tools/research/c2/ai/tools/c2_attempt_resume.py", T219_SHA256,
                                 "c2_replacement_t219_append")
    events, raw = t219.read_jsonl(events_path) if events_path.exists() else ([], b"")
    if sha256(raw) != expected_journal_sha256:
        raise ReconcileError("journal changed; reconcile before continuing")
    cases, case_raw = t219.read_jsonl(cases_path)
    if sha256(case_raw) != case_inventory_sha256:
        raise ReconcileError("case inventory changed; preserve and stop")
    cases_by_id = {row.get("case_id"): row for row in cases}
    if len(cases_by_id) != len(cases):
        raise ReconcileError("case inventory contains duplicate identities")
    if (validation.get("denominator") != FROZEN_COUNTS["denominator_cases"]
            or validation.get("ready") != FROZEN_COUNTS["ready_requests"]
            or validation.get("preparation_errors") != FROZEN_COUNTS["retained_preparation_errors"]
            or validation.get("result_count") != FROZEN_COUNTS["ready_requests"]
            or len(validation.get("results", [])) != FROZEN_COUNTS["ready_requests"]):
        raise ReconcileError("validation receipt does not preserve the frozen denominator")
    journal_lock = events_path.with_name(events_path.name + ".lock")
    receipt_lock = ingestion_receipt_path.with_name(ingestion_receipt_path.name + ".lock")
    protected_paths = [cases_path, events_path, journal_lock, *source_artifact_paths]
    if not source_artifact_paths:
        raise ReconcileError("source artifact paths are required for non-destructive ingestion")
    try:
        if any(not path.is_file() for path in source_artifact_paths):
            raise ReconcileError("a saved source artifact path is missing or not a file")
        resolved = [path.resolve(strict=False) for path in protected_paths]
        if (len({str(path).casefold() for path in resolved}) != len(resolved)
                or ingestion_receipt_path.resolve(strict=False) in resolved
                or receipt_lock.resolve(strict=False) in resolved):
            raise ReconcileError("ingestion receipt or lock aliases a protected artifact")
    except OSError:
        raise ReconcileError("local artifact path identity is ambiguous; stop") from None
    try:
        if ingestion_receipt_path.exists() and any(os.path.samefile(ingestion_receipt_path, path)
                for path in protected_paths if path.exists()):
            raise ReconcileError("ingestion receipt path aliases a protected artifact")
    except OSError:
        raise ReconcileError("local artifact path identity is ambiguous; stop") from None
    with t219._cooperative_file_lock(receipt_lock):
        locked_events, locked_raw = t219.read_jsonl(events_path) if events_path.exists() else ([], b"")
        if sha256(locked_raw) != expected_journal_sha256:
            raise ReconcileError("journal changed while waiting for lock; refresh and stop")
        loaded = _read_ingestion_receipt(ingestion_receipt_path)
        expected_binding = {
            "schema": CONTRACT, "bundle_sha256": bundle_sha256,
            "package_manifest_sha256": package_manifest_sha256,
            "case_inventory_sha256": case_inventory_sha256,
            "result_count": len(validation["results"]),
        }
        if loaded is None:
            prior_rows, _ = t219.read_jsonl(events_path) if events_path.exists() else ([], b"")
            if any(isinstance(row.get("event_id"), str) and row["event_id"].startswith("saved:") for row in prior_rows):
                raise ReconcileError("legacy partial saved-result events lack a bundle receipt; preserve and stop")
            started = {**expected_binding, "state": "STARTED",
                       "base_journal_sha256": expected_journal_sha256,
                       "final_journal_sha256": None}
            _create_receipt_no_overwrite(ingestion_receipt_path, _receipt_bytes(started))
            loaded = _read_ingestion_receipt(ingestion_receipt_path)
        if loaded is None:
            raise ReconcileError("ingestion receipt did not verify; preserve and stop")
        receipt, old_receipt_raw = loaded
        if any(receipt.get(key) != value for key, value in expected_binding.items()):
            raise ReconcileError("saved bundle differs from durable ingestion binding; preserve and stop")
        if receipt["state"] == "COMPLETE" and receipt["final_journal_sha256"] != expected_journal_sha256:
            raise ReconcileError("completed journal digest changed; preserve and stop")

        existing_by_id = {event.get("event_id"): event for event in locked_events}
        pending: list[dict[str, Any]] = []
        expected_ids: set[str] = set()
        for result in validation["results"]:
            case_id = result["case_id"]
            event_id = "saved:" + bundle_sha256[:24] + ":" + sha256(case_id.encode("utf-8"))[:16]
            expected_ids.add(event_id)
            found = existing_by_id.get(event_id)
            if found is not None:
                expected = {"event_id": event_id, "case_id": case_id,
                            "package_manifest_sha256": package_manifest_sha256,
                            "request_body_sha256": cases_by_id.get(case_id, {}).get("request_body_sha256"),
                            "event_type": result["event_type"]}
                if result["event_type"] == "RESPONSE_RECEIVED":
                    expected["response_sha256"] = result["response_sha256"]
                else:
                    expected["failure_code"] = result["failure_code"]
                expected["sequence"] = found.get("sequence")
                if found != expected:
                    raise ReconcileError("existing event conflicts with saved result; preserve and stop")
                continue
            if receipt["state"] == "COMPLETE":
                raise ReconcileError("completed bundle is missing an event; preserve and stop")
            pending.append({**result, "event_id": event_id})

        appended, current = _append_events_once(
            t219=t219, cases_path=cases_path, events_path=events_path,
            package_manifest_sha256=package_manifest_sha256,
            case_inventory_sha256=case_inventory_sha256,
            expected_journal_sha256=expected_journal_sha256,
            dispositions=pending)

        final_events, final_raw = t219.read_jsonl(events_path) if events_path.exists() else ([], b"")
        final_state = t219.reconcile_attempts(
            cases, final_events, package_manifest_sha256,
            expected_case_inventory_sha256=case_inventory_sha256,
            event_ledger_sha256=sha256(final_raw),
        )
        saved_prefix = "saved:" + bundle_sha256[:24] + ":"
        actual_ids = {event.get("event_id") for event in final_events
                      if isinstance(event.get("event_id"), str) and event["event_id"].startswith(saved_prefix)}
        if (final_state.get("dispatch_authorized") is not False or sha256(final_raw) != current
                or actual_ids != expected_ids):
            raise ReconcileError("post-append journal verification failed; preserve and stop")
        if receipt["state"] == "COMPLETE":
            if receipt["final_journal_sha256"] != sha256(final_raw):
                raise ReconcileError("completed receipt does not match journal; preserve and stop")
        else:
            complete = dict(receipt, state="COMPLETE", final_journal_sha256=sha256(final_raw))
            _replace_receipt_cas(ingestion_receipt_path, old_receipt_raw, _receipt_bytes(complete))
    return {"contract": CONTRACT, "appended": appended, "journal_sha256": current,
            "dispatch_authorized": False, "provider_called": False, "retry_authorized": False}
