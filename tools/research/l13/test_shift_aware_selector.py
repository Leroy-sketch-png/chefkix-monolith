"""
Unit tests for L13' Shift-Aware Selector Interface and Leakage Guard.

Verifies:
1. Request-time feature validation and fail-closed forbidden feature leakage guard.
2. Deterministic routing across frozen scorer arms.
3. Fallback behavior when preferred arm is absent or policy fails.
4. Denominator preservation: failure and empty-pool abstain states are strictly preserved.
5. Tradeoff evaluation against individual arms with fail-closed no_guarantee status.
"""

import pytest

from tools.research.l13.shift_aware_selector import (
    FORBIDDEN_FEATURE_KEYWORDS,
    ArmPerformanceSummary,
    RequestFeatures,
    ScorerOutputSlate,
    SelectorEvaluationCase,
    SelectorRoutingDecision,
    SelectorTradeoffReport,
    ShiftAwareSelector,
    detect_forbidden_feature_leakage,
    evaluate_arm_performance,
    evaluate_shift_aware_selector,
)


class TestForbiddenFeatureLeakage:
    def test_clean_features_pass(self):
        clean_features = {
            "token_count": 42,
            "cuisine": "vietnamese",
            "has_pantry_oil": True,
            "region": "central",
        }
        detect_forbidden_feature_leakage(clean_features)

    @pytest.mark.parametrize("keyword", FORBIDDEN_FEATURE_KEYWORDS)
    def test_forbidden_keywords_fail_closed(self, keyword):
        dirty_features = {
            f"user_pref_{keyword}_flag": 1,
            "cuisine": "vietnamese",
        }
        with pytest.raises(ValueError, match="Forbidden feature leakage detected"):
            detect_forbidden_feature_leakage(dirty_features)


class TestRequestFeatures:
    def test_valid_request_features(self):
        rf = RequestFeatures(
            case_id="case_001",
            recipe_id="rec_42",
            target_slot_id="slot_salt",
            train_source_presence=True,
            foodb_profile_presence=False,
            catalog_membership_state="resolved",
            recipe_token_count=120,
            ingredient_count=8,
            cuisine_tag="vietnamese",
            extra_features={"is_quick_meal": True},
        )
        assert rf.case_id == "case_001"
        assert rf.catalog_membership_state == "resolved"

    @pytest.mark.parametrize(
        "case_id,recipe_id,target_slot_id",
        [
            ("", "rec_42", "slot_salt"),
            ("case_001", "   ", "slot_salt"),
            ("case_001", "rec_42", ""),
        ],
    )
    def test_empty_identifiers_raise(self, case_id, recipe_id, target_slot_id):
        with pytest.raises(ValueError):
            RequestFeatures(
                case_id=case_id,
                recipe_id=recipe_id,
                target_slot_id=target_slot_id,
                train_source_presence=True,
                foodb_profile_presence=False,
            )

    def test_invalid_catalog_membership_state(self):
        with pytest.raises(ValueError, match="Invalid catalog_membership_state"):
            RequestFeatures(
                case_id="case_001",
                recipe_id="rec_42",
                target_slot_id="slot_salt",
                train_source_presence=True,
                foodb_profile_presence=False,
                catalog_membership_state="invalid_state",
            )

    def test_extra_features_with_leakage_raises(self):
        with pytest.raises(ValueError, match="Forbidden feature leakage detected"):
            RequestFeatures(
                case_id="case_001",
                recipe_id="rec_42",
                target_slot_id="slot_salt",
                train_source_presence=True,
                foodb_profile_presence=False,
                extra_features={"human_valid_adjudication": True},
            )


class TestScorerOutputSlate:
    def test_slate_statuses(self):
        s_avail = ScorerOutputSlate("c1", "arm1", ["cand_a", "cand_b"])
        assert not s_avail.is_abstain()
        assert not s_avail.is_failure()

        s_abstain = ScorerOutputSlate("c1", "arm1", [], status="EMPTY_POOL_ABSTAIN")
        assert s_abstain.is_abstain()
        assert not s_abstain.is_failure()

        s_fail = ScorerOutputSlate("c1", "arm1", [], status="SCORING_FAILURE", failure=True)
        assert not s_fail.is_abstain()
        assert s_fail.is_failure()


