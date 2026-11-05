from __future__ import annotations

import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

import preflight_c2_with_token_receipts as bridge  # noqa: E402
from test_preflight_c2_openai_cost import C2PreflightTests, NOW  # noqa: E402


class CompositionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.receipts = self.root / "receipts.jsonl"
        capture = self.root / "capture.json"
        capture.write_text(json.dumps({"object": "response.input_tokens", "input_tokens": 123}), encoding="utf-8")
        self.capture_ref = {"path": capture.name, "sha256": hashlib.sha256(capture.read_bytes()).hexdigest()}
        self.report = {
            "status": "BLOCKED",
            "protocol_sha256": "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12",
            "model": "gpt-4o-2024-08-06",
            "dispatch_authorized": False,
            "cohort_selected": False,
            "selected_cohort": None,
            "conditional_scenario_if_receipts_are_true": None,
            "reservation_installed": False,
            "evidence": {"manifest_sha256": "a" * 64},
            "serializer_source_sha256": "b" * 64,
            "request_body_sha256_by_case": {"opaque-a": "c" * 64, "opaque-b": "d" * 64},
            "model_input_tokens": None,
            "model_input_token_cost_usd": None,
        }
        self._write_receipts(self.report["request_body_sha256_by_case"])

    def tearDown(self) -> None:
        self.temp.cleanup()

    def _write_receipts(self, expected: dict[str, str]) -> None:
        rows = []
        for ref, digest in expected.items():
            rows.append({
                "case_ref": ref,
                "request_body_sha256": digest,
                "model": "gpt-4o-2024-08-06",
                "endpoint": "https://api.openai.com/v1/responses/input_tokens",
                "observed_at": "2026-10-23T00:00:00Z",
                "input_tokens": 123,
                "response_capture": self.capture_ref,
            })
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")

    def test_current_real_preflight_report_shape_composes_without_changing_gate(self) -> None:
        fixture = C2PreflightTests()
        fixture.setUp()
        try:
            manifest_path, _ = fixture.valid_inputs()
            current = bridge.preflight(manifest_path, now=NOW)
            self._write_receipts(current["request_body_sha256_by_case"])
            generated_reports: list[dict] = []
            real_validator = bridge.validate_receipts

            def inspect_then_validate(report_path: Path, receipt_path: Path) -> dict:
                generated_reports.append(json.loads(report_path.read_text(encoding="utf-8")))
                return real_validator(report_path, receipt_path)

            with patch.object(bridge, "validate_receipts", side_effect=inspect_then_validate) as validator:
                result = bridge.compose_preflight_and_receipts(manifest_path, self.receipts, now=NOW)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertIs(result["dispatch_authorized"], False)
            self.assertIs(result["cohort_selected"], False)
            self.assertIsNone(result["selected_cohort"])
            self.assertEqual(result["preflight"], current)
            self.assertEqual(result["input_token_receipt_consistency"]["requestable_cases"], len(current["request_body_sha256_by_case"]))
            self.assertIsNone(result["preflight"]["full_test_request_size"]["model_input_tokens"])
            self.assertIsNone(result["preflight"]["full_test_request_size"]["model_input_token_cost_usd"])
            self.assertIsNone(result["preflight"]["fallback_2000_request_size"]["model_input_tokens"])
            self.assertIsNone(result["preflight"]["fallback_2000_request_size"]["model_input_token_cost_usd"])
            self.assertEqual(validator.call_count, 1)
            self.assertEqual(generated_reports, [current])
        finally:
            fixture.tearDown()

    def test_rejects_nonblocked_preflight(self) -> None:
        scenario = {**self.report, "status": "SCENARIO_ESTIMATE_ONLY"}
        with patch.object(bridge, "preflight", return_value=scenario):
            with self.assertRaisesRegex(bridge.PreflightError, "requires the existing blocked preflight"):
                bridge.compose_preflight_and_receipts(self.root / "manifest.json", self.receipts)

    def test_rejects_preflight_that_authorizes_dispatch(self) -> None:
        unsafe = {**self.report, "dispatch_authorized": True}
        with patch.object(bridge, "preflight", return_value=unsafe):
            with self.assertRaisesRegex(bridge.PreflightError, "unexpectedly selected"):
                bridge.compose_preflight_and_receipts(self.root / "manifest.json", self.receipts)

    def test_rejects_cost_or_selection_fields_becoming_resolved(self) -> None:
        unsafe = {**self.report, "model_input_tokens": 246}
        with patch.object(bridge, "preflight", return_value=unsafe):
            with self.assertRaisesRegex(bridge.PreflightError, "remain unresolved"):
                bridge.compose_preflight_and_receipts(self.root / "manifest.json", self.receipts)

    def test_rejects_receipt_hash_not_bound_to_genuine_preflight(self) -> None:
        self._write_receipts({"opaque-a": "f" * 64, "opaque-b": "d" * 64})
        with patch.object(bridge, "preflight", return_value=self.report):
            with self.assertRaisesRegex(bridge.ReceiptError, "not bound to the frozen request body"):
                bridge.compose_preflight_and_receipts(self.root / "manifest.json", self.receipts)

    def test_rejects_existing_output_instead_of_overwriting(self) -> None:
        target = self.root / "already.json"
        target.write_text("preserve", encoding="utf-8")
        with patch.object(bridge, "compose_preflight_and_receipts", return_value={"status": "BLOCKED"}):
            with patch.object(sys, "argv", ["bridge", "--manifest", "m", "--input-token-receipts", "r", "--output", str(target)]):
                self.assertEqual(bridge.main(), 2)
        self.assertEqual(target.read_text(encoding="utf-8"), "preserve")
