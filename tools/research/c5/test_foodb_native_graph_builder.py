from __future__ import annotations

import json
import hashlib
import tempfile
import unittest
from pathlib import Path

try:
    from tools.research.c5.foodb_native_graph_builder import (
        aggregate_rows,
        extract_profile_scope,
        _iter_member,
        _seal_manifest,
        project_object,
        require_profile_ids_in_compounds,
    )
except ModuleNotFoundError:
    from foodb_native_graph_builder import (
        aggregate_rows,
        extract_profile_scope,
        _iter_member,
        _seal_manifest,
        project_object,
        require_profile_ids_in_compounds,
    )


class ProjectedJsonTests(unittest.TestCase):
    def test_member_stream_projects_rows_and_hashes_original_bytes(self):
        raw = b'{"id":1,"unused":{"text":"private"}}\n{"id":2,"unused":[1,2]}\n'
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "fixture.jsonl"
            path.write_bytes(raw)
            digest = hashlib.sha256()
            rows = list(_iter_member(path, "fixture", {"id"}, digest))
        self.assertEqual(rows, [{"id": 1}, {"id": 2}])
        self.assertEqual(digest.hexdigest(), hashlib.sha256(raw).hexdigest())

    def test_unselected_nested_values_are_skipped(self):
        row = project_object(
            '{"id":4,"name":"never-retained","nested":[{"text":"x\\u1234"}],"flag":true}',
            {"id", "flag"},
        )
        self.assertEqual(row, {"id": 4, "flag": True})

    def test_duplicate_keys_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "duplicate"):
            project_object('{"id":1,"id":2}', {"id"})

    def test_malformed_unselected_json_is_rejected(self):
        with self.assertRaises(ValueError):
            project_object('{"id":1,"unused":{"a":,}}', {"id"})

    def test_non_json_constants_are_rejected_in_selected_and_skipped_values(self):
        for token in ("NaN", "Infinity", "-Infinity"):
            for allowed in ({"export"}, {"id"}):
                with self.subTest(token=token, allowed=allowed), self.assertRaises(ValueError):
                    project_object('{"id":1,"export":' + token + "}", allowed)

    def test_duplicate_profile_keys_are_rejected_after_json_decoding(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "profile.json"
            path.write_text(r'{"same":[],"s\u0061me":[]}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate top-level profile key"):
                extract_profile_scope(path, None, None, None)

    def test_trailing_data_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "trailing"):
            project_object('{"id":1}x', {"id"})

    def test_profile_extraction_does_not_return_profile_names(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "profile.json"
            profile = {
                f"profile-name-{i}": [
                    {"compound_id": (i % 206) + 1, "food_public_ids": [f"food-public-{i}"], "name": "not-needed"}
                ]
                for i in range(971)
            }
            path.write_text(json.dumps(profile), encoding="utf-8")
            ids, pairs, profile_count = extract_profile_scope(
                path, expected_profile_names=971, expected_compound_ids=206, expected_pairs=971
            )
        self.assertEqual(len(ids), 206)
        self.assertEqual(len(pairs), 971)
        self.assertEqual(profile_count, 971)
        self.assertEqual(min(ids), 1)
        self.assertEqual(max(ids), 206)


class ManifestSealTests(unittest.TestCase):
    def test_persisted_manifest_contains_digest_of_canonical_payload(self):
        original = {"schema": "fixture.v1", "counts": {"pairs": 2}}
        sealed, encoded = _seal_manifest(original)
        self.assertIn(b'"manifest_payload_sha256"', encoded)
        self.assertEqual(sealed["manifest_payload_sha256"], hashlib.sha256(
            (json.dumps(original, sort_keys=True, indent=2, ensure_ascii=True) + "\n").encode("utf-8")
        ).hexdigest())
        self.assertNotIn("manifest_payload_sha256", original)


class NativeEdgeAggregationTests(unittest.TestCase):
    def setUp(self):
        self.foods = [
            {"id": 10, "public_id": "food-a", "export_to_foodb": True},
            {"id": 20, "public_id": "food-b", "export_to_foodb": False},
        ]
        self.compounds = [{"id": 100}, {"id": 200}]
        self.profile_ids = {100}
        self.profile_pairs = {(100, "food-a"), (100, "food-b")}

    def test_exact_typed_join_counts_support_and_export_state(self):
        contents = [
            {"food_id": 10, "source_type": "Compound", "source_id": 100, "export": 1},
            {"food_id": 10, "source_type": "Compound", "source_id": 100, "export": 0},
            {"food_id": 20, "source_type": "Compound", "source_id": 100, "export": 1},
            {"food_id": 10, "source_type": "Nutrient", "source_id": 100, "export": 1},
            {"food_id": 10, "source_type": "Compound", "source_id": 200, "export": 1},
        ]
        foods, compounds, edges, counts = aggregate_rows(
            self.foods, self.compounds, contents, self.profile_ids, self.profile_pairs
        )
        self.assertEqual(edges, {(10, 100): [2, 1], (20, 100): [1, 0]})
        self.assertEqual(counts["selected_content_rows"], 3)
        self.assertTrue(foods[10])
        self.assertFalse(foods[20])
        self.assertEqual(compounds, {100, 200})

    def test_profile_ids_must_resolve_to_compound_table(self):
        with self.assertRaisesRegex(ValueError, "do not resolve"):
            aggregate_rows(self.foods, [{"id": 200}], [], {100}, self.profile_pairs)

    def test_duplicate_food_public_ids_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "duplicate selected Food.public_id"):
            aggregate_rows(
                self.foods + [{"id": 30, "public_id": "food-a", "export_to_foodb": True}],
                self.compounds,
                [],
                self.profile_ids,
                self.profile_pairs,
            )

    def test_unresolved_profile_public_id_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "does not resolve"):
            aggregate_rows(
                self.foods,
                self.compounds,
                [],
                self.profile_ids,
                {(100, "missing-food")},
            )

    def test_unselected_food_public_id_is_not_required(self):
        foods = self.foods + [{"id": 30, "export_to_foodb": False}]
        contents = [
            {"food_id": 10, "source_type": "Compound", "source_id": 100, "export": 1},
            {"food_id": 20, "source_type": "Compound", "source_id": 100, "export": 0},
        ]
        _foods, _compounds, edges, _counts = aggregate_rows(
            foods, self.compounds, contents, self.profile_ids, self.profile_pairs
        )
        self.assertEqual(set(edges), {(10, 100), (20, 100)})

    def test_profile_subset_gate_rejects_unbound_id(self):
        require_profile_ids_in_compounds({100}, {100, 200})
        with self.assertRaisesRegex(ValueError, "do not resolve"):
            require_profile_ids_in_compounds({100, 300}, {100, 200})

    def test_unselected_orphan_content_food_is_not_promoted_into_profile_scope(self):
        with self.assertRaisesRegex(ValueError, "lack typed Content support"):
            aggregate_rows(
                self.foods,
                self.compounds,
                [{"food_id": 30, "source_type": "Compound", "source_id": 100, "export": 1}],
                self.profile_ids,
                self.profile_pairs,
            )

    def test_identity_normalization_is_not_an_input(self):
        with self.assertRaisesRegex(ValueError, "Content.food_id"):
            aggregate_rows(
                self.foods,
                self.compounds,
                [{"food_name": "same-looking-name", "source_type": "Compound", "source_id": 100}],
                self.profile_ids,
                self.profile_pairs,
            )

    def test_boolean_ids_are_not_integer_ids(self):
        with self.assertRaisesRegex(ValueError, "Food.id"):
            aggregate_rows(
                [{"id": True, "public_id": "food-a", "export_to_foodb": True}],
                self.compounds,
                [],
                self.profile_ids,
                self.profile_pairs,
            )

    def test_boolean_export_is_not_integer_one(self):
        _foods, _compounds, edges, _counts = aggregate_rows(
            self.foods,
            self.compounds,
            [
                {"food_id": 10, "source_type": "Compound", "source_id": 100, "export": True},
                {"food_id": 20, "source_type": "Compound", "source_id": 100, "export": 0},
            ],
            self.profile_ids,
            self.profile_pairs,
        )
        self.assertEqual(edges[(10, 100)], [1, 0])

    def test_unselected_food_for_selected_compound_is_ignored(self):
        foods = self.foods + [{"id": 30, "public_id": "food-c", "export_to_foodb": True}]
        contents = [
            {"food_id": 10, "source_type": "Compound", "source_id": 100, "export": 1},
            {"food_id": 20, "source_type": "Compound", "source_id": 100, "export": 0},
            {"food_id": 30, "source_type": "Compound", "source_id": 100, "export": 1},
            {"food_id": 10, "source_type": "Compound", "source_id": 100, "export": 1},
        ]
        _foods, _compounds, edges, counts = aggregate_rows(
            foods, self.compounds, contents, self.profile_ids, self.profile_pairs
        )
        self.assertEqual(edges, {(10, 100): [2, 2], (20, 100): [1, 0]})
        self.assertEqual(counts["selected_content_rows"], 3)


if __name__ == "__main__":
    unittest.main()
