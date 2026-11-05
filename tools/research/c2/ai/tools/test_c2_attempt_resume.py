"""Synthetic-only tests for the local C2 attempt-state reconciler."""

from __future__ import annotations

import hashlib
import json
import multiprocessing
import os
from pathlib import Path

import pytest

import c2_attempt_resume as tool

PACKAGE = "a" * 64
BODY_READY = "b" * 64
BODY_SECOND = "c" * 64
RESPONSE = "d" * 64


def cases():
    return [
        {"case_id": "synthetic-case-a", "package_manifest_sha256": PACKAGE,
         "request_body_sha256": BODY_READY, "disposition": "READY"},
        {"case_id": "synthetic-case-b", "package_manifest_sha256": PACKAGE,
         "request_body_sha256": BODY_SECOND, "disposition": "READY"},
        {"case_id": "synthetic-case-c", "package_manifest_sha256": PACKAGE,
         "disposition": "PREPARATION_ERROR", "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"},
    ]


def event(seq, case_id, event_type, **extra):
    body = {"synthetic-case-a": BODY_READY, "synthetic-case-b": BODY_SECOND}[case_id]
    return {
        "sequence": seq,
        "event_id": f"synthetic-event-{seq}",
        "case_id": case_id,
        "package_manifest_sha256": PACKAGE,
        "request_body_sha256": body,
        "event_type": event_type,
        **extra,
    }


def reconcile(case_rows, event_rows, package=PACKAGE, *, expected_case_inventory_sha256=None, **kwargs):
    canonical_inventory = b"".join(tool.canonical_json(row) + b"\n" for row in case_rows)
    expected = expected_case_inventory_sha256 or hashlib.sha256(canonical_inventory).hexdigest()
    return tool.reconcile_attempts(
        case_rows, event_rows, package,
        expected_case_inventory_sha256=expected, **kwargs,
    )


def test_crash_before_intent_and_preparation_error_preserve_denominator():
    result = reconcile(cases(), [], PACKAGE)
    assert result["case_count"] == 3
    assert result["state_counts"] == {"NOT_STARTED": 2, "PREPARATION_ERROR": 1}
    states = {row["case_id"]: row["state"] for row in result["cases"]}
    assert states["synthetic-case-a"] == "NOT_STARTED"
    assert states["synthetic-case-c"] == "PREPARATION_ERROR"
    assert result["dispatch_authorized"] is False
    assert result["provider_called"] is False


def test_crash_after_intent_is_uncertain_and_never_offers_retry():
    result = reconcile(
        cases(), [event(1, "synthetic-case-a", "DISPATCH_INTENT")], PACKAGE
    )
    row = next(row for row in result["cases"] if row["case_id"] == "synthetic-case-a")
    assert row["state"] == "UNCERTAIN_NO_RETRY"
    assert row["resume_action"] == "STOP_NO_RETRY_UNCERTAIN_OUTCOME"


@pytest.mark.parametrize(
    ("terminal", "expected"),
    [
        (event(2, "synthetic-case-a", "RESPONSE_RECEIVED", response_sha256=RESPONSE),
         "RESPONSE_RECEIVED"),
        (event(2, "synthetic-case-a", "TERMINAL_FAILURE", failure_code="timeout"),
         "TERMINAL_FAILURE"),
    ],
)
def test_terminal_response_or_failure_consumes_the_one_attempt(terminal, expected):
    result = reconcile(
        cases(), [event(1, "synthetic-case-a", "DISPATCH_INTENT"), terminal], PACKAGE
    )
    row = next(row for row in result["cases"] if row["case_id"] == "synthetic-case-a")
    assert row["state"] == expected
    assert row["resume_action"] == "STOP_ATTEMPT_CONSUMED"


@pytest.mark.parametrize(
    "events",
    [
        [event(1, "synthetic-case-a", "RESPONSE_RECEIVED", response_sha256=RESPONSE)],
        [event(1, "synthetic-case-a", "TERMINAL_FAILURE", failure_code="timeout")],
        [event(1, "synthetic-case-a", "DISPATCH_INTENT"),
         event(2, "synthetic-case-a", "DISPATCH_INTENT")],
        [event(1, "synthetic-case-a", "DISPATCH_INTENT"),
         event(2, "synthetic-case-a", "RESPONSE_RECEIVED", response_sha256=RESPONSE),
         event(3, "synthetic-case-a", "TERMINAL_FAILURE", failure_code="timeout")],
        [event(2, "synthetic-case-a", "DISPATCH_INTENT")],
        [event(1, "synthetic-case-a", "DISPATCH_INTENT"),
         event(3, "synthetic-case-b", "DISPATCH_INTENT")],
    ],
)
def test_invalid_transitions_duplicate_intents_terminal_or_sequence_are_rejected(events):
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), events, PACKAGE)


