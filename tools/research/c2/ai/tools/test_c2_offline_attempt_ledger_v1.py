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
import c2_offline_attempt_ledger_v1 as ledger
import senath_c2_batch_transport as driver


def _canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _jsonl(rows):
    return b"".join(_canonical(row) + b"\n" for row in rows)


def _synthetic_package(tmp_path, monkeypatch):
    root = tmp_path / "root"
    package_dir = tmp_path / "candidate"
    t208_dir = root / "ai/out/T208/package-v3"
    package_dir.mkdir(parents=True)
    t208_dir.mkdir(parents=True)
    error_indices = {i * 43 for i in range(248)}
    dispositions = []
    mapping = []
    batch = []
    ordinal = 0
    for index in range(10_747):
        case_ref = f"synthetic-ref-{index:05d}"
        if index in error_indices:
            dispositions.append({"case_ref": case_ref, "status": "PREPARATION_ERROR",
                                "reason": "TARGET_SLOT_MATCH_MULTIPLE"})
            continue
        dispositions.append({"case_ref": case_ref, "status": "READY", "request_ordinal": ordinal})
        token = f"c2_{ordinal:043d}"
        body = {"model": "synthetic-model", "input": [{"role": "user", "content": "synthetic"}]}
        mapping.append({"custom_id": token, "case_ref": case_ref})
        batch.append({"custom_id": token, "method": "POST", "url": "/v1/responses", "body": body})
        ordinal += 1
    dispositions_raw = _jsonl(dispositions)
    t208_manifest = {
        "protocol_sha256": ledger.PROTOCOL_SHA256,
        "cohort_count": 10_747, "ready_count": 10_499,
        "preparation_error_count": 248,
    }
    t208_manifest_raw = _canonical(t208_manifest) + b"\n"
    (t208_dir / "manifest.json").write_bytes(t208_manifest_raw)
    (t208_dir / "dispositions.jsonl").write_bytes(dispositions_raw)
    (package_dir / "batch_requests.jsonl").write_bytes(_jsonl(batch))
    (package_dir / "local_only_mapping.jsonl").write_bytes(_jsonl(mapping))
    package_manifest = {
        "protocol_sha256": ledger.PROTOCOL_SHA256,
        "parent_t208_manifest_sha256": hashlib.sha256(t208_manifest_raw).hexdigest(),
        "parent_dispositions_sha256": hashlib.sha256(dispositions_raw).hexdigest(),
    }
    (package_dir / "manifest.json").write_bytes(_canonical(package_manifest) + b"\n")

    class T253:
        T208_DIR = Path("ai/out/T208/package-v3")
        T208_MANIFEST_SHA256 = hashlib.sha256(t208_manifest_raw).hexdigest()
        T208_DISPOSITIONS_SHA256 = hashlib.sha256(dispositions_raw).hexdigest()
        METHOD = "POST"
        ENDPOINT = "/v1/responses"

        @staticmethod
        def read_jsonl(path):
            raw = path.read_bytes()
            return ledger._jsonl(raw), [line for line in raw[:-1].split(b"\n")]

        @staticmethod
        def validate_dispositions(rows):
            if len(rows) != 10_747 or len({row["case_ref"] for row in rows}) != 10_747:
                raise ValueError("invalid synthetic disposition inventory")
            ready = sorted((row for row in rows if row["status"] == "READY"),
                           key=lambda row: row["request_ordinal"])
            errors = [row for row in rows if row["status"] == "PREPARATION_ERROR"]
            if (len(ready) != 10_499 or len(errors) != 248
                    or any(row.get("reason") != "TARGET_SLOT_MATCH_MULTIPLE" for row in errors)):
                raise ValueError("invalid synthetic counts")
            return ready

        @staticmethod
        def validate_saved_package(_root, path):
            manifest_raw = (path / "manifest.json").read_bytes()
            package = json.loads(manifest_raw)
            batch_raw = (path / "batch_requests.jsonl").read_bytes()
            map_raw = (path / "local_only_mapping.jsonl").read_bytes()
            if (package["parent_t208_manifest_sha256"] != T253.T208_MANIFEST_SHA256
                    or package["parent_dispositions_sha256"] != T253.T208_DISPOSITIONS_SHA256):
                raise ValueError("synthetic package binding mismatch")
            return {
                "manifest_sha256": hashlib.sha256(manifest_raw).hexdigest(),
                "batch_requests_sha256": hashlib.sha256(batch_raw).hexdigest(),
                "local_mapping_sha256": hashlib.sha256(map_raw).hexdigest(),
                "denominator_cases": 10_747, "ready_requests": 10_499,
                "retained_preparation_errors": 248,
                "dispatch_authorized": False, "provider_called": False,
            }

    monkeypatch.setattr(ledger, "_dependencies", lambda _root: (T253, t219))
    return root, package_dir


