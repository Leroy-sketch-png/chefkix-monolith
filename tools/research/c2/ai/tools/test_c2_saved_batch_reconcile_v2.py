from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

import pytest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
sys.path.insert(0, str(HERE))
import c2_attempt_resume as t219
import c2_saved_batch_reconcile_v2 as v2


def _body():
    text = json.dumps({"substitutions": [f"candidate-{i:02d}" for i in range(20)]}, separators=(",", ":"))
    return {"status": "completed", "output": [{"type": "message", "role": "assistant",
            "status": "completed", "content": [{"type": "output_text", "text": text}]}]}


def test_response_validation_returns_only_digest_and_fixed_top20_disposition():
    record = v2.validate_response(_body(), case_id="opaque-local-case", request_body_sha256="a" * 64)
    assert record["disposition"] == "RESPONSE_VALID"
    assert record["response_sha256"] == hashlib.sha256(v2.canonical(_body())).hexdigest()
    assert "candidate-00" not in json.dumps(record)
    assert set(record) == {"case_id", "disposition", "response_sha256", "request_body_sha256"}


@pytest.mark.parametrize("body,code", [
    ({"status": "incomplete", "output": []}, "response_truncated"),
    ({"status": "completed", "output": []}, "response_invalid"),
    ({"status": "completed", "output": [{"type": "message", "role": "assistant",
      "status": "completed", "content": [{"type": "output_text", "text": "not-json"}]}]}, "response_invalid"),
])
def test_invalid_or_truncated_response_is_a_digest_only_terminal_record(body, code):
    record = v2.validate_response(body, case_id="case", request_body_sha256="b" * 64)
    assert record["disposition"] == "TERMINAL_FAILURE"
    assert record["failure_code"] == code
    assert len(record["response_sha256"]) == 64
    assert "output" not in record and "candidate" not in json.dumps(record)


def test_duplicate_json_properties_are_rejected():
    with pytest.raises(v2.ReconcileError, match="malformed UTF-8 JSON"):
        v2._json(b'{"status":"completed","status":"incomplete"}')


def test_fixed_denominator_retains_all_preparation_errors():
    cases = ([{"disposition": "READY"}] * 10_499
             + [{"disposition": "PREPARATION_ERROR"}] * 248)
    assert v2._case_counts(cases) == v2.FROZEN_COUNTS
    assert v2._case_counts(cases[:-1]) != v2.FROZEN_COUNTS


def test_uncertain_intent_stops_and_saved_event_replay_is_noop(tmp_path):
    package = "c" * 64
    cases = [
        {"case_id": "case-ready", "package_manifest_sha256": package,
         "request_body_sha256": "d" * 64, "disposition": "READY"},
        {"case_id": "case-error", "package_manifest_sha256": package,
         "disposition": "PREPARATION_ERROR", "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"},
    ]
    cases_path, events_path = tmp_path / "cases.jsonl", tmp_path / "events.jsonl"
    cases_path.write_bytes(t219.jsonl_bytes(cases))
    case_hash = hashlib.sha256(cases_path.read_bytes()).hexdigest()
    intent = t219.append_attempt_event(cases_path, events_path, package, case_hash,
        expected_journal_sha256=hashlib.sha256(b"").hexdigest(), case_id="case-ready",
        event_id="intent-once", event_type="DISPATCH_INTENT")
    uncertain = t219.reconcile_files(cases_path, events_path, package, case_hash)[0]
    assert uncertain["dispatch_authorized"] is False
    assert next(row for row in uncertain["cases"] if row["case_id"] == "case-ready")["resume_action"] == "STOP_NO_RETRY_UNCERTAIN_OUTCOME"

def test_changed_bundle_is_refused_before_resuming_started_ingestion(tmp_path):
    package = "c" * 64
    cases = [{"case_id": "case-ready", "package_manifest_sha256": package,
              "request_body_sha256": "d" * 64, "disposition": "READY"}]
    cases_path, events_path = tmp_path / "cases.jsonl", tmp_path / "events.jsonl"
    cases_path.write_bytes(t219.jsonl_bytes(cases))
    case_hash = hashlib.sha256(cases_path.read_bytes()).hexdigest()
    intent = t219.append_attempt_event(cases_path, events_path, package, case_hash,
        expected_journal_sha256=hashlib.sha256(b"").hexdigest(), case_id="case-ready",
        event_id="intent-once", event_type="DISPATCH_INTENT")
    receipt_path = tmp_path / "ingestion.json"
    output_path = tmp_path / "saved-output.jsonl"
    output_path.write_bytes(b"synthetic\n")
    started = {"schema": v2.CONTRACT, "state": "STARTED", "bundle_sha256": "e" * 64,
        "package_manifest_sha256": package, "case_inventory_sha256": case_hash,
        "base_journal_sha256": intent["event_ledger_sha256"], "final_journal_sha256": None,
        "result_count": v2.FROZEN_COUNTS["ready_requests"]}
    v2._create_receipt_no_overwrite(receipt_path, v2._receipt_bytes(started))
    validation = {"contract": v2.CONTRACT, "bundle_sha256": "1" * 64,
        "package_manifest_sha256": package, "case_inventory_sha256": case_hash,
        "journal_sha256": intent["event_ledger_sha256"], "dispatch_authorized": False,
        "retry_authorized": False, "denominator": 10_747, "ready": 10_499,
        "preparation_errors": 248, "result_count": 10_499,
        "results": [{"case_id": "case-ready", "event_type": "TERMINAL_FAILURE",
                     "failure_code": "provider_error"}] * 10_499}
    before = events_path.read_bytes()
    with pytest.raises(v2.ReconcileError, match="differs from durable ingestion binding"):
        v2.append_result_events(root=ROOT, cases_path=cases_path, events_path=events_path,
            package_manifest_sha256=package, case_inventory_sha256=case_hash,
            expected_journal_sha256=intent["event_ledger_sha256"], validation=validation,
            bundle_sha256="1" * 64, ingestion_receipt_path=receipt_path,
            source_artifact_paths=[output_path])
    assert events_path.read_bytes() == before


