"""Unit tests for the L49 / H4 Counterbalance Schedule, Scoring, and Estimators."""
from __future__ import annotations

import unittest
from collections import Counter, defaultdict

from h4_counterbalance_schedule import (
    CASES_PER_CONDITION,
    CASES_PER_PARTICIPANT,
    MIN_COMPLETE_FLOOR,
    PLANNED_COHORT_N,
    PLANNED_PAIRS,
    RESPONSE_CODES,
    CounterbalanceSchedule,
    H4EvaluationReport,
    ParticipantSummary,
    ParticipantTrialResponse,
    compute_h4_report,
    evaluate_participant,
    generate_schedule,
    score_trial,
)


class CounterbalanceScheduleTests(unittest.TestCase):
    def setUp(self):
        self.schedule = generate_schedule(seed=20261107)

    def test_schedule_size_and_roster(self):
        """Schedule must produce exactly 288 trials for 24 participants."""
        self.assertEqual(len(self.schedule.trials), PLANNED_COHORT_N * CASES_PER_PARTICIPANT)
        p_ids = {t.participant_id for t in self.schedule.trials}
        self.assertEqual(len(p_ids), PLANNED_COHORT_N)

    def test_each_participant_sees_12_distinct_cases(self):
        """No participant sees any case more than once."""
        by_p = defaultdict(list)
        for t in self.schedule.trials:
            by_p[t.participant_id].append(t.case_index)
        for p_id, cases in by_p.items():
            self.assertEqual(len(cases), CASES_PER_PARTICIPANT)
            self.assertEqual(len(set(cases)), CASES_PER_PARTICIPANT, f"{p_id} has duplicate cases")
            self.assertEqual(sorted(cases), list(range(12)))

    def test_each_participant_has_balanced_conditions(self):
        """Each participant sees exactly 6 displayed and 6 withheld cases."""
        by_p = defaultdict(list)
        for t in self.schedule.trials:
            by_p[t.participant_id].append(t.condition)
        for p_id, conds in by_p.items():
            c = Counter(conds)
            self.assertEqual(c["DISPLAYED"], CASES_PER_CONDITION, f"{p_id} unbalanced displayed")
            self.assertEqual(c["WITHHELD"], CASES_PER_CONDITION, f"{p_id} unbalanced withheld")

    def test_each_case_balanced_across_cohort(self):
        """Across the cohort, each case is seen by 12 displayed and 12 withheld participants."""
        by_case = defaultdict(list)
        for t in self.schedule.trials:
            by_case[t.case_index].append(t.condition)
        for case_k in range(12):
            c = Counter(by_case[case_k])
            self.assertEqual(c["DISPLAYED"], 12, f"Case {case_k} displayed count != 12")
            self.assertEqual(c["WITHHELD"], 12, f"Case {case_k} withheld count != 12")

    def test_each_position_balanced_across_cohort(self):
        """At each serial position (0..11), exactly 12 see displayed and 12 see withheld."""
        by_pos = defaultdict(list)
        for t in self.schedule.trials:
            by_pos[t.position].append(t.condition)
        for pos_i in range(12):
            c = Counter(by_pos[pos_i])
            self.assertEqual(c["DISPLAYED"], 12, f"Position {pos_i} displayed count != 12")
            self.assertEqual(c["WITHHELD"], 12, f"Position {pos_i} withheld count != 12")

    def test_pair_level_case_complementarity(self):
        """Within each pair j, for every case k, one member sees DISPLAYED and other WITHHELD."""
        by_pair_case = defaultdict(lambda: defaultdict(list))
        for t in self.schedule.trials:
            by_pair_case[t.pair_index][t.case_index].append(t.condition)
        for pair_j in range(PLANNED_PAIRS):
            for case_k in range(12):
                conds = by_pair_case[pair_j][case_k]
                self.assertEqual(len(conds), 2)
                self.assertEqual(set(conds), {"DISPLAYED", "WITHHELD"})

    def test_case_position_exact_frequency(self):
        """Each case-position pair (k, i) occurs exactly twice across the 24 participants."""
        counts = Counter((t.case_index, t.position) for t in self.schedule.trials)
        self.assertEqual(len(counts), 144)
        for (k, i), cnt in counts.items():
            self.assertEqual(cnt, 2, f"Pair ({k}, {i}) count {cnt} != 2")

    def test_deterministic_hash_stability(self):
        """Regenerating schedule with same seed produces identical SHA-256."""
        sched2 = generate_schedule(seed=20261107)
        self.assertEqual(self.schedule.schedule_sha256, sched2.schedule_sha256)
        self.assertEqual(len(self.schedule.schedule_sha256), 64)