class TestShiftAwareSelector:
    @pytest.fixture
    def selector(self):
        def policy(rf: RequestFeatures):
            if rf.foodb_profile_presence:
                return "arm_foodb", "route_to_foodb"
            if rf.train_source_presence:
                return "arm_train_graph", "route_to_train_graph"
            return "arm_baseline", "route_to_baseline_default"

        return ShiftAwareSelector(
            selector_id="selector_v1",
            available_scorer_ids={"arm_foodb", "arm_train_graph", "arm_baseline"},
            fallback_scorer_id="arm_baseline",
            routing_policy=policy,
        )

    def test_init_validation(self):
        with pytest.raises(ValueError, match="available_scorer_ids must be non-empty"):
            ShiftAwareSelector("s", set(), "arm1", lambda rf: ("arm1", "r"))

        with pytest.raises(ValueError, match="fallback_scorer_id 'arm2' must be in available_scorer_ids"):
            ShiftAwareSelector("s", {"arm1"}, "arm2", lambda rf: ("arm1", "r"))

    def test_normal_routing(self, selector):
        rf_foodb = RequestFeatures("c1", "r1", "s1", train_source_presence=False, foodb_profile_presence=True)
        slates = {
            "arm_foodb": ScorerOutputSlate("c1", "arm_foodb", ["f_1", "f_2"]),
            "arm_train_graph": ScorerOutputSlate("c1", "arm_train_graph", ["t_1"]),
            "arm_baseline": ScorerOutputSlate("c1", "arm_baseline", ["b_1"]),
        }
        dec = selector.route(rf_foodb, slates)
        assert dec.selected_scorer_id == "arm_foodb"
        assert dec.routed_by_rule == "route_to_foodb"
        assert not dec.is_fallback
        assert dec.chosen_slate.candidates == ["f_1", "f_2"]

    def test_fallback_when_arm_missing_from_slates(self, selector):
        rf_foodb = RequestFeatures("c1", "r1", "s1", train_source_presence=False, foodb_profile_presence=True)
        # arm_foodb omitted from slates
        slates = {
            "arm_baseline": ScorerOutputSlate("c1", "arm_baseline", ["b_1"]),
        }
        dec = selector.route(rf_foodb, slates)
        assert dec.selected_scorer_id == "arm_baseline"
        assert dec.is_fallback
        assert "falling back" in dec.routing_reason

    def test_fallback_when_policy_raises(self, selector):
        def bad_policy(rf: RequestFeatures):
            raise RuntimeError("simulated policy failure")

        faulty_selector = ShiftAwareSelector(
            selector_id="faulty_sel",
            available_scorer_ids={"arm_foodb", "arm_baseline"},
            fallback_scorer_id="arm_baseline",
            routing_policy=bad_policy,
        )
        rf = RequestFeatures("c1", "r1", "s1", train_source_presence=True, foodb_profile_presence=True)
        slates = {
            "arm_foodb": ScorerOutputSlate("c1", "arm_foodb", ["f_1"]),
            "arm_baseline": ScorerOutputSlate("c1", "arm_baseline", ["b_1"]),
        }
        dec = faulty_selector.route(rf, slates)
        assert dec.selected_scorer_id == "arm_baseline"
        assert "policy_exception_fallback" in dec.routed_by_rule

    def test_missing_fallback_in_slates_raises(self, selector):
        rf = RequestFeatures("c1", "r1", "s1", train_source_presence=True, foodb_profile_presence=True)
        # Even fallback arm is missing
        slates = {
            "unknown_arm": ScorerOutputSlate("c1", "unknown_arm", ["u_1"]),
        }
        with pytest.raises(ValueError, match="Fallback arm 'arm_baseline' is missing"):
            selector.route(rf, slates)

    def test_denominator_preservation_failure_and_abstain(self, selector):
        """
        When the chosen scorer outputs an abstain or failure slate, the selector MUST NOT
        silently substitute candidates from other arms.
        """
        rf = RequestFeatures("c1", "r1", "s1", train_source_presence=False, foodb_profile_presence=True)
        slates = {
            "arm_foodb": ScorerOutputSlate("c1", "arm_foodb", [], status="EMPTY_POOL_ABSTAIN"),
            "arm_baseline": ScorerOutputSlate("c1", "arm_baseline", ["b_1", "b_2"]),
        }
        dec = selector.route(rf, slates)
        assert dec.selected_scorer_id == "arm_foodb"
        assert not dec.is_fallback
        assert dec.chosen_slate.is_abstain()
        assert len(dec.chosen_slate.candidates) == 0


