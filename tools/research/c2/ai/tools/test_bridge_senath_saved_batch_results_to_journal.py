from __future__ import annotations

import hashlib
import importlib.util
import json
import os
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import bridge_senath_saved_batch_results_to_journal as bridge


def _load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec is not None and spec.loader is not None
    spec.loader.exec_module(module)
    return module


v1 = _load("t314_v1", ROOT / "ai/tools/c2_attempt_resume.py")
v2 = _load("t314_v2", ROOT / "ai/tools/c2_attempt_journal_v2.py")
parser = _load("t314_parser", ROOT / "ai/tools/validate_c2_openai_response.py")

PACKAGE_SHA = "a" * 64
REQUEST_SHA = "b" * 64
CASE_ID = "synthetic-ready-case"
TOKEN = "c2_" + "A" * 43
TOKEN_B = "c2_" + "B" * 43
BATCH_ID = "batch_synthetic"
EXPECTED_JOURNAL = hashlib.sha256(b"").hexdigest()


class _CandidateError(Exception):
    pass


class _T253:
    CandidateError = _CandidateError

    @staticmethod
    def read_json(path: Path):
        return json.loads(path.read_text(encoding="utf-8"))

    @staticmethod
    def read_jsonl(path: Path):
        raw = path.read_bytes()
        rows = [json.loads(line) for line in raw.splitlines()]
        return rows, [line for line in raw.splitlines()]

    @staticmethod
    def validate_saved_package(_root: Path, package_dir: Path):
        data = (package_dir / "batch_requests.jsonl").read_bytes()
        return {"batch_requests_sha256": hashlib.sha256(data).hexdigest()}


def _write_jsonl(path: Path, rows: list[dict]):
    path.write_bytes(b"".join(json.dumps(row, sort_keys=True, separators=(",", ":")).encode() + b"\n" for row in rows))


def _success_body():
    return {
        "status": "completed",
        "output": [{
            "type": "message", "role": "assistant", "status": "completed",
            "content": [{"type": "output_text", "text": json.dumps(
                {"substitutions": [f"candidate-{i:02d}" for i in range(20)]},
                separators=(",", ":"),
            )}],
        }],
    }


def _setup(tmp_path: Path, monkeypatch, *, output_rows=None, error_rows=None,
           receipt_overrides=None, with_intent=True, ready_count=1):
    package = tmp_path / "package"
    package.mkdir()
    tokens = [TOKEN, TOKEN_B, "c2_" + "C" * 43][:ready_count]
    case_ids = [CASE_ID, "synthetic-ready-case-b", "synthetic-ready-case-c"][:ready_count]
    request_lines = [{"custom_id": token, "method": "POST", "url": "/v1/responses",
                      "body": {"model": bridge.EXPECTED_MODEL}} for token in tokens]
    _write_jsonl(package / "batch_requests.jsonl", request_lines)
    _write_jsonl(package / "local_only_mapping.jsonl", [
        {"custom_id": token, "case_ref": case_id} for token, case_id in zip(tokens, case_ids)
    ])
    (package / "manifest.json").write_text(json.dumps({"parent_t208_manifest_sha256": PACKAGE_SHA}), encoding="utf-8")
    cases_path, events_path = tmp_path / "cases.jsonl", tmp_path / "events.jsonl"
    cases = [{"case_id": case_id, "package_manifest_sha256": PACKAGE_SHA,
              "request_body_sha256": REQUEST_SHA, "disposition": "READY"}
             for case_id in case_ids]
    cases.append({"case_id": "synthetic-preparation-error", "package_manifest_sha256": PACKAGE_SHA,
                  "disposition": "PREPARATION_ERROR", "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"})
    cases_path.write_bytes(v1.jsonl_bytes(cases))
    if with_intent:
        current = EXPECTED_JOURNAL
        for index, case_id in enumerate(case_ids):
            appended = v1.append_attempt_event(
                cases_path, events_path, PACKAGE_SHA, hashlib.sha256(cases_path.read_bytes()).hexdigest(),
                expected_journal_sha256=current, case_id=case_id,
                event_id=f"synthetic-intent-{index}", event_type="DISPATCH_INTENT",
            )
            current = appended["event_ledger_sha256"]
    output_rows = output_rows if output_rows is not None else [
        {"id": f"batch_req_synthetic_{index}", "custom_id": token,
         "response": {"status_code": 200, "request_id": f"req_synthetic_{index}", "body": _success_body()}}
        for index, token in enumerate(tokens)
    ]
    error_rows = error_rows if error_rows is not None else []
    output_path, error_path = tmp_path / "output.jsonl", tmp_path / "errors.jsonl"
    _write_jsonl(output_path, output_rows) if output_rows else output_path.write_bytes(b"")
    _write_jsonl(error_path, error_rows) if error_rows else error_path.write_bytes(b"")
    receipt = {
        "schema": "C2_SAVED_BATCH_RECEIPT_V1", "status": "completed", "batch_id": BATCH_ID,
        "input_file_id": "file_synthetic", "input_file_sha256": hashlib.sha256((package / "batch_requests.jsonl").read_bytes()).hexdigest(),
        "endpoint": "/v1/responses", "model": bridge.EXPECTED_MODEL,
        "output_file_sha256": hashlib.sha256(output_path.read_bytes()).hexdigest(),
        "error_file_sha256": hashlib.sha256(error_path.read_bytes()).hexdigest(),
        "request_counts": {"total": ready_count, "completed": len(output_rows), "failed": len(error_rows)},
    }
    receipt.update(receipt_overrides or {})
    receipt_path = tmp_path / "receipt.json"
    receipt_path.write_text(json.dumps(receipt), encoding="utf-8")
    original = bridge._load_pinned
    monkeypatch.setattr(bridge, "_load_pinned", lambda name, path, expected: _T253 if name == "t253" else original(name, path, expected))
    deps = {
        "t253": ROOT / "ai/tools/build_senath_c2_batch_transport_candidate.py",
        "t219": ROOT / "ai/tools/c2_attempt_resume.py",
        "t249": ROOT / "ai/tools/c2_attempt_journal_v2.py",
        "parser": ROOT / "ai/tools/validate_c2_openai_response.py",
    }
    ingestion_receipt_path = tmp_path / "ingestion_receipt.json"
    return package, cases_path, events_path, receipt_path, output_path, error_path, ingestion_receipt_path, deps


