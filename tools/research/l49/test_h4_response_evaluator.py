"""Unit tests for L49 H4 Response Evaluator and Estimand Engine."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

# Add monolith root to sys.path
monolith_root = Path(__file__).resolve().parents[3]
if str(monolith_root) not in sys.path:
    sys.path.insert(0, str(monolith_root))

try:
    from tools.research.l49.h4_response_evaluator import (
        MINIMUM_COMPLETE_FLOOR,
        TOTAL_RANDOMIZED_N,
        H4ResponseCode,
        ParticipantScoredRecord,
        SingleTrialResponse,
        StimulusCondition,
        evaluate_cohort_responses,
        score_participant,
        score_single_trial,
    )
except ModuleNotFoundError:
    from h4_response_evaluator import (
        MINIMUM_COMPLETE_FLOOR,
        TOTAL_RANDOMIZED_N,
        H4ResponseCode,
        ParticipantScoredRecord,
        SingleTrialResponse,
        StimulusCondition,
        evaluate_cohort_responses,
        score_participant,
        score_single_trial,
    )


class TestH4ResponseEvaluator(unittest.TestCase):
    def test_single_trial_scoring_displayed(self):
        # Key for displayed is PRESENCE_ONLY
        score_corr, harmful, contra = score_single_trial(
            StimulusCondition.DISPLAYED, H4ResponseCode.PRESENCE_ONLY
        )
        self.assertEqual(score_corr, 1)
        self.assertFalse(harmful)
        self.assertFalse(contra)

        # Non-key valid response
        score_incorr, harmful, contra = score_single_trial(
            StimulusCondition.DISPLAYED, H4ResponseCode.UNKNOWN_NOT_SHOWN
        )
        self.assertEqual(score_incorr, 0)
        self.assertFalse(harmful)
        self.assertFalse(contra)

        # Harmful code
        score_h, harmful, contra = score_single_trial(
            StimulusCondition.DISPLAYED, H4ResponseCode.SAFETY
        )
        self.assertEqual(score_h, 0)
        self.assertTrue(harmful)
        self.assertFalse(contra)

        # Contradiction (ABSENCE in displayed)
        score_c, harmful, contra = score_single_trial(
            StimulusCondition.DISPLAYED, H4ResponseCode.ABSENCE
        )
        self.assertEqual(score_c, 0)
        self.assertFalse(harmful)
        self.assertTrue(contra)

    def test_single_trial_scoring_withheld(self):
        # Key for withheld is UNKNOWN_NOT_SHOWN
        score_corr, harmful, contra = score_single_trial(
            StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN
        )
        self.assertEqual(score_corr, 1)
        self.assertFalse(harmful)

        # Harmful codes in withheld: PRESENCE_ONLY or ABSENCE
        score_p, harmful_p, _ = score_single_trial(
            StimulusCondition.WITHHELD, H4ResponseCode.PRESENCE_ONLY
        )
        self.assertEqual(score_p, 0)
        self.assertTrue(harmful_p)

        score_a, harmful_a, _ = score_single_trial(
            StimulusCondition.WITHHELD, H4ResponseCode.ABSENCE
        )
        self.assertEqual(score_a, 0)
        self.assertTrue(harmful_a)

    def test_single_trial_missing_response(self):
        score, harmful, contra = score_single_trial(StimulusCondition.DISPLAYED, None)
        self.assertIsNone(score)
        self.assertFalse(harmful)
        self.assertFalse(contra)

    def test_score_participant_complete_perfect(self):
        # 6 displayed all PRESENCE_ONLY, 6 withheld all UNKNOWN_NOT_SHOWN
        trials = [
            SingleTrialResponse(i, StimulusCondition.DISPLAYED, H4ResponseCode.PRESENCE_ONLY)
            for i in range(6)
        ] + [
            SingleTrialResponse(i + 6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
            for i in range(6)
        ]
        rec = score_participant("P01", pair_id=0, pair_member="A", trials=trials)
        self.assertTrue(rec.is_complete)
        self.assertEqual(rec.displayed_correct, 6)
        self.assertEqual(rec.withheld_correct, 6)
        self.assertEqual(rec.displayed_accuracy, 1.0)
        self.assertEqual(rec.withheld_accuracy, 1.0)
        self.assertEqual(rec.paired_difference, 0.0)
        self.assertEqual(rec.bound_lower, 0.0)
        self.assertEqual(rec.bound_upper, 0.0)

    def test_score_participant_with_missing(self):
        trials = [
            SingleTrialResponse(i, StimulusCondition.DISPLAYED, H4ResponseCode.PRESENCE_ONLY)
            for i in range(5)
        ] + [
            SingleTrialResponse(5, StimulusCondition.DISPLAYED, None)  # 1 missing
        ] + [
            SingleTrialResponse(i + 6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
            for i in range(6)
        ]
        rec = score_participant("P02", pair_id=0, pair_member="B", trials=trials)
        self.assertFalse(rec.is_complete)
        self.assertIsNone(rec.paired_difference)
        self.assertEqual(rec.displayed_missing, 1)
        # bound lower: 5/6 - 6/6 = -1/6
        self.assertAlmostEqual(rec.bound_lower, 5/6 - 1.0)
        # bound upper: (5+1)/6 - 6/6 = 0.0
        self.assertAlmostEqual(rec.bound_upper, 0.0)

    def test_cohort_evaluation_all_complete_no_harm(self):
        participants = []
        for i in range(24):
            # Each participant has displayed acc = 5/6, withheld acc = 3/6 -> diff = 2/6 = 0.3333
            trials = [
                SingleTrialResponse(t, StimulusCondition.DISPLAYED, H4ResponseCode.PRESENCE_ONLY if t < 5 else H4ResponseCode.UNKNOWN_NOT_SHOWN)
                for t in range(6)
            ] + [
                SingleTrialResponse(t + 6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN if t < 3 else H4ResponseCode.UNKNOWN_NOT_SHOWN) # actually lets put non-keys that aren't harmful
                for t in range(6)
            ]
            # to make 3/6 correct:
            trials[6] = SingleTrialResponse(6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
            trials[7] = SingleTrialResponse(7, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
            trials[8] = SingleTrialResponse(8, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
            # trials 9, 10, 11 are non-keys (let's say a non-harmful code doesn't exist other than unknown, but if it was missing or let's say incomplete)
            # Wait, any non-key in withheld is either harmful or not? If we put PRESENCE_ONLY it's harmful.
            # But in our test, let's keep all 6 withheld correct for clean diff:
            p_trials = [
                SingleTrialResponse(t, StimulusCondition.DISPLAYED, H4ResponseCode.PRESENCE_ONLY)
                for t in range(6)
            ] + [
                SingleTrialResponse(t + 6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
                for t in range(6)
            ]
            rec = score_participant(f"P{i:02d}", pair_id=i//2, pair_member="A" if i%2==0 else "B", trials=p_trials)
            participants.append(rec)

        res = evaluate_cohort_responses(participants)
        self.assertEqual(res.complete_participants_count, 24)
        self.assertTrue(res.meets_planned_floor)
        self.assertAlmostEqual(res.primary_mean_difference, 0.0)
        self.assertTrue(res.favorable_interpretation_allowed)
        self.assertEqual(res.status_summary, "SUPPORT_H4_ESTIMAND")
        self.assertEqual(res.participants_with_overread_count, 0)
        self.assertIsNotNone(res.binomial_zero_event_upper_bound)

    def test_cohort_evaluation_below_floor(self):
        # Only 18 complete participants (< 20 floor)
        participants = []
        for i in range(24):
            is_miss = (i >= 18)
            trials = [
                SingleTrialResponse(t, StimulusCondition.DISPLAYED, None if is_miss and t == 0 else H4ResponseCode.PRESENCE_ONLY)
                for t in range(6)
            ] + [
                SingleTrialResponse(t + 6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
                for t in range(6)
            ]
            rec = score_participant(f"P{i:02d}", pair_id=i//2, pair_member="A" if i%2==0 else "B", trials=trials)
            participants.append(rec)

        res = evaluate_cohort_responses(participants)
        self.assertEqual(res.complete_participants_count, 18)
        self.assertFalse(res.meets_planned_floor)
        self.assertIsNone(res.primary_mean_difference)
        self.assertEqual(res.status_summary, "BELOW_FLOOR_INCONCLUSIVE")
        self.assertFalse(res.favorable_interpretation_allowed)

    def test_cohort_evaluation_disqualified_by_overreading(self):
        participants = []
        for i in range(24):
            has_harmful = (i == 0)
            trials = [
                SingleTrialResponse(t, StimulusCondition.DISPLAYED, H4ResponseCode.SAFETY if has_harmful and t == 0 else H4ResponseCode.PRESENCE_ONLY)
                for t in range(6)
            ] + [
                SingleTrialResponse(t + 6, StimulusCondition.WITHHELD, H4ResponseCode.UNKNOWN_NOT_SHOWN)
                for t in range(6)
            ]
            rec = score_participant(f"P{i:02d}", pair_id=i//2, pair_member="A" if i%2==0 else "B", trials=trials)
            participants.append(rec)

        res = evaluate_cohort_responses(participants)
        self.assertEqual(res.complete_participants_count, 24)
        self.assertTrue(res.meets_planned_floor)
        self.assertEqual(res.participants_with_overread_count, 1)
        self.assertFalse(res.favorable_interpretation_allowed)
        self.assertEqual(res.status_summary, "HARMFUL_OVERREAD_DETECTED")
        self.assertIsNone(res.binomial_zero_event_upper_bound)


if __name__ == "__main__":
    unittest.main()
