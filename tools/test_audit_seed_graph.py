import csv
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from audit_seed_graph import EXPECTED_SOURCE_SHA256, build_seed_graph, write_artifacts


ROOT = Path(__file__).resolve().parents[1]
SOURCE_PATH = ROOT / "culinary/src/main/java/com/chefkix/culinary/features/knowledge/service/KnowledgeGraphSeeder.java"

FIXTURE = '''
var ingredients = List.of(
    KnowledgeIngredient.builder()
        .canonicalName("milk")
        .name("Milk")
        .aliases(List.of("cow milk"))
        .category("dairy")
        .substitutions(List.of(
            KnowledgeIngredient.Substitution.builder().alternative("cow milk").context("alias target").ratio(1.0).build(),
            KnowledgeIngredient.Substitution.builder().alternative("butter").context("canonical target").ratio(1.0).build(),
            KnowledgeIngredient.Substitution.builder().alternative("butter").context("repeated statement is preserved").ratio(0.5).build(),
            KnowledgeIngredient.Substitution.builder().alternative("mystery leaf").context("unmatched target").ratio(1.0).build()
        ))
        .build(),
    KnowledgeIngredient.builder()
        .canonicalName("butter")
        .name("Butter")
        .aliases(List.of())
        .category("dairy")
        .substitutions(List.of())
        .build(),
    KnowledgeIngredient.builder()
        .canonicalName("salt")
        .name("Salt")
        .aliases(List.of())
        .category("pantry")
        .substitutions(List.of())
        .build()
);
'''


class SeedGraphAuditTests(unittest.TestCase):
    def test_fixture_resolves_canonical_and_alias_targets_and_keeps_isolates(self):
        graph = build_seed_graph(FIXTURE, "fixture.java")

        self.assertEqual(3, len(graph["source_records"]))
        self.assertEqual(4, len(graph["edges"]))
        self.assertEqual(4, len(graph["nodes"]))
        self.assertEqual(["alias"], [edge["target_resolution"] for edge in graph["edges"] if edge["raw_target"] == "cow milk"])
        butter_edges = [edge for edge in graph["edges"] if edge["raw_target"] == "butter"]
        self.assertEqual(["canonical", "canonical"], [edge["target_resolution"] for edge in butter_edges])
        self.assertEqual(2, len({edge["edge_id"] for edge in butter_edges}))
        self.assertEqual(["unmatched"], [edge["target_resolution"] for edge in graph["edges"] if edge["raw_target"] == "mystery leaf"])
        self.assertEqual(1, graph["summary"]["isolated_node_count"])
        self.assertEqual(4, graph["summary"]["degree_sums"]["in"])
        self.assertEqual(4, graph["summary"]["degree_sums"]["out"])

    def test_duplicate_canonical_identity_fails_closed(self):
        duplicate = FIXTURE.replace('canonicalName("salt")', 'canonicalName("milk")')

        with self.assertRaisesRegex(ValueError, "duplicate canonicalName"):
            build_seed_graph(duplicate, "fixture.java")

    def test_alias_collision_is_not_silently_resolved(self):
        ambiguous = FIXTURE.replace(
            '.canonicalName("butter")\n        .name("Butter")\n        .aliases(List.of())',
            '.canonicalName("butter")\n        .name("Butter")\n        .aliases(List.of("cow milk"))',
        )

        graph = build_seed_graph(ambiguous, "fixture.java")
        edge = next(edge for edge in graph["edges"] if edge["raw_target"] == "cow milk")
        self.assertEqual("ambiguous", edge["target_resolution"])
        self.assertEqual(1, graph["summary"]["resolution_counts"]["ambiguous"])

    def test_blank_target_fails_closed(self):
        malformed = FIXTURE.replace('alternative("mystery leaf")', 'alternative("  ")')

        with self.assertRaisesRegex(ValueError, "alternative cannot be blank"):
            build_seed_graph(malformed, "fixture.java")

    def test_current_seed_source_matches_pinned_shape_and_directed_invariant(self):
        source_bytes = SOURCE_PATH.read_bytes()
        source = source_bytes.decode("utf-8")
        source_hash = hashlib.sha256(source_bytes).hexdigest()
        graph = build_seed_graph(source, str(SOURCE_PATH.relative_to(ROOT)).replace("\\", "/"), source_hash)

        self.assertEqual(EXPECTED_SOURCE_SHA256, source_hash)
        self.assertEqual(EXPECTED_SOURCE_SHA256, graph["provenance"]["source_sha256"])
        self.assertEqual(25, len(graph["source_records"]))
        self.assertEqual(59, len(graph["edges"]))
        self.assertEqual(79, len(graph["nodes"]))
        self.assertEqual(5, graph["summary"]["resolved_target_statement_count"])
        self.assertEqual(1, graph["summary"]["resolution_counts"]["alias"])
        self.assertEqual(54, graph["summary"]["resolution_counts"]["unmatched"])
        self.assertEqual(59, graph["summary"]["degree_sums"]["in"])
        self.assertEqual(59, graph["summary"]["degree_sums"]["out"])
        self.assertEqual(118, graph["summary"]["degree_sums"]["total"])

    def test_artifacts_are_reproducible_and_include_source_provenance(self):
        graph = build_seed_graph(FIXTURE, "fixture.java")
        expected_hash = hashlib.sha256(FIXTURE.encode("utf-8")).hexdigest()

        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            write_artifacts(graph, Path(first))
            write_artifacts(graph, Path(second))

            for name in ("GRAPH_TRUTH.csv", "GRAPH_NODES.csv", "GRAPH_EDGES.csv", "seed_graph_report.json", "audit_receipt.json"):
                left = (Path(first) / name).read_bytes()
                right = (Path(second) / name).read_bytes()
                self.assertEqual(left, right, name)

            report = json.loads((Path(first) / "seed_graph_report.json").read_text(encoding="utf-8"))
            self.assertEqual(expected_hash, report["provenance"]["source_sha256"])
            self.assertEqual("NOT_COMPUTED_NO_QUERY_COHORT_DENOMINATOR", report["coverage_curve_disposition"])
            with (Path(first) / "GRAPH_TRUTH.csv").open(encoding="utf-8", newline="") as handle:
                rows = list(csv.DictReader(handle))
            self.assertTrue(rows)
            self.assertEqual({"SOURCE_ONLY_STATIC_SOURCE"}, {row["provenance_status"] for row in rows})


if __name__ == "__main__":
    unittest.main()