def _run(tmp_path, monkeypatch, **kwargs):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch, **kwargs)
    return bridge.ingest_saved_batch(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
        output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID,
        expected_case_inventory_sha256=hashlib.sha256(cases.read_bytes()).hexdigest(),
        expected_journal_sha256=hashlib.sha256(events.read_bytes()).hexdigest() if events.exists() else EXPECTED_JOURNAL,
        root=ROOT, dependency_paths=deps,
    ), events


def test_success_maps_opaque_id_and_appends_only_digest_bound_response(tmp_path, monkeypatch):
    result, events_path = _run(tmp_path, monkeypatch)
    rows, raw = v1.read_jsonl(events_path)
    assert result["result_count"] == result["event_count"] == 1
    assert result["dispatch_authorized"] is False and result["provider_called"] is False
    assert rows[-1]["case_id"] == CASE_ID
    assert rows[-1]["event_type"] == "RESPONSE_RECEIVED"
    assert rows[-1]["response_sha256"] == hashlib.sha256(bridge._canonical(_success_body())).hexdigest()
    assert b"candidate-00" not in raw
    assert v2.reconcile_attempts_v2(
        v1.read_jsonl(tmp_path / "cases.jsonl")[0], rows, PACKAGE_SHA,
        expected_case_inventory_sha256=hashlib.sha256((tmp_path / "cases.jsonl").read_bytes()).hexdigest(),
        v1_path=ROOT / "ai/tools/c2_attempt_resume.py",
    )["state_counts"] == {"RESPONSE_RECEIVED": 1, "PREPARATION_ERROR": 1}


def test_batch_error_becomes_terminal_failure_without_provider_text(tmp_path, monkeypatch):
    error = [{"id": "batch_req_synthetic", "custom_id": TOKEN,
              "error": {"code": "synthetic_error", "message": "do not persist this"}}]
    result, events_path = _run(tmp_path, monkeypatch, output_rows=[], error_rows=error)
    rows, raw = v1.read_jsonl(events_path)
    assert result["batch_error_count"] == 1
    assert rows[-1]["event_type"] == "TERMINAL_FAILURE"
    assert rows[-1]["failure_code"] == "provider_error"
    assert b"do not persist this" not in raw