def test_full_fixed_denominator_saved_bundle_is_atomic_and_exact_replay_is_noop(tmp_path):
    package = "c" * 64
    cases = [{"case_id": f"ready-{i:05d}", "package_manifest_sha256": package,
              "request_body_sha256": "d" * 64, "disposition": "READY"}
             for i in range(10_499)]
    cases += [{"case_id": f"error-{i:03d}", "package_manifest_sha256": package,
               "disposition": "PREPARATION_ERROR", "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"}
              for i in range(248)]
    cases_path, events_path = tmp_path / "cases.jsonl", tmp_path / "events.jsonl"
    cases_raw = t219.jsonl_bytes(cases)
    cases_path.write_bytes(cases_raw)
    case_hash = hashlib.sha256(cases_raw).hexdigest()
    intents = [{"sequence": i + 1, "event_id": f"intent-{i:05d}", "case_id": row["case_id"],
                "package_manifest_sha256": package, "event_type": "DISPATCH_INTENT",
                "request_body_sha256": row["request_body_sha256"]}
               for i, row in enumerate(cases[:10_499])]
    journal_raw = t219.jsonl_bytes(intents)
    events_path.write_bytes(journal_raw)
    journal_hash = hashlib.sha256(journal_raw).hexdigest()
    results = [{"case_id": row["case_id"], "event_type": "TERMINAL_FAILURE",
                "failure_code": "provider_error"} for row in cases[:10_499]]
    validation = {"contract": v2.CONTRACT, "bundle_sha256": "e" * 64,
        "package_manifest_sha256": package, "case_inventory_sha256": case_hash,
        "journal_sha256": journal_hash, "dispatch_authorized": False,
        "retry_authorized": False, "denominator": 10_747, "ready": 10_499,
        "preparation_errors": 248, "result_count": 10_499, "results": results}
    receipt_path = tmp_path / "ingestion.json"
    output_path = tmp_path / "saved-output.jsonl"
    output_path.write_bytes(b"synthetic\n")
    first = v2.append_result_events(root=ROOT, cases_path=cases_path, events_path=events_path,
        package_manifest_sha256=package, case_inventory_sha256=case_hash,
        expected_journal_sha256=journal_hash, validation=validation,
        bundle_sha256="e" * 64, ingestion_receipt_path=receipt_path,
        source_artifact_paths=[output_path])
    assert first["appended"] == 10_499
    assert first["dispatch_authorized"] is False and first["retry_authorized"] is False
    assert json.loads(receipt_path.read_text(encoding="utf-8"))["state"] == "COMPLETE"
    before = events_path.read_bytes()
    replay = v2.append_result_events(root=ROOT, cases_path=cases_path, events_path=events_path,
        package_manifest_sha256=package, case_inventory_sha256=case_hash,
        expected_journal_sha256=first["journal_sha256"],
        validation=dict(validation, journal_sha256=first["journal_sha256"]),
        bundle_sha256="e" * 64, ingestion_receipt_path=receipt_path,
        source_artifact_paths=[output_path])
    assert replay["appended"] == 0
    assert events_path.read_bytes() == before
    report = t219.reconcile_attempts(cases, t219.read_jsonl(events_path)[0], package,
        expected_case_inventory_sha256=case_hash)
    assert report["case_count"] == 10_747
    assert report["state_counts"]["PREPARATION_ERROR"] == 248
    assert report["state_counts"]["TERMINAL_FAILURE"] == 10_499


def test_ingestion_receipt_cannot_overwrite_saved_source_artifact(tmp_path):
    saved_output = tmp_path / "output.jsonl"
    saved_output.write_bytes(b"immutable fixture")
    cases = [{"case_id": f"case-{i:05d}", "disposition": "READY",
              "package_manifest_sha256": "a" * 64, "request_body_sha256": "d" * 64}
             for i in range(10_499)]
    cases += [{"case_id": f"prep-{i:03d}", "disposition": "PREPARATION_ERROR",
               "package_manifest_sha256": "a" * 64, "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"}
              for i in range(248)]
    cases_path = tmp_path / "cases.jsonl"
    cases_raw = t219.jsonl_bytes(cases)
    cases_path.write_bytes(cases_raw)
    case_hash = hashlib.sha256(cases_raw).hexdigest()
    results = [{"case_id": f"case-{i}", "event_type": "TERMINAL_FAILURE",
                "failure_code": "provider_error"} for i in range(10_499)]
    validation = {"contract": v2.CONTRACT, "dispatch_authorized": False,
        "retry_authorized": False, "bundle_sha256": "c" * 64,
        "package_manifest_sha256": "a" * 64, "case_inventory_sha256": case_hash,
        "journal_sha256": hashlib.sha256(b"").hexdigest(),
        "denominator": 10_747, "ready": 10_499, "preparation_errors": 248,
        "result_count": 10_499, "results": results}
    with pytest.raises(v2.ReconcileError, match="aliases a protected artifact"):
        v2.append_result_events(root=ROOT, cases_path=cases_path,
            events_path=tmp_path / "events.jsonl", package_manifest_sha256="a" * 64,
            case_inventory_sha256=case_hash, expected_journal_sha256=hashlib.sha256(b"").hexdigest(),
            validation=validation, bundle_sha256="c" * 64, ingestion_receipt_path=saved_output,
            source_artifact_paths=[saved_output])
    assert saved_output.read_bytes() == b"immutable fixture"