def test_duplicate_and_unknown_cases_or_event_ids_are_rejected():
    rows = cases()
    with pytest.raises(tool.ReconcileError):
        reconcile(rows + [rows[0]], [], PACKAGE)
    unknown_event = event(1, "synthetic-case-a", "DISPATCH_INTENT")
    unknown_event["case_id"] = "synthetic-case-z"
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), [unknown_event], PACKAGE)
    duplicate_event_id = event(2, "synthetic-case-b", "DISPATCH_INTENT")
    duplicate_event_id["event_id"] = "synthetic-event-1"
    with pytest.raises(tool.ReconcileError):
        reconcile(
            cases(), [event(1, "synthetic-case-a", "DISPATCH_INTENT"), duplicate_event_id], PACKAGE
        )


def test_missing_case_from_frozen_inventory_is_rejected_by_digest():
    full = cases()
    full_bytes = b"".join(tool.canonical_json(row) + b"\n" for row in full)
    frozen_digest = hashlib.sha256(full_bytes).hexdigest()
    with pytest.raises(tool.ReconcileError, match="inventory digest"):
        reconcile(full[:-1], [], PACKAGE, expected_case_inventory_sha256=frozen_digest)


def test_preparation_error_cannot_have_an_attempt_event():
    bad = {
        "sequence": 1, "event_id": "synthetic-event-1", "case_id": "synthetic-case-c",
        "package_manifest_sha256": PACKAGE, "event_type": "DISPATCH_INTENT",
        "request_body_sha256": BODY_READY,
    }
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), [bad], PACKAGE)


def test_package_body_and_terminal_receipt_hash_drift_are_rejected():
    wrong_package = event(1, "synthetic-case-a", "DISPATCH_INTENT")
    wrong_package["package_manifest_sha256"] = "f" * 64
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), [wrong_package], PACKAGE)
    wrong_body = event(1, "synthetic-case-a", "DISPATCH_INTENT")
    wrong_body["request_body_sha256"] = "f" * 64
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), [wrong_body], PACKAGE)
    invalid_receipt = [event(1, "synthetic-case-a", "DISPATCH_INTENT"),
                       event(2, "synthetic-case-a", "RESPONSE_RECEIVED", response_sha256="bad")]
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), invalid_receipt, PACKAGE)


def test_exact_record_fields_reject_authorization_claims_and_untrusted_codes():
    claimed = event(1, "synthetic-case-a", "DISPATCH_INTENT")
    claimed["dispatch_authorized"] = True
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), [claimed], PACKAGE)
    unsafe = [event(1, "synthetic-case-a", "DISPATCH_INTENT"),
              event(2, "synthetic-case-a", "TERMINAL_FAILURE", failure_code="retry_now")]
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), unsafe, PACKAGE)
    bad_case = cases()[0] | {"dispatch_authorized": True}
    with pytest.raises(tool.ReconcileError):
        reconcile([bad_case], [], PACKAGE)
    malformed_type = event(1, "synthetic-case-a", "DISPATCH_INTENT")
    malformed_type["event_type"] = []
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), [malformed_type], PACKAGE)
    malformed_reason = cases()[2].copy()
    malformed_reason["reason_code"] = []
    with pytest.raises(tool.ReconcileError):
        reconcile([malformed_reason], [], PACKAGE)
    malformed_failure = [event(1, "synthetic-case-a", "DISPATCH_INTENT"),
                         event(2, "synthetic-case-a", "TERMINAL_FAILURE", failure_code=[])]
    with pytest.raises(tool.ReconcileError):
        reconcile(cases(), malformed_failure, PACKAGE)


