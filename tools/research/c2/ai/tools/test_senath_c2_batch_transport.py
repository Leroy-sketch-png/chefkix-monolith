#!/usr/bin/env python3
"""Synthetic-only state-machine tests for the default-off T316 driver."""

from __future__ import annotations

import hashlib
import importlib.util
import io
import json
import copy
import shutil
import sys
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
DRIVER_PATH = Path(__file__).with_name("senath_c2_batch_transport.py")
SPEC = importlib.util.spec_from_file_location("senath_c2_batch_transport_test", DRIVER_PATH)
assert SPEC is not None and SPEC.loader is not None
driver = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(driver)


def digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


class FakeFiles:
    def __init__(self, request_bytes: bytes, output_bytes: bytes = b"", error_bytes: bytes = b""):
        self.request_bytes = request_bytes
        self.output_bytes = output_bytes
        self.error_bytes = error_bytes
        self.upload_count = 0
        self.content_calls = []
        self.remote_files = []

    def create(self, *, file, purpose):
        filename, stream = file
        assert filename.startswith("senath-c2-") and filename.endswith(".jsonl")
        assert purpose == "batch"
        assert stream.read() == self.request_bytes
        self.upload_count += 1
        row = SimpleNamespace(id="file-input", purpose="batch", object="file", bytes=len(self.request_bytes),
                              filename=filename, created_at=datetime.now(timezone.utc).timestamp())
        self.remote_files.append(row)
        return row

    def content(self, file_id):
        self.content_calls.append(file_id)
        if file_id == "file-input":
            return io.BytesIO(self.request_bytes)
        return io.BytesIO({"file-output": self.output_bytes, "file-errors": self.error_bytes}[file_id])

    def list(self, **kwargs):
        assert kwargs.get("purpose") == "batch"
        return SimpleNamespace(data=self.remote_files, has_more=False,
                               last_id=self.remote_files[-1].id if self.remote_files else None)


class FakeBatches:
    def __init__(self):
        self.create_count = 0
        self.retrieve_count = 0
        self.cancel_count = 0
        self.remote_batches = []
        self.status_output_file_id = "file-output"
        self.status_error_file_id = None
        self.status_counts = (1, 0)
        self.status_usage = None

    def create(self, **kwargs):
        assert set(kwargs) == {"input_file_id", "endpoint", "completion_window", "metadata"}
        assert {key: value for key, value in kwargs.items() if key != "metadata"} == {
            "input_file_id": "file-input", "endpoint": driver.EXPECTED_ENDPOINT,
            "completion_window": "24h"}
        assert set(kwargs["metadata"]) == {"senath_reconciliation_id"}
        assert len(kwargs["metadata"]["senath_reconciliation_id"]) == 64
        self.create_count += 1
        row = SimpleNamespace(
            id="batch-known", endpoint=driver.EXPECTED_ENDPOINT, input_file_id="file-input",
            completion_window="24h", status="validating", metadata=kwargs["metadata"],
            created_at=datetime.now(timezone.utc).timestamp(),
            request_counts=SimpleNamespace(total=1, completed=0, failed=0),
        )
        self.remote_batches.append(row)
        return row

    def retrieve(self, batch_id):
        assert batch_id == "batch-known"
        self.retrieve_count += 1
        created = next((row for row in self.remote_batches if row.id == batch_id), None)
        return SimpleNamespace(
            id="batch-known", endpoint=driver.EXPECTED_ENDPOINT, input_file_id="file-input",
            completion_window=created.completion_window if created else "24h",
            metadata=created.metadata if created else {"senath_reconciliation_id": "synthetic"},
            status="completed", output_file_id=self.status_output_file_id,
            error_file_id=self.status_error_file_id,
            request_counts=SimpleNamespace(total=1, completed=self.status_counts[0], failed=self.status_counts[1]),
            model=driver.EXPECTED_MODEL, usage=self.status_usage,
        )

    def list(self, **kwargs):
        return SimpleNamespace(data=self.remote_batches, has_more=False,
                               last_id=self.remote_batches[-1].id if self.remote_batches else None)


class FakeTransport:
    def __init__(self, request_bytes: bytes, output_bytes: bytes = b"", error_bytes: bytes = b""):
        self.files = FakeFiles(request_bytes, output_bytes, error_bytes)
        self.batches = FakeBatches()


class SenathBatchTransportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        base = Path(self.temp.name)
        self.root = base / "repo"
        (self.root / "ai/tools").mkdir(parents=True)
        shutil.copyfile(ROOT / "ai/tools/c2_attempt_resume.py", self.root / "ai/tools/c2_attempt_resume.py")
        self.package = base / "package"
        self.package.mkdir()
        self.cases_path = base / "cases.jsonl"
        self.events_path = base / "events.jsonl"
        self.state_path = base / "state.json"
        self.recovery_path = base / "recovered"
        self.parent_manifest_hash = "a" * 64
        self.package_manifest_bytes = b'{"schema":"synthetic"}\n'
        (self.package / "manifest.json").write_bytes(self.package_manifest_bytes)
        self.package_hash = digest(self.package_manifest_bytes)
        self.release_hash = "c" * 64
        self.cases = [
            {"case_id": "case-ready", "package_manifest_sha256": self.parent_manifest_hash,
             "request_body_sha256": "d" * 64, "disposition": "READY"},
            {"case_id": "case-prep-error", "package_manifest_sha256": self.parent_manifest_hash,
             "disposition": "PREPARATION_ERROR", "reason_code": "TARGET_SLOT_MATCH_MULTIPLE"},
        ]
        self.request_bytes = b'{"body":{"model":"gpt-4o-2024-08-06"},"custom_id":"tok1","method":"POST","url":"/v1/responses"}\n'
        self.output_bytes = (b'{"id":"result-1","custom_id":"tok1","response":{"status_code":200,'
                             b'"request_id":"req-1","body":{}}}\n')
        self.error_bytes = b""
        self.cases_path.write_bytes(driver.canonical_json(self.cases[0]) + b"\n" +
                                    driver.canonical_json(self.cases[1]) + b"\n")
        self.events_path.write_bytes(b"")
        (self.package / "batch_requests.jsonl").write_bytes(self.request_bytes)
        (self.package / "local_only_mapping.jsonl").write_bytes(
            b'{"case_ref":"case-ready","custom_id":"tok1"}\n')
        self.contracts = driver.load_contracts(ROOT)
        self.facts = {
            "root": self.root, "package_dir": self.package,
            "cases_path": self.cases_path, "events_path": self.events_path,
            "package_manifest": {"schema": "synthetic"},
            "package_manifest_sha256": self.package_hash,
            "parent_t208_manifest_sha256": self.parent_manifest_hash,
            "request_bodies_sha256": "e" * 64, "dispositions_sha256": "f" * 64,
            "batch_requests_sha256": digest(self.request_bytes),
            "case_inventory_sha256": digest(self.cases_path.read_bytes()),
            "local_mapping_sha256": digest((self.package / "local_only_mapping.jsonl").read_bytes()),
            "journal_sha256": digest(b""), "cases": self.cases, "events": [],
            "state_counts": {}, "ready_ids": ["case-ready"],
            "preparation_error_count": 1, "denominator": 2,
            "request_count": 1, "provider_called": False,
            "dispatch_authorized": False, "cohort_selected": False,
        }
        self.fake = FakeTransport(self.request_bytes, self.output_bytes, self.error_bytes)

    def tearDown(self):
        self.temp.cleanup()

    def phase(self, name, *, recovery_dir=None, transport=None):
        return driver.execute_phase(
            phase=name, facts=self.facts, contracts=self.contracts,
            state_path=self.state_path, release={}, release_hash=self.release_hash,
            transport=transport or self.fake, recovery_dir=recovery_dir,
        )

    def test_upload_submit_status_recover_compose_and_receipt_matches_t314(self):
        uploaded = self.phase("upload")
        self.assertEqual(uploaded["file_id"], "file-input")
        self.assertEqual(self.fake.files.upload_count, 1)
        submitted = self.phase("submit")
        self.assertEqual(submitted["batch_id"], "batch-known")
        self.assertEqual(self.fake.batches.create_count, 1)
        rows, _ = self.contracts["t219"].read_jsonl(self.events_path)
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0]["case_id"], "case-ready")
        self.assertEqual(rows[0]["event_type"], "DISPATCH_INTENT")
        self.assertNotIn("case-prep-error", {row["case_id"] for row in rows})
        status = self.phase("status")
        self.assertEqual(status["status"], "completed")
        recovered = self.phase("recover", recovery_dir=self.recovery_path)
        self.assertEqual(recovered["result_count"], 1)
        receipt = json.loads((self.recovery_path / "receipt.json").read_text(encoding="utf-8"))
        self.contracts["t314"]._validate_receipt(
            receipt, expected_batch_id="batch-known", expected_input_sha256=digest(self.request_bytes),
            output_sha256=digest(self.output_bytes), error_sha256=digest(self.error_bytes),
            output_count=1, error_count=0, total_intents=1, model=driver.EXPECTED_MODEL,
        )
        self.assertEqual((self.recovery_path / "output.jsonl").read_bytes(), self.output_bytes)
        self.assertEqual((self.recovery_path / "errors.jsonl").read_bytes(), b"")

    def test_status_appends_bound_usage_observations_and_never_calls_them_cost(self):
        self.phase("upload")
        self.phase("submit")

        omitted = self.phase("status")["usage_observation"]
        omitted_record = json.loads((self.state_path.parent / omitted["path"]).read_text(encoding="utf-8"))
        self.assertEqual(omitted_record["usage"]["availability"], "OMITTED")
        self.assertIsNone(omitted_record["usage"]["total_tokens"])
        self.assertEqual(omitted_record["cost_evidence"]["status"], "NOT_OBSERVED")
        self.assertFalse(omitted_record["closeout"]["operation_cost_attributable"])
        self.assertFalse(omitted_record["closeout"]["usage_bound_to_batch"])

        self.fake.batches.status_usage = SimpleNamespace(
            input_tokens=12, input_tokens_details=SimpleNamespace(cached_tokens=2),
            output_tokens=3, output_tokens_details=SimpleNamespace(reasoning_tokens=1),
            total_tokens=15,
        )
        present = self.phase("status")["usage_observation"]
        present_record = json.loads((self.state_path.parent / present["path"]).read_text(encoding="utf-8"))
        self.assertEqual(present_record["usage"]["availability"], "PRESENT")
        self.assertEqual(present_record["usage"]["completeness"], "COMPLETE")
        self.assertEqual(present_record["usage"]["total_tokens"], 15)
        self.assertTrue(present_record["closeout"]["usage_bound_to_batch"])
        self.assertEqual(present_record["binding"]["batch_id"], "batch-known")
        self.assertEqual(present_record["binding"]["reconciliation_id"],
                         json.loads(self.state_path.read_text(encoding="utf-8"))["reconciliation_id"])
        self.assertEqual(present_record["binding"]["request_jsonl_sha256"], digest(self.request_bytes))
        self.assertEqual(present_record["provider_snapshot_sha256"],
                         digest(driver.canonical_json({
                             "batch_id": "batch-known", "input_file_id": "file-input",
                             "endpoint": driver.EXPECTED_ENDPOINT, "completion_window": "24h",
                             "reconciliation_id": present_record["binding"]["reconciliation_id"],
                             "status": "completed", "request_counts": {"total": 1, "completed": 1, "failed": 0},
                             "model": driver.EXPECTED_MODEL, "usage": present_record["usage"],
                         })))
        self.assertEqual(present_record["rate_derived_estimate"]["status"], "NOT_COMPUTED")
        self.assertIsNone(present_record["rate_derived_estimate"]["amount"])
        self.assertEqual(len(list(self.state_path.parent.glob("state.json.accounting/*/usage-*.json"))), 2)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["history"][-1]["usage_observation_sha256"], present["sha256"])

    def test_status_preserves_partial_usage_and_rejects_invalid_or_inconsistent_counts(self):
        self.phase("upload")
        self.phase("submit")
        self.fake.batches.status_usage = SimpleNamespace(input_tokens=9, total_tokens=9)
        partial = self.phase("status")["usage_observation"]
        partial_record = json.loads((self.state_path.parent / partial["path"]).read_text(encoding="utf-8"))
        self.assertEqual(partial_record["usage"]["availability"], "PRESENT")
        self.assertEqual(partial_record["usage"]["completeness"], "PARTIAL")
        self.assertIsNone(partial_record["usage"]["output_tokens"])

        for invalid_usage in (
                "unexpected-usage-shape",
                SimpleNamespace(unrecognized=9),
                SimpleNamespace(input_tokens=1, input_tokens_details="bad-details",
                                output_tokens=0, total_tokens=1),
                SimpleNamespace(input_tokens=True, output_tokens=1, total_tokens=2),
                SimpleNamespace(input_tokens=2, output_tokens=2, total_tokens=5),
                SimpleNamespace(input_tokens=3, input_tokens_details=SimpleNamespace(cached_tokens=4),
                                output_tokens=0, total_tokens=3)):
            before = self.state_path.read_bytes()
            self.fake.batches.status_usage = invalid_usage
            with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                self.phase("status")
            self.assertEqual(self.state_path.read_bytes(), before)

    def test_status_receipt_write_failure_leaves_no_partial_record_or_state_advance(self):
        self.phase("upload")
        self.phase("submit")
        before = self.state_path.read_bytes()
        self.fake.batches.status_usage = SimpleNamespace(input_tokens=1, output_tokens=0, total_tokens=1)
        with mock.patch.object(driver.os, "link", side_effect=PermissionError("synthetic")):
            with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                self.phase("status")
        self.assertEqual(self.state_path.read_bytes(), before)
        self.assertEqual(list(self.state_path.parent.glob("state.json.accounting/*/usage-*.json")), [])
        self.assertEqual(list(self.state_path.parent.glob("state.json.accounting/*/*.pending")), [])

    def test_status_state_link_failure_preserves_standalone_receipt_for_reconciliation(self):
        self.phase("upload")
        self.phase("submit")
        before = self.state_path.read_bytes()
        self.fake.batches.status_usage = SimpleNamespace(input_tokens=4, output_tokens=2, total_tokens=6)
        with mock.patch.object(driver, "_write_state", side_effect=OSError("synthetic state-write failure")):
            with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                self.phase("status")
        self.assertEqual(self.state_path.read_bytes(), before)
        records = list(self.state_path.parent.glob("state.json.accounting/*/usage-*.json"))
        self.assertEqual(len(records), 1)
        receipt = json.loads(records[0].read_text(encoding="utf-8"))
        self.assertEqual(receipt["binding"]["batch_id"], "batch-known")
        self.assertEqual(receipt["binding"]["reconciliation_id"],
                         json.loads(before)["reconciliation_id"])
        self.assertEqual(receipt["usage"]["total_tokens"], 6)

    def test_offline_accounting_reconciliation_links_orphan_once_without_provider(self):
        self.phase("upload")
        self.phase("submit")
        before = self.state_path.read_bytes()
        self.fake.batches.status_usage = SimpleNamespace(input_tokens=4, output_tokens=2, total_tokens=6)
        with mock.patch.object(driver, "_write_state", side_effect=OSError("synthetic state-write failure")):
            with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                self.phase("status")
        orphan = next(self.state_path.parent.glob("state.json.accounting/*/usage-*.json"))
        orphan_before = orphan.read_bytes()
        unexpected = orphan.parent / "unrecognized-evidence.txt"
        unexpected_before = b"preserve and surface this evidence\n"
        unexpected.write_bytes(unexpected_before)

        # This operation has no transport parameter and must not construct an SDK.
        with mock.patch.object(driver, "_create_sdk_client") as sdk:
            result = driver.reconcile_usage_observations(state_path=self.state_path, facts=self.facts)
            sdk.assert_not_called()
        self.assertEqual(result["linked"], 1)
        self.assertEqual(result["already_linked"], 0)
        self.assertEqual(result["unrecognized_entries"], [unexpected.name])
        self.assertEqual(result["pending_entries"], [])
        self.assertFalse(result["provider_called"])
        self.assertFalse(result["dispatch_authorized"])
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["revision"], json.loads(before)["revision"] + 1)
        link = state["history"][-1]
        self.assertEqual(link["phase"], "reconcile-accounting")
        self.assertEqual(link["usage_observation_sha256"], digest(orphan_before))
        self.assertEqual(link["usage_observation_path"], orphan.relative_to(self.state_path.parent).as_posix())
        self.assertEqual(orphan.read_bytes(), orphan_before)

        linked_state = self.state_path.read_bytes()
        repeated = driver.reconcile_usage_observations(state_path=self.state_path, facts=self.facts)
        self.assertEqual(repeated["linked"], 0)
        self.assertEqual(repeated["already_linked"], 1)
        self.assertEqual(repeated["unrecognized_entries"], [unexpected.name])
        self.assertEqual(self.state_path.read_bytes(), linked_state)
        self.assertEqual(orphan.read_bytes(), orphan_before)
        self.assertEqual(unexpected.read_bytes(), unexpected_before)

    def test_offline_accounting_reconciliation_rejects_tampered_orphan_and_preserves_it(self):
        self.phase("upload")
        self.phase("submit")
        before = self.state_path.read_bytes()
        self.fake.batches.status_usage = SimpleNamespace(input_tokens=4, output_tokens=2, total_tokens=6)
        with mock.patch.object(driver, "_write_state", side_effect=OSError("synthetic state-write failure")):
            with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                self.phase("status")
        orphan = next(self.state_path.parent.glob("state.json.accounting/*/usage-*.json"))
        tampered = orphan.read_bytes().replace(b'"total_tokens":6', b'"total_tokens":7')
        self.assertNotEqual(tampered, orphan.read_bytes())
        orphan.write_bytes(tampered)
        with self.assertRaises(driver.DriverError):
            driver.reconcile_usage_observations(state_path=self.state_path, facts=self.facts)
        self.assertEqual(self.state_path.read_bytes(), before)
        self.assertEqual(orphan.read_bytes(), tampered)

    def test_cli_accounting_reconciliation_is_offline_and_needs_no_release(self):
        self.phase("upload")
        self.phase("submit")
        self.fake.batches.status_usage = SimpleNamespace(input_tokens=4, output_tokens=2, total_tokens=6)
        with mock.patch.object(driver, "_write_state", side_effect=OSError("synthetic state-write failure")):
            with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                self.phase("status")
        args = SimpleNamespace(
            root=self.root, package_dir=self.package, cases=self.cases_path, events=self.events_path,
            phase="reconcile-accounting", execute=False, release=None, state=self.state_path,
            recovery_dir=None,
        )
        stdout = io.StringIO()
        with mock.patch.object(driver, "load_contracts", return_value=self.contracts), \
                mock.patch.object(driver, "validate_local_inputs", return_value=self.facts), \
                mock.patch.object(driver, "_create_sdk_client") as sdk, \
                mock.patch("sys.stdout", stdout):
            result = driver._run_cli(args)
        sdk.assert_not_called()
        self.assertEqual(result, 0)
        output = json.loads(stdout.getvalue())
        self.assertEqual(output["linked"], 1)
        self.assertFalse(output["provider_called"])
        self.assertFalse(output["dispatch_authorized"])

    def test_usage_observation_collision_never_overwrites_existing_bytes(self):
        self.phase("upload")
        self.phase("submit")
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        response = self.fake.batches.retrieve("batch-known")
        observed_at = datetime(2026, 10, 26, tzinfo=timezone.utc)
        kwargs = {
            "state_path": self.state_path, "state": state, "facts": self.facts,
            "response": response, "status": "completed",
            "counts": {"total": 1, "completed": 1, "failed": 0}, "observed_at": observed_at,
        }
        first = driver._persist_batch_usage_observation(**kwargs)
        target = self.state_path.parent / first["path"]
        target.write_bytes(b"preserved conflicting evidence\n")
        with self.assertRaisesRegex(driver.DriverError, "observation conflicts"):
            driver._persist_batch_usage_observation(**kwargs)
        self.assertEqual(target.read_bytes(), b"preserved conflicting evidence\n")
        self.assertEqual(list(target.parent.glob("*.pending")), [])

    def test_upload_payload_hash_change_blocks_before_fake_transport(self):
        (self.package / "batch_requests.jsonl").write_bytes(b"changed\n")
        with self.assertRaises(driver.DriverError):
            self.phase("upload")
        self.assertEqual(self.fake.files.upload_count, 0)
        self.assertFalse(self.state_path.exists())

    def test_ambiguous_upload_is_recorded_and_never_retried(self):
        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                self.remote_files.append(SimpleNamespace(
                    id="file-reconciled", purpose="batch", object="file",
                    bytes=len(self.request_bytes), filename=kwargs["file"][0],
                    created_at=datetime.now(timezone.utc).timestamp(),
                ))
                raise RuntimeError("synthetic opaque exception")

            def content(self, file_id):
                if file_id == "file-reconciled":
                    return io.BytesIO(self.request_bytes)
                return super().content(file_id)

        fake = FakeTransport(self.request_bytes)
        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaisesRegex(driver.DriverError, "uncertain"):
            self.phase("upload", transport=fake)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["upload_phase"], "unknown")
        expected_filename = driver._upload_filename(state["reconciliation_id"])
        reconciled = self.phase("reconcile-upload", transport=fake)
        self.assertEqual(reconciled["status"], "reconciled")
        self.assertEqual(reconciled["file_id"], "file-reconciled")
        self.assertEqual(fake.files.upload_count, 1)
        with self.assertRaisesRegex(driver.DriverError, "never retry"):
            self.phase("upload", transport=fake)
        self.assertEqual(fake.files.upload_count, 1)

    def test_upload_reconciliation_keeps_zero_and_multiple_byte_matches_uncertain(self):
        self.phase("upload", transport=FakeTransport(self.request_bytes))
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        state["upload_phase"] = "unknown"
        state["file_id"] = None
        self.state_path.write_bytes(driver.canonical_json(state) + b"\n")
        empty = FakeTransport(self.request_bytes)
        no_match = self.phase("reconcile-upload", transport=empty)
        self.assertEqual(no_match["status"], "not_found")
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["upload_phase"], "unknown")
        expected_filename = driver._upload_filename(state["reconciliation_id"])

        duplicate = FakeTransport(self.request_bytes)
        duplicate.files.remote_files = [
            SimpleNamespace(id="file-a", purpose="batch", filename=expected_filename,
                            bytes=len(self.request_bytes), created_at=datetime.now(timezone.utc).timestamp()),
            SimpleNamespace(id="file-b", purpose="batch", filename=expected_filename,
                            bytes=len(self.request_bytes), created_at=datetime.now(timezone.utc).timestamp()),
        ]
        duplicate.files.content = lambda file_id: io.BytesIO(self.request_bytes)
        ambiguous = self.phase("reconcile-upload", transport=duplicate)
        self.assertEqual(ambiguous["status"], "ambiguous")
        self.assertEqual(state["upload_phase"], "unknown")

    def test_upload_reconciliation_rejects_wrong_bytes_and_duplicate_listing_ids(self):
        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost response")

        fake = FakeTransport(self.request_bytes)
        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaises(driver.DriverError):
            self.phase("upload", transport=fake)
        operation_state = json.loads(self.state_path.read_text(encoding="utf-8"))
        expected_filename = driver._upload_filename(operation_state["reconciliation_id"])
        fake.files.remote_files = [SimpleNamespace(
            id="file-wrong", purpose="batch", filename=expected_filename,
            bytes=len(self.request_bytes), created_at=datetime.now(timezone.utc).timestamp(),
        )]
        fake.files.content = lambda file_id: io.BytesIO(b"different bytes\n")
        self.assertEqual(self.phase("reconcile-upload", transport=fake)["status"], "not_found")
        self.assertEqual(fake.files.upload_count, 1)
        fake.files.remote_files.append(fake.files.remote_files[0])
        with self.assertRaisesRegex(driver.DriverError, "duplicate identities"):
            self.phase("reconcile-upload", transport=fake)
        self.assertEqual(fake.files.upload_count, 1)

    def test_upload_reconciliation_sanitizes_provider_content_acquisition_errors(self):
        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost upload response")

        fake = FakeTransport(self.request_bytes)
        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaisesRegex(driver.DriverError, "uncertain"):
            self.phase("upload", transport=fake)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        fake.files.remote_files = [SimpleNamespace(
            id="file-reconciled", purpose="batch", filename=driver._upload_filename(
                state["reconciliation_id"]), bytes=len(self.request_bytes),
            created_at=datetime.now(timezone.utc).timestamp(),
        )]
        fake.files.content = lambda _file_id: (_ for _ in ()).throw(
            RuntimeError("synthetic-provider-detail must-not-escape"))
        with self.assertRaises(driver.DriverError) as caught:
            self.phase("reconcile-upload", transport=fake)
        self.assertEqual(str(caught.exception), "remote file download failed")
        self.assertNotIn("synthetic-provider-detail", str(caught.exception))
        state_after = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state_after["upload_phase"], "unknown")
        self.assertIsNone(state_after["file_id"])

    def test_result_download_helper_sanitizes_provider_content_acquisition_errors(self):
        target = self.recovery_path / "output.partial"

        class FailingFiles:
            @staticmethod
            def content(_file_id):
                raise RuntimeError("synthetic-provider-detail must-not-escape")

        with self.assertRaises(driver.DriverError) as caught:
            driver._download_to_path(SimpleNamespace(files=FailingFiles()), "file-output", target,
                                     expected_count=1)
        self.assertEqual(str(caught.exception), "Batch result download failed")
        self.assertNotIn("synthetic-provider-detail", str(caught.exception))
        self.assertFalse(target.exists())

    def test_status_requires_matching_operation_metadata_and_completion_window(self):
        self.phase("upload")
        self.phase("submit")
        retrieve = self.fake.batches.retrieve
        state_before = json.loads(self.state_path.read_text(encoding="utf-8"))

        mutations = (
            lambda response: setattr(response, "completion_window", "12h"),
            lambda response: setattr(response, "metadata", {"senath_reconciliation_id": "0" * 64}),
            lambda response: setattr(response, "metadata", None),
        )
        for mutate in mutations:
            with self.subTest(mutation=mutate):
                def altered(batch_id):
                    response = retrieve(batch_id)
                    mutate(response)
                    return response

                self.fake.batches.retrieve = altered
                with self.assertRaisesRegex(driver.DriverError, "status retrieval failed"):
                    self.phase("status")
                self.assertEqual(json.loads(self.state_path.read_text(encoding="utf-8")), state_before)
        self.fake.batches.retrieve = retrieve
        self.assertEqual(self.phase("status")["status"], "completed")

    def test_upload_reconciliation_does_not_bind_an_identical_older_legacy_file(self):
        fake = FakeTransport(self.request_bytes)

        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost response")

        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaises(driver.DriverError):
            self.phase("upload", transport=fake)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        fake.files.remote_files = [SimpleNamespace(
            id="file-identical-legacy", purpose="batch", filename="batch_requests.jsonl",
            bytes=len(self.request_bytes), created_at=datetime.now(timezone.utc).timestamp(),
        )]
        fake.files.content = lambda file_id: io.BytesIO(self.request_bytes)
        result = self.phase("reconcile-upload", transport=fake)
        self.assertEqual(result["status"], "not_found")
        self.assertIsNone(json.loads(self.state_path.read_text(encoding="utf-8"))["file_id"])
        self.assertEqual(fake.files.upload_count, 1)
        self.assertNotEqual(driver._upload_filename(state["reconciliation_id"]), "batch_requests.jsonl")

    def test_legacy_uncertain_state_requires_manual_review_without_binding(self):
        fake = FakeTransport(self.request_bytes)

        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost response")

        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaises(driver.DriverError):
            self.phase("upload", transport=fake)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        state.pop("operation_nonce")
        state["reconciliation_id"] = driver._reconciliation_id(self.facts, self.release_hash)
        self.state_path.write_bytes(driver.canonical_json(state) + b"\n")
        fake.files.remote_files = [SimpleNamespace(
            id="file-legacy", purpose="batch", filename="batch_requests.jsonl",
            bytes=len(self.request_bytes), created_at=datetime.now(timezone.utc).timestamp(),
        )]
        with self.assertRaisesRegex(driver.DriverError, "manual review"):
            self.phase("reconcile-upload", transport=fake)
        self.assertIsNone(json.loads(self.state_path.read_text(encoding="utf-8"))["file_id"])
        self.assertEqual(fake.files.upload_count, 1)

    def test_legacy_pristine_state_upgrades_identity_before_upload(self):
        state, _ = driver._read_state(self.state_path, self.facts, self.release_hash)
        state.pop("operation_nonce")
        state["reconciliation_id"] = driver._reconciliation_id(self.facts, self.release_hash)
        self.state_path.write_bytes(driver.canonical_json(state) + b"\n")
        result = self.phase("upload")
        self.assertEqual(result["status"], "complete")
        upgraded = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(len(upgraded["operation_nonce"]), 32)
        self.assertEqual(upgraded["history"][0]["phase"], "identity_upgrade")
        self.assertEqual(self.fake.files.upload_count, 1)

    def test_upload_reconciliation_rejects_objects_outside_symmetric_time_window(self):
        fake = FakeTransport(self.request_bytes)

        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost response")

        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaises(driver.DriverError):
            self.phase("upload", transport=fake)
        operation_state = json.loads(self.state_path.read_text(encoding="utf-8"))
        fake.files.remote_files = [SimpleNamespace(
            id="file-outside-window", purpose="batch", filename=driver._upload_filename(
                operation_state["reconciliation_id"]),
            bytes=len(self.request_bytes),
            created_at=datetime.now(timezone.utc).timestamp() - 600,
        )]
        fake.files.content = lambda file_id: io.BytesIO(self.request_bytes)
        past = self.phase("reconcile-upload", transport=fake)
        self.assertEqual(past["status"], "not_found")
        fake.files.remote_files[0].created_at = datetime.now(timezone.utc).timestamp() + 600
        future = self.phase("reconcile-upload", transport=fake)
        self.assertEqual(future["status"], "not_found")
        self.assertEqual(fake.files.upload_count, 1)

    def test_reconciliation_rejects_nonfinite_remote_creation_time(self):
        fake = FakeTransport(self.request_bytes)

        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost response")

        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaises(driver.DriverError):
            self.phase("upload", transport=fake)
        operation_state = json.loads(self.state_path.read_text(encoding="utf-8"))
        fake.files.remote_files = [SimpleNamespace(
            id="file-nan-time", purpose="batch", filename=driver._upload_filename(
                operation_state["reconciliation_id"]),
            bytes=len(self.request_bytes), created_at=float("nan"),
        )]
        with self.assertRaisesRegex(driver.DriverError, "creation time"):
            self.phase("reconcile-upload", transport=fake)
        self.assertEqual(fake.files.upload_count, 1)

    def test_reconciliation_rejects_malformed_pagination_and_listing_errors(self):
        class AmbiguousFiles(FakeFiles):
            def create(self, **kwargs):
                self.upload_count += 1
                raise RuntimeError("synthetic lost response")

        fake = FakeTransport(self.request_bytes)
        fake.files = AmbiguousFiles(self.request_bytes)
        with self.assertRaises(driver.DriverError):
            self.phase("upload", transport=fake)
        fake.files.list = lambda **kwargs: SimpleNamespace(data=[], has_more=True, last_id=None)
        with self.assertRaisesRegex(driver.DriverError, "cursor"):
            self.phase("reconcile-upload", transport=fake)
        fake.files.list = lambda **kwargs: SimpleNamespace(
            data=[SimpleNamespace(id="file-page-item")], has_more=True, last_id="different-id")
        with self.assertRaisesRegex(driver.DriverError, "cursor"):
            self.phase("reconcile-upload", transport=fake)
        fake.files.list = lambda **kwargs: (_ for _ in ()).throw(RuntimeError("opaque provider error"))
        with self.assertRaisesRegex(driver.DriverError, "listing failed"):
            self.phase("reconcile-upload", transport=fake)
        self.assertEqual(fake.files.upload_count, 1)

    def test_post_upload_persistence_ambiguity_keeps_in_flight_and_never_retries(self):
        original_write = driver._write_state
        calls = 0

        def fail_after_mutation(path, new_state, expected_raw):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise driver.DriverError("synthetic durable state failure")
            return original_write(path, new_state, expected_raw)

        with mock.patch.object(driver, "_write_state", side_effect=fail_after_mutation):
            with self.assertRaisesRegex(driver.DriverError, "durable state failure"):
                self.phase("upload")
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["upload_phase"], "in_flight")
        with self.assertRaisesRegex(driver.DriverError, "never retry"):
            self.phase("upload")
        self.assertEqual(self.fake.files.upload_count, 1)

    def test_failed_batch_create_is_recorded_and_never_retried(self):
        self.phase("upload")

        class AmbiguousBatches(FakeBatches):
            def create(self, **kwargs):
                self.create_count += 1
                self.remote_batches.append(SimpleNamespace(
                    id="batch-reconciled", endpoint=kwargs["endpoint"],
                    input_file_id=kwargs["input_file_id"], completion_window=kwargs["completion_window"],
                    status="validating", metadata=kwargs["metadata"],
                    created_at=datetime.now(timezone.utc).timestamp(),
                    request_counts=SimpleNamespace(total=1, completed=0, failed=0),
                ))
                raise RuntimeError("synthetic opaque exception")

        fake = FakeTransport(self.request_bytes)
        fake.files = self.fake.files
        fake.batches = AmbiguousBatches()
        with self.assertRaisesRegex(driver.DriverError, "never retry"):
            self.phase("submit", transport=fake)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["submit_phase"], "unknown")
        rows, _ = self.contracts["t219"].read_jsonl(self.events_path)
        self.assertEqual([row["event_type"] for row in rows], ["DISPATCH_INTENT"])
        reconciled = self.phase("reconcile-submit", transport=fake)
        self.assertEqual(reconciled["status"], "reconciled")
        self.assertEqual(reconciled["batch_id"], "batch-reconciled")
        self.assertEqual(fake.batches.create_count, 1)
        with self.assertRaisesRegex(driver.DriverError, "never retry"):
            self.phase("submit", transport=fake)
        self.assertEqual(fake.batches.create_count, 1)

    def test_uncertain_submit_without_remote_match_stays_blocked_and_no_mutation(self):
        self.phase("upload")
        fake = FakeTransport(self.request_bytes)

        class LostBatches(FakeBatches):
            def create(self, **kwargs):
                self.create_count += 1
                raise RuntimeError("synthetic lost response")

        fake.batches = LostBatches()
        with self.assertRaises(driver.DriverError):
            self.phase("submit", transport=fake)
        result = self.phase("reconcile-submit", transport=fake)
        self.assertEqual(result["status"], "not_found")
        self.assertEqual(fake.batches.create_count, 1)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["submit_phase"], "unknown")

    def test_submit_reconciliation_requires_exact_metadata_endpoint_and_request_count(self):
        self.phase("upload")
        fake = FakeTransport(self.request_bytes)

        class LostBatches(FakeBatches):
            def create(self, **kwargs):
                self.create_count += 1
                raise RuntimeError("synthetic lost response")

        fake.batches = LostBatches()
        with self.assertRaises(driver.DriverError):
            self.phase("submit", transport=fake)
        operation_state = json.loads(self.state_path.read_text(encoding="utf-8"))
        wrong = SimpleNamespace(
            id="batch-wrong", input_file_id="file-input", endpoint=driver.EXPECTED_ENDPOINT,
            completion_window="24h", status="validating",
            metadata={"senath_reconciliation_id": "0" * 64},
            created_at=datetime.now(timezone.utc).timestamp(),
            request_counts=SimpleNamespace(total=1, completed=0, failed=0),
        )
        fake.batches.remote_batches = [wrong]
        self.assertEqual(self.phase("reconcile-submit", transport=fake)["status"], "not_found")
        wrong.metadata = {"senath_reconciliation_id": operation_state["reconciliation_id"]}
        wrong.endpoint = "/v1/chat/completions"
        self.assertEqual(self.phase("reconcile-submit", transport=fake)["status"], "not_found")
        wrong.endpoint = driver.EXPECTED_ENDPOINT
        wrong.completion_window = "12h"
        self.assertEqual(self.phase("reconcile-submit", transport=fake)["status"], "not_found")
        wrong.completion_window = "24h"
        wrong.request_counts = SimpleNamespace(total=2, completed=0, failed=0)
        result = self.phase("reconcile-submit", transport=fake)
        self.assertEqual(result["status"], "mismatched")
        self.assertEqual(result["candidate_count"], 1)
        self.assertEqual(fake.batches.create_count, 1)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["submit_phase"], "unknown")
        self.assertEqual(state["history"][-2]["result"], "mismatched")

    def test_invalid_result_identity_keeps_recovery_pending_and_blocks_repeat(self):
        self.phase("upload")
        self.phase("submit")
        self.phase("status")
        bad_bytes = self.output_bytes.replace(b"tok1", b"wrong")
        fake = FakeTransport(self.request_bytes, bad_bytes, self.error_bytes)
        with self.assertRaisesRegex(driver.DriverError, "uncertain"):
            self.phase("recover", transport=fake, recovery_dir=self.recovery_path)
        self.assertFalse(self.recovery_path.exists())
        self.assertEqual(len(list(self.recovery_path.parent.glob(".recovered.pending-*"))), 1)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["recovery_phase"], "unknown")
        with self.assertRaisesRegex(driver.DriverError, "preserve evidence and reconcile"):
            self.phase("reconcile-recovery", transport=self.fake, recovery_dir=self.recovery_path)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["recovery_phase"], "unknown")
        self.assertEqual(len(list(self.recovery_path.parent.glob(".recovered.pending-*"))), 2)
        with self.assertRaisesRegex(driver.DriverError, "recovery already started"):
            self.phase("recover", transport=fake, recovery_dir=self.recovery_path)

    def test_interrupted_output_read_reconciles_same_bytes_without_replaying_batch(self):
        self.phase("upload")
        self.phase("submit")
        self.phase("status")
        original_content = self.fake.files.content

        class InterruptedStream:
            def __init__(self, raw):
                self.raw = raw
                self.reads = 0

            def read(self, _size=-1):
                self.reads += 1
                if self.reads == 1:
                    return self.raw[:24]
                raise OSError("synthetic interrupted output transfer")

        def content(file_id):
            self.fake.files.content_calls.append(file_id)
            if file_id == "file-output":
                bound = json.loads(self.state_path.read_text(encoding="utf-8"))["recovery_binding"]
                self.assertEqual(bound["output_file_id"], "file-output")
                self.assertEqual(bound["expected_counts"], {"total": 1, "completed": 1, "failed": 0})
                self.assertEqual(bound["destination"], str(self.recovery_path.resolve()))
                return InterruptedStream(self.output_bytes)
            return original_content(file_id)

        self.fake.files.content = content
        with self.assertRaisesRegex(driver.DriverError, "reconcile"):
            self.phase("recover", recovery_dir=self.recovery_path)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["recovery_phase"], "unknown")
        first_attempt = state["recovery_binding"]["attempts"][0]
        partial = Path(first_attempt["pending_dir"]) / "output.partial"
        self.assertEqual(partial.read_bytes(), self.output_bytes[:24])
        self.assertEqual(first_attempt["partial_sha256"]["output"], digest(self.output_bytes[:24]))
        self.fake.files.content = original_content
        recovered = self.phase("reconcile-recovery", recovery_dir=self.recovery_path)
        self.assertEqual(recovered["status"], "complete")
        self.assertEqual((self.recovery_path / "output.jsonl").read_bytes(), self.output_bytes)
        self.assertEqual(len(list(self.recovery_path.parent.glob(".recovered.pending-*"))), 1)
        self.assertEqual(self.fake.files.upload_count, 1)
        self.assertEqual(self.fake.batches.create_count, 1)
        self.assertEqual(self.fake.batches.cancel_count, 0)

    def test_interrupted_error_read_reconciles_zero_output_one_error(self):
        self.phase("upload")
        self.phase("submit")
        error_bytes = b'{"id":"result-error","custom_id":"tok1","error":{"code":"synthetic"}}\n'
        self.fake.files.error_bytes = error_bytes
        self.fake.batches.status_output_file_id = None
        self.fake.batches.status_error_file_id = "file-errors"
        self.fake.batches.status_counts = (0, 1)
        self.phase("status")
        original_content = self.fake.files.content

        class InterruptedStream:
            def __init__(self, raw):
                self.raw = raw
                self.reads = 0

            def read(self, _size=-1):
                self.reads += 1
                if self.reads == 1:
                    return self.raw[:18]
                raise OSError("synthetic interrupted error transfer")

        def content(file_id):
            self.fake.files.content_calls.append(file_id)
            if file_id == "file-errors":
                return InterruptedStream(error_bytes)
            return original_content(file_id)

        self.fake.files.content = content
        with self.assertRaisesRegex(driver.DriverError, "reconcile"):
            self.phase("recover", recovery_dir=self.recovery_path)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["recovery_binding"]["expected_counts"],
                         {"total": 1, "completed": 0, "failed": 1})
        self.fake.files.content = original_content
        result = self.phase("reconcile-recovery", recovery_dir=self.recovery_path)
        self.assertEqual(result["status"], "complete")
        self.assertEqual((self.recovery_path / "output.jsonl").read_bytes(), b"")
        self.assertEqual((self.recovery_path / "errors.jsonl").read_bytes(), error_bytes)
        self.assertEqual(self.fake.files.upload_count, 1)
        self.assertEqual(self.fake.batches.create_count, 1)
        self.assertEqual(self.fake.batches.cancel_count, 0)

    def test_mismatched_partial_and_changed_journal_remain_blocked_without_reads(self):
        self.phase("upload")
        self.phase("submit")
        self.phase("status")
        original_content = self.fake.files.content

        class InterruptedStream:
            def __init__(self, raw):
                self.raw = raw
                self.reads = 0

            def read(self, _size=-1):
                self.reads += 1
                if self.reads == 1:
                    return b"not-a-prefix"
                raise OSError("synthetic interruption")

        def content(file_id):
            self.fake.files.content_calls.append(file_id)
            return InterruptedStream(self.output_bytes) if file_id == "file-output" else original_content(file_id)

        self.fake.files.content = content
        with self.assertRaises(driver.DriverError):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.fake.files.content = original_content
        reads_before = len(self.fake.files.content_calls)
        with self.assertRaisesRegex(driver.DriverError, "preserve evidence and reconcile"):
            self.phase("reconcile-recovery", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), reads_before + 1)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["recovery_phase"], "unknown")
        self.assertEqual(len(state["recovery_binding"]["attempts"]), 2)
        self.assertFalse(self.recovery_path.exists())
        self.assertEqual(self.fake.files.upload_count, 1)
        self.assertEqual(self.fake.batches.create_count, 1)
        self.assertEqual(self.fake.batches.cancel_count, 0)

    def test_changed_journal_and_target_collision_block_before_recovery_download(self):
        self.phase("upload")
        self.phase("submit")
        self.phase("status")
        original_events = self.events_path.read_bytes()
        self.events_path.write_bytes(b"changed\n")
        initial_reads = len(self.fake.files.content_calls)
        original_request = (self.package / "batch_requests.jsonl").read_bytes()
        (self.package / "batch_requests.jsonl").write_bytes(b"changed package\n")
        with self.assertRaisesRegex(driver.DriverError, "local package input changed"):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), initial_reads)
        (self.package / "batch_requests.jsonl").write_bytes(original_request)
        manifest_path = self.package / "manifest.json"
        manifest_path.write_bytes(b'{"schema":"changed"}\n')
        with self.assertRaisesRegex(driver.DriverError, "local package input changed"):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), initial_reads)
        manifest_path.write_bytes(self.package_manifest_bytes)
        original_mapping = (self.package / "local_only_mapping.jsonl").read_bytes()
        (self.package / "local_only_mapping.jsonl").write_bytes(b"changed mapping\n")
        with self.assertRaisesRegex(driver.DriverError, "local package input changed"):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), initial_reads)
        (self.package / "local_only_mapping.jsonl").write_bytes(original_mapping)
        with self.assertRaisesRegex(driver.DriverError, "journal input changed"):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), initial_reads)
        self.events_path.write_bytes(original_events)
        self.recovery_path.mkdir()
        with self.assertRaisesRegex(driver.DriverError, "target exists"):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), initial_reads)
        self.assertEqual(self.fake.files.upload_count, 1)
        self.assertEqual(self.fake.batches.create_count, 1)
        self.assertEqual(self.fake.batches.cancel_count, 0)

    def test_raced_empty_target_is_not_replaced_by_promotion(self):
        self.phase("upload")
        self.phase("submit")
        self.phase("status")
        real_promote = driver._promote_directory_noreplace

        def race_empty_target(source, destination):
            # This runs after execute_phase's pre-promotion exists() check,
            # deterministically modeling another process creating the target.
            destination.mkdir()
            return real_promote(source, destination)

        with mock.patch.object(driver, "_promote_directory_noreplace", side_effect=race_empty_target):
            with self.assertRaisesRegex(driver.DriverError, "preserve evidence and reconcile"):
                self.phase("recover", recovery_dir=self.recovery_path)

        self.assertTrue(self.recovery_path.is_dir())
        self.assertEqual(list(self.recovery_path.iterdir()), [])
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        self.assertEqual(state["recovery_phase"], "unknown")
        pending = Path(state["recovery_binding"]["attempts"][-1]["pending_dir"])
        self.assertEqual((pending / "output.jsonl").read_bytes(), self.output_bytes)
        self.assertEqual((pending / "errors.jsonl").read_bytes(), b"")
        self.assertTrue((pending / "receipt.json").is_file())
        self.assertEqual(self.fake.files.upload_count, 1)
        self.assertEqual(self.fake.batches.create_count, 1)
        self.assertEqual(self.fake.batches.cancel_count, 0)

    def test_promotion_fails_closed_when_platform_primitive_is_unavailable(self):
        pending = self.recovery_path.parent / ".recovered.pending-test"
        pending.mkdir()
        (pending / "receipt.json").write_bytes(b"synthetic receipt\n")
        with mock.patch.object(driver.sys, "platform", "darwin"):
            with self.assertRaisesRegex(driver.DriverError, "promotion is unavailable"):
                driver._promote_directory_noreplace(pending, self.recovery_path)
        self.assertTrue(pending.is_dir())
        self.assertEqual((pending / "receipt.json").read_bytes(), b"synthetic receipt\n")
        self.assertFalse(self.recovery_path.exists())

    def test_stale_recovery_binding_and_destination_mismatch_fail_before_download(self):
        self.phase("upload")
        self.phase("submit")
        self.phase("status")
        original_content = self.fake.files.content

        class InterruptedStream:
            def read(self, _size=-1):
                raise OSError("synthetic interruption")

        def content(file_id):
            self.fake.files.content_calls.append(file_id)
            return InterruptedStream() if file_id == "file-output" else original_content(file_id)

        self.fake.files.content = content
        with self.assertRaises(driver.DriverError):
            self.phase("recover", recovery_dir=self.recovery_path)
        self.fake.files.content = original_content
        before = len(self.fake.files.content_calls)
        with self.assertRaisesRegex(driver.DriverError, "destination differs"):
            self.phase("reconcile-recovery", recovery_dir=self.recovery_path.parent / "other")
        self.assertEqual(len(self.fake.files.content_calls), before)
        state = json.loads(self.state_path.read_text(encoding="utf-8"))
        state["recovery_binding"]["output_file_id"] = "stale-file-id"
        self.state_path.write_bytes(driver.canonical_json(state) + b"\n")
        with self.assertRaisesRegex(driver.DriverError, "recovery operation binding"):
            self.phase("reconcile-recovery", recovery_dir=self.recovery_path)
        self.assertEqual(len(self.fake.files.content_calls), before)

    def test_recovery_rejects_missing_extra_and_duplicate_result_ids(self):
        invalid_payloads = [
            b"",  # missing row / zero lines
            self.output_bytes + self.output_bytes,  # extra line and duplicate custom ID
            self.output_bytes.replace(b"tok1", b"unknown-token"),  # unknown intent identity
        ]
        for index, payload in enumerate(invalid_payloads):
            with self.subTest(index=index):
                instance = SenathBatchTransportTests()
                instance.setUp()
                try:
                    instance.phase("upload")
                    instance.phase("submit")
                    instance.phase("status")
                    instance.fake.files.output_bytes = payload
                    with self.assertRaises(driver.DriverError):
                        instance.phase("recover", recovery_dir=instance.recovery_path)
                    self.assertFalse(instance.recovery_path.exists())
                    self.assertEqual(instance.fake.files.upload_count, 1)
                    self.assertEqual(instance.fake.batches.create_count, 1)
                    self.assertEqual(instance.fake.batches.cancel_count, 0)
                finally:
                    instance.tearDown()

    def test_release_validation_requires_independent_source_pin(self):
        with self.assertRaisesRegex(driver.DriverError, "no independently lead-pinned"):
            driver.validate_release(b"{}", self.facts, trusted_release_sha256=None)

    def test_release_without_cost_scenario_fails_before_any_provider_phase(self):
        release = {"schema": "SENATH_C2_BATCH_RELEASE_V1"}
        raw = (json.dumps(release, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode()
        with self.assertRaisesRegex(driver.DriverError, "unexpected schema"):
            driver.validate_release(raw, self.facts, trusted_release_sha256=digest(raw))

    def test_exact_release_gates_are_bound_and_stale_or_changed_receipts_fail(self):
        now = datetime(2026, 1, 1, tzinfo=timezone.utc)
        base_receipt = {"sha256": "1" * 64, "observed_at": "2025-12-31T00:00:00Z",
                        "valid_through": "2026-01-03T00:00:00Z", "attributed_to": "synthetic reviewer"}
        release = {
            "schema": "SENATH_C2_BATCH_RELEASE_V2", "release_id": "synthetic-only",
            "issued_at": "2025-12-31T00:00:00Z", "expires_at": "2026-01-02T00:00:00Z",
            "protocol_sha256": driver.PROTOCOL_SHA256,
            "t253_manifest_sha256": self.facts["package_manifest_sha256"],
            "t208_manifest_sha256": self.facts["parent_t208_manifest_sha256"],
            "request_bodies_sha256": self.facts["request_bodies_sha256"],
            "dispositions_sha256": self.facts["dispositions_sha256"],
            "case_inventory_sha256": self.facts["case_inventory_sha256"],
            "ready_case_set_sha256": digest(driver.canonical_json(self.facts["ready_ids"])),
            "denominator": 2, "request_count": 1, "preparation_error_count": 1,
            "payload_fields": driver.EXPECTED_PAYLOAD_FIELDS, "model": driver.EXPECTED_MODEL,
            "endpoint": driver.EXPECTED_ENDPOINT, "completion_window": "24h",
            "max_attempts_per_case": 1, "reserved_attempts": 1, "reserved_cost_usd": 2.0,
            "cost_scenario": {
                "schema": "SENATH_C2_PINNED_COST_SCENARIO_V1",
                "scenario_sha256": "a" * 64, "evidence_bundle_sha256": "b" * 64,
                "preflight_report_sha256": "c" * 64, "token_receipt_report_sha256": "d" * 64,
                "scenario_id": "FULL_TEST_CANDIDATE", "request_count": 1,
                "maximum_total_cost_usd": "1.75",
            },
            "rights_receipt": {**base_receipt, "scope": "provider_batch_upload"},
            "capacity_receipt": {**base_receipt, "scope": "openai_batch_capacity",
                                  "remaining_authorized_calls": 5},
            "cost_receipt": {**base_receipt, "scope": "openai_batch_cost",
                              "remaining_authorized_budget_usd": 5.0},
        }
        raw = (json.dumps(release, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode()
        trusted = digest(raw)
        checked, checked_hash = driver.validate_release(raw, self.facts, now=now,
                                                         trusted_release_sha256=trusted)
        self.assertEqual(checked_hash, trusted)
        self.assertEqual(checked["release_id"], "synthetic-only")
        invalid_changes = [
            ("reserved_attempts", 2),
            ("endpoint", "/v1/chat/completions"),
            ("model", "substitute-model"),
            ("t253_manifest_sha256", "0" * 64),
            ("request_count", 2),
            ("expires_at", "2025-12-31T00:00:00Z"),
            ("reserved_cost_usd", 1.74),
        ]
        for field, value in invalid_changes:
            with self.subTest(field=field):
                changed = copy.deepcopy(release)
                changed[field] = value
                tampered = (json.dumps(changed, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode()
                with self.assertRaises(driver.DriverError):
                    driver.validate_release(tampered, self.facts, now=now,
                                            trusted_release_sha256=digest(tampered))

        fallback_scenario = copy.deepcopy(release)
        fallback_scenario["cost_scenario"]["scenario_id"] = "FALLBACK_2000_CANDIDATE"
        tampered = (json.dumps(fallback_scenario, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode()
        with self.assertRaisesRegex(driver.DriverError, "cost scenario"):
            driver.validate_release(tampered, self.facts, now=now, trusted_release_sha256=digest(tampered))

        nonfinite_reservation = copy.deepcopy(release)
        nonfinite_reservation["reserved_cost_usd"] = float("nan")
        tampered = (json.dumps(nonfinite_reservation, sort_keys=True, separators=(",", ":"), allow_nan=True) + "\n").encode()
        with self.assertRaises(driver.DriverError):
            driver.validate_release(tampered, self.facts, now=now, trusted_release_sha256=digest(tampered))

        stale_receipt = copy.deepcopy(release)
        stale_receipt["rights_receipt"]["valid_through"] = "2025-12-30T00:00:00Z"
        tampered = (json.dumps(stale_receipt, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode()
        with self.assertRaisesRegex(driver.DriverError, "receipt"):
            driver.validate_release(tampered, self.facts, now=now,
                                    trusted_release_sha256=digest(tampered))

    def test_unpinned_cli_release_fails_before_sdk_construction(self):
        release_path = Path(self.temp.name) / "synthetic-release.json"
        release_path.write_bytes(b"{}\n")
        args = SimpleNamespace(
            root=self.root, package_dir=self.package, cases=self.cases_path, events=self.events_path,
            phase="upload", execute=True, release=release_path, state=self.state_path, recovery_dir=None,
        )
        with mock.patch.object(driver, "load_contracts", return_value=self.contracts), \
                mock.patch.object(driver, "validate_local_inputs", return_value=self.facts), \
                mock.patch.object(driver, "_create_sdk_client") as sdk:
            with self.assertRaises(driver.DriverError):
                driver._run_cli(args)
            sdk.assert_not_called()

    def test_malformed_driver_state_fails_before_sdk_construction(self):
        malformed = {
            "schema": "SENATH_C2_BATCH_DRIVER_STATE_V1",
            "revision": 0,
            "package_manifest_sha256": self.package_hash,
            "case_inventory_sha256": self.facts["case_inventory_sha256"],
            "release_sha256": self.release_hash,
            "history": [],
            "dispatch_authorized": False,
            "provider_called": False,
        }
        self.state_path.write_bytes(driver.canonical_json(malformed) + b"\n")
        with mock.patch.object(driver, "_create_sdk_client") as sdk:
            with self.assertRaises(driver.DriverError):
                driver.execute_phase(
                    phase="upload", facts=self.facts, contracts=self.contracts,
                    state_path=self.state_path, release={}, release_hash=self.release_hash,
                    transport=None,
                )
            sdk.assert_not_called()

    def test_in_flight_upload_state_blocks_before_sdk_construction(self):
        state, _ = driver._read_state(self.state_path, self.facts, self.release_hash)
        state["upload_phase"] = "in_flight"
        self.state_path.write_bytes(driver.canonical_json(state) + b"\n")
        with mock.patch.object(driver, "_create_sdk_client") as sdk:
            with self.assertRaisesRegex(driver.DriverError, "never retry"):
                driver.execute_phase(
                    phase="upload", facts=self.facts, contracts=self.contracts,
                    state_path=self.state_path, release={}, release_hash=self.release_hash,
                    transport=None,
                )
            sdk.assert_not_called()

    def test_sdk_retry_policy_is_explicitly_zero(self):
        fake_openai = SimpleNamespace(OpenAI=mock.Mock(return_value="client"))
        with mock.patch.dict(sys.modules, {"openai": fake_openai}):
            self.assertEqual(driver._create_sdk_client(), "client")
        fake_openai.OpenAI.assert_called_once_with(max_retries=0, timeout=60.0)

    def test_batch_create_response_requires_status_and_counts(self):
        response = SimpleNamespace(
            id="batch", endpoint=driver.EXPECTED_ENDPOINT, input_file_id="file-input",
            completion_window="24h", status=None, request_counts=None,
        )
        with self.assertRaisesRegex(driver.DriverError, "status/count"):
            driver._validate_batch_response(response, "file-input")


if __name__ == "__main__":
    unittest.main()
