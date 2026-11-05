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
import senath_c2_batch_transport as driver


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


def test_t316_offline_phase_loads_only_the_hash_pinned_v2_reconciler():
    loaded = driver._load_saved_result_reconciler_v2(ROOT)
    assert loaded.CONTRACT == v2.CONTRACT
    t253 = driver._load_pinned(
        "t253", ROOT / "tools/research/c2/ai/tools/build_senath_c2_batch_transport_candidate.py",
        driver.T253_SOURCE_SHA256,
    )
    assert loaded.PROTOCOL_SHA256 == driver.PROTOCOL_SHA256 == t253.PROTOCOL_SHA256


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

def _full_saved_bundle(tmp_path, *, without_intents=False, duplicate_result=False):
    package_sha = "c" * 64
    package_dir = tmp_path / "package"
    package_dir.mkdir()
    cases = [{"case_id": f"ready-{i:05d}", "package_manifest_sha256": package_sha,
              "request_body_sha256": "d" * 64, "disposition": "READY"}
             for i in range(10_499)]
    cases += [{"case_id": f"prep-{i:03d}", "package_manifest_sha256": package_sha,
               "disposition": "PREPARATION_ERROR", "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"}
              for i in range(248)]
    cases_path, events_path = tmp_path / "cases.jsonl", tmp_path / "events.jsonl"
    cases_raw = t219.jsonl_bytes(cases)
    cases_path.write_bytes(cases_raw)
    case_hash = hashlib.sha256(cases_raw).hexdigest()
    tokens = [f"c2_{i:043d}" for i in range(10_499)]
    mapping = [{"custom_id": token, "case_ref": case["case_id"]}
               for token, case in zip(tokens, cases[:10_499])]
    requests = [{"custom_id": token, "method": "POST", "url": "/v1/responses",
                 "body": {"model": v2.EXPECTED_MODEL}}
                for token in tokens]
    mapping_raw, requests_raw = t219.jsonl_bytes(mapping), t219.jsonl_bytes(requests)
    (package_dir / "local_only_mapping.jsonl").write_bytes(mapping_raw)
    (package_dir / "batch_requests.jsonl").write_bytes(requests_raw)
    (package_dir / "manifest.json").write_bytes(v2.canonical({
        "parent_t208_manifest_sha256": package_sha,
        "counts": v2.FROZEN_COUNTS,
        "output_inventory": {"batch_requests.jsonl": hashlib.sha256(requests_raw).hexdigest(),
                              "local_only_mapping.jsonl": hashlib.sha256(mapping_raw).hexdigest()},
    }) + b"\n")
    intents = [{"sequence": i + 1, "event_id": f"intent-{i:05d}", "case_id": row["case_id"],
                "package_manifest_sha256": package_sha, "event_type": "DISPATCH_INTENT",
                "request_body_sha256": row["request_body_sha256"]}
               for i, row in enumerate(cases[:10_499])]
    journal_raw = b"" if without_intents else t219.jsonl_bytes(intents)
    events_path.write_bytes(journal_raw)
    case_hash, journal_hash = hashlib.sha256(cases_raw).hexdigest(), hashlib.sha256(journal_raw).hexdigest()
    output_path, error_path = tmp_path / "output.jsonl", tmp_path / "error.jsonl"
    output_path.write_bytes(b"")
    errors = [{"id": f"result-{i:05d}", "custom_id": token,
               "error": {"code": "synthetic_failure"}}
              for i, token in enumerate(tokens)]
    if duplicate_result:
        errors[-1]["custom_id"] = tokens[0]
    error_raw = t219.jsonl_bytes(errors)
    error_path.write_bytes(error_raw)
    receipt_path = tmp_path / "provider-receipt.json"
    receipt_path.write_bytes(v2.canonical({
        "schema": "C2_SAVED_BATCH_RECEIPT_V1", "status": "completed",
        "batch_id": "batch-synthetic", "input_file_id": "file-synthetic",
        "input_file_sha256": hashlib.sha256(requests_raw).hexdigest(),
        "endpoint": v2.EXPECTED_ENDPOINT, "model": v2.EXPECTED_MODEL,
        "output_file_sha256": hashlib.sha256(b"").hexdigest(),
        "error_file_sha256": hashlib.sha256(error_raw).hexdigest(),
        "request_counts": {"total": 10_499, "completed": 0, "failed": 10_499},
    }) + b"\n")
    return {"package_sha": package_sha, "package_dir": package_dir,
        "cases_path": cases_path, "events_path": events_path,
        "receipt_path": receipt_path, "output_path": output_path,
        "error_path": error_path, "case_hash": case_hash,
        "journal_hash": journal_hash, "cases": cases}


def _synthetic_t253(package_dir):
    class T253:
        @staticmethod
        def validate_saved_package(_root, _package_dir):
            manifest = json.loads((package_dir / "manifest.json").read_text(encoding="utf-8"))
            request_raw = (package_dir / "batch_requests.jsonl").read_bytes()
            mapping_raw = (package_dir / "local_only_mapping.jsonl").read_bytes()
            inventory = manifest["output_inventory"]
            assert hashlib.sha256(request_raw).hexdigest() == inventory["batch_requests.jsonl"]
            assert hashlib.sha256(mapping_raw).hexdigest() == inventory["local_only_mapping.jsonl"]
            return {"batch_requests_sha256": hashlib.sha256(request_raw).hexdigest()}
    return T253()