def test_torn_and_duplicate_key_journal_records_are_rejected(tmp_path: Path):
    torn = tmp_path / "torn.jsonl"
    torn.write_bytes(b'{"event":true}')
    with pytest.raises(tool.ReconcileError, match="LF-terminated"):
        tool.read_jsonl(torn)
    duplicate = tmp_path / "duplicate.jsonl"
    duplicate.write_bytes(b'{"a":1,"a":2}\n')
    with pytest.raises(tool.ReconcileError, match="duplicate-key"):
        tool.read_jsonl(duplicate)
    malformed = tmp_path / "malformed.jsonl"
    malformed.write_bytes(b'{oops}\n')
    with pytest.raises(tool.ReconcileError, match="malformed"):
        tool.read_jsonl(malformed)


def test_output_bytes_are_deterministic_and_published_without_overwrite(tmp_path: Path):
    first = reconcile(cases(), [event(1, "synthetic-case-a", "DISPATCH_INTENT")], PACKAGE)
    second = reconcile(cases(), [event(1, "synthetic-case-a", "DISPATCH_INTENT")], PACKAGE)
    content_a = json.dumps(first, ensure_ascii=False, sort_keys=True, indent=2).encode() + b"\n"
    content_b = json.dumps(second, ensure_ascii=False, sort_keys=True, indent=2).encode() + b"\n"
    assert content_a == content_b
    target = tmp_path / "receipt.json"
    tool.atomic_write_no_overwrite(target, content_a)
    assert target.read_bytes() == content_a
    with pytest.raises(tool.ReconcileError, match="already exists"):
        tool.atomic_write_no_overwrite(target, b"replacement")
    assert target.read_bytes() == content_a
    assert not list(tmp_path.glob(".receipt.json.pending-*"))


def test_empty_ledger_has_stable_sha_and_no_implicit_dispatch():
    report = reconcile(cases(), [], PACKAGE, event_ledger_sha256=hashlib.sha256(b"").hexdigest())
    assert report["event_count"] == 0
    assert report["event_ledger_sha256"] == hashlib.sha256(b"").hexdigest()
    assert report["dispatch_authorized"] is False
    assert report["cost_or_reservation_inferred"] is False


def test_file_reconciliation_hashes_exact_ledger_bytes_and_writes_only_synthetic_summary(tmp_path: Path):
    case_rows = cases()
    event_rows = [event(1, "synthetic-case-a", "DISPATCH_INTENT")]
    cases_path = tmp_path / "synthetic-cases.jsonl"
    events_path = tmp_path / "synthetic-events.jsonl"
    cases_bytes = b"".join(tool.canonical_json(row) + b"\n" for row in case_rows)
    event_bytes = b"".join(tool.canonical_json(row) + b"\n" for row in event_rows)
    cases_path.write_bytes(cases_bytes)
    events_path.write_bytes(event_bytes)

    report, report_bytes = tool.reconcile_files(
        cases_path, events_path, PACKAGE, hashlib.sha256(cases_bytes).hexdigest()
    )
    written = tmp_path / "synthetic-report.json"
    tool.atomic_write_no_overwrite(written, report_bytes)
    decoded = json.loads(written.read_text(encoding="utf-8"))
    assert decoded == report
    assert report["event_ledger_sha256"] == hashlib.sha256(event_bytes).hexdigest()
    assert report["case_count"] == 3
    assert report["dispatch_authorized"] is False
    assert "request_body" not in written.read_text(encoding="utf-8")


def _concurrent_append_worker(cases_path, events_path, inventory_sha, expected_journal_sha, event_id, result_queue):
    try:
        result = tool.append_attempt_event(
            Path(cases_path), Path(events_path), PACKAGE, inventory_sha,
            expected_journal_sha256=expected_journal_sha,
            case_id="synthetic-case-a", event_id=event_id,
            event_type="DISPATCH_INTENT", lock_timeout_seconds=5,
        )
        result_queue.put(("success", result["event_ledger_sha256"]))
    except tool.ReconcileError as exc:
        result_queue.put(("stopped", str(exc)))


def _write_synthetic_inventory(path: Path):
    rows = cases()
    raw = b"".join(tool.canonical_json(row) + b"\n" for row in rows)
    path.write_bytes(raw)
    return hashlib.sha256(raw).hexdigest()


