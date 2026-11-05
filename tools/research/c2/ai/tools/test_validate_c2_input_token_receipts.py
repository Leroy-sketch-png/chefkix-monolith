from __future__ import annotations

import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_c2_input_token_receipts import PROTOCOL_SHA256, ReceiptError, validate_receipts  # noqa: E402


MODEL = "gpt-4o-2024-08-06"
ENDPOINT = "https://api.openai.com/v1/responses/input_tokens"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class InputTokenReceiptTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.report = self.root / "preflight.json"
        self.receipts = self.root / "counts.jsonl"
        self.report_data = {
            "status": "BLOCKED",
            "selected_cohort": None,
            "conditional_scenario_if_receipts_are_true": None,
            "protocol_sha256": PROTOCOL_SHA256,
            "model": MODEL,
            "dispatch_authorized": False,
            "cohort_selected": False,
            "evidence": {"manifest_sha256": "1" * 64},
            "serializer_source_sha256": "2" * 64,
            "request_body_sha256_by_case": {
                "opaque-1": hashlib.sha256(b"request-1").hexdigest(),
                "opaque-2": hashlib.sha256(b"request-2").hexdigest(),
            },
        }
        self.write_report()
        self.write_receipts()

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_protocol_pin_matches_frozen_protocol_file(self) -> None:
        protocol_path = Path(__file__).resolve().parents[2] / "research" / "protocols" / "IRON-CHEF-PROTOCOL-v1.md"
        self.assertEqual(len(PROTOCOL_SHA256), 64)
        self.assertEqual(digest(protocol_path), PROTOCOL_SHA256)

    def write_report(self) -> None:
        self.report.write_text(json.dumps(self.report_data), encoding="utf-8")

    def write_receipts(self, *, counts: tuple[int, int] = (101, 202), refs: tuple[str, str] = ("opaque-1", "opaque-2")) -> None:
        lines = []
        for index, (ref, count) in enumerate(zip(refs, counts)):
            capture = self.root / f"capture-{index}-{ref}.json"
            capture.write_text(json.dumps({"object": "response.input_tokens", "input_tokens": count}), encoding="utf-8")
            lines.append({
                "case_ref": ref,
                "request_body_sha256": self.report_data["request_body_sha256_by_case"].get(ref, "0" * 64),
                "model": MODEL,
                "endpoint": ENDPOINT,
                "observed_at": "2026-10-23T12:00:00Z",
                "input_tokens": count,
                "response_capture": {"path": capture.name, "sha256": digest(capture)},
            })
        self.receipts.write_text("".join(json.dumps(line) + "\n" for line in lines), encoding="utf-8")

    def test_valid_exact_receipt_set_is_aggregated_but_not_authorized(self) -> None:
        result = validate_receipts(self.report, self.receipts)
        self.assertEqual(result["status"], "LOCAL_CONSISTENCY_CHECK_ONLY_UNAUTHENTICATED_INPUTS")
        self.assertEqual(result["input_tokens_total"], 303)
        self.assertEqual(result["input_tokens_by_case_ref"], {"opaque-1": 101, "opaque-2": 202})
        self.assertFalse(result["cohort_selected"])
        self.assertFalse(result["reservation_installed"])
        self.assertFalse(result["dispatch_authorized"])
        self.assertEqual(result["billing_and_call_cap_treatment"], "UNKNOWN_NOT_EVALUATED")
        self.assertEqual(result["preflight_report_authenticity"], "UNVERIFIED_CALLER_SUPPLIED_REPORT")
        self.assertEqual(result["request_body_binding_claim"], "MATCHES_HASH_MAP_IN_UNAUTHENTICATED_REPORT_ONLY")

    def test_missing_duplicate_and_unknown_cases_rejected(self) -> None:
        self.write_receipts(refs=("opaque-1", "opaque-1"))
        with self.assertRaisesRegex(ReceiptError, "duplicates"):
            validate_receipts(self.report, self.receipts)
        self.write_receipts(refs=("opaque-1", "unknown"))
        with self.assertRaisesRegex(ReceiptError, "unknown case"):
            validate_receipts(self.report, self.receipts)
        self.receipts.write_text("", encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "does not cover"):
            validate_receipts(self.report, self.receipts)

    def test_changed_model_endpoint_or_request_hash_rejected(self) -> None:
        rows = [json.loads(line) for line in self.receipts.read_text(encoding="utf-8").splitlines()]
        rows[0]["model"] = "another-model"
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "frozen model"):
            validate_receipts(self.report, self.receipts)
        rows[0]["model"] = MODEL
        rows[0]["request_body_sha256"] = "0" * 64
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "not bound"):
            validate_receipts(self.report, self.receipts)

    def test_nonpositive_boolean_or_float_counts_rejected(self) -> None:
        for value in (0, -1, True, 1.5):
            with self.subTest(value=value):
                self.write_receipts()
                rows = [json.loads(line) for line in self.receipts.read_text(encoding="utf-8").splitlines()]
                rows[0]["input_tokens"] = value
                self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
                with self.assertRaisesRegex(ReceiptError, "positive integer"):
                    validate_receipts(self.report, self.receipts)

    def test_capture_hash_and_response_shape_are_verified(self) -> None:
        rows = [json.loads(line) for line in self.receipts.read_text(encoding="utf-8").splitlines()]
        capture = self.root / rows[0]["response_capture"]["path"]
        capture.write_text('{"object":"response.input_tokens","input_tokens":101,"secret":"not-allowed"}', encoding="utf-8")
        rows[0]["response_capture"]["sha256"] = digest(capture)
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "official minimal input-token response"):
            validate_receipts(self.report, self.receipts)

    def test_boolean_capture_count_is_not_accepted_as_integer_one(self) -> None:
        rows = [json.loads(line) for line in self.receipts.read_text(encoding="utf-8").splitlines()]
        capture = self.root / rows[0]["response_capture"]["path"]
        capture.write_text('{"object":"response.input_tokens","input_tokens":true}', encoding="utf-8")
        rows[0]["input_tokens"] = 1
        rows[0]["response_capture"]["sha256"] = digest(capture)
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "official minimal input-token response"):
            validate_receipts(self.report, self.receipts)

    def test_capture_path_escape_and_extra_receipt_fields_rejected(self) -> None:
        rows = [json.loads(line) for line in self.receipts.read_text(encoding="utf-8").splitlines()]
        rows[0]["response_capture"] = {"path": "../outside.json", "sha256": "0" * 64}
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "escapes"):
            validate_receipts(self.report, self.receipts)

        self.write_receipts()
        rows = [json.loads(line) for line in self.receipts.read_text(encoding="utf-8").splitlines()]
        rows[0]["recipe_title"] = "must not be present"
        self.receipts.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "unsupported shape"):
            validate_receipts(self.report, self.receipts)

    def test_dispatching_or_wrong_protocol_report_rejected(self) -> None:
        self.report_data["dispatch_authorized"] = True
        self.write_report()
        with self.assertRaisesRegex(ReceiptError, "non-dispatch-authorizing"):
            validate_receipts(self.report, self.receipts)

    def test_conflicting_preflight_decisions_and_duplicate_json_keys_rejected(self) -> None:
        self.report_data["status"] = "BLOCKED"
        self.report_data["reservation_installed"] = True
        self.write_report()
        with self.assertRaisesRegex(ReceiptError, "non-dispatch-authorizing"):
            validate_receipts(self.report, self.receipts)

        self.report_data.pop("reservation_installed")
        self.write_report()
        self.receipts.write_text(
            '{"case_ref":"opaque-1","case_ref":"opaque-2"}\n{"case_ref":"opaque-1","case_ref":"opaque-2"}\n',
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ReceiptError, "duplicate JSON key"):
            validate_receipts(self.report, self.receipts)

        capture = self.root / "duplicate-capture.json"
        capture.write_text('{"object":"response.input_tokens","input_tokens":101,"input_tokens":202}', encoding="utf-8")
        with self.assertRaisesRegex(ReceiptError, "duplicate JSON key"):
            from validate_c2_input_token_receipts import read_object
            read_object(capture, "capture")
        self.report_data["dispatch_authorized"] = False
        self.report_data["status"] = "READY"
        self.write_report()
        with self.assertRaisesRegex(ReceiptError, "non-dispatch-authorizing"):
            validate_receipts(self.report, self.receipts)

    def test_selected_cohort_and_conditional_scenario_rejected(self) -> None:
        for field in ("selected_cohort", "conditional_scenario_if_receipts_are_true"):
            with self.subTest(field=field):
                self.report_data[field] = {"case_count": 1}
                self.write_report()
                with self.assertRaisesRegex(ReceiptError, "non-dispatch-authorizing"):
                    validate_receipts(self.report, self.receipts)
                self.report_data.pop(field)


if __name__ == "__main__":
    unittest.main()