def _run_driver_bundle(bundle, monkeypatch, *, apply=True):
    original_load = v2._load_verified_module
    monkeypatch.setattr(v2, "_load_verified_module",
        lambda root, rel, expected, name: _synthetic_t253(bundle["package_dir"])
        if rel.endswith("build_senath_c2_batch_transport_candidate.py")
        else original_load(root, rel, expected, name))
    monkeypatch.setattr(driver, "_load_saved_result_reconciler_v2", lambda _root: v2)
    args = type("Args", (), {
        "root": ROOT, "package_dir": bundle["package_dir"], "cases": bundle["cases_path"],
        "events": bundle["events_path"], "phase": "offline-saved-results-v2",
        "execute": False, "release": None, "provider_receipt": bundle["receipt_path"],
        "output_jsonl": bundle["output_path"], "error_jsonl": bundle["error_path"],
        "expected_batch_id": "batch-synthetic",
        "case_inventory_sha256": bundle["case_hash"], "journal_sha256": bundle["journal_hash"],
        "apply_saved_results": apply,
    })()
    return driver._run_cli(args)


def _bundle_receipt_path(bundle, monkeypatch):
    original_load = v2._load_verified_module
    with monkeypatch.context() as patch:
        patch.setattr(v2, "_load_verified_module",
            lambda root, rel, expected, name: _synthetic_t253(bundle["package_dir"])
            if rel.endswith("build_senath_c2_batch_transport_candidate.py")
            else original_load(root, rel, expected, name))
        checked = v2.reconcile_saved_bundle(
            root=ROOT, package_dir=bundle["package_dir"], cases_path=bundle["cases_path"],
            events_path=bundle["events_path"], receipt_path=bundle["receipt_path"],
            output_path=bundle["output_path"], error_path=bundle["error_path"],
            expected_batch_id="batch-synthetic", expected_case_inventory_sha256=bundle["case_hash"],
            expected_journal_sha256=bundle["journal_hash"])
    return v2._ingestion_receipt_path(bundle["events_path"], checked["bundle_sha256"])


def test_existing_driver_offline_v2_mode_reconciles_all_failures_and_replays_exactly(tmp_path, monkeypatch):
    bundle = _full_saved_bundle(tmp_path)
    ingestion_receipt = _bundle_receipt_path(bundle, monkeypatch)
    output = __import__("io").StringIO()
    with monkeypatch.context() as patch:
        patch.setattr(driver, "load_contracts", lambda _root: pytest.fail("historical pins should not load"))
        patch.setattr("sys.stdout", output)
        assert _run_driver_bundle(bundle, patch, apply=True) == 0
        first = json.loads(output.getvalue())
    assert first["status"] == "INGESTED_OFFLINE"
    assert first["appended"] == 10_499
    assert first["provider_called"] is False and first["dispatch_authorized"] is False
    assert ingestion_receipt.exists()
    journal, _ = t219.read_jsonl(bundle["events_path"])
    report = t219.reconcile_attempts(bundle["cases"], journal, bundle["package_sha"],
        expected_case_inventory_sha256=bundle["case_hash"])
    assert report["case_count"] == 10_747
    assert report["state_counts"]["PREPARATION_ERROR"] == 248
    assert report["state_counts"]["TERMINAL_FAILURE"] == 10_499
    before = bundle["events_path"].read_bytes()
    bundle["journal_hash"] = hashlib.sha256(before).hexdigest()
    replay_output = __import__("io").StringIO()
    with monkeypatch.context() as patch:
        patch.setattr(driver, "load_contracts", lambda _root: pytest.fail("historical pins should not load"))
        patch.setattr("sys.stdout", replay_output)
        assert _run_driver_bundle(bundle, patch, apply=True) == 0
    replay = json.loads(replay_output.getvalue())
    assert replay["appended"] == 0
    assert bundle["events_path"].read_bytes() == before


@pytest.mark.parametrize("without_intents,duplicate_result", [(True, False), (False, True)])
def test_uncertain_or_duplicate_saved_bundle_fails_before_receipt_or_journal_write(
        tmp_path, monkeypatch, without_intents, duplicate_result):
    bundle = _full_saved_bundle(tmp_path, without_intents=without_intents,
                                duplicate_result=duplicate_result)
    before = bundle["events_path"].read_bytes()
    receipts_before = list(tmp_path.glob("events.jsonl.saved-*.ingestion.json"))
    with pytest.raises(v2.ReconcileError):
        _run_driver_bundle(bundle, monkeypatch, apply=True)
    assert list(tmp_path.glob("events.jsonl.saved-*.ingestion.json")) == receipts_before
    assert bundle["events_path"].read_bytes() == before


def test_ingestion_receipt_path_cannot_alias_output_or_package_inputs(tmp_path):
    bundle = _full_saved_bundle(tmp_path)
    with pytest.raises(v2.ReconcileError, match="paths must be distinct"):
        v2._validate_distinct_artifact_paths(
            root=ROOT, package_dir=bundle["package_dir"], cases_path=bundle["cases_path"],
            events_path=bundle["events_path"], receipt_path=bundle["receipt_path"],
            output_path=bundle["output_path"], error_path=bundle["error_path"],
            ingestion_receipt_path=bundle["output_path"])