class ScoringAndOverreadingTests(unittest.TestCase):
    def test_displayed_condition_scoring(self):
        # Option 1 (PRESENCE_ONLY) is key -> 1
        s1 = score_trial("DISPLAYED", 1)
        self.assertTrue(s1.is_correct)
        self.assertFalse(s1.is_harmful_overread)
        self.assertFalse(s1.is_contradiction_misread)

        # Option 2 (ABSENCE) -> contradiction, not overread
        s2 = score_trial("DISPLAYED", 2)
        self.assertFalse(s2.is_correct)
        self.assertFalse(s2.is_harmful_overread)
        self.assertTrue(s2.is_contradiction_misread)

        # Options 3-7 -> harmful overreads
        for opt in (3, 4, 5, 6, 7):
            s = score_trial("DISPLAYED", opt)
            self.assertFalse(s.is_correct)
            self.assertTrue(s.is_harmful_overread, f"Option {opt} should be harmful overread")
            self.assertFalse(s.is_contradiction_misread)

        # Option 8 (UNKNOWN_NOT_SHOWN) -> incorrect in displayed
        s8 = score_trial("DISPLAYED", 8)
        self.assertFalse(s8.is_correct)
        self.assertFalse(s8.is_harmful_overread)

    def test_withheld_condition_scoring(self):
        # Option 8 (UNKNOWN_NOT_SHOWN) is key -> 1
        s8 = score_trial("WITHHELD", 8)
        self.assertTrue(s8.is_correct)
        self.assertFalse(s8.is_harmful_overread)
        self.assertFalse(s8.is_contradiction_misread)

        # Options 1 & 2 in withheld -> harmful overreading
        s1 = score_trial("WITHHELD", 1)
        self.assertFalse(s1.is_correct)
        self.assertTrue(s1.is_harmful_overread)

        s2 = score_trial("WITHHELD", 2)
        self.assertFalse(s2.is_correct)
        self.assertTrue(s2.is_harmful_overread)

        # Options 3-7 in withheld -> harmful overreading
        for opt in (3, 4, 5, 6, 7):
            s = score_trial("WITHHELD", opt)
            self.assertFalse(s.is_correct)
            self.assertTrue(s.is_harmful_overread)

    def test_missing_and_invalid_responses(self):
        for val in (None, 0, 9, 99):
            s = score_trial("DISPLAYED", val)
            self.assertTrue(s.is_missing)
            self.assertFalse(s.is_correct)
            self.assertFalse(s.is_harmful_overread)


class ParticipantEvaluationAndBoundsTests(unittest.TestCase):
    def test_perfect_complete_participant(self):
        """Participant answering all 6 displayed with Option 1 and all 6 withheld with Option 8."""
        responses = [
            ParticipantTrialResponse(position=i, case_index=i, condition="DISPLAYED", selected_option=1)
            for i in range(6)
        ] + [
            ParticipantTrialResponse(position=i+6, case_index=i+6, condition="WITHHELD", selected_option=8)
            for i in range(6)
        ]
        summary = evaluate_participant("P01", pair_index=0, role="A", responses=responses)
        self.assertTrue(summary.is_complete)
        self.assertEqual(summary.displayed_correct, 6)
        self.assertEqual(summary.withheld_correct, 6)
        self.assertAlmostEqual(summary.accuracy_difference, 0.0)
        self.assertAlmostEqual(summary.lower_bound, 0.0)
        self.assertAlmostEqual(summary.upper_bound, 0.0)
        self.assertFalse(summary.has_harmful_overread)

    def test_asymmetric_complete_participant(self):
        """Participant with 6/6 displayed correct, but only 3/6 withheld correct (others picked 3, 4, 5)."""
        responses = [
            ParticipantTrialResponse(position=i, case_index=i, condition="DISPLAYED", selected_option=1)
            for i in range(6)
        ] + [
            ParticipantTrialResponse(position=6, case_index=6, condition="WITHHELD", selected_option=8),
            ParticipantTrialResponse(position=7, case_index=7, condition="WITHHELD", selected_option=8),
            ParticipantTrialResponse(position=8, case_index=8, condition="WITHHELD", selected_option=8),
            ParticipantTrialResponse(position=9, case_index=9, condition="WITHHELD", selected_option=3),  # overread
            ParticipantTrialResponse(position=10, case_index=10, condition="WITHHELD", selected_option=4), # overread
            ParticipantTrialResponse(position=11, case_index=11, condition="WITHHELD", selected_option=5), # overread
        ]
        summary = evaluate_participant("P02", pair_index=0, role="B", responses=responses)
        self.assertTrue(summary.is_complete)
        self.assertEqual(summary.displayed_correct, 6)
        self.assertEqual(summary.withheld_correct, 3)
        # acc_diff = 6/6 - 3/6 = 0.5
        self.assertAlmostEqual(summary.accuracy_difference, 0.5)
        self.assertAlmostEqual(summary.lower_bound, 0.5)
        self.assertAlmostEqual(summary.upper_bound, 0.5)
        self.assertTrue(summary.has_harmful_overread)
        self.assertEqual(summary.overread_event_count, 3)

    def test_missing_responses_bound_calculation(self):
        """Participant with 2 missing displayed and 1 missing withheld."""
        responses = [
            ParticipantTrialResponse(position=0, case_index=0, condition="DISPLAYED", selected_option=1),
            ParticipantTrialResponse(position=1, case_index=1, condition="DISPLAYED", selected_option=1),
            ParticipantTrialResponse(position=2, case_index=2, condition="DISPLAYED", selected_option=1),
            ParticipantTrialResponse(position=3, case_index=3, condition="DISPLAYED", selected_option=1),
            ParticipantTrialResponse(position=4, case_index=4, condition="DISPLAYED", selected_option=None), # missing
            ParticipantTrialResponse(position=5, case_index=5, condition="DISPLAYED", selected_option=None), # missing
            ParticipantTrialResponse(position=6, case_index=6, condition="WITHHELD", selected_option=8),
            ParticipantTrialResponse(position=7, case_index=7, condition="WITHHELD", selected_option=8),
            ParticipantTrialResponse(position=8, case_index=8, condition="WITHHELD", selected_option=8),
            ParticipantTrialResponse(position=9, case_index=9, condition="WITHHELD", selected_option=2),    # wrong
            ParticipantTrialResponse(position=10, case_index=10, condition="WITHHELD", selected_option=2),   # wrong
            ParticipantTrialResponse(position=11, case_index=11, condition="WITHHELD", selected_option=None),# missing
        ]
        summary = evaluate_participant("P03", pair_index=1, role="A", responses=responses)
        self.assertFalse(summary.is_complete)
        self.assertIsNone(summary.accuracy_difference)
        # c_s = 4, m_s = 2; c_w = 3, m_w = 1
        # L[p] = c_s/6 - (c_w + m_w)/6 = 4/6 - (3 + 1)/6 = 0.0
        # U[p] = (c_s + m_s)/6 - c_w/6 = (4 + 2)/6 - 3/6 = 0.5
        self.assertAlmostEqual(summary.lower_bound, 0.0)
        self.assertAlmostEqual(summary.upper_bound, 0.5)


