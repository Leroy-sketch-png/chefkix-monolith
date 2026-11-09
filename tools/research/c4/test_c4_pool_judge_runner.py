"""Unit tests for C4 Pool-Level LLM Judge Contract, Runner, and Operational Gates."""
from __future__ import annotations

import json
import unittest

from c4_pool_judge_runner import (
    BOOTSTRAP_SEED,
    GATE_INSTABILITY_UPPER_MAX,
    GATE_INVALID_OUTPUT_MAX,
    GATE_KAPPA_LOWER95_MIN,
    GATE_KAPPA_MIN,
    GATE_LENIENCY_DELTA_MAX,
    SYSTEM_PROMPT_SHA256,
    USER_TEMPLATE_SHA256,
    CandidateItem,
    PoolJudgeCase,
    PoolResponse,
    SingleJudgment,
    compute_case_cluster_bootstrap_ci,
    compute_unweighted_cohens_kappa,
    evaluate_c4_operational_gates,
    format_user_prompt,
    parse_and_validate_pool_response,
    verify_prompt_assets,
)


class PromptAssetAndFormatTests(unittest.TestCase):
    def test_prompt_asset_hashes(self):
        """Hardcoded system and user template prompts must match exact SHA-256 pins."""
        res = verify_prompt_assets()
        self.assertTrue(res["system_prompt_verified"])
        self.assertTrue(res["user_template_verified"])

    def test_format_user_prompt_order_reversal(self):
        """Formatting user prompt must properly order candidates and escape JSON."""
        case = PoolJudgeCase(
            case_id="case_001",
            recipe_title="Test Chocolate Cake",
            ingredients=["flour", "sugar", "butter"],
            target_ingredient="butter",
            candidates=[
                CandidateItem(opaque_id="cand_A", candidate_text="margarine"),
                CandidateItem(opaque_id="cand_B", candidate_text="coconut oil"),
            ],
        )

        fwd_prompt = format_user_prompt(case, reverse_order=False)
        rev_prompt = format_user_prompt(case, reverse_order=True)

        self.assertIn('"cand_A"', fwd_prompt)
        self.assertIn('"cand_B"', fwd_prompt)

        # In forward prompt cand_A comes before cand_B
        idx_a_fwd = fwd_prompt.find('"cand_A"')
        idx_b_fwd = fwd_prompt.find('"cand_B"')
        self.assertLess(idx_a_fwd, idx_b_fwd)

        # In reverse prompt cand_B comes before cand_A
        idx_a_rev = rev_prompt.find('"cand_A"')
        idx_b_rev = rev_prompt.find('"cand_B"')
        self.assertLess(idx_b_rev, idx_a_rev)


class ResponseValidationTests(unittest.TestCase):
    def setUp(self):
        self.case = PoolJudgeCase(
            case_id="case_001",
            recipe_title="Pancake",
            ingredients=["milk", "flour"],
            target_ingredient="milk",
            candidates=[
                CandidateItem(opaque_id="c1", candidate_text="soy milk"),
                CandidateItem(opaque_id="c2", candidate_text="water"),
            ],
        )

    def test_valid_pool_response(self):
        raw = json.dumps({
            "judgments": {
                "c1": {"label": "A", "reason": "Works as a direct 1 to 1 replacement in batter."},
                "c2": {"label": "N", "reason": "Lacks fat and proteins, altering texture completely."},
            }
        })
        resp = parse_and_validate_pool_response(raw, self.case, "FORWARD")
        self.assertTrue(resp.is_valid)
        self.assertEqual(len(resp.judgments), 2)
        self.assertEqual(resp.judgments["c1"].label, "A")
        self.assertEqual(resp.judgments["c2"].label, "N")

    def test_missing_opaque_id_rejected(self):
        raw = json.dumps({
            "judgments": {
                "c1": {"label": "A", "reason": "Good substitute."},
            }
        })
        resp = parse_and_validate_pool_response(raw, self.case, "FORWARD")
        self.assertFalse(resp.is_valid)
        self.assertIn("Missing", resp.error_message)

    def test_foreign_opaque_id_rejected(self):
        raw = json.dumps({
            "judgments": {
                "c1": {"label": "A", "reason": "Good substitute."},
                "c2": {"label": "N", "reason": "Bad."},
                "c_foreign": {"label": "U", "reason": "Who is this?"},
            }
        })
        resp = parse_and_validate_pool_response(raw, self.case, "FORWARD")
        self.assertFalse(resp.is_valid)
        self.assertIn("Foreign", resp.error_message)

    def test_overlong_reason_rejected(self):
        # 21 words reason
        words_21 = " ".join(["word"] * 21)
        raw = json.dumps({
            "judgments": {
                "c1": {"label": "A", "reason": words_21},
                "c2": {"label": "N", "reason": "Valid reason."},
            }
        })
        resp = parse_and_validate_pool_response(raw, self.case, "FORWARD")
        self.assertFalse(resp.is_valid)
        self.assertIn("exceeds 20 words", resp.error_message)

    def test_invalid_label_rejected(self):
        raw = json.dumps({
            "judgments": {
                "c1": {"label": "POSITIVE", "reason": "Valid reason."},
                "c2": {"label": "N", "reason": "Valid reason."},
            }
        })
        resp = parse_and_validate_pool_response(raw, self.case, "FORWARD")
        self.assertFalse(resp.is_valid)
        self.assertIn("invalid label", resp.error_message)


