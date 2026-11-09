"""
Unit tests for L37: Strata and Cluster-Aware Comparative Metric Evaluator.

Verifies:
1. Wilson score confidence interval computation and boundary handling.
2. Cluster-aware bootstrap confidence interval computation and deterministic repeatability.
3. Metric extractors (Hit@1, Hit@5, Hit@10, MRR@20).
4. Stratified comparative cohort report generation across Oracle Strata and Partition-1 cells.
5. Denominator preservation: failures and abstentions remain in denominator.
6. Unmatched rate calculation and >5% flag triggering.
7. Primary conditional rescue estimand P(V=1 | H=0) and decision threshold logic (SUPPORT_H1, REFUTE_H1, INCONCLUSIVE).
8. Protocol v1 constants and strata count alignment (10,747 total, 1,162 profiled).
"""

import pytest

from tools.research.l37.strata_cluster_comparator import (
    CANONICAL_PROFILED_TOTAL,
    CANONICAL_TEST_DENOMINATOR,
    DecisionStatus,
    EXPECTED_ORACLE_STRATA_COUNTS,
    OracleStratum,
    Partition1Cell,
    RESCUE_DECISION_THRESHOLD,
    StrataCaseRecord,
    analyze_stratified_cohort,
    compute_cluster_bootstrap_interval,
    compute_wilson_interval,
    evaluate_conditional_rescue,
    evaluate_metric_with_intervals,
    extract_hit_1,
    extract_hit_5,
    extract_hit_10,
    extract_mrr_20,
)


class TestProtocolConstants:
    def test_oracle_strata_counts_sum_to_canonical_denominator(self):
        total = sum(EXPECTED_ORACLE_STRATA_COUNTS.values())
        assert total == CANONICAL_TEST_DENOMINATOR
        assert total == 10747

    def test_profiled_total_matches_claim_r14(self):
        profiled_sum = (
            EXPECTED_ORACLE_STRATA_COUNTS["seen_profiled"]
            + EXPECTED_ORACLE_STRATA_COUNTS["unseen_profiled"]
        )
        assert profiled_sum == CANONICAL_PROFILED_TOTAL
        assert profiled_sum == 1162

    def test_rescue_threshold_is_15_percent(self):
        assert RESCUE_DECISION_THRESHOLD == 0.15


class TestWilsonInterval:
    def test_empty_or_zero_cases(self):
        low, high = compute_wilson_interval(0, 0)
        assert low == 0.0 and high == 0.0

    def test_zero_successes(self):
        low, high = compute_wilson_interval(0, 100)
        assert low == 0.0
        assert 0.0 < high < 0.05

    def test_all_successes(self):
        low, high = compute_wilson_interval(100, 100)
        assert 0.95 < low < 1.0
        assert high == 1.0

    def test_symmetric_proportion(self):
        low, high = compute_wilson_interval(50, 100)
        assert 0.39 < low < 0.41
        assert 0.59 < high < 0.61
        assert low <= 0.5 <= high


class TestClusterBootstrapInterval:
    @pytest.fixture
    def mock_cases(self):
        cases = []
        for r_idx in range(20):
            recipe_id = f"recipe_{r_idx}"
            for c_idx in range(5):
                case_id = f"{recipe_id}_case_{c_idx}"
                hit = (r_idx % 2 == 0)  # half the recipes hit
                cases.append(
                    StrataCaseRecord(
                        case_id=case_id,
                        recipe_id=recipe_id,
                        oracle_stratum=OracleStratum.SEEN_UNPROFILED,
                        partition1_cell=Partition1Cell.TRAIN_NOT_PROFILED,
                        hit_at_1=hit,
                        hit_at_5=hit,
                        hit_at_10=hit,
                        reciprocal_rank_20=1.0 if hit else 0.0,
                    )
                )
        return cases

    def test_deterministic_repeatability(self, mock_cases):
        pt1, low1, high1 = compute_cluster_bootstrap_interval(
            mock_cases, extract_hit_5, num_resamples=500, seed=42
        )
        pt2, low2, high2 = compute_cluster_bootstrap_interval(
            mock_cases, extract_hit_5, num_resamples=500, seed=42
        )
        assert pt1 == pt2 == 0.5
        assert low1 == low2
        assert high1 == high2
        assert low1 <= pt1 <= high1

    def test_empty_cases_return_zero(self):
        pt, low, high = compute_cluster_bootstrap_interval([], extract_hit_5)
        assert pt == 0.0 and low == 0.0 and high == 0.0


