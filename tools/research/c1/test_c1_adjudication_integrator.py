"""Unit tests for C1 Adjudication Integrator and Estimand Pipeline."""
from __future__ import annotations

import json
import unittest
from pathlib import Path
import sys

_monolith_root = Path(__file__).resolve().parents[3]
if str(_monolith_root) not in sys.path:
    sys.path.insert(0, str(_monolith_root))
_c1_dir = Path(__file__).resolve().parent
if str(_c1_dir) not in sys.path:
    sys.path.insert(0, str(_c1_dir))

from tools.research.c1.c1_adjudication_integrator import (
    AGREED_COUNT,
    DISAGREEMENT_COUNT,
    TOTAL_CANDIDATE_JUDGMENTS,
    C1AdjudicationIntegrationResult,
    IntegratedCandidateJudgment,
    evaluate_integrated_audit,
    integrate_judgments,
)
from tools.research.l36.audit_estimand_evaluator import (
    CaseValue,
    PROTOCOL_SHA256,
    SPECIFICATION_ID,
    TOTAL_SAMPLE,
    UNSEEN_SAMPLE,
)


class TestC1AdjudicationIntegrator(unittest.TestCase):
    def setUp(self):
        # 1. Build 717 agreed pairs: (case_id, cand_id) -> label
        self.agreed_map = {}
        for i in range(AGREED_COUNT):
            cid = f"case_agr_{i // 5}"
            cand_id = f"cand_agr_{i}"
            # Mix labels: mostly N and C, some A
            label = "A" if i % 10 == 0 else ("C" if i % 3 == 0 else "N")
            self.agreed_map[(cid, cand_id)] = label

        # 2. Build 150 sealed key items
        self.key_items = []
        for i in range(DISAGREEMENT_COUNT):
            item_id = f"item_{i:03d}"
            cid = f"case_dis_{i // 2}"
            cand_id = f"cand_dis_{i}"
            self.key_items.append({
                "item_id": item_id,
                "case_id": cid,
                "identity_id": cand_id,
            })
        self.sealed_key = {
            "format": "c1-l36-adjudication-key-v1",
            "items": self.key_items,
        }

        # 3. Build 150 decisions for adjudication return
        self.decisions = []
        for i in range(DISAGREEMENT_COUNT):
            item_id = f"item_{i:03d}"
            label = "A" if i % 4 == 0 else ("U" if i % 5 == 0 else "N")
            self.decisions.append({
                "item_id": item_id,
                "final_label": label,
                "rationale": f"Adjudicated rationale for {item_id}",
            })
        self.adjudication_return = {
            "format": "c1-l36-adjudication-return-v1",
            "decisions": self.decisions,
        }

    def test_integrate_judgments_happy_path(self):
        integrated = integrate_judgments(
            self.sealed_key,
            self.adjudication_return,
            self.agreed_map,
        )
        self.assertEqual(len(integrated), TOTAL_CANDIDATE_JUDGMENTS)
        self.assertEqual(len(integrated), 867)

        # Check source types
        agreed_count = sum(1 for v in integrated.values() if v.source_type == "AGREED")
        adj_count = sum(1 for v in integrated.values() if v.source_type == "ADJUDICATED")
        self.assertEqual(agreed_count, AGREED_COUNT)
        self.assertEqual(adj_count, DISAGREEMENT_COUNT)

        # Check specific items
        first_dis = integrated[("case_dis_0", "cand_dis_0")]
        self.assertEqual(first_dis.source_type, "ADJUDICATED")
        self.assertEqual(first_dis.final_label, "A")
        self.assertEqual(first_dis.rationale, "Adjudicated rationale for item_000")

    def test_integrate_judgments_unanswered_defaults_to_u(self):
        # Provide only 100 decisions out of 150
        partial_decisions = self.decisions[:100]
        partial_return = {
            "format": "c1-l36-adjudication-return-v1",
            "decisions": partial_decisions,
        }
        integrated = integrate_judgments(
            self.sealed_key,
            partial_return,
            self.agreed_map,
        )
        self.assertEqual(len(integrated), 867)

        # Item 120 was not answered -> should be "U"
        unanswered_item = self.key_items[120]
        pair = (unanswered_item["case_id"], unanswered_item["identity_id"])
        self.assertIn(pair, integrated)
        self.assertEqual(integrated[pair].final_label, "U")
        self.assertIn("defaulted to U", integrated[pair].rationale)

    def test_format_validation(self):
        bad_key = {"format": "wrong-key-format", "items": self.key_items}
        with self.assertRaises(ValueError) as ctx:
            integrate_judgments(bad_key, self.adjudication_return, self.agreed_map)
        self.assertIn("Invalid sealed key format", str(ctx.exception))

        bad_return = {"format": "wrong-return-format", "decisions": self.decisions}
        with self.assertRaises(ValueError) as ctx:
            integrate_judgments(self.sealed_key, bad_return, self.agreed_map)
        self.assertIn("Invalid adjudication return format", str(ctx.exception))

        # Invalid label in return
        bad_decisions = list(self.decisions)
        bad_decisions[0] = {"item_id": "item_000", "final_label": "X"}
        bad_ret_label = {"format": "c1-l36-adjudication-return-v1", "decisions": bad_decisions}
        with self.assertRaises(ValueError) as ctx:
            integrate_judgments(self.sealed_key, bad_ret_label, self.agreed_map)
        self.assertIn("Invalid adjudicated label", str(ctx.exception))

    def test_key_collision_raises(self):
        # If an item in sealed key has the exact same (case_id, identity_id) as agreed map
        colliding_key_items = list(self.key_items)
        colliding_key_items[0] = {
            "item_id": "item_000",
            "case_id": "case_agr_0",
            "identity_id": "cand_agr_0",
        }
        colliding_key = {
            "format": "c1-l36-adjudication-key-v1",
            "items": colliding_key_items,
        }
        with self.assertRaises(ValueError) as ctx:
            integrate_judgments(colliding_key, self.adjudication_return, self.agreed_map)
        self.assertIn("Collision", str(ctx.exception))

    def test_evaluate_integrated_audit_full_flow(self):
        # Build unified 867 judgments
        # We need a case manifest of exactly 150 cases:
        # 50 unseen_profiled, 50 unseen_unprofiled, 25 seen_profiled, 25 seen_unprofiled
        case_manifest = []
        custom_integrated = {}

        # 50 unseen_profiled
        for i in range(50):
            cid = f"unseen_prof_{i:03d}"
            cands = [f"{cid}_c{j}" for j in range(5)]
            case_manifest.append({
                "case_id": cid,
                "stratum": "unseen_profiled",
                "recipe_cluster_id": f"cluster_p_{i // 5}",
                "candidate_identities": cands,
            })
            # First 10 cases have an 'A', next 10 have 'U' without 'A', rest have 'N'
            for j, cand in enumerate(cands):
                if i < 10 and j == 0:
                    lbl = "A"
                elif 10 <= i < 20 and j == 0:
                    lbl = "U"
                else:
                    lbl = "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid,
                    identity_id=cand,
                    final_label=lbl,
                    source_type="AGREED" if i % 2 == 0 else "ADJUDICATED",
                )

        # 50 unseen_unprofiled
        for i in range(50):
            cid = f"unseen_unprof_{i:03d}"
            cands = [f"{cid}_c{j}" for j in range(5)]
            case_manifest.append({
                "case_id": cid,
                "stratum": "unseen_unprofiled",
                "recipe_cluster_id": f"cluster_u_{i // 5}",
                "candidate_identities": cands,
            })
            # First 15 cases have an 'A', next 5 have 'U', rest 'N'
            for j, cand in enumerate(cands):
                if i < 15 and j == 0:
                    lbl = "A"
                elif 15 <= i < 20 and j == 0:
                    lbl = "U"
                else:
                    lbl = "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid,
                    identity_id=cand,
                    final_label=lbl,
                    source_type="AGREED" if i % 2 == 0 else "ADJUDICATED",
                )

        # 25 seen_profiled
        for i in range(25):
            cid = f"seen_prof_{i:03d}"
            cands = [f"{cid}_c{j}" for j in range(5)]
            case_manifest.append({
                "case_id": cid,
                "stratum": "seen_profiled",
                "recipe_cluster_id": f"cluster_sp_{i // 5}",
                "candidate_identities": cands,
            })
            for j, cand in enumerate(cands):
                lbl = "A" if j == 0 else "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid,
                    identity_id=cand,
                    final_label=lbl,
                    source_type="AGREED",
                )

        # 25 seen_unprofiled
        for i in range(25):
            cid = f"seen_unprof_{i:03d}"
            cands = [f"{cid}_c{j}" for j in range(5)]
            case_manifest.append({
                "case_id": cid,
                "stratum": "seen_unprofiled",
                "recipe_cluster_id": f"cluster_su_{i // 5}",
                "candidate_identities": cands,
            })
            for j, cand in enumerate(cands):
                lbl = "A" if j == 0 else "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid,
                    identity_id=cand,
                    final_label=lbl,
                    source_type="AGREED",
                )

        # Add 117 padding judgments to reach 867 total (750 from 150*5 + 117 extra)
        # to strictly test arbitrary candidate sets
        for i in range(TOTAL_CANDIDATE_JUDGMENTS - len(custom_integrated)):
            custom_integrated[(f"extra_case_{i}", f"extra_cand_{i}")] = IntegratedCandidateJudgment(
                case_id=f"extra_case_{i}",
                identity_id=f"extra_cand_{i}",
                final_label="N",
                source_type="AGREED",
            )

        self.assertEqual(len(custom_integrated), 867)

        result = evaluate_integrated_audit(custom_integrated, case_manifest)

        self.assertEqual(result.total_cases_evaluated, 150)
        self.assertEqual(result.unseen_cases_count, 100)
        self.assertEqual(result.seen_cases_count, 50)
        self.assertEqual(result.total_candidate_judgments, 867)
        self.assertTrue(result.is_denominator_preserved)

        # Unseen profiled: 10 success, 10 unknown, 30 failure
        # Unseen unprofiled: 15 success, 5 unknown, 30 failure
        # Seen profiled: 25 success
        # Seen unprofiled: 25 success
        # Total successes = 10 + 15 + 25 + 25 = 75
        self.assertEqual(result.v1_count_total, 75)
        self.assertEqual(result.vu_count_total, 15)  # 10 + 5
        self.assertEqual(result.v0_count_total, 60)  # 30 + 30

        # Partial bounds
        self.assertLess(result.point_bounds.p_lower, result.point_bounds.p_upper)
        self.assertAlmostEqual(
            result.point_bounds.weighted_unknown_mass,
            result.point_bounds.p_upper - result.point_bounds.p_lower,
            places=5,
        )
        self.assertEqual(result.point_bounds.total_unseen_cases, 100)
        self.assertEqual(result.point_bounds.n_unknown_cases, 15)
        self.assertAlmostEqual(result.point_bounds.total_weighted_denominator, 3815.0)

        # Verify receipt dictionary and SHA256 integrity
        receipt = result.to_receipt_dict()
        self.assertEqual(receipt["format"], "c1-l36-adjudication-integration-receipt-v1")
        self.assertEqual(receipt["specification_id"], SPECIFICATION_ID)
        self.assertEqual(receipt["protocol_sha256"], PROTOCOL_SHA256)
        self.assertEqual(receipt["total_cases_evaluated"], 150)
        self.assertEqual(receipt["v1_count_total"], 75)
        self.assertIn("receipt_sha256", receipt)
        self.assertEqual(len(receipt["receipt_sha256"]), 64)


if __name__ == "__main__":
    unittest.main()
