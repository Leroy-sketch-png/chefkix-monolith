from __future__ import annotations

import hashlib
import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
from preflight_c2_openai_cost import (  # noqa: E402
    FALLBACK_CASES,
    FULL_CASES,
    PreflightError,
    case_identity,
    canonical_json,
    main,
    ordered_ids_sha256,
    preflight,
    request_body,
    request_size_report,
    validate_cohort,
)


NOW = datetime.now(timezone.utc)
SYSTEM = (
    "You generate ranked ingredient substitutions for a recipe. Given only the recipe title, its ingredient list, "
    "and the ingredient to replace, return exactly 20 distinct ingredient names that could replace that ingredient "
    "in this recipe, ordered from most to least suitable in this recipe context. The ingredient list is an array of slots: "
    "each inner list is one ingredient slot, and its strings are aliases for the same ingredient, not separate ingredients. "
    "Do not infer that the first alias is canonical. The exact ingredient-to-replace spelling identifies one slot; do not return any alias from that entire slot. Do not include the ingredient to "
    "replace itself. Do not include explanations, scores, citations, or any text outside the required JSON object."
)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def json_write(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")


class C2PreflightTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def records(self, path: Path, count: int, *, start: int = 0) -> list[dict]:
        result = []
        with path.open("w", encoding="utf-8", newline="\n") as stream:
            for i in range(start, start + count):
                rec = {
                    "case_id": f"case-{i:05d}",
                    "source_fields": {"recipe_title": "A recipe title", "ingredients": [["salt"], ["oil", "neutral oil"], ["beans", "legumes"]],
                                      "target_ingredient": "beans"},
                    "payload": {"recipe_title": "A recipe title", "ingredient_list": [["salt"], ["oil", "neutral oil"], ["beans", "legumes"]],
                                "ingredient_to_replace": "beans"},
                }
                result.append(rec)
                stream.write(json.dumps(rec, ensure_ascii=False, separators=(",", ":")) + "\n")
        return result

    def capture_ref(self, name: str, content: str) -> dict:
        path = self.root / name
        path.write_text(content, encoding="utf-8", newline="\n")
        return {"path": name, "sha256": digest(path)}

    def valid_inputs(self, *, account_available: str = "100.00", max_age_hours: int = 1):
        full_path, fallback_path = self.root / "full.jsonl", self.root / "fallback.jsonl"
        full = self.records(full_path, FULL_CASES)
        fallback = self.records(fallback_path, FALLBACK_CASES)
        observed = (NOW - timedelta(hours=max_age_hours)).isoformat().replace("+00:00", "Z")
        selector = self.root / "selector.py"
        selector.write_text("# synthetic selector fixture only\n", encoding="utf-8")
        selector_ref = {"path": selector.name, "sha256": digest(selector)}
        receipts = {
            "pricing.json": {
                "observed_at": observed, "model": "gpt-4o-2024-08-06", "currency": "USD",
                "input_usd_per_million": "2.50", "output_usd_per_million": "10.00",
                "source_url": "https://developers.openai.com/api/docs/models/gpt-4o",
                "capture_evidence": self.capture_ref("pricing-capture.txt", "SYNTHETIC TEST FIXTURE: price capture\n"),
            },
            "availability.json": {
                "observed_at": observed, "model": "gpt-4o-2024-08-06", "status": "verified_callable",
                "source_url": "https://platform.openai.com/docs/models",
                "capture_evidence": self.capture_ref("availability-capture.txt", "SYNTHETIC TEST FIXTURE: availability\n"),
            },
            "ledger.json": {
                "observed_at": observed, "evidence_kind": "synthetic-local-ledger-fixture", "account_alias": "test-account",
                "reconciliation_status": "reconciled", "spent_usd": "0", "active_reservations_usd": "0",
                "uncertain_calls_usd": "0", "calls_spent": 0, "calls_reserved": 0, "uncertain_calls": 0,
                "entries": [{"entry_id": "zero-spend", "state": "spent", "usd": "0", "calls": 0}],
                "capture_evidence": self.capture_ref("ledger-capture.txt", "SYNTHETIC TEST FIXTURE: ledger capture\n"),
            },
            "account.json": {
                "observed_at": observed, "account_alias": "test-account", "scope": "project_available_balance",
                "available_usd": account_available, "source_url": "https://platform.openai.com/settings/organization/billing/overview",
                "capture_evidence": self.capture_ref("account-capture.txt", "SYNTHETIC TEST FIXTURE: account capture\n"),
            },
        }
        refs = {}
        for name, receipt in receipts.items():
            path = self.root / name
            json_write(path, receipt)
            refs[name] = {"path": name, "sha256": digest(path)}
        selection = {
            "observed_at": observed, "evidence_kind": "synthetic-local-selection-fixture", "seed": 20261021,
            "method": "stratified_without_replacement_partition1_x_oracle_v1",
            "full_cohort_sha256": digest(full_path), "fallback_cohort_sha256": digest(fallback_path),
            "full_case_ids_sha256": ordered_ids_sha256(full), "selected_case_ids_sha256": ordered_ids_sha256(fallback),
            "selector_code_sha256": selector_ref["sha256"], "selector_code": selector_ref,
            "stratum_counts": {"unseen_profiled": 800, "unseen_unprofiled": 600,
                               "seen_profiled": 400, "seen_unprofiled": 200},
            "capture_evidence": self.capture_ref("selection-capture.txt", "SYNTHETIC TEST FIXTURE: selection receipt\n"),
        }
        selection_path = self.root / "selection.json"
        json_write(selection_path, selection)
        refs["selection"] = {"path": selection_path.name, "sha256": digest(selection_path)}
        manifest = {
            "protocol_sha256": "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12",
            "model": "gpt-4o-2024-08-06", "system_message": SYSTEM, "max_output_tokens": 256,
            "pricing_evidence": refs["pricing.json"],
            "model_availability": refs["availability.json"], "budget_ledger": refs["ledger.json"],
            "account_availability": refs["account.json"], "full_cohort": {"path": full_path.name, "sha256": digest(full_path)},
            "fallback_cohort": {"path": fallback_path.name, "sha256": digest(fallback_path)},
            "fallback_selection_receipt": refs["selection"],
        }
        manifest_path = self.root / "manifest.json"
        json_write(manifest_path, manifest)
        return manifest_path, full_path

    def set_account_available(self, manifest_path: Path, amount: Decimal) -> None:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        receipt_path = self.root / "account.json"
        receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
        receipt["available_usd"] = str(amount)
        json_write(receipt_path, receipt)
        manifest["account_availability"]["sha256"] = digest(receipt_path)
        json_write(manifest_path, manifest)

    def mutate_receipt(self, manifest_path: Path, key: str, filename: str, update) -> None:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        receipt_path = self.root / filename
        receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
        update(receipt)
        json_write(receipt_path, receipt)
        manifest[key]["sha256"] = digest(receipt_path)
        json_write(manifest_path, manifest)

    def test_full_cohort_remains_blocked_without_exact_input_token_counts(self):
        manifest, _ = self.valid_inputs(account_available="100.00")
        report = preflight(manifest, now=NOW)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertIsNone(report["conditional_scenario_if_receipts_are_true"])
        self.assertFalse(report["cohort_selected"])
        self.assertIsNone(report["selected_cohort"])
        self.assertFalse(report["dispatch_authorized"])
        self.assertIsNone(report["full_test_request_size"]["model_input_tokens"])
        self.assertNotIn("call_reservation", report["full_test_request_size"])
        self.assertIn("exact model input-token counts are unavailable", report["blocked_reasons"][0])

    def test_fallback_remains_blocked_without_exact_input_token_counts(self):
        manifest, _ = self.valid_inputs(account_available="20.00")
        report = preflight(manifest, now=NOW)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertIsNone(report["conditional_scenario_if_receipts_are_true"])
        self.assertFalse(report["cohort_selected"])
        self.assertIsNone(report["selected_cohort"])

    def test_fallback_blocked_when_hard_money_residual_does_not_fit(self):
        manifest, _ = self.valid_inputs(account_available="0.01")
        report = preflight(manifest, now=NOW)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertIsNone(report["conditional_scenario_if_receipts_are_true"])
        self.assertFalse(report["cohort_selected"])
        self.assertIsNone(report["selected_cohort"])

    def test_exact_payload_mapping_and_exclusion_of_label(self):
        path = self.root / "bad.jsonl"
        rec = {"case_id": "x", "source_fields": {"recipe_title": "soup", "ingredients": [["salt"]],
                                                        "target_ingredient": "salt"},
               "payload": {"recipe_title": "soup", "ingredient_list": [["salt"]],
                           "ingredient_to_replace": "salt", "gold_substitute": "gold"}}
        path.write_text(json.dumps(rec) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "payload keys"):
            validate_cohort(path, 1, "bad")

    def test_wrong_target_mapping_rejected(self):
        path = self.root / "bad-mapping.jsonl"
        rec = {"case_id": "x", "source_fields": {"recipe_title": "soup", "ingredients": [["beans", "legumes"], ["salt"]],
                                                       "target_ingredient": "beans"},
               "payload": {"recipe_title": "soup", "ingredient_list": [["beans", "legumes"], ["salt"]],
                           "ingredient_to_replace": "mushrooms"}}
        path.write_text(json.dumps(rec) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "exact mapping"):
            validate_cohort(path, 1, "bad mapping")

    def test_flattened_alias_groups_rejected(self):
        path = self.root / "flattened.jsonl"
        flat = ["beans", "legumes", "salt"]
        row = {"case_id": "flattened", "source_fields": {"recipe_title": "Soup", "ingredients": flat,
                                                              "target_ingredient": "beans"},
               "payload": {"recipe_title": "Soup", "ingredient_list": flat,
                           "ingredient_to_replace": "beans"}}
        path.write_text(json.dumps(row) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "preserve nested alias groups"):
            validate_cohort(path, 1, "flattened")

    def test_reordered_or_rewritten_nested_aliases_rejected(self):
        source_groups = [["beans", "legumes"], ["salt"]]
        mutated_payload_groups = {
            "outer_slot_reorder": [["salt"], ["beans", "legumes"]],
            "within_slot_alias_reorder": [["legumes", "beans"], ["salt"]],
            "alias_rewrite": [["beans", "pulses"], ["salt"]],
        }
        for mutation, payload_groups in mutated_payload_groups.items():
            with self.subTest(mutation=mutation):
                path = self.root / f"{mutation}.jsonl"
                row = {
                    "case_id": mutation,
                    "source_fields": {
                        "recipe_title": "Soup",
                        "ingredients": source_groups,
                        "target_ingredient": "beans",
                    },
                    "payload": {
                        "recipe_title": "Soup",
                        "ingredient_list": payload_groups,
                        "ingredient_to_replace": "beans",
                    },
                }
                path.write_text(json.dumps(row) + "\n", encoding="utf-8")
                with self.assertRaisesRegex(PreflightError, "exact mapping"):
                    validate_cohort(path, 1, mutation)

    def test_exact_target_resolves_even_when_normalized_variant_occurs_elsewhere(self):
        path = self.root / "ambiguous-target.jsonl"
        groups = [["beans", "legumes"], ["Beans!"], ["salt"]]
        row = {"case_id": "ambiguous", "source_fields": {"recipe_title": "Soup", "ingredients": groups,
                                                              "target_ingredient": "beans"},
               "payload": {"recipe_title": "Soup", "ingredient_list": groups,
                           "ingredient_to_replace": "beans"}}
        path.write_text(json.dumps(row) + "\n", encoding="utf-8")
        validate_cohort(path, 1, "exact-match")

    def test_normalized_collision_without_exact_target_fails_closed(self):
        path = self.root / "normalized-collision.jsonl"
        groups = [["Beans!"], ["BEANS"], ["salt"]]
        row = {"case_id": "collision", "source_fields": {"recipe_title": "Soup", "ingredients": groups,
                                                                "target_ingredient": "beans"},
               "payload": {"recipe_title": "Soup", "ingredient_list": groups,
                           "ingredient_to_replace": "beans"}}
        path.write_text(json.dumps(row) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "exact target spelling must occur in exactly one alias group"):
            validate_cohort(path, 1, "normalized-collision")

    def test_zero_rates_and_non_authoritative_capture_url_rejected(self):
        manifest, _ = self.valid_inputs()
        self.mutate_receipt(manifest, "pricing_evidence", "pricing.json", lambda r: r.update(input_usd_per_million="0", output_usd_per_million="0"))
        with self.assertRaisesRegex(PreflightError, "positive"):
            preflight(manifest, now=NOW)
        manifest, _ = self.valid_inputs()
        updated = json.loads(manifest.read_text(encoding="utf-8"))
        updated["framing_allowance_tokens"] = 1
        json_write(manifest, updated)
        with self.assertRaisesRegex(PreflightError, "manifest keys differ"):
            preflight(manifest, now=NOW)

    def test_wrong_full_cohort_size_rejected_without_override(self):
        manifest, full_path = self.valid_inputs()
        rows = full_path.read_text(encoding="utf-8").splitlines()
        full_path.write_text("\n".join(rows[:-1]) + "\n", encoding="utf-8")
        data = json.loads(manifest.read_text(encoding="utf-8"))
        data["full_cohort"]["sha256"] = digest(full_path)
        json_write(manifest, data)
        with self.assertRaisesRegex(PreflightError, "expected 10747"):
            preflight(manifest, now=NOW)

    def test_fallback_seed_or_cohort_binding_mismatch_rejected(self):
        manifest, _ = self.valid_inputs()
        self.mutate_receipt(manifest, "fallback_selection_receipt", "selection.json", lambda r: r.update(seed=42))
        with self.assertRaisesRegex(PreflightError, "frozen seed"):
            preflight(manifest, now=NOW)

    def test_receipt_and_capture_hashes_are_both_pinned(self):
        manifest, _ = self.valid_inputs()
        capture = self.root / "pricing-capture.txt"
        capture.write_text("altered", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "SHA-256 mismatch"):
            preflight(manifest, now=NOW)

    def test_itemized_ledger_mismatch_and_uncertain_underreservation_rejected(self):
        manifest, _ = self.valid_inputs()
        self.mutate_receipt(manifest, "budget_ledger", "ledger.json", lambda r: r.update(spent_usd="1"))
        with self.assertRaisesRegex(PreflightError, "aggregate spent_usd"):
            preflight(manifest, now=NOW)
        manifest, _ = self.valid_inputs()
        def uncertain(r):
            r.update(uncertain_calls=1, uncertain_calls_usd="0.00000001",
                     entries=[{"entry_id": "unknown", "state": "uncertain", "usd": "0.00000001", "calls": 1,
                               "worst_case_usd_per_call": "0.00000001"}])
        self.mutate_receipt(manifest, "budget_ledger", "ledger.json", uncertain)
        with self.assertRaisesRegex(PreflightError, "input token cost is unknown"):
            preflight(manifest, now=NOW)

    def test_call_cap_boundary_blocks_when_no_calls_remain(self):
        manifest, _ = self.valid_inputs()
        def full_calls(r):
            r.update(calls_spent=25_000, entries=[{"entry_id": "spent", "state": "spent", "usd": "0", "calls": 25_000}])
        self.mutate_receipt(manifest, "budget_ledger", "ledger.json", full_calls)
        report = preflight(manifest, now=NOW)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertIsNone(report["conditional_scenario_if_receipts_are_true"])
        self.assertFalse(report["cohort_selected"])
        self.assertIsNone(report["selected_cohort"])

    def test_preflight_does_not_emit_unsubstantiated_cost_estimates(self):
        manifest, _ = self.valid_inputs()
        report = preflight(manifest, now=NOW)
        self.assertIsNone(report["full_test_request_size"]["output_cost_usd"])
        self.assertIsNone(report["full_test_request_size"]["model_input_token_cost_usd"])

    def test_request_size_counts_utf8_bytes_without_claiming_model_tokens(self):
        payload = {
            "recipe_title": "Crème brûlée 猫",
            "ingredient_list": [["café", "coffee"], ["jalapeño"]],
            "ingredient_to_replace": "café",
        }
        manifest = {"system_message": SYSTEM, "max_output_tokens": 256}
        prices = {
            "input_usd_per_million": "2.50",
            "output_usd_per_million": "10.00",
        }
        serialized_body = canonical_json(request_body(manifest, payload))
        from preflight_c2_openai_cost import request_size_report
        report = request_size_report([{"payload": payload}], manifest)
        self.assertGreater(len(serialized_body), len(serialized_body.decode("utf-8")))
        self.assertEqual(
            report["canonical_request_body_utf8_bytes_total"],
            len(serialized_body),
        )
        self.assertEqual(
            report["canonical_request_body_utf8_bytes_max_per_case"],
            len(serialized_body),
        )
        self.assertIsNone(report["model_input_tokens"])

    def test_stale_evidence_blocks(self):
        manifest, _ = self.valid_inputs(max_age_hours=25)
        with self.assertRaisesRegex(PreflightError, "24-hour"):
            preflight(manifest, now=NOW)

    def test_duplicate_case_ids_rejected(self):
        path = self.root / "duplicate.jsonl"
        row = {"case_id": "same", "source_fields": {"recipe_title": "Soup", "ingredients": [["salt"]],
                                                              "target_ingredient": "salt"},
               "payload": {"recipe_title": "Soup", "ingredient_list": [["salt"]],
                           "ingredient_to_replace": "salt"}}
        path.write_text(json.dumps(row) + "\n" + json.dumps(row) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "duplicate"):
            validate_cohort(path, 2, "duplicate")

    def test_malformed_nested_payload_emits_structured_blocked_report(self):
        manifest_path, full_path = self.valid_inputs()
        rows = full_path.read_text(encoding="utf-8").splitlines()
        first = json.loads(rows[0])
        first["payload"] = None
        rows[0] = json.dumps(first, ensure_ascii=False, separators=(",", ":"))
        full_path.write_text("\n".join(rows) + "\n", encoding="utf-8")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["full_cohort"]["sha256"] = digest(full_path)
        json_write(manifest_path, manifest)

        output = io.StringIO()
        with patch.object(sys, "argv", ["preflight_c2_openai_cost.py", "--manifest", str(manifest_path)]):
            with redirect_stdout(output):
                exit_code = main()

        self.assertEqual(exit_code, 2)
        parsed = json.loads(output.getvalue())
        self.assertEqual(parsed["status"], "BLOCKED")
        self.assertFalse(parsed["dispatch_authorized"])
        self.assertIn("payload keys must be exactly", parsed["error"])

    def test_request_excludes_identity_and_body_hash_is_emitted(self):
        request = request_body({"system_message": SYSTEM, "max_output_tokens": 256}, {
            "recipe_title": "Soup", "ingredient_list": [["beans", "legumes"]], "ingredient_to_replace": "beans"
        })
        self.assertEqual(request["model"], "gpt-4o-2024-08-06")
        self.assertNotIn("case_id", json.dumps(request))
        self.assertNotIn("target_ingredient", json.dumps(request))
        self.assertEqual(request["text"]["format"]["schema"]["properties"]["substitutions"]["minItems"], 20)
        self.assertEqual(request["text"]["format"]["schema"]["properties"]["substitutions"]["maxItems"], 20)
        manifest, _ = self.valid_inputs()
        report = preflight(manifest, now=NOW)
        self.assertEqual(len(report["request_body_sha256_by_case"]), FULL_CASES)
        self.assertRegex(report["serializer_source_sha256"], r"^[0-9a-f]{64}$")

    def test_mixed_ready_and_sanitized_errors_preserve_denominator_and_body_scope(self):
        path = self.root / "mixed.jsonl"
        ready = {
            "case_id": "opaque-ready", "source_fields": {"recipe_title": "Soup", "ingredients": [["salt"]], "target_ingredient": "salt"},
            "payload": {"recipe_title": "Soup", "ingredient_list": [["salt"]], "ingredient_to_replace": "salt"},
        }
        error = {"case_ref": "opaque-error", "reason": "TARGET_SLOT_MATCH_MULTIPLE", "status": "PREPARATION_ERROR"}
        path.write_text(json.dumps(ready) + "\n" + json.dumps(error) + "\n", encoding="utf-8")
        records, _ = validate_cohort(path, 2, "mixed")
        summary = request_size_report(records, {"system_message": SYSTEM, "max_output_tokens": 256})
        self.assertEqual(summary["denominator_cases"], 2)
        self.assertEqual(summary["requestable_cases"], 1)
        self.assertEqual(summary["preparation_error_cases"], 1)
        self.assertEqual(summary["preparation_error_reasons"], {"TARGET_SLOT_MATCH_MULTIPLE": 1})
        self.assertEqual(summary["canonical_request_body_utf8_bytes_total"], len(canonical_json(request_body({"system_message": SYSTEM, "max_output_tokens": 256}, ready["payload"]))))
        self.assertEqual(ordered_ids_sha256(records), hashlib.sha256(b"opaque-ready\nopaque-error\n").hexdigest())

    def test_duplicate_preparation_error_identity_rejected(self):
        path = self.root / "duplicate-errors.jsonl"
        error = {"case_ref": "same-error", "reason": "TARGET_SLOT_MATCH_MULTIPLE", "status": "PREPARATION_ERROR"}
        path.write_text(json.dumps(error) + "\n" + json.dumps(error) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "duplicate"):
            validate_cohort(path, 2, "duplicate errors")

    def test_unknown_or_unsanitized_preparation_error_rejected(self):
        path = self.root / "bad-error.jsonl"
        unknown = {"case_ref": "opaque-error", "reason": "RAW_RECIPE_TEXT: leaked", "status": "PREPARATION_ERROR"}
        path.write_text(json.dumps(unknown) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "unknown preparation-error reason"):
            validate_cohort(path, 1, "unknown error")
        payload_bearing = {"case_ref": "opaque-error", "reason": "TARGET_SLOT_MATCH_MULTIPLE", "status": "PREPARATION_ERROR", "payload": {}}
        path.write_text(json.dumps(payload_bearing) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "unsupported cohort record"):
            validate_cohort(path, 1, "payload-bearing error")

    def test_generated_ready_requires_exact_existing_payload_contract(self):
        path = self.root / "generated-ready.jsonl"
        row = {
            "case_id": "opaque-id", "case_ref": "opaque-ref", "status": "READY",
            "source_fields": {"recipe_title": "Soup", "ingredients": [["salt"]], "target_ingredient": "salt"},
            "payload": {"recipe_title": "Soup", "ingredient_list": [["salt"]], "ingredient_to_replace": "salt"},
        }
        path.write_text(json.dumps(row) + "\n", encoding="utf-8")
        records, _ = validate_cohort(path, 1, "generated ready")
        self.assertEqual(case_identity(records[0]), "opaque-ref")
        row["payload"]["gold_substitute"] = "forbidden"
        path.write_text(json.dumps(row) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(PreflightError, "payload keys"):
            validate_cohort(path, 1, "forbidden payload")


if __name__ == "__main__":
    unittest.main()