def test_response_parser_failure_is_terminal_and_keeps_only_digest(tmp_path, monkeypatch):
    row = {"id": "batch_req_synthetic", "custom_id": TOKEN,
           "response": {"status_code": 200, "request_id": "req_synthetic", "body": {"status": "incomplete"}}}
    _, events_path = _run(tmp_path, monkeypatch, output_rows=[row])
    rows, _ = v1.read_jsonl(events_path)
    assert rows[-1]["event_type"] == "TERMINAL_FAILURE"
    assert rows[-1]["failure_code"] == "response_truncated"
    assert "response_sha256" in rows[-1]


@pytest.mark.parametrize("receipt_overrides", [
    {"batch_id": "batch_other"},
    {"endpoint": "/v1/chat/completions"},
    {"model": "other-model"},
    {"request_counts": {"total": 2, "completed": 1, "failed": 0}},
])
def test_wrong_receipt_binding_is_rejected_before_wal_write(tmp_path, monkeypatch, receipt_overrides):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch, receipt_overrides=receipt_overrides)
    before = events.read_bytes()
    with pytest.raises(bridge.BridgeError):
        bridge.ingest_saved_batch(
            package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
            output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
            expected_batch_id=BATCH_ID,
            expected_case_inventory_sha256=hashlib.sha256(cases.read_bytes()).hexdigest(),
            expected_journal_sha256=hashlib.sha256(before).hexdigest(), root=ROOT,
            dependency_paths=deps,
        )
    assert events.read_bytes() == before
    assert not ingestion_receipt.exists()


@pytest.mark.parametrize("rows", [
    [],
    [
        {"id": "r1", "custom_id": TOKEN, "response": {"status_code": 200, "request_id": "q", "body": _success_body()}},
        {"id": "r2", "custom_id": TOKEN, "response": {"status_code": 200, "request_id": "q2", "body": _success_body()}},
    ],
    [{"id": "r1", "custom_id": "c2_" + "B" * 43,
      "response": {"status_code": 200, "request_id": "q", "body": _success_body()}}],
])
def test_missing_duplicate_and_unknown_results_fail_before_wal_write(tmp_path, monkeypatch, rows):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch, output_rows=rows)
    before = events.read_bytes()
    with pytest.raises(bridge.BridgeError):
        bridge.ingest_saved_batch(
            package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
            output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
            expected_batch_id=BATCH_ID,
            expected_case_inventory_sha256=hashlib.sha256(cases.read_bytes()).hexdigest(),
            expected_journal_sha256=hashlib.sha256(before).hexdigest(), root=ROOT,
            dependency_paths=deps,
        )
    assert events.read_bytes() == before


def test_result_without_prior_intent_is_rejected_without_authority(tmp_path, monkeypatch):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch, with_intent=False)
    before = events.read_bytes() if events.exists() else b""
    with pytest.raises(bridge.BridgeError, match="full frozen READY intent set"):
        bridge.ingest_saved_batch(
            package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
            output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
            expected_batch_id=BATCH_ID,
            expected_case_inventory_sha256=hashlib.sha256(cases.read_bytes()).hexdigest(),
            expected_journal_sha256=hashlib.sha256(before).hexdigest(), root=ROOT,
            dependency_paths=deps,
        )
    assert not events.exists()


def test_complete_bundle_reinvocation_is_verified_noop(tmp_path, monkeypatch):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch)
    case_hash = hashlib.sha256(cases.read_bytes()).hexdigest()
    first = bridge.ingest_saved_batch(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
        output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID,
        expected_case_inventory_sha256=case_hash,
        expected_journal_sha256=hashlib.sha256(events.read_bytes()).hexdigest(),
        root=ROOT, dependency_paths=deps,
    )
    before = events.read_bytes()
    second = bridge.ingest_saved_batch(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
        output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID, expected_case_inventory_sha256=case_hash,
        expected_journal_sha256=first["event_ledger_sha256"], root=ROOT, dependency_paths=deps,
    )
    assert events.read_bytes() == before
    assert second["ingestion_state"] == "NO_OP_VERIFIED"
    assert second["appended_event_count"] == 0
    assert second["reconciled_existing_count"] == 1