class ReportAggregationTests(unittest.TestCase):
    def test_full_cohort_floor_pass(self):
        """24 complete participants -> floor met, 95% CI computed."""
        summaries = []
        for i in range(PLANNED_COHORT_N):
            # Vary diffs slightly: half diff=0.333, half diff=0.5
            c_s = 5 if i % 2 == 0 else 6
            c_w = 3
            acc_diff = (c_s - c_w) / 6.0
            summaries.append(ParticipantSummary(
                participant_id=f"P{i+1:02d}",
                pair_index=i // 2,
                role="A" if i % 2 == 0 else "B",
                is_complete=True,
                displayed_correct=c_s,
                displayed_missing=0,
                withheld_correct=c_w,
                withheld_missing=0,
                accuracy_difference=acc_diff,
                lower_bound=acc_diff,
                upper_bound=acc_diff,
                has_harmful_overread=False,
                overread_event_count=0,
                contradiction_count=0,
            ))
        report = compute_h4_report(summaries)
        self.assertTrue(report.planned_floor_met)
        self.assertEqual(report.complete_count, 24)
        self.assertEqual(report.status, "COMPLETE_EVALUATION_PASS")
        self.assertIsNotNone(report.mean_accuracy_difference)
        self.assertIsNotNone(report.ci95_lower)
        self.assertIsNotNone(report.ci95_upper)
        self.assertLess(report.ci95_lower, report.mean_accuracy_difference)
        self.assertGreater(report.ci95_upper, report.mean_accuracy_difference)
        # Zero overreading events -> binomial upper bound should be computed
        self.assertIsNotNone(report.zero_event_binomial_upper95)
        # 1 - 0.05^(1/24) ≈ 0.1175
        self.assertAlmostEqual(report.zero_event_binomial_upper95, 1.0 - (0.05 ** (1.0 / 24)), places=4)

    def test_shortfall_cohort_inconclusive(self):
        """Only 18 complete participants (< 20 floor) -> below floor inconclusive."""
        summaries = []
        for i in range(PLANNED_COHORT_N):
            is_comp = (i < 18)
            summaries.append(ParticipantSummary(
                participant_id=f"P{i+1:02d}",
                pair_index=i // 2,
                role="A" if i % 2 == 0 else "B",
                is_complete=is_comp,
                displayed_correct=4,
                displayed_missing=0 if is_comp else 2,
                withheld_correct=3,
                withheld_missing=0 if is_comp else 1,
                accuracy_difference=1/6 if is_comp else None,
                lower_bound=0.0,
                upper_bound=0.5,
                has_harmful_overread=False,
                overread_event_count=0,
                contradiction_count=0,
            ))
        report = compute_h4_report(summaries)
        self.assertFalse(report.planned_floor_met)
        self.assertEqual(report.complete_count, 18)
        self.assertEqual(report.status, "BELOW_FLOOR_INCONCLUSIVE")
        self.assertIsNone(report.mean_accuracy_difference)
        self.assertIsNone(report.ci95_lower)


if __name__ == "__main__":
    unittest.main()

