"""
Unit tests for L39 / C3 Allergen Observation Graph Model and Admission Validator.

Verifies:
1. Destination boundary: strictly graph_product_v4, graph_eval_v4 forbidden.
2. Source admission rules:
   - FooDB rejection as allergen evidence.
   - Open Food Facts ingredient-level deferral.
   - WHO/IUIS scope and predicate restriction (species_protein and PROTEIN_SOURCE only).
3. Tri-state fail-closed resolution:
   - Missing relation returns UNKNOWN, never ABSENT.
   - ABSENT state requires explicit closed-world universe proof.
4. Predicate isolation under FATO guide.
5. Deterministic manifest digest calculation.
"""

import unittest

from tools.research.c3.allergen_observation_graph import (
    AllergenObservationEdge,
    AllergenObservationGraph,
    PredicateType,
    ObservationState,
    ObservationScope,
    TargetGraphDestination,
)


def make_valid_edge(
    edge_id: str,
    subject_id: str = "foodon:00001234",
    object_id: str = "allergen:peanut",
    predicate: PredicateType = PredicateType.INGREDIENT_DECLARATION,
    state: ObservationState = ObservationState.PRESENT,
    scope: ObservationScope = ObservationScope.INGREDIENT,
    source_id: str = "usda_fdc_2026",
    destination: TargetGraphDestination = TargetGraphDestination.GRAPH_PRODUCT_V4,
    closed_world_proof: str = None,
) -> AllergenObservationEdge:
    return AllergenObservationEdge(
        edge_id=edge_id,
        source_id=source_id,
        release_version="v2026.1",
        subject_id=subject_id,
        predicate_type=predicate,
        object_id=object_id,
        observation_state=state,
        scope=scope,
        geography_or_population="US",
        effective_date="2026-01-01",
        license="CC-BY-4.0",
        tier="T2",
        artifact_hash="abcdef0123456789" * 4,
        target_destination=destination,
        closed_world_universe_proof=closed_world_proof,
    )


class DestinationBoundaryTests(unittest.TestCase):
    def test_forbid_graph_eval_v4(self):
        edge = make_valid_edge("e1", destination=TargetGraphDestination.GRAPH_EVAL_V4)
        with self.assertRaises(ValueError) as ctx:
            edge.validate()
        self.assertIn("strictly forbidden from destination 'graph_eval_v4'", str(ctx.exception))

    def test_allow_graph_product_v4(self):
        edge = make_valid_edge("e2", destination=TargetGraphDestination.GRAPH_PRODUCT_V4)
        edge.validate()


class SourceAdmissionTests(unittest.TestCase):
    def test_reject_foodb_source(self):
        edge = make_valid_edge("e3", source_id="foodb_v1_compounds")
        with self.assertRaises(ValueError) as ctx:
            edge.validate()
        self.assertIn("FooDB compound/nutrient co-presence is strictly rejected", str(ctx.exception))

    def test_reject_off_ingredient_level_admission(self):
        edge = make_valid_edge(
            "e4",
            source_id="openfoodfacts_dump_2026",
            scope=ObservationScope.INGREDIENT,
        )
        with self.assertRaises(ValueError) as ctx:
            edge.validate()
        self.assertIn("Open Food Facts is deferred from ingredient-level admission", str(ctx.exception))

    def test_allow_off_product_level_observation(self):
        edge = make_valid_edge(
            "e5",
            source_id="openfoodfacts_dump_2026",
            scope=ObservationScope.PRODUCT,
            predicate=PredicateType.PRECAUTIONARY_LABEL,
        )
        edge.validate()

    def test_who_iuis_scope_and_predicate_restriction(self):
        # Invalid predicate for WHO/IUIS
        edge_bad_pred = make_valid_edge(
            "e6",
            source_id="who_iuis_nomenclature",
            predicate=PredicateType.INGREDIENT_DECLARATION,
            scope=ObservationScope.SPECIES_PROTEIN,
        )
        with self.assertRaises(ValueError) as ctx:
            edge_bad_pred.validate()
        self.assertIn("WHO/IUIS source may only instantiate predicate PROTEIN_SOURCE", str(ctx.exception))

        # Invalid scope for WHO/IUIS
        edge_bad_scope = make_valid_edge(
            "e7",
            source_id="who_iuis_nomenclature",
            predicate=PredicateType.PROTEIN_SOURCE,
            scope=ObservationScope.INGREDIENT,
        )
        with self.assertRaises(ValueError) as ctx:
            edge_bad_scope.validate()
        self.assertIn("WHO/IUIS source must have scope 'species_protein'", str(ctx.exception))

        # Valid WHO/IUIS
        edge_valid = make_valid_edge(
            "e8",
            source_id="who_iuis_nomenclature",
            predicate=PredicateType.PROTEIN_SOURCE,
            scope=ObservationScope.SPECIES_PROTEIN,
            object_id="protein:Ara_h_2",
        )
        edge_valid.validate()