class TestMetricExtractorsAndIntervals:
    def test_extractors(self):
        cases = [
            StrataCaseRecord(
                case_id="c1",
                recipe_id="r1",
                oracle_stratum=OracleStratum.SEEN_UNPROFILED,
                partition1_cell=Partition1Cell.TRAIN_NOT_PROFILED,
                hit_at_1=True,
                hit_at_5=True,
                hit_at_10=True,
                reciprocal_rank_20=1.0,
            ),
            StrataCaseRecord(
                case_id="c2",
                recipe_id="r1",
                oracle_stratum=OracleStratum.SEEN_UNPROFILED,
                partition1_cell=Partition1Cell.TRAIN_NOT_PROFILED,
                hit_at_1=False,
                hit_at_5=True,
                hit_at_10=True,
                reciprocal_rank_20=0.2,  # rank 5
            ),
            StrataCaseRecord(
                case_id="c3",
                recipe_id="r2",
                oracle_stratum=OracleStratum.SEEN_UNPROFILED,
                partition1_cell=Partition1Cell.TRAIN_NOT_PROFILED,
                hit_at_1=False,
                hit_at_5=False,
                hit_at_10=False,
                reciprocal_rank_20=0.0,  # failure / unranked
            ),
        ]
        assert extract_hit_1(cases) == pytest.approx(1 / 3)
        assert extract_hit_5(cases) == pytest.approx(2 / 3)
        assert extract_hit_10(cases) == pytest.approx(2 / 3)
        assert extract_mrr_20(cases) == pytest.approx(1.2 / 3)

    def test_evaluate_metric_wilson(self):
        cases = [
            StrataCaseRecord(
                case_id=f"c{i}",
                recipe_id=f"r{i}",
                oracle_stratum=OracleStratum.SEEN_UNPROFILED,
                partition1_cell=Partition1Cell.TRAIN_NOT_PROFILED,
                hit_at_1=(i < 40),
                hit_at_5=(i < 60),
                hit_at_10=(i < 70),
                reciprocal_rank_20=1.0 if i < 40 else 0.0,
            )
            for i in range(100)
        ]
        summary = evaluate_metric_with_intervals(
            cases, extract_hit_1, is_proportion=True, use_cluster_bootstrap=False
        )
        assert summary.point_estimate == 0.40
        assert summary.denominator == 100
        assert summary.numerator == 40
        assert summary.ci_method == "wilson"
        assert summary.ci_lower <= 0.40 <= summary.ci_upper


class TestStratifiedCohortAnalysis:
    @pytest.fixture
    def cohort_sample(self):
        records = []
        strata = list(OracleStratum)
        cells = list(Partition1Cell)

        for i in range(120):
            rec_id = f"recipe_{i // 4}"
            strat = strata[i % 4]
            cell = cells[i % 4]
            is_unmatched = (i < 8)  # 8 / 120 = 6.67% > 5%
            is_fail = (i >= 110)
            hit1 = (i % 3 == 0) and not is_fail
            hit5 = (i % 2 == 0) and not is_fail
            hit10 = (i % 2 == 0 or i % 3 == 0) and not is_fail
            mrr = 1.0 if hit1 else (0.2 if hit5 else (0.1 if hit10 else 0.0))

            records.append(
                StrataCaseRecord(
                    case_id=f"case_{i}",
                    recipe_id=rec_id,
                    oracle_stratum=strat,
                    partition1_cell=cell,
                    hit_at_1=hit1,
                    hit_at_5=hit5,
                    hit_at_10=hit10,
                    reciprocal_rank_20=mrr,
                    is_unmatched=is_unmatched,
                    is_failure=is_fail,
                )
            )
        return records

    def test_analyze_cohort_full_breakdown(self, cohort_sample):
        report = analyze_stratified_cohort(cohort_sample, seed=123)
        assert report.total_cases == 120
        assert report.unmatched_count == 8
        assert report.unmatched_rate == pytest.approx(8 / 120)
        assert report.unmatched_rate_flag is True  # 6.67% > 5% flag triggered
        assert report.failure_count == 10

        # Denominator preservation: point estimates reflect full N=120
        assert report.hit_at_1.denominator == 120
        assert report.hit_at_5.denominator == 120
        assert report.mrr_at_20.denominator == 120

        # Oracle strata breakdown covers all 4 strata
        assert len(report.oracle_strata_breakdown) == 4
        for s in OracleStratum:
            assert s.value in report.oracle_strata_breakdown
            s_metrics = report.oracle_strata_breakdown[s.value]
            assert "hit_at_1" in s_metrics
            assert "hit_at_5" in s_metrics
            assert "mrr_at_20" in s_metrics

        # Partition-1 cells breakdown covers all 4 cells
        assert len(report.partition1_breakdown) == 4
        for c in Partition1Cell:
            assert c.value in report.partition1_breakdown

    def test_unmatched_flag_false_when_under_5_percent(self):
        records = [
            StrataCaseRecord(
                case_id=f"case_{i}",
                recipe_id=f"rec_{i}",
                oracle_stratum=OracleStratum.SEEN_UNPROFILED,
                partition1_cell=Partition1Cell.TRAIN_NOT_PROFILED,
                hit_at_1=True,
                hit_at_5=True,
                hit_at_10=True,
                reciprocal_rank_20=1.0,
                is_unmatched=(i == 0),  # 1 / 50 = 2% <= 5%
            )
            for i in range(50)
        ]
        report = analyze_stratified_cohort(records)
        assert report.unmatched_count == 1
        assert report.unmatched_rate == 0.02
        assert report.unmatched_rate_flag is False