class KappaAndBootstrapTests(unittest.TestCase):
    def test_cohens_kappa_perfect_agreement(self):
        pairs = [("A", "A"), ("C", "C"), ("N", "N"), ("U", "U")] * 10
        k = compute_unweighted_cohens_kappa(pairs)
        self.assertIsNotNone(k)
        self.assertAlmostEqual(k, 1.0)

    def test_cohens_kappa_known_value(self):
        # 10 (A, A), 5 (A, C), 5 (C, A), 10 (C, C)
        pairs = [("A", "A")] * 10 + [("A", "C")] * 5 + [("C", "A")] * 5 + [("C", "C")] * 10
        k = compute_unweighted_cohens_kappa(pairs)
        self.assertIsNotNone(k)
        # N=30, po = 20/30 = 0.6667, pe = 0.5*0.5 + 0.5*0.5 = 0.5
        # kappa = (0.6667 - 0.5) / 0.5 = 0.3333
        self.assertAlmostEqual(k, 1.0 / 3.0, places=4)

    def test_case_cluster_bootstrap_ci_reproducible(self):
        case_pairs = {
            f"c_{i}": [("A", "A"), ("N", "N"), ("C", "C")] if i % 2 == 0 else [("A", "A"), ("A", "C")]
            for i in range(20)
        }
        ci1_low, ci1_high, b1 = compute_case_cluster_bootstrap_ci(case_pairs, draws=200, seed=42)
        ci2_low, ci2_high, b2 = compute_case_cluster_bootstrap_ci(case_pairs, draws=200, seed=42)
        self.assertEqual(ci1_low, ci2_low)
        self.assertEqual(ci1_high, ci2_high)
        self.assertLess(ci1_low, ci1_high)


