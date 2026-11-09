"""Unit tests for C5 Section 12 FooDB-Native Chemical Product Graph Evaluator."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

# Add monolith root to sys.path
monolith_root = Path(__file__).resolve().parents[3]
if str(monolith_root) not in sys.path:
    sys.path.insert(0, str(monolith_root))

try:
    from tools.research.c5.foodb_native_graph_evaluator import (
        EXCLUDED_DANGLING_IDS,
        EXCLUDED_FOOD_RECORDS_WITHOUT_COMPOUNDS,
        EXPORT_ELIGIBLE_PAIRS,
        NOT_EXPORT_ELIGIBLE_PAIRS,
        PINNED_FOODB_COMPOUNDS_JSON_SHA256,
        PINNED_FOODB_OFFICIAL_ARCHIVE_SHA256,
        PINNED_FOODB_SIDECAR_SHA256,
        TOP_255_ARTIFACT_COMPOUND_COUNT,
        TOTAL_UNIQUE_PAIRS,
        PublicationFilterState,
        TriStatePresence,
        audit_foodb_native_graph,
        create_native_edge,
        validate_node_ids,
    )
except ModuleNotFoundError:
    from foodb_native_graph_evaluator import (
        EXCLUDED_DANGLING_IDS,
        EXCLUDED_FOOD_RECORDS_WITHOUT_COMPOUNDS,
        EXPORT_ELIGIBLE_PAIRS,
        NOT_EXPORT_ELIGIBLE_PAIRS,
        PINNED_FOODB_COMPOUNDS_JSON_SHA256,
        PINNED_FOODB_OFFICIAL_ARCHIVE_SHA256,
        PINNED_FOODB_SIDECAR_SHA256,
        TOP_255_ARTIFACT_COMPOUND_COUNT,
        TOTAL_UNIQUE_PAIRS,
        PublicationFilterState,
        TriStatePresence,
        audit_foodb_native_graph,
        create_native_edge,
        validate_node_ids,
    )


class TestFooDBNativeGraphEvaluator(unittest.TestCase):
    def test_pinned_hashes_invariants(self):
        self.assertEqual(len(PINNED_FOODB_COMPOUNDS_JSON_SHA256), 64)
        self.assertEqual(len(PINNED_FOODB_SIDECAR_SHA256), 64)
        self.assertEqual(len(PINNED_FOODB_OFFICIAL_ARCHIVE_SHA256), 64)

    def test_exact_pair_counts_and_partition(self):
        self.assertEqual(TOTAL_UNIQUE_PAIRS, 125992)
        self.assertEqual(EXPORT_ELIGIBLE_PAIRS, 107496)
        self.assertEqual(NOT_EXPORT_ELIGIBLE_PAIRS, 18496)
        self.assertEqual(EXPORT_ELIGIBLE_PAIRS + NOT_EXPORT_ELIGIBLE_PAIRS, TOTAL_UNIQUE_PAIRS)
        self.assertEqual(TOP_255_ARTIFACT_COMPOUND_COUNT, 206)
        self.assertEqual(EXCLUDED_DANGLING_IDS, 49)
        self.assertEqual(EXCLUDED_FOOD_RECORDS_WITHOUT_COMPOUNDS, 4)

    def test_node_id_validation_valid(self):
        food_id, compound_id = validate_node_ids("foodb:1.0:food-id:971", "foodb:1.0:compound-id:42")
        self.assertEqual(food_id, 971)
        self.assertEqual(compound_id, 42)

    def test_node_id_validation_invalid(self):
        with self.assertRaises(ValueError):
            validate_node_ids("foodb:food-id:abc", "foodb:1.0:compound-id:42")
        with self.assertRaises(ValueError):
            validate_node_ids("foodb:1.0:food-id:123", "invalid-compound")

    def test_create_native_edge_export_eligible(self):
        edge = create_native_edge(food_id=10, compound_id=20, content_row_count=5, is_export=True, is_public_food=True)
        self.assertEqual(edge.food_node_id, "foodb:1.0:food-id:10")
        self.assertEqual(edge.compound_node_id, "foodb:1.0:compound-id:20")
        self.assertEqual(edge.content_row_support, 5)
        self.assertEqual(edge.export_filter_state, PublicationFilterState.EXPORT_ELIGIBLE)
        self.assertEqual(edge.presence, TriStatePresence.PRESENT)

    def test_create_native_edge_present_not_export_eligible(self):
        edge = create_native_edge(food_id=10, compound_id=20, content_row_count=3, is_export=False, is_public_food=True)
        self.assertEqual(edge.export_filter_state, PublicationFilterState.PRESENT_NOT_EXPORT_ELIGIBLE)
        self.assertEqual(edge.presence, TriStatePresence.PRESENT)

    def test_create_native_edge_zero_count_defaults_unknown(self):
        edge = create_native_edge(food_id=10, compound_id=20, content_row_count=0, is_export=True, is_public_food=True, closed_world_complete=False)
        self.assertEqual(edge.presence, TriStatePresence.UNKNOWN)

    def test_create_native_edge_zero_count_closed_world_absent(self):
        edge = create_native_edge(food_id=10, compound_id=20, content_row_count=0, is_export=True, is_public_food=True, closed_world_complete=True)
        self.assertEqual(edge.presence, TriStatePresence.ABSENT)

    def test_audit_foodb_native_graph_success(self):
        res = audit_foodb_native_graph(
            destination="graph_product_v4",
            unique_pairs_count=125992,
            export_eligible_count=107496,
            not_export_eligible_count=18496,
            artifact_compound_count=206,
            dangling_ids_count=49,
            empty_foods_count=4,
            joined_by_normalized_name=False,
            inferred_absence_from_omission=False,
            inferred_allergen_from_compound=False,
        )
        self.assertTrue(res.all_predicates_valid)
        self.assertEqual(len(res.violations), 0)
        self.assertEqual(res.lead_status, "OPEN")

    def test_audit_rejects_graph_eval_v4_destination(self):
        res = audit_foodb_native_graph(
            destination="graph_eval_v4",
            unique_pairs_count=125992,
            export_eligible_count=107496,
            not_export_eligible_count=18496,
            artifact_compound_count=206,
            dangling_ids_count=49,
            empty_foods_count=4,
        )
        self.assertFalse(res.all_predicates_valid)
        self.assertTrue(any("graph_product_v4" in v for v in res.violations))

    def test_audit_rejects_normalized_name_joining(self):
        res = audit_foodb_native_graph(
            destination="graph_product_v4",
            unique_pairs_count=125992,
            export_eligible_count=107496,
            not_export_eligible_count=18496,
            artifact_compound_count=206,
            dangling_ids_count=49,
            empty_foods_count=4,
            joined_by_normalized_name=True,
        )
        self.assertFalse(res.all_predicates_valid)
        self.assertTrue(any("normalized profile names" in v for v in res.violations))

    def test_audit_rejects_inferred_absence(self):
        res = audit_foodb_native_graph(
            destination="graph_product_v4",
            unique_pairs_count=125992,
            export_eligible_count=107496,
            not_export_eligible_count=18496,
            artifact_compound_count=206,
            dangling_ids_count=49,
            empty_foods_count=4,
            inferred_absence_from_omission=True,
        )
        self.assertFalse(res.all_predicates_valid)
        self.assertTrue(any("UNKNOWN, never ABSENCE" in v for v in res.violations))

    def test_audit_rejects_inferred_allergens(self):
        res = audit_foodb_native_graph(
            destination="graph_product_v4",
            unique_pairs_count=125992,
            export_eligible_count=107496,
            not_export_eligible_count=18496,
            artifact_compound_count=206,
            dangling_ids_count=49,
            empty_foods_count=4,
            inferred_allergen_from_compound=True,
        )
        self.assertFalse(res.all_predicates_valid)
        self.assertTrue(any("Compound co-presence CANNOT imply allergens" in v for v in res.violations))


if __name__ == "__main__":
    unittest.main()
