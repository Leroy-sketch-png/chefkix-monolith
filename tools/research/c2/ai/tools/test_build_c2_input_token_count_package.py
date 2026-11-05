import unittest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_c2_input_token_count_package as token_package
from build_c2_input_token_count_package import PackageError, project_generation_body, validate_dispositions

serializer = token_package.serializer


class ProjectGenerationBodyTests(unittest.TestCase):
    def test_uses_exact_frozen_serializer_source(self):
        import hashlib

        source_hash = hashlib.sha256(Path(serializer.__file__).read_bytes()).hexdigest()
        self.assertEqual(source_hash, token_package.SERIALIZER_SHA256)

    def setUp(self):
        payload = {
            "recipe_title": "Synthetic recipe",
            "ingredient_list": [["salt", "table_salt"], ["oil"]],
            "ingredient_to_replace": "salt",
        }
        manifest = {"system_message": "Synthetic system instruction", "max_output_tokens": 256}
        self.body = serializer.request_body(manifest, payload)

    def test_projects_minimal_fields_and_preserves_entire_input_and_schema(self):
        projected = project_generation_body(self.body)
        self.assertEqual(set(projected), {"model", "input", "text"})
        self.assertEqual(projected["model"], self.body["model"])
        self.assertEqual(projected["input"], self.body["input"])
        self.assertEqual(projected["text"], self.body["text"])
        self.assertEqual(projected["input"][1]["content"], self.body["input"][1]["content"])
        self.assertIn("table_salt", projected["input"][1]["content"])
        self.assertNotIn("temperature", projected)
        self.assertNotIn("max_output_tokens", projected)
        self.assertNotIn("store", projected)

    def test_rejects_unapproved_top_level_field(self):
        body = dict(self.body, case_ref="local-identity")
        with self.assertRaises(PackageError):
            project_generation_body(body)

    def test_rejects_wrong_model_or_generation_setting(self):
        for key, value in (("model", "another-model"), ("temperature", 0.1), ("max_output_tokens", 257), ("store", True)):
            body = dict(self.body)
            body[key] = value
            with self.subTest(key=key), self.assertRaises(PackageError):
                project_generation_body(body)

    def test_rejects_changed_message_order(self):
        body = dict(self.body)
        body["input"] = list(reversed(self.body["input"]))
        with self.assertRaises(PackageError):
            project_generation_body(body)

    def test_rejects_payload_alias_slot_flattening(self):
        body = dict(self.body)
        body["input"] = [dict(row) for row in self.body["input"]]
        altered = {"recipe_title": "Synthetic recipe", "ingredient_list": ["salt", "table_salt", ["oil"]], "ingredient_to_replace": "salt"}
        body["input"][1]["content"] = serializer.canonical_json(altered).decode("utf-8")
        with self.assertRaises(PackageError):
            project_generation_body(body)

    def test_rejects_ambiguous_or_missing_target_slot(self):
        for slots in ([["salt"], ["salt", "table_salt"]], [["oil"]]):
            body = dict(self.body)
            body["input"] = [dict(row) for row in self.body["input"]]
            payload = {"recipe_title": "Synthetic recipe", "ingredient_list": slots, "ingredient_to_replace": "salt"}
            body["input"][1]["content"] = serializer.canonical_json(payload).decode("utf-8")
            with self.subTest(slots=slots), self.assertRaises(PackageError):
                project_generation_body(body)

    def test_rejects_changed_structured_output(self):
        body = dict(self.body)
        body["text"] = {"format": {"type": "json_object"}}
        with self.assertRaises(PackageError):
            project_generation_body(body)

    def test_disposition_helper_preserves_ready_error_denominator(self):
        rows = [
            {"case_ref": "ref-a", "request_ordinal": 0, "status": "READY"},
            {"case_ref": "ref-b", "request_ordinal": 1, "status": "READY"},
            {"case_ref": "ref-c", "reason": "TARGET_SLOT_MATCH_MULTIPLE", "status": "PREPARATION_ERROR"},
        ]
        mapping, errors = validate_dispositions(
            rows,
            expected_ready=2,
            expected_denominator=3,
            expected_error_reasons={"TARGET_SLOT_MATCH_MULTIPLE": 1},
        )
        self.assertEqual([row["request_ordinal"] for row in mapping], [0, 1])
        self.assertEqual(errors, {"TARGET_SLOT_MATCH_MULTIPLE": 1})

    def test_disposition_helper_rejects_duplicate_identity_and_ordinal(self):
        base = [
            {"case_ref": "ref-a", "request_ordinal": 0, "status": "READY"},
            {"case_ref": "ref-b", "request_ordinal": 1, "status": "READY"},
            {"case_ref": "ref-c", "reason": "TARGET_SLOT_MATCH_MULTIPLE", "status": "PREPARATION_ERROR"},
        ]
        duplicate_identity = [dict(row) for row in base]
        duplicate_identity[1]["case_ref"] = "ref-a"
        with self.assertRaises(PackageError):
            validate_dispositions(duplicate_identity, expected_ready=2, expected_denominator=3, expected_error_reasons={"TARGET_SLOT_MATCH_MULTIPLE": 1})
        duplicate_ordinal = [dict(row) for row in base]
        duplicate_ordinal[1]["request_ordinal"] = 0
        with self.assertRaises(PackageError):
            validate_dispositions(duplicate_ordinal, expected_ready=2, expected_denominator=3, expected_error_reasons={"TARGET_SLOT_MATCH_MULTIPLE": 1})

    def test_disposition_helper_rejects_missing_ordinal_and_wrong_denominator(self):
        rows = [
            {"case_ref": "ref-a", "request_ordinal": 0, "status": "READY"},
            {"case_ref": "ref-b", "request_ordinal": 1, "status": "READY"},
            {"case_ref": "ref-c", "reason": "TARGET_SLOT_MATCH_MULTIPLE", "status": "PREPARATION_ERROR"},
        ]
        missing_ordinal = [dict(row) for row in rows]
        missing_ordinal[1]["request_ordinal"] = None
        with self.assertRaises(PackageError):
            validate_dispositions(missing_ordinal, expected_ready=2, expected_denominator=3, expected_error_reasons={"TARGET_SLOT_MATCH_MULTIPLE": 1})
        with self.assertRaises(PackageError):
            validate_dispositions(rows, expected_ready=2, expected_denominator=4, expected_error_reasons={"TARGET_SLOT_MATCH_MULTIPLE": 1})


if __name__ == "__main__":
    unittest.main()