def test_partial_local_append_resumes_only_missing_events_for_same_bundle(tmp_path, monkeypatch):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(
        tmp_path, monkeypatch, ready_count=2,
    )
    case_hash = hashlib.sha256(cases.read_bytes()).hexdigest()
    current_hash = hashlib.sha256(events.read_bytes()).hexdigest()
    original_loader = bridge._load_pinned
    real_v2 = original_loader("t249", deps["t249"], bridge.T249_V2_SOURCE_SHA256)

    class FailSecondAppend:
        def __init__(self):
            self.append_count = 0

        def __getattr__(self, name):
            return getattr(real_v2, name)

        def append_validated_response(self, *args, **kwargs):
            self.append_count += 1
            if self.append_count == 2:
                raise bridge.BridgeError("synthetic interrupted append")
            return real_v2.append_validated_response(*args, **kwargs)

    interrupted = FailSecondAppend()
    monkeypatch.setattr(
        bridge, "_load_pinned",
        lambda name, path, expected: interrupted if name == "t249" else original_loader(name, path, expected),
    )
    call = dict(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
        output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID, expected_case_inventory_sha256=case_hash,
        root=ROOT, dependency_paths=deps,
    )
    with pytest.raises(bridge.BridgeError, match="interrupted append"):
        bridge.ingest_saved_batch(expected_journal_sha256=current_hash, **call)
    started = json.loads(ingestion_receipt.read_text(encoding="utf-8"))
    assert started["state"] == "STARTED"
    assert len(v1.read_jsonl(events)[0]) == 3  # two intents plus exactly one terminal event

    monkeypatch.setattr(bridge, "_load_pinned", original_loader)
    recovered = bridge.ingest_saved_batch(
        expected_journal_sha256=hashlib.sha256(events.read_bytes()).hexdigest(), **call,
    )
    assert recovered["ingestion_state"] == "COMPLETE"
    assert recovered["appended_event_count"] == 1
    assert recovered["reconciled_existing_count"] == 1
    assert len(v1.read_jsonl(events)[0]) == 4
    complete = json.loads(ingestion_receipt.read_text(encoding="utf-8"))
    assert complete["state"] == "COMPLETE"
    assert complete["appended_event_count"] + complete["reconciled_existing_count"] == 2


def test_completed_receipt_rejects_changed_journal_without_appending(tmp_path, monkeypatch):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(
        tmp_path, monkeypatch, ready_count=2,
    )
    case_hash = hashlib.sha256(cases.read_bytes()).hexdigest()
    call = dict(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
        output_path=output, error_path=error, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID, expected_case_inventory_sha256=case_hash,
        root=ROOT, dependency_paths=deps,
    )
    complete = bridge.ingest_saved_batch(
        expected_journal_sha256=hashlib.sha256(events.read_bytes()).hexdigest(), **call,
    )
    rows, _ = v1.read_jsonl(events)
    assert len([row for row in rows if row["event_id"].startswith(f"batch:{BATCH_ID}:")]) == 2

    # Preserve a canonical but truncated WAL prefix that leaves the second case
    # uncertain; a COMPLETE receipt may not silently repair it as a no-op.
    truncated = v1.jsonl_bytes(rows[:-1])
    events.write_bytes(truncated)
    with pytest.raises(bridge.BridgeError, match="completed ingestion journal digest changed"):
        bridge.ingest_saved_batch(
            expected_journal_sha256=hashlib.sha256(truncated).hexdigest(), **call,
        )
    assert events.read_bytes() == truncated
    assert complete["ingestion_state"] == "COMPLETE"


@pytest.mark.parametrize("alias_role", ["output", "journal_lock"])
def test_ingestion_receipt_path_cannot_alias_artifacts_or_journal_lock(tmp_path, monkeypatch, alias_role):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch)
    alias_path = output if alias_role == "output" else events.with_name(events.name + ".lock")
    before_events = events.read_bytes()
    before_alias = alias_path.read_bytes() if alias_path.exists() else None
    with pytest.raises(bridge.BridgeError, match="local artifact and lock paths must be distinct"):
        bridge.ingest_saved_batch(
            package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
            output_path=output, error_path=error, ingestion_receipt_path=alias_path,
            expected_batch_id=BATCH_ID,
            expected_case_inventory_sha256=hashlib.sha256(cases.read_bytes()).hexdigest(),
            expected_journal_sha256=hashlib.sha256(before_events).hexdigest(),
            root=ROOT, dependency_paths=deps,
        )
    assert events.read_bytes() == before_events
    assert (alias_path.read_bytes() if alias_path.exists() else None) == before_alias