class TestEvaluationAndTradeoffs:
    def test_evaluate_arm_performance_empty(self):
        summary = evaluate_arm_performance("arm1", [], lambda c: ScorerOutputSlate("c", "arm1", []))
        assert summary.total_cases == 0
        assert summary.hit_at_1_rate == 0.0

    def test_evaluate_shift_aware_selector_metrics(self):
        # 3 cases
        # c1: foodb present -> arm_foodb (hit@1)
        # c2: train present -> arm_train (hit@1)
        # c3: neither present -> arm_baseline (abstain)
        c1 = SelectorEvaluationCase(
            features=RequestFeatures("c1", "r1", "s1", False, True),
            slates={
                "arm_foodb": ScorerOutputSlate("c1", "arm_foodb", ["f_good", "f_bad"]),
                "arm_train": ScorerOutputSlate("c1", "arm_train", ["t_bad"]),
                "arm_baseline": ScorerOutputSlate("c1", "arm_baseline", ["b_bad"]),
            },
            candidate_labels={"f_good": "A", "f_bad": "N", "t_bad": "N", "b_bad": "N"},
        )
        c2 = SelectorEvaluationCase(
            features=RequestFeatures("c2", "r2", "s2", True, False),
            slates={
                "arm_foodb": ScorerOutputSlate("c2", "arm_foodb", ["f_bad"]),
                "arm_train": ScorerOutputSlate("c2", "arm_train", ["t_good", "t_other"]),
                "arm_baseline": ScorerOutputSlate("c2", "arm_baseline", ["b_bad"]),
            },
            candidate_labels={"f_bad": "N", "t_good": "C", "t_other": "N", "b_bad": "N"},
        )
        c3 = SelectorEvaluationCase(
            features=RequestFeatures("c3", "r3", "s3", False, False),
            slates={
                "arm_foodb": ScorerOutputSlate("c3", "arm_foodb", ["f_bad"]),
                "arm_train": ScorerOutputSlate("c3", "arm_train", ["t_bad"]),
                "arm_baseline": ScorerOutputSlate("c3", "arm_baseline", [], status="EMPTY_POOL_ABSTAIN"),
            },
            candidate_labels={"f_bad": "N", "t_bad": "N"},
        )

        def policy(rf: RequestFeatures):
            if rf.foodb_profile_presence:
                return "arm_foodb", "rule_foodb"
            if rf.train_source_presence:
                return "arm_train", "rule_train"
            return "arm_baseline", "rule_baseline"

        sel = ShiftAwareSelector(
            selector_id="sel_eval",
            available_scorer_ids={"arm_foodb", "arm_train", "arm_baseline"},
            fallback_scorer_id="arm_baseline",
            routing_policy=policy,
        )

        report = evaluate_shift_aware_selector(sel, [c1, c2, c3])
        assert report.total_eval_cases == 3
        assert report.guarantee_status == "no_guarantee"
        assert report.status == "EXPLORATORY_DESCRIPTIVE_ONLY"

        # Check selector summary
        # c1 hit, c2 hit, c3 abstain -> hit1 count = 2 / 3
        assert report.selector_summary.hit_at_1_count == 2
        assert report.selector_summary.abstain_cases == 1
        assert pytest.approx(report.selector_summary.hit_at_1_rate) == 2 / 3
        # Mean set size: (2 + 2 + 0) / 3 = 4/3
        assert pytest.approx(report.selector_summary.mean_set_size) == 4 / 3

        # Check arm_baseline alone (all 3 cases routed to arm_baseline):
        # c1: b_bad (miss), c2: b_bad (miss), c3: abstain
        base_summ = report.individual_arm_summaries["arm_baseline"]
        assert base_summ.hit_at_1_count == 0
        assert base_summ.abstain_cases == 1