def test_t316_loader_verifies_the_versioned_ledger_source_pin():
    loaded = driver._load_offline_attempt_ledger_v1(ROOT)
    assert loaded.CONTRACT == ledger.CONTRACT
    t253, _ = ledger._dependencies(ROOT)
    assert loaded.PROTOCOL_SHA256 == driver.PROTOCOL_SHA256 == t253.PROTOCOL_SHA256


def test_driver_prepares_then_validates_complete_synthetic_case_journal(tmp_path, monkeypatch, capsys):
    root, package_dir = _synthetic_package(tmp_path, monkeypatch)
    ledger_dir = tmp_path / "prepared-ledger"
    monkeypatch.setattr(driver, "_load_offline_attempt_ledger_v1", lambda _root: ledger)
    base = ["--root", str(root), "--package-dir", str(package_dir), "--ledger-dir", str(ledger_dir),
            "--phase", "offline-attempt-ledger-v1"]

    assert driver.main(base + ["--ledger-action", "prepare"]) == 0
    prepared = json.loads(capsys.readouterr().out)
    assert prepared["status"] == "OFFLINE_ATTEMPT_LEDGER_PREPARED"
    assert prepared["counts"] == ledger.EXPECTED_COUNTS
    assert prepared["remote_attempt_events_created"] == 0
    assert prepared["provider_called"] is False and prepared["dispatch_authorized"] is False
    assert (ledger_dir / "attempt_journal.jsonl").read_bytes() == b""

    assert driver.main(base + ["--ledger-action", "validate"]) == 0
    validated = json.loads(capsys.readouterr().out)
    assert validated["status"] == "OFFLINE_ATTEMPT_LEDGER_VALIDATED"
    assert validated["initial_state_counts"] == {"NOT_STARTED": 10_499, "PREPARATION_ERROR": 248}
    rows = t219.read_jsonl(ledger_dir / "case_inventory.jsonl")[0]
    assert len(rows) == 10_747
    assert sum(row["disposition"] == "READY" for row in rows) == 10_499
    assert sum(row["disposition"] == "PREPARATION_ERROR" for row in rows) == 248
    assert [row["case_id"] for row in rows[:3]] == ["synthetic-ref-00000", "synthetic-ref-00001", "synthetic-ref-00002"]


def test_validator_rejects_changed_journal_without_replacing_any_bytes(tmp_path, monkeypatch):
    root, package_dir = _synthetic_package(tmp_path, monkeypatch)
    ledger_dir = tmp_path / "prepared-ledger"
    ledger.prepare(root, package_dir, ledger_dir)
    journal = ledger_dir / "attempt_journal.jsonl"
    journal.write_bytes(b"synthetic-remote-submit\n")
    before = journal.read_bytes()
    with pytest.raises(ledger.LedgerError, match="differs from its pinned package"):
        ledger.validate(root, package_dir, ledger_dir)
    assert journal.read_bytes() == before


def test_prepare_refuses_existing_output_and_preserves_it(tmp_path, monkeypatch):
    root, package_dir = _synthetic_package(tmp_path, monkeypatch)
    ledger_dir = tmp_path / "prepared-ledger"
    ledger_dir.mkdir()
    marker = ledger_dir / "operator-note.txt"
    marker.write_text("preserve", encoding="utf-8")
    with pytest.raises(ledger.LedgerError, match="destination exists"):
        ledger.prepare(root, package_dir, ledger_dir)
    assert marker.read_text(encoding="utf-8") == "preserve"


def test_prepare_refuses_output_inside_package_and_does_not_create_it(tmp_path, monkeypatch):
    root, package_dir = _synthetic_package(tmp_path, monkeypatch)
    ledger_dir = package_dir / "ledger"
    with pytest.raises(ledger.LedgerError, match="aliases a pinned input"):
        ledger.prepare(root, package_dir, ledger_dir)
    assert not ledger_dir.exists()


def test_t316_rejects_historical_options_on_offline_attempt_phase(tmp_path, monkeypatch, capsys):
    root, package_dir = _synthetic_package(tmp_path, monkeypatch)
    args = ["--root", str(root), "--package-dir", str(package_dir),
            "--phase", "offline-attempt-ledger-v1", "--ledger-action", "prepare",
            "--ledger-dir", str(tmp_path / "ledger"), "--state", str(tmp_path / "state.json")]
    assert driver.main(args) == 2
    result = json.loads(capsys.readouterr().out)
    assert result["status"] == "BLOCKED"
    assert not (tmp_path / "ledger").exists()