class TriStateFailClosedTests(unittest.TestCase):
    def test_absent_requires_closed_world_proof(self):
        edge_no_proof = make_valid_edge(
            "e9",
            state=ObservationState.ABSENT,
            closed_world_proof=None,
        )
        with self.assertRaises(ValueError) as ctx:
            edge_no_proof.validate()
        self.assertIn("State ABSENT is strictly forbidden without an explicit", str(ctx.exception))

    def test_absent_with_proof_validates(self):
        edge_with_proof = make_valid_edge(
            "e10",
            state=ObservationState.ABSENT,
            closed_world_proof="FSIS-FORMAL-CLOSURE-CERTIFICATE-2026-US-ALLERGEN-FREE",
        )
        edge_with_proof.validate()

    def test_query_tri_state_behavior(self):
        graph = AllergenObservationGraph()

        # Add present edge for peanut in recipe
        graph.add_edge(make_valid_edge("e11", subject_id="dish:pad_thai", object_id="allergen:peanut"))

        # Add absent edge with proof for milk in vegan dish
        graph.add_edge(
            make_valid_edge(
                "e12",
                subject_id="dish:vegan_salad",
                object_id="allergen:milk",
                state=ObservationState.ABSENT,
                closed_world_proof="CERTIFIED_VEGAN_AUDIT_DOC_2026",
            )
        )

        # 1. Direct positive match -> PRESENT
        self.assertEqual(
            graph.query_relation_state("dish:pad_thai", "allergen:peanut"),
            ObservationState.PRESENT,
        )

        # 2. Direct closed-world absent match -> ABSENT
        self.assertEqual(
            graph.query_relation_state("dish:vegan_salad", "allergen:milk"),
            ObservationState.ABSENT,
        )

        # 3. Missing / unobserved relation -> UNKNOWN (never ABSENT!)
        self.assertEqual(
            graph.query_relation_state("dish:pad_thai", "allergen:soy"),
            ObservationState.UNKNOWN,
        )


class PredicateIsolationTests(unittest.TestCase):
    def test_predicate_filtered_query(self):
        graph = AllergenObservationGraph()

        # Add precautionary label edge
        graph.add_edge(
            make_valid_edge(
                "e13",
                subject_id="product:chocolate_bar",
                object_id="allergen:tree_nuts",
                predicate=PredicateType.PRECAUTIONARY_LABEL,
                scope=ObservationScope.PRODUCT,
            )
        )

        # Querying specifically for INGREDIENT_DECLARATION returns UNKNOWN (isolated!)
        self.assertEqual(
            graph.query_relation_state(
                "product:chocolate_bar",
                "allergen:tree_nuts",
                predicate=PredicateType.INGREDIENT_DECLARATION,
            ),
            ObservationState.UNKNOWN,
        )

        # Querying for PRECAUTIONARY_LABEL returns PRESENT
        self.assertEqual(
            graph.query_relation_state(
                "product:chocolate_bar",
                "allergen:tree_nuts",
                predicate=PredicateType.PRECAUTIONARY_LABEL,
            ),
            ObservationState.PRESENT,
        )


class ManifestDigestTests(unittest.TestCase):
    def test_digest_order_invariance(self):
        graph1 = AllergenObservationGraph()
        graph2 = AllergenObservationGraph()

        e1 = make_valid_edge("edge_aaa")
        e2 = make_valid_edge("edge_bbb")

        # Insert in different order
        graph1.add_edge(e1)
        graph1.add_edge(e2)

        graph2.add_edge(e2)
        graph2.add_edge(e1)

        self.assertEqual(graph1.compute_manifest_digest(), graph2.compute_manifest_digest())
        summary = graph1.get_summary()
        self.assertEqual(summary["total_edges"], 2)
        self.assertEqual(summary["target_destination"], "graph_product_v4")


if __name__ == "__main__":
    unittest.main()
