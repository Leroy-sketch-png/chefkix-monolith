#!/usr/bin/env python3
"""Reconcile a synthetic/local C2 attempt ledger without dispatching requests.

The tool has no provider, credential, network, cost, or cohort-selection path.
Every event binds an opaque local case reference to one package manifest and
the exact request-body digest. It only reports whether an attempt may already
have been consumed; it never tells a caller to dispatch.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import re
import tempfile
import time
from contextlib import contextmanager
from pathlib import Path
from typing import Any


SHA256_RE = re.compile(r"[0-9a-f]{64}")
PREPARATION_ERROR_REASONS = {"TARGET_SLOT_MATCH_MULTIPLE"}
TERMINAL_FAILURE_CODES = {
    "transport_error", "timeout", "provider_error", "provider_refusal",
    "response_truncated", "response_invalid",
}
CASE_READY_FIELDS = {
    "case_id", "package_manifest_sha256", "request_body_sha256", "disposition",
}
CASE_ERROR_FIELDS = {
    "case_id", "package_manifest_sha256", "disposition", "reason_code",
}
EVENT_BASE_FIELDS = {
    "sequence", "event_id", "case_id", "package_manifest_sha256", "event_type",
}
EVENT_FIELDS = {
    "DISPATCH_INTENT": EVENT_BASE_FIELDS | {"request_body_sha256"},
    "RESPONSE_RECEIVED": EVENT_BASE_FIELDS | {"request_body_sha256", "response_sha256"},
    "TERMINAL_FAILURE": EVENT_BASE_FIELDS | {"request_body_sha256", "failure_code"},
}


class ReconcileError(ValueError):
    """Input is not a complete, hash-bound, one-attempt event journal."""


def _no_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ReconcileError("duplicate JSON object key")
        result[key] = value
    return result


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")


def jsonl_bytes(rows: list[dict[str, Any]]) -> bytes:
    return b"".join(canonical_json(row) + b"\n" for row in rows)


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def valid_sha256(value: Any) -> bool:
    return isinstance(value, str) and SHA256_RE.fullmatch(value) is not None


def read_jsonl(path: Path) -> tuple[list[dict[str, Any]], bytes]:
    try:
        raw = path.read_bytes()
        text = raw.decode("utf-8", errors="strict")
    except (OSError, UnicodeError):
        raise ReconcileError("JSONL input could not be read as UTF-8") from None
    if raw and (not text.endswith("\n") or "\r" in text):
        raise ReconcileError("journal must contain complete LF-terminated records")
    if not raw:
        return [], raw
    rows: list[dict[str, Any]] = []
    for line in text[:-1].split("\n"):
        if not line:
            raise ReconcileError("journal contains a blank or torn record")
        try:
            value = json.loads(line, object_pairs_hook=_no_duplicate_keys)
        except (json.JSONDecodeError, ReconcileError):
            raise ReconcileError("journal contains malformed or duplicate-key JSON") from None
        if not isinstance(value, dict):
            raise ReconcileError("journal records must be JSON objects")
        rows.append(value)
    return rows, raw


def _case_identity(record: dict[str, Any]) -> str:
    value = record.get("case_id")
    if not isinstance(value, str) or not value:
        raise ReconcileError("case identity must be a nonempty opaque local reference")
    return value


def reconcile_attempts(
    cases: list[dict[str, Any]],
    events: list[dict[str, Any]],
    package_manifest_sha256: str,
    *,
    expected_case_inventory_sha256: str,
    event_ledger_sha256: str | None = None,
) -> dict[str, Any]:
    """Return deterministic per-case state without granting dispatch permission."""
    if not valid_sha256(package_manifest_sha256):
        raise ReconcileError("package manifest digest must be lowercase SHA-256")
    case_inventory_sha256 = sha256_bytes(jsonl_bytes(cases))
    if not valid_sha256(expected_case_inventory_sha256) or case_inventory_sha256 != expected_case_inventory_sha256:
        raise ReconcileError("case inventory digest differs from its frozen package pin")
    case_index: dict[str, dict[str, Any]] = {}
    states: dict[str, str] = {}
    for case in cases:
        if not isinstance(case, dict):
            raise ReconcileError("case entries must be objects")
        identity = _case_identity(case)
        if identity in case_index:
            raise ReconcileError("duplicate case identity")
        if case.get("package_manifest_sha256") != package_manifest_sha256:
            raise ReconcileError("case package manifest binding mismatch")
        disposition = case.get("disposition")
        if disposition == "READY":
            if set(case) != CASE_READY_FIELDS or not valid_sha256(case.get("request_body_sha256")):
                raise ReconcileError("READY case has invalid fields or body digest")
            states[identity] = "NOT_STARTED"
        elif disposition == "PREPARATION_ERROR":
            reason_code = case.get("reason_code")
            if (set(case) != CASE_ERROR_FIELDS or not isinstance(reason_code, str)
                    or reason_code not in PREPARATION_ERROR_REASONS):
                raise ReconcileError("preparation-error case has invalid fields or reason")
            states[identity] = "PREPARATION_ERROR"
        else:
            raise ReconcileError("case disposition is not allowlisted")
        case_index[identity] = case

    event_ids: set[str] = set()
    last_sequence = 0
    for event in events:
        if not isinstance(event, dict):
            raise ReconcileError("event entries must be objects")
        event_type = event.get("event_type")
        if not isinstance(event_type, str):
            raise ReconcileError("event type is invalid")
        allowed_fields = EVENT_FIELDS.get(event_type)
        if allowed_fields is None or set(event) != allowed_fields:
            raise ReconcileError("event type or exact event fields are invalid")
        sequence = event.get("sequence")
        if isinstance(sequence, bool) or not isinstance(sequence, int) or sequence != last_sequence + 1:
            raise ReconcileError("event sequence is duplicated, missing, or out of order")
        last_sequence = sequence
        event_id = event.get("event_id")
        if not isinstance(event_id, str) or not event_id or event_id in event_ids:
            raise ReconcileError("event ID is empty or duplicated")
        event_ids.add(event_id)
        identity = event.get("case_id")
        case = case_index.get(identity) if isinstance(identity, str) else None
        if case is None:
            raise ReconcileError("event refers to an unknown case")
        if event.get("package_manifest_sha256") != package_manifest_sha256:
            raise ReconcileError("event package manifest binding mismatch")
        if case["disposition"] != "READY":
            raise ReconcileError("preparation-error case cannot have attempt events")
        if event.get("request_body_sha256") != case.get("request_body_sha256"):
            raise ReconcileError("event request-body digest differs from frozen case")
        state = states[identity]
        if event_type == "DISPATCH_INTENT":
            if state != "NOT_STARTED":
                raise ReconcileError("dispatch intent is invalid after a prior intent or terminal event")
            states[identity] = "UNCERTAIN_NO_RETRY"
        elif event_type == "RESPONSE_RECEIVED":
            if state != "UNCERTAIN_NO_RETRY" or not valid_sha256(event.get("response_sha256")):
                raise ReconcileError("response receipt lacks one prior intent or valid response digest")
            states[identity] = "RESPONSE_RECEIVED"
        else:
            failure_code = event.get("failure_code")
            if (state != "UNCERTAIN_NO_RETRY" or not isinstance(failure_code, str)
                    or failure_code not in TERMINAL_FAILURE_CODES):
                raise ReconcileError("terminal failure lacks one prior intent or safe failure code")
            states[identity] = "TERMINAL_FAILURE"

    if event_ledger_sha256 is not None and not valid_sha256(event_ledger_sha256):
        raise ReconcileError("event ledger digest must be lowercase SHA-256")
    if event_ledger_sha256 is None:
        event_ledger_sha256 = sha256_bytes(b"".join(canonical_json(event) + b"\n" for event in events))
    records = []
    for identity, case in case_index.items():
        state = states[identity]
        resume_action = {
            "PREPARATION_ERROR": "NOT_DISPATCHABLE",
            "NOT_STARTED": "NO_PRIOR_INTENT_REQUIRES_SEPARATE_AUTHORIZATION",
            "UNCERTAIN_NO_RETRY": "STOP_NO_RETRY_UNCERTAIN_OUTCOME",
            "RESPONSE_RECEIVED": "STOP_ATTEMPT_CONSUMED",
            "TERMINAL_FAILURE": "STOP_ATTEMPT_CONSUMED",
        }[state]
        records.append({
            "case_id": identity,
            "disposition": case["disposition"],
            "state": state,
            "resume_action": resume_action,
        })
    records.sort(key=lambda row: row["case_id"])
    counts: dict[str, int] = {}
    for row in records:
        counts[row["state"]] = counts.get(row["state"], 0) + 1
    return {
        "schema": "C2_ATTEMPT_RECONCILIATION_V1",
        "status": "LOCAL_PREPARATION_ONLY",
        "package_manifest_sha256": package_manifest_sha256,
        "case_inventory_sha256": case_inventory_sha256,
        "event_ledger_sha256": event_ledger_sha256,
        "event_count": len(events),
        "case_count": len(records),
        "state_counts": dict(sorted(counts.items())),
        "cases": records,
        "cohort_selected": False,
        "dispatch_authorized": False,
        "provider_called": False,
        "cost_or_reservation_inferred": False,
    }


def reconcile_files(
    cases_path: Path,
    events_path: Path,
    package_digest: str,
    expected_case_inventory_sha256: str,
) -> tuple[dict[str, Any], bytes]:
    cases, case_bytes = read_jsonl(cases_path)
    events, event_bytes = read_jsonl(events_path)
    if case_bytes != jsonl_bytes(cases):
        raise ReconcileError("case inventory is not in canonical JSONL form")
    if sha256_bytes(case_bytes) != expected_case_inventory_sha256:
        raise ReconcileError("case inventory file digest differs from its frozen package pin")
    report = reconcile_attempts(
        cases, events, package_digest,
        expected_case_inventory_sha256=expected_case_inventory_sha256,
        event_ledger_sha256=sha256_bytes(event_bytes),
    )
    return report, json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2).encode("utf-8") + b"\n"


def atomic_write_no_overwrite(path: Path, content: bytes) -> None:
    """Publish complete bytes atomically; an existing destination is never replaced."""
    path = path.resolve()
    if not path.parent.is_dir():
        raise ReconcileError("output parent directory does not exist")
    if path.exists():
        raise ReconcileError("output already exists; refusing overwrite")
    fd, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.pending-", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        # Hard-link creation is atomic and fails if the destination exists.
        os.link(temporary, path)
    except FileExistsError:
        raise ReconcileError("output appeared during write; refusing overwrite") from None
    except OSError as exc:
        raise ReconcileError(f"atomic no-overwrite publication failed: {exc.__class__.__name__}") from None
    finally:
        try:
            temporary.unlink(missing_ok=True)
        except OSError:
            pass


@contextmanager
def _cooperative_file_lock(lock_path: Path, timeout_seconds: float = 15.0):
    """Hold an OS-released cross-process lock for cooperating journal writers."""
    if (isinstance(timeout_seconds, bool) or not isinstance(timeout_seconds, (int, float))
            or not math.isfinite(timeout_seconds) or timeout_seconds <= 0):
        raise ReconcileError("lock timeout must be positive")
    lock_path = lock_path.resolve()
    if not lock_path.parent.is_dir():
        raise ReconcileError("journal parent directory does not exist")
    try:
        stream = lock_path.open("a+b")
    except OSError as exc:
        raise ReconcileError(f"journal lock could not be opened: {exc.__class__.__name__}") from None
    acquired = False
    try:
        # msvcrt byte-range locking needs a byte to exist in the lock file.
        if os.name == "nt" and stream.seek(0, os.SEEK_END) == 0:
            stream.write(b"\0")
            stream.flush()
        deadline = time.monotonic() + timeout_seconds
        while not acquired:
            try:
                if os.name == "nt":
                    import msvcrt

                    stream.seek(0)
                    msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
                else:
                    import fcntl

                    fcntl.flock(stream.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                acquired = True
            except (OSError, BlockingIOError):
                if time.monotonic() >= deadline:
                    raise ReconcileError("journal lock acquisition timed out") from None
                time.sleep(0.01)
        yield
    finally:
        if acquired:
            try:
                if os.name == "nt":
                    import msvcrt

                    stream.seek(0)
                    msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)
                else:
                    import fcntl

                    fcntl.flock(stream.fileno(), fcntl.LOCK_UN)
            except OSError as exc:
                # Even if close would release the OS lock, an explicit unlock
                # failure makes the operation's synchronization ambiguous.
                stream.close()
                raise ReconcileError(
                    f"journal lock release failed: {exc.__class__.__name__}"
                ) from None
        try:
            stream.close()
        except OSError as exc:
            raise ReconcileError(f"journal lock close failed: {exc.__class__.__name__}") from None


def append_attempt_event(
    cases_path: Path,
    events_path: Path,
    package_manifest_sha256: str,
    expected_case_inventory_sha256: str,
    *,
    expected_journal_sha256: str,
    case_id: str,
    event_id: str,
    event_type: str,
    response_sha256: str | None = None,
    failure_code: str | None = None,
    lock_timeout_seconds: float = 15.0,
) -> dict[str, Any]:
    """Durably append one synthetic/local attempt event; never authorizes dispatch.

    Every call is compare-and-append: a stale expected digest is rejected under
    the journal lock. Any write/sync/read-back ambiguity raises ReconcileError.
    The caller must stop on that error and must not retry with a stale digest.
    """
    if not valid_sha256(expected_journal_sha256):
        raise ReconcileError("expected journal digest must be lowercase SHA-256")
    if not isinstance(event_id, str) or not event_id.strip():
        raise ReconcileError("event ID must be a nonempty opaque reference")
    if not isinstance(case_id, str) or not case_id:
        raise ReconcileError("case identity must be a nonempty opaque local reference")
    if not isinstance(event_type, str) or event_type not in EVENT_FIELDS:
        raise ReconcileError("event type is not allowlisted")
    if not cases_path.is_file():
        raise ReconcileError("frozen case inventory is unavailable")
    if not events_path.parent.is_dir():
        raise ReconcileError("journal parent directory does not exist")

    lock_path = events_path.with_name(events_path.name + ".lock")
    with _cooperative_file_lock(lock_path, lock_timeout_seconds):
        cases, case_bytes = read_jsonl(cases_path)
        if case_bytes != jsonl_bytes(cases):
            raise ReconcileError("case inventory is not canonical JSONL")
        if sha256_bytes(case_bytes) != expected_case_inventory_sha256:
            raise ReconcileError("case inventory digest differs from its frozen package pin")
        if events_path.exists():
            events, prefix = read_jsonl(events_path)
            if prefix != jsonl_bytes(events):
                raise ReconcileError("journal prefix is not canonical JSONL")
        else:
            events, prefix = [], b""
        current_digest = sha256_bytes(prefix)
        if current_digest != expected_journal_sha256:
            raise ReconcileError("journal digest changed; refresh state, do not retry stale append")

        prior = reconcile_attempts(
            cases, events, package_manifest_sha256,
            expected_case_inventory_sha256=expected_case_inventory_sha256,
            event_ledger_sha256=current_digest,
        )
        case = next((row for row in cases if row.get("case_id") == case_id), None)
        if case is None:
            raise ReconcileError("event refers to an unknown case")
        if case.get("disposition") != "READY":
            raise ReconcileError("preparation-error case cannot have attempt events")

        event: dict[str, Any] = {
            "sequence": len(events) + 1,
            "event_id": event_id,
            "case_id": case_id,
            "package_manifest_sha256": package_manifest_sha256,
            "event_type": event_type,
            "request_body_sha256": case["request_body_sha256"],
        }
        if event_type == "RESPONSE_RECEIVED":
            if not valid_sha256(response_sha256) or failure_code is not None:
                raise ReconcileError("response receipt requires a lowercase SHA-256 digest")
            event["response_sha256"] = response_sha256
        elif event_type == "TERMINAL_FAILURE":
            if failure_code not in TERMINAL_FAILURE_CODES or response_sha256 is not None:
                raise ReconcileError("terminal failure code is not allowlisted")
            event["failure_code"] = failure_code
        elif response_sha256 is not None or failure_code is not None:
            raise ReconcileError("event has fields not valid for its type")

        # Validate transition before touching disk. Reconciliation also enforces
        # exact event fields and one-attempt/no-retry state semantics.
        next_events = events + [event]
        reconcile_attempts(
            cases, next_events, package_manifest_sha256,
            expected_case_inventory_sha256=expected_case_inventory_sha256,
        )
        event_bytes = canonical_json(event) + b"\n"
        flags = os.O_WRONLY | os.O_CREAT | os.O_APPEND | getattr(os, "O_BINARY", 0)
        try:
            descriptor = os.open(events_path, flags, 0o600)
        except OSError as exc:
            raise ReconcileError(f"journal could not be opened for append: {exc.__class__.__name__}") from None
        try:
            written = os.write(descriptor, event_bytes)
            if written != len(event_bytes):
                raise ReconcileError("journal append was partial; outcome is uncertain, stop")
            os.fsync(descriptor)
        except OSError as exc:
            raise ReconcileError(f"journal write or fsync failed; outcome is uncertain: {exc.__class__.__name__}") from None
        finally:
            try:
                os.close(descriptor)
            except OSError as exc:
                raise ReconcileError(f"journal descriptor close failed; outcome is uncertain: {exc.__class__.__name__}") from None

        try:
            actual = events_path.read_bytes()
        except OSError as exc:
            raise ReconcileError(f"journal read-back failed after append: {exc.__class__.__name__}") from None
        expected_bytes = prefix + event_bytes
        if actual != expected_bytes:
            raise ReconcileError("journal read-back differs from exact prefix plus event; stop")
        actual_events, parsed_bytes = read_jsonl(events_path)
        if parsed_bytes != expected_bytes:
            raise ReconcileError("journal read-back is not a complete canonical record; stop")
        verified = reconcile_attempts(
            cases, actual_events, package_manifest_sha256,
            expected_case_inventory_sha256=expected_case_inventory_sha256,
            event_ledger_sha256=sha256_bytes(actual),
        )
        if (verified["event_count"] != prior["event_count"] + 1
                or verified["case_count"] != prior["case_count"]
                or verified["dispatch_authorized"]):
            raise ReconcileError("post-append state verification failed; stop")
        return {
            "status": "LOCAL_PREPARATION_ONLY",
            "event_ledger_sha256": sha256_bytes(actual),
            "event_count": verified["event_count"],
            "case_count": verified["case_count"],
            "dispatch_authorized": False,
            "provider_called": False,
        }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cases", type=Path, required=True)
    parser.add_argument("--events", type=Path, required=True)
    parser.add_argument("--package-manifest-sha256", required=True)
    parser.add_argument("--expected-case-inventory-sha256", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        report, content = reconcile_files(
            args.cases, args.events, args.package_manifest_sha256,
            args.expected_case_inventory_sha256,
        )
        atomic_write_no_overwrite(args.output, content)
    except (OSError, ReconcileError) as exc:
        parser.exit(1, f"C2 resume reconciliation stopped: {exc}\n")
    print(json.dumps({
        "status": report["status"],
        "case_count": report["case_count"],
        "event_count": report["event_count"],
        "dispatch_authorized": report["dispatch_authorized"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