def test_wal_competing_processes_with_same_digest_allow_exactly_one_append(tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    expected = hashlib.sha256(b"").hexdigest()
    context = multiprocessing.get_context("spawn")
    result_queue = context.Queue()
    workers = [
        context.Process(
            target=_concurrent_append_worker,
            args=(str(case_path), str(event_path), inventory_sha, expected,
                  f"synthetic-race-{index}", result_queue),
        )
        for index in (1, 2)
    ]
    for worker in workers:
        worker.start()
    for worker in workers:
        worker.join(10)
    assert all(worker.exitcode == 0 for worker in workers)
    results = [result_queue.get(timeout=2) for _ in workers]
    assert [kind for kind, _ in results].count("success") == 1
    assert [kind for kind, _ in results].count("stopped") == 1
    events, raw = tool.read_jsonl(event_path)
    assert len(events) == 1
    assert raw == tool.jsonl_bytes(events)
    report = tool.reconcile_attempts(
        cases(), events, PACKAGE,
        expected_case_inventory_sha256=inventory_sha,
    )
    assert report["case_count"] == 3
    assert report["dispatch_authorized"] is False


def test_wal_fsync_is_requested_after_append_before_success(tmp_path: Path, monkeypatch):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    order = []
    real_write, real_fsync = os.write, os.fsync

    def observed_write(fd, payload):
        result = real_write(fd, payload)
        order.append("write")
        return result

    def observed_fsync(fd):
        order.append("fsync")
        return real_fsync(fd)

    monkeypatch.setattr(tool.os, "write", observed_write)
    monkeypatch.setattr(tool.os, "fsync", observed_fsync)
    result = tool.append_attempt_event(
        case_path, event_path, PACKAGE, inventory_sha,
        expected_journal_sha256=hashlib.sha256(b"").hexdigest(),
        case_id="synthetic-case-a", event_id="synthetic-event-wal-1",
        event_type="DISPATCH_INTENT",
    )
    assert order == ["write", "fsync"]
    assert result["dispatch_authorized"] is False
    assert result["provider_called"] is False


def test_wal_failure_before_append_keeps_empty_ledger_and_stops(monkeypatch, tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)

    def fail_write(_fd, _payload):
        raise OSError("synthetic write failure")

    monkeypatch.setattr(tool.os, "write", fail_write)
    with pytest.raises(tool.ReconcileError, match="write or fsync failed"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=hashlib.sha256(b"").hexdigest(),
            case_id="synthetic-case-a", event_id="synthetic-event-before",
            event_type="DISPATCH_INTENT",
        )
    assert not event_path.exists() or event_path.read_bytes() == b""


def test_wal_fsync_failure_after_append_is_uncertain_and_stale_digest_cannot_retry(monkeypatch, tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    expected = hashlib.sha256(b"").hexdigest()

    def fail_fsync(_fd):
        raise OSError("synthetic fsync failure")

    monkeypatch.setattr(tool.os, "fsync", fail_fsync)
    with pytest.raises(tool.ReconcileError, match="uncertain"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=expected,
            case_id="synthetic-case-a", event_id="synthetic-event-after",
            event_type="DISPATCH_INTENT",
        )
    events, raw = tool.read_jsonl(event_path)
    assert len(events) == 1
    report = tool.reconcile_attempts(
        cases(), events, PACKAGE, expected_case_inventory_sha256=inventory_sha,
        event_ledger_sha256=hashlib.sha256(raw).hexdigest(),
    )
    row = next(row for row in report["cases"] if row["case_id"] == "synthetic-case-a")
    assert row["state"] == "UNCERTAIN_NO_RETRY"
    with pytest.raises(tool.ReconcileError, match="digest changed"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=expected,
            case_id="synthetic-case-a", event_id="synthetic-event-stale-retry",
            event_type="DISPATCH_INTENT",
        )
    assert len(tool.read_jsonl(event_path)[0]) == 1


def test_wal_readback_failure_after_append_is_ambiguous_and_never_returns_success(monkeypatch, tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    real_read_bytes = Path.read_bytes

    def fail_event_readback(path):
        if path == event_path:
            raise OSError("synthetic readback failure")
        return real_read_bytes(path)

    monkeypatch.setattr(Path, "read_bytes", fail_event_readback)
    with pytest.raises(tool.ReconcileError, match="read-back failed"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=hashlib.sha256(b"").hexdigest(),
            case_id="synthetic-case-a", event_id="synthetic-event-readback",
            event_type="DISPATCH_INTENT",
        )
    monkeypatch.setattr(Path, "read_bytes", real_read_bytes)
    events, _raw = tool.read_jsonl(event_path)
    assert len(events) == 1


def test_wal_rejects_invalid_transition_without_modifying_immutable_prefix(tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    first = tool.append_attempt_event(
        case_path, event_path, PACKAGE, inventory_sha,
        expected_journal_sha256=hashlib.sha256(b"").hexdigest(),
        case_id="synthetic-case-a", event_id="synthetic-event-prefix",
        event_type="DISPATCH_INTENT",
    )
    prefix = event_path.read_bytes()
    with pytest.raises(tool.ReconcileError, match="prior intent"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=first["event_ledger_sha256"],
            case_id="synthetic-case-a", event_id="synthetic-event-duplicate",
            event_type="DISPATCH_INTENT",
        )
    assert event_path.read_bytes() == prefix


def test_wal_partial_write_preserves_torn_evidence_and_refuses_followup(monkeypatch, tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    real_write = os.write

    def partial_write(fd, payload):
        real_write(fd, payload[:5])
        return 5

    monkeypatch.setattr(tool.os, "write", partial_write)
    with pytest.raises(tool.ReconcileError, match="partial"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=hashlib.sha256(b"").hexdigest(),
            case_id="synthetic-case-a", event_id="synthetic-event-torn",
            event_type="DISPATCH_INTENT",
        )
    assert event_path.read_bytes() == b'{"cas'
    monkeypatch.setattr(tool.os, "write", real_write)
    with pytest.raises(tool.ReconcileError, match="LF-terminated"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=hashlib.sha256(event_path.read_bytes()).hexdigest(),
            case_id="synthetic-case-a", event_id="synthetic-event-no-retry",
            event_type="DISPATCH_INTENT",
        )


def test_wal_refuses_preexisting_torn_journal_without_mutation(tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    torn = b'{"sequence":1'
    event_path.write_bytes(torn)
    with pytest.raises(tool.ReconcileError, match="LF-terminated"):
        tool.append_attempt_event(
            case_path, event_path, PACKAGE, inventory_sha,
            expected_journal_sha256=hashlib.sha256(torn).hexdigest(),
            case_id="synthetic-case-a", event_id="synthetic-event-noappend",
            event_type="DISPATCH_INTENT",
        )
    assert event_path.read_bytes() == torn


def test_wal_persists_terminal_receipts_and_keeps_preparation_error_denominator(tmp_path: Path):
    case_path = tmp_path / "synthetic-cases.jsonl"
    event_path = tmp_path / "synthetic-events.jsonl"
    inventory_sha = _write_synthetic_inventory(case_path)
    journal_sha = hashlib.sha256(b"").hexdigest()
    common = dict(
        cases_path=case_path, events_path=event_path,
        package_manifest_sha256=PACKAGE,
        expected_case_inventory_sha256=inventory_sha,
    )
    first = tool.append_attempt_event(
        **common, expected_journal_sha256=journal_sha,
        case_id="synthetic-case-a", event_id="synthetic-event-terminal-1",
        event_type="DISPATCH_INTENT",
    )
    prefix_after_first = event_path.read_bytes()
    second = tool.append_attempt_event(
        **common, expected_journal_sha256=first["event_ledger_sha256"],
        case_id="synthetic-case-a", event_id="synthetic-event-terminal-2",
        event_type="RESPONSE_RECEIVED", response_sha256=RESPONSE,
    )
    assert event_path.read_bytes().startswith(prefix_after_first)
    third = tool.append_attempt_event(
        **common, expected_journal_sha256=second["event_ledger_sha256"],
        case_id="synthetic-case-b", event_id="synthetic-event-terminal-3",
        event_type="DISPATCH_INTENT",
    )
    fourth = tool.append_attempt_event(
        **common, expected_journal_sha256=third["event_ledger_sha256"],
        case_id="synthetic-case-b", event_id="synthetic-event-terminal-4",
        event_type="TERMINAL_FAILURE", failure_code="timeout",
    )
    events, raw = tool.read_jsonl(event_path)
    report = tool.reconcile_attempts(
        cases(), events, PACKAGE, expected_case_inventory_sha256=inventory_sha,
        event_ledger_sha256=hashlib.sha256(raw).hexdigest(),
    )
    states = {row["case_id"]: row["state"] for row in report["cases"]}
    assert report["case_count"] == 3
    assert states == {
        "synthetic-case-a": "RESPONSE_RECEIVED",
        "synthetic-case-b": "TERMINAL_FAILURE",
        "synthetic-case-c": "PREPARATION_ERROR",
    }
    assert fourth["dispatch_authorized"] is False
    assert fourth["provider_called"] is False