class TestConditionalRescueEstimator:
    def test_support_h1_when_lower_bound_exceeds_threshold(self):
        # 80 unseen cases where H=0 (gold not matched in top 5)
        # 60 have V=1 (acceptable alternative) -> 60/80 = 75% rescue
        cases = []
        for i in range(80):
            cases.append(
                StrataCaseRecord(
                    case_id=f"case_{i}",
                    recipe_id=f"rec_{i // 2}",
                    oracle_stratum=OracleStratum.UNSEEN_UNPROFILED,
                    partition1_cell=Partition1Cell.NOT_TRAIN_NOT_PROFILED,
                    hit_at_1=False,
                    hit_at_5=False,  # H=0
                    hit_at_10=False,
                    reciprocal_rank_20=0.0,
                    is_gold_matched_at_5=False,
                    is_valid_candidate_at_5=(i < 60),  # 60/80 = 75%
                )
            )

        report = evaluate_conditional_rescue(
            cases,
            target_strata=[OracleStratum.UNSEEN_UNPROFILED],
            threshold=0.15,
            num_resamples=500,
            seed=20261021,
        )
        assert report.h0_denominator == 80
        assert report.v1_count == 60
        assert report.conditional_rescue_rate == 0.75
        assert report.ci_95_lower > 0.15
        assert report.decision == DecisionStatus.SUPPORT_H1
        assert report.joint_p_v1_h0.point_estimate == 0.75

    def test_refute_h1_when_upper_bound_below_threshold(self):
        # 100 unseen cases where H=0
        # only 2 have V=1 -> 2/100 = 2% rescue
        cases = []
        for i in range(100):
            cases.append(
                StrataCaseRecord(
                    case_id=f"case_{i}",
                    recipe_id=f"rec_{i // 4}",
                    oracle_stratum=OracleStratum.UNSEEN_UNPROFILED,
                    partition1_cell=Partition1Cell.NOT_TRAIN_NOT_PROFILED,
                    hit_at_1=False,
                    hit_at_5=False,  # H=0
                    hit_at_10=False,
                    reciprocal_rank_20=0.0,
                    is_gold_matched_at_5=False,
                    is_valid_candidate_at_5=(i < 2),  # 2%
                )
            )

        report = evaluate_conditional_rescue(
            cases,
            target_strata=[OracleStratum.UNSEEN_UNPROFILED],
            threshold=0.15,
            num_resamples=500,
            seed=20261021,
        )
        assert report.conditional_rescue_rate == 0.02
        assert report.ci_95_upper < 0.15
        assert report.decision == DecisionStatus.REFUTE_H1

    def test_inconclusive_when_interval_spans_threshold(self):
        # 40 cases where H=0
        # 6 have V=1 -> 15% rescue with wide interval spanning 0.15
        cases = []
        for i in range(40):
            cases.append(
                StrataCaseRecord(
                    case_id=f"case_{i}",
                    recipe_id=f"rec_{i // 2}",
                    oracle_stratum=OracleStratum.UNSEEN_UNPROFILED,
                    partition1_cell=Partition1Cell.NOT_TRAIN_NOT_PROFILED,
                    hit_at_1=False,
                    hit_at_5=False,
                    hit_at_10=False,
                    reciprocal_rank_20=0.0,
                    is_gold_matched_at_5=False,
                    is_valid_candidate_at_5=(i < 6),  # 6/40 = 15%
                )
            )

        report = evaluate_conditional_rescue(
            cases,
            target_strata=[OracleStratum.UNSEEN_UNPROFILED],
            threshold=0.15,
            num_resamples=500,
            seed=20261021,
        )
        assert report.conditional_rescue_rate == 0.15
        assert report.ci_95_lower < 0.15 < report.ci_95_upper
        assert report.decision == DecisionStatus.INCONCLUSIVE