def test_ingestion_receipt_path_rejects_hardlink_to_saved_output(tmp_path, monkeypatch):
    package, cases, events, receipt, output, error, ingestion_receipt, deps = _setup(tmp_path, monkeypatch)
    alias_path = tmp_path / "output-hardlink.jsonl"
    try:
        os.link(output, alias_path)
    except OSError as exc:
        pytest.skip(f"temporary hard-link creation unavailable: {exc.__class__.__name__}")
    before_events, before_output = events.read_bytes(), output.read_bytes()
    with pytest.raises(bridge.BridgeError, match="local artifact and lock paths must be distinct"):
        bridge.ingest_saved_batch(
            package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt,
            output_path=output, error_path=error, ingestion_receipt_path=alias_path,
            expected_batch_id=BATCH_ID,
            expected_case_inventory_sha256=hashlib.sha256(cases.read_bytes()).hexdigest(),
            expected_journal_sha256=hashlib.sha256(before_events).hexdigest(),
            root=ROOT, dependency_paths=deps,
        )
    assert events.read_bytes() == before_events
    assert output.read_bytes() == before_output


def test_changed_bundle_is_rejected_after_ingestion_binding(tmp_path, monkeypatch):
    package, cases, events, receipt_path, output_path, error_path, ingestion_receipt, deps = _setup(tmp_path, monkeypatch)
    case_hash = hashlib.sha256(cases.read_bytes()).hexdigest()
    call = dict(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt_path,
        output_path=output_path, error_path=error_path, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID, expected_case_inventory_sha256=case_hash,
        root=ROOT, dependency_paths=deps,
    )
    first = bridge.ingest_saved_batch(
        expected_journal_sha256=hashlib.sha256(events.read_bytes()).hexdigest(), **call,
    )
    before = events.read_bytes()
    changed_rows = [json.loads(line) for line in output_path.read_text(encoding="utf-8").splitlines()]
    changed_rows[0]["response"]["request_id"] = "req_changed_after_binding"
    _write_jsonl(output_path, changed_rows)
    saved_receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
    saved_receipt["output_file_sha256"] = hashlib.sha256(output_path.read_bytes()).hexdigest()
    receipt_path.write_text(json.dumps(saved_receipt), encoding="utf-8")
    with pytest.raises(bridge.BridgeError, match="differs from its durable ingestion binding"):
        bridge.ingest_saved_batch(
            expected_journal_sha256=first["event_ledger_sha256"], **call,
        )
    assert events.read_bytes() == before


def test_legacy_partial_batch_events_without_bundle_binding_stop(tmp_path, monkeypatch):
    package, cases, events, receipt_path, output_path, error_path, ingestion_receipt, deps = _setup(
        tmp_path, monkeypatch, ready_count=2,
    )
    case_hash = hashlib.sha256(cases.read_bytes()).hexdigest()
    call = dict(
        package_dir=package, cases_path=cases, events_path=events, receipt_path=receipt_path,
        output_path=output_path, error_path=error_path, ingestion_receipt_path=ingestion_receipt,
        expected_batch_id=BATCH_ID, expected_case_inventory_sha256=case_hash,
        root=ROOT, dependency_paths=deps,
    )
    first = bridge.ingest_saved_batch(
        expected_journal_sha256=hashlib.sha256(events.read_bytes()).hexdigest(), **call,
    )
    # A completed legacy receipt is removed only inside this temporary fixture
    # to model a prior T314 partial append that had no bundle-binding record.
    ingestion_receipt.unlink()
    before = events.read_bytes()
    with pytest.raises(bridge.BridgeError, match="lack a prior bundle binding"):
        bridge.ingest_saved_batch(
            expected_journal_sha256=first["event_ledger_sha256"], **call,
        )
    assert events.read_bytes() == before
    assert not ingestion_receipt.exists()


def test_non_200_response_is_terminal_provider_failure_with_saved_body_digest(tmp_path, monkeypatch):
    row = {"id": "batch_req_synthetic", "custom_id": TOKEN,
           "response": {"status_code": 429, "request_id": "req_synthetic", "body": {"error": {"code": "rate_limit"}}}}
    _, events_path = _run(tmp_path, monkeypatch, output_rows=[row])
    rows, _ = v1.read_jsonl(events_path)
    assert rows[-1]["failure_code"] == "provider_error"
    assert rows[-1]["response_sha256"] == hashlib.sha256(bridge._canonical(row["response"]["body"])).hexdigest()