class OperationalGatesEvaluationTests(unittest.TestCase):
    def setUp(self):
        # 10 cases with 2 candidates each = 20 candidate units
        self.cases = [
            PoolJudgeCase(
                case_id=f"case_{i}",
                recipe_title=f"Recipe {i}",
                ingredients=["ing1", "ing2"],
                target_ingredient="ing1",
                candidates=[
                    CandidateItem(opaque_id=f"c_{i}_1", candidate_text="cand1", cell_id="cell_A"),
                    CandidateItem(opaque_id=f"c_{i}_2", candidate_text="cand2", cell_id="cell_B"),
                ]
            )
            for i in range(10)
        ]

    def test_all_operational_gates_pass(self):
        """Clean run where agreement is high, order is stable, and requests succeed."""
        fwd_resp = []
        rev_resp = []
        human_adjudications = {}

        for c in self.cases:
            judgments = {
                c.candidates[0].opaque_id: SingleJudgment(c.candidates[0].opaque_id, "A", "Good."),
                c.candidates[1].opaque_id: SingleJudgment(c.candidates[1].opaque_id, "N", "Bad."),
            }
            fwd_resp.append(PoolResponse(c.case_id, "FORWARD", "{}", True, judgments=judgments))
            rev_resp.append(PoolResponse(c.case_id, "REVERSE", "{}", True, judgments=judgments))

            # Human agrees 100%
            human_adjudications[c.case_id] = {
                c.candidates[0].opaque_id: "A",
                c.candidates[1].opaque_id: "N",
            }

        report = evaluate_c4_operational_gates(
            self.cases, fwd_resp, rev_resp, human_adjudications
        )
        self.assertEqual(report.overall_status, "ACCEPTED")
        self.assertEqual(report.invalid_output_gate.status, "PASS")
        self.assertEqual(report.order_instability_gate.status, "PASS")
        self.assertEqual(report.agreement_kappa_gate.status, "PASS")
        self.assertEqual(report.kappa_ci_lower_gate.status, "PASS")
        for lg in report.leniency_cell_gates:
            self.assertEqual(lg.status, "PASS")

    def test_rejection_on_low_kappa(self):
        """When judge disagrees systematically with human -> Kappa Gate FAIL -> REJECTED."""
        fwd_resp = []
        rev_resp = []
        human_adjudications = {}

        for c in self.cases:
            judgments = {
                c.candidates[0].opaque_id: SingleJudgment(c.candidates[0].opaque_id, "A", "Good."),
                c.candidates[1].opaque_id: SingleJudgment(c.candidates[1].opaque_id, "A", "Good."),
            }
            fwd_resp.append(PoolResponse(c.case_id, "FORWARD", "{}", True, judgments=judgments))
            rev_resp.append(PoolResponse(c.case_id, "REVERSE", "{}", True, judgments=judgments))

            # Human says N for both -> agreement 0%
            human_adjudications[c.case_id] = {
                c.candidates[0].opaque_id: "N",
                c.candidates[1].opaque_id: "N",
            }

        report = evaluate_c4_operational_gates(
            self.cases, fwd_resp, rev_resp, human_adjudications
        )
        self.assertEqual(report.overall_status, "REJECTED")
        self.assertEqual(report.agreement_kappa_gate.status, "FAIL")

    def test_rejection_on_high_instability(self):
        """When reverse pass differs on too many candidates -> Instability Gate FAIL -> REJECTED."""
        fwd_resp = []
        rev_resp = []
        human_adjudications = {}

        for c in self.cases:
            fwd_j = {
                c.candidates[0].opaque_id: SingleJudgment(c.candidates[0].opaque_id, "A", "Good."),
                c.candidates[1].opaque_id: SingleJudgment(c.candidates[1].opaque_id, "N", "Bad."),
            }
            # Reverse pass flips all labels
            rev_j = {
                c.candidates[0].opaque_id: SingleJudgment(c.candidates[0].opaque_id, "N", "Flip."),
                c.candidates[1].opaque_id: SingleJudgment(c.candidates[1].opaque_id, "A", "Flip."),
            }
            fwd_resp.append(PoolResponse(c.case_id, "FORWARD", "{}", True, judgments=fwd_j))
            rev_resp.append(PoolResponse(c.case_id, "REVERSE", "{}", True, judgments=rev_j))

            human_adjudications[c.case_id] = {
                c.candidates[0].opaque_id: "A",
                c.candidates[1].opaque_id: "N",
            }

        report = evaluate_c4_operational_gates(
            self.cases, fwd_resp, rev_resp, human_adjudications
        )
        self.assertEqual(report.overall_status, "REJECTED")
        self.assertEqual(report.order_instability_gate.status, "FAIL")
        self.assertGreater(report.order_instability_gate.observed_value, GATE_INSTABILITY_UPPER_MAX)

    def test_rejection_on_invalid_output_rate(self):
        """When too many pool responses are invalid -> Invalid Output Gate FAIL -> REJECTED."""
        fwd_resp = []
        rev_resp = []
        human_adjudications = {}

        for i, c in enumerate(self.cases):
            # Make half of the responses invalid
            is_valid = (i >= 5)
            fwd_resp.append(PoolResponse(c.case_id, "FORWARD", "bad json", is_valid, judgments={}))
            rev_resp.append(PoolResponse(c.case_id, "REVERSE", "bad json", is_valid, judgments={}))

        report = evaluate_c4_operational_gates(
            self.cases, fwd_resp, rev_resp, human_adjudications
        )
        self.assertEqual(report.overall_status, "REJECTED")
        self.assertEqual(report.invalid_output_gate.status, "FAIL")
        self.assertGreater(report.invalid_output_gate.observed_value, GATE_INVALID_OUTPUT_MAX)

    def test_unavailable_when_human_references_absent(self):
        """When human references are missing -> Gate UNAVAILABLE -> INCOMPLETE_GATES_UNAVAILABLE."""
        fwd_resp = []
        rev_resp = []
        for c in self.cases:
            judgments = {
                c.candidates[0].opaque_id: SingleJudgment(c.candidates[0].opaque_id, "A", "Good."),
                c.candidates[1].opaque_id: SingleJudgment(c.candidates[1].opaque_id, "N", "Bad."),
            }
            fwd_resp.append(PoolResponse(c.case_id, "FORWARD", "{}", True, judgments=judgments))
            rev_resp.append(PoolResponse(c.case_id, "REVERSE", "{}", True, judgments=judgments))

        # No human adjudications supplied
        report = evaluate_c4_operational_gates(
            self.cases, fwd_resp, rev_resp, human_adjudications={}
        )
        self.assertEqual(report.overall_status, "INCOMPLETE_GATES_UNAVAILABLE")
        self.assertEqual(report.agreement_kappa_gate.status, "UNAVAILABLE")


if __name__ == "__main__":
    unittest.main()
