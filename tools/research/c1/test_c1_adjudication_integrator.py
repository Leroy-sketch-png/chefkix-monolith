"""Unit tests for C1 Adjudication Integrator and Estimand Pipeline."""
from __future__ import annotations

import hashlib
import json
import tempfile
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
    evaluate_full_audit,
    evaluate_integrated_audit,
    extract_agreed_map_from_returns,
    integrate_judgments,
    main,
)
from tools.research.l36.audit_estimand_evaluator import (
    CaseValue,
    DecisionStatus,
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

    def _build_synthetic_manifest(self) -> list[dict]:
        manifest = []
        for i in range(50):
            cid = f"unseen_prof_{i:03d}"
            manifest.append({
                "case_id": cid,
                "stratum": "unseen_profiled",
                "recipe_cluster_id": f"cluster_p_{i // 5}",
                "candidate_identities": [f"{cid}_c{j}" for j in range(5)],
            })
        for i in range(50):
            cid = f"unseen_unprof_{i:03d}"
            manifest.append({
                "case_id": cid,
                "stratum": "unseen_unprofiled",
                "recipe_cluster_id": f"cluster_u_{i // 5}",
                "candidate_identities": [f"{cid}_c{j}" for j in range(5)],
            })
        for i in range(25):
            cid = f"seen_prof_{i:03d}"
            manifest.append({
                "case_id": cid,
                "stratum": "seen_profiled",
                "recipe_cluster_id": f"cluster_sp_{i // 5}",
                "candidate_identities": [f"{cid}_c{j}" for j in range(5)],
            })
        for i in range(25):
            cid = f"seen_unprof_{i:03d}"
            manifest.append({
                "case_id": cid,
                "stratum": "seen_unprofiled",
                "recipe_cluster_id": f"cluster_su_{i // 5}",
                "candidate_identities": [f"{cid}_c{j}" for j in range(5)],
            })
        return manifest

    def test_evaluate_integrated_audit_full_flow(self):
        case_manifest = self._build_synthetic_manifest()
        custom_integrated = {}

        # 50 unseen_profiled: 10 A, 10 U, 30 N
        for i, c in enumerate(case_manifest[:50]):
            cid = c["case_id"]
            for j, cand in enumerate(c["candidate_identities"]):
                if i < 10 and j == 0:
                    lbl = "A"
                elif 10 <= i < 20 and j == 0:
                    lbl = "U"
                else:
                    lbl = "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid, identity_id=cand, final_label=lbl, source_type="AGREED"
                )

        # 50 unseen_unprofiled: 15 A, 5 U, 30 N
        for i, c in enumerate(case_manifest[50:100]):
            cid = c["case_id"]
            for j, cand in enumerate(c["candidate_identities"]):
                if i < 15 and j == 0:
                    lbl = "A"
                elif 15 <= i < 20 and j == 0:
                    lbl = "U"
                else:
                    lbl = "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid, identity_id=cand, final_label=lbl, source_type="AGREED"
                )

        # 50 seen cases: all A on first candidate
        for c in case_manifest[100:]:
            cid = c["case_id"]
            for j, cand in enumerate(c["candidate_identities"]):
                lbl = "A" if j == 0 else "N"
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid, identity_id=cand, final_label=lbl, source_type="AGREED"
                )

        # 117 padding judgments to reach 867 total
        for i in range(TOTAL_CANDIDATE_JUDGMENTS - len(custom_integrated)):
            custom_integrated[(f"extra_case_{i}", f"extra_cand_{i}")] = IntegratedCandidateJudgment(
                case_id=f"extra_case_{i}", identity_id=f"extra_cand_{i}", final_label="N", source_type="AGREED"
            )

        self.assertEqual(len(custom_integrated), 867)

        result = evaluate_integrated_audit(custom_integrated, case_manifest)

        self.assertEqual(result.total_cases_evaluated, 150)
        self.assertEqual(result.unseen_cases_count, 100)
        self.assertEqual(result.seen_cases_count, 50)
        self.assertEqual(result.total_candidate_judgments, 867)
        self.assertTrue(result.is_denominator_preserved)

        self.assertEqual(result.v1_count_total, 75)
        self.assertEqual(result.vu_count_total, 15)
        self.assertEqual(result.v0_count_total, 60)

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

        # Conditional H=0 estimand is explicit, source-pinned, and descriptive
        # only; all 100 assigned unseen cases remain in the cohort accounting.
        h0_by_case = {
            c["case_id"]: index < 10
            for stratum in ("unseen_profiled", "unseen_unprofiled")
            for index, c in enumerate(row for row in case_manifest if row["stratum"] == stratum)
        }
        conditional_result = evaluate_integrated_audit(
            custom_integrated,
            case_manifest,
            h0_by_case=h0_by_case,
            membership_source_sha256="a" * 64,
            membership_receipt_sha256="b" * 64,
        )
        conditional_receipt = conditional_result.to_receipt_dict()
        self.assertEqual(conditional_receipt["format"], "c1-l36-adjudication-integration-receipt-v2")
        receipt_digest = conditional_receipt.pop("receipt_sha256")
        canonical = json.dumps(conditional_receipt, sort_keys=True, separators=(",", ":")).encode("utf-8")
        self.assertEqual(hashlib.sha256(canonical).hexdigest(), receipt_digest)
        self.assertEqual(conditional_result.conditional_point_bounds.assigned_unseen_cases, 100)
        self.assertEqual(conditional_result.conditional_point_bounds.h0_unseen_cases, 20)
        self.assertAlmostEqual(conditional_result.conditional_point_bounds.weighted_h0_denominator, 763.0)
        conditional_payload = conditional_receipt["conditional_h0_descriptive_bounds"]
        self.assertEqual(conditional_payload["interpretation"], "DESCRIPTIVE_POINT_BOUNDS_ONLY_NO_INTERVAL_OR_H1_DECISION")
        self.assertEqual(conditional_payload["membership_provenance_status"], "CALLER_VERIFICATION_REQUIRED")
        self.assertEqual(conditional_payload["p_lower"], 1.0)
        self.assertNotIn("h1_decision", conditional_payload)
        self.assertNotIn("case_id", conditional_payload)

        with self.assertRaisesRegex(ValueError, "exactly the unseen cohort"):
            evaluate_integrated_audit(
                custom_integrated,
                case_manifest,
                h0_by_case={},
                membership_source_sha256="a" * 64,
                membership_receipt_sha256="b" * 64,
            )

    def test_extract_agreed_map_from_returns(self):
        # Create synthetic private key with 867 distinct pairs (1734 entries for R1 and R2)
        entries = []
        r1_judgments = []
        r2_judgments = []

        for i in range(TOTAL_CANDIDATE_JUDGMENTS):
            cid = f"case_{i // 5}"
            iid = f"cand_{i}"
            vid = f"view_{i // 5}"
            bcand = f"blind_{i % 5}"

            entries.append({"reviewer": "R1", "case_id": cid, "identity_id": iid, "view_id": vid, "blind_candidate": bcand})
            entries.append({"reviewer": "R2", "case_id": cid, "identity_id": iid, "view_id": vid, "blind_candidate": bcand})

            # 717 agree, 150 disagree
            if i < AGREED_COUNT:
                lbl1, lbl2 = "A", "A"
            else:
                lbl1, lbl2 = "A", "N"

            r1_judgments.append({"view_id": vid, "blind_candidate": bcand, "label": lbl1})
            r2_judgments.append({"view_id": vid, "blind_candidate": bcand, "label": lbl2})

        priv_key = {"format": "c1-private-key-v1", "entries": entries}
        r1_return = {"format": "c1-reviewer-return-v1", "reviewer": "R1", "judgments": r1_judgments}
        r2_return = {"format": "c1-reviewer-return-v1", "reviewer": "R2", "judgments": r2_judgments}

        agreed = extract_agreed_map_from_returns(priv_key, r1_return, r2_return)
        self.assertEqual(len(agreed), AGREED_COUNT)
        self.assertEqual(len(agreed), 717)

    def test_evaluate_full_audit_with_bootstrap(self):
        case_manifest = self._build_synthetic_manifest()
        custom_integrated = {}
        for c in case_manifest:
            cid = c["case_id"]
            for cand in c["candidate_identities"]:
                custom_integrated[(cid, cand)] = IntegratedCandidateJudgment(
                    case_id=cid, identity_id=cand, final_label="A", source_type="AGREED"
                )
        for i in range(TOTAL_CANDIDATE_JUDGMENTS - len(custom_integrated)):
            custom_integrated[(f"pad_{i}", f"pad_{i}")] = IntegratedCandidateJudgment(
                case_id=f"pad_{i}", identity_id=f"pad_{i}", final_label="A", source_type="AGREED"
            )

        res, report = evaluate_full_audit(
            integrated_judgments=custom_integrated,
            case_manifest=case_manifest,
            n_bootstrap_draws=50,
            seed=20261021,
        )
        self.assertEqual(res.total_cases_evaluated, 150)
        self.assertEqual(report.h1_decision, DecisionStatus.SUPPORT_H1)
        self.assertIsNotNone(report.bootstrap_interval)

    def test_cli_execution(self):
        case_manifest = self._build_synthetic_manifest()
        with tempfile.TemporaryDirectory() as tmpdir:
            tmppath = Path(tmpdir)
            key_file = tmppath / "sealed_key.json"
            ret_file = tmppath / "adj_return.json"
            man_file = tmppath / "manifest.json"
            agr_file = tmppath / "agreed_map.json"
            out_file = tmppath / "receipt.json"

            key_file.write_text(json.dumps(self.sealed_key), encoding="utf-8")
            ret_file.write_text(json.dumps(self.adjudication_return), encoding="utf-8")
            man_file.write_text(json.dumps(case_manifest), encoding="utf-8")

            # write agreed_map as list of items
            agr_items = [{"case_id": k[0], "identity_id": k[1], "final_label": v} for k, v in self.agreed_map.items()]
            agr_file.write_text(json.dumps(agr_items), encoding="utf-8")

            exit_code = main([
                "--sealed-key", str(key_file),
                "--adjudication-return", str(ret_file),
                "--case-manifest", str(man_file),
                "--agreed-map", str(agr_file),
                "--output", str(out_file),
                "--bootstrap-draws", "50",
            ])
            self.assertEqual(exit_code, 0)
            self.assertTrue(out_file.exists())
            receipt = json.loads(out_file.read_text(encoding="utf-8"))
            self.assertEqual(receipt["format"], "c1-l36-adjudication-integration-receipt-v1")
            self.assertEqual(receipt["total_cases_evaluated"], 150)
            self.assertIn("receipt_sha256", receipt)


if __name__ == "__main__":
    unittest.main()
