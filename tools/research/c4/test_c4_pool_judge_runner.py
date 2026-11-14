"""Source-free contract tests for the current L43 C4 pool-judge path."""
from __future__ import annotations

import hashlib
import json
import unittest

from c4_pool_judge_runner import (
    BOOTSTRAP_DRAWS,
    BOOTSTRAP_SEED,
    DIAGNOSTIC_JUDGE_MODEL,
    PRIMARY_JUDGE_MODEL,
    SYSTEM_PROMPT_SHA256,
    USER_TEMPLATE_SHA256,
    CandidateItem,
    PoolJudgeCase,
    PoolResponse,
    SingleJudgment,
    build_gpt4o_common_slate,
    common_slate_fingerprint,
    compute_case_cluster_bootstrap_result,
    compute_unweighted_cohens_kappa,
    evaluate_c4_operational_gates,
    format_user_prompt,
    normalize_case_candidates,
    parse_and_validate_pool_response,
    render_c4_report_markdown,
    verify_prompt_assets,
)


def make_case(index: int, *, arms: tuple[str, ...] = ("GISMo",)) -> PoolJudgeCase:
    return PoolJudgeCase(
        case_id=f"case-{index:02d}",
        recipe_title=f"Synthetic recipe {index}",
        ingredients=["flour", "water"],
        target_ingredient="flour",
        candidates=[
            CandidateItem(f"c-{index:02d}-a", "oat flour", "cell-a", arm=arms[0], arm_memberships=arms),
            CandidateItem(f"c-{index:02d}-n", "motor oil", "cell-a", arm=arms[0], arm_memberships=arms),
        ],
    )


def make_response(
    case: PoolJudgeCase,
    pass_name: str,
    labels: dict[str, str],
    *,
    model: str = PRIMARY_JUDGE_MODEL,
    returned: str | None = None,
    temperature: float = 0.0,
    attempt: int = 1,
    raw_text: str | None = None,
) -> PoolResponse:
    if raw_text is None:
        raw_text = json.dumps(
            {
                "judgments": {
                    candidate.opaque_id: {
                        "label": labels[candidate.opaque_id],
                        "reason": "Synthetic contract fixture.",
                    }
                    for candidate in case.candidates
                }
            },
            separators=(",", ":"),
        )
    return parse_and_validate_pool_response(
        raw_text,
        case,
        pass_name,
        model_requested=model,
        model_returned=returned or model,
        temperature=temperature,
        system_prompt_sha256=SYSTEM_PROMPT_SHA256,
        user_template_sha256=USER_TEMPLATE_SHA256,
        attempt_number=attempt,
    )


def perfect_responses(cases: list[PoolJudgeCase]):
    forward: list[PoolResponse] = []
    reverse: list[PoolResponse] = []
    human: dict[str, dict[str, str]] = {}
    for case in cases:
        labels = {
            case.candidates[0].opaque_id: "A",
            case.candidates[1].opaque_id: "N",
        }
        human[case.case_id] = dict(labels)
        forward.append(make_response(case, "FORWARD", labels))
        reverse.append(make_response(case, "REVERSE", labels))
    return forward, reverse, human


class PromptAndResponseContractTests(unittest.TestCase):
    def setUp(self) -> None:
        self.case = make_case(0)

    def test_prompt_assets_are_checked_against_both_committed_files(self):
        verified = verify_prompt_assets()
        self.assertTrue(verified["system_prompt_verified"])
        self.assertTrue(verified["user_template_verified"])
        self.assertTrue(verified["system_prompt_file_verified"])
        self.assertTrue(verified["user_template_file_verified"])

    def test_prompt_reversal_changes_only_candidate_array_order(self):
        forward = format_user_prompt(self.case)
        reverse = format_user_prompt(self.case, reverse_order=True)
        self.assertLess(forward.index('"c-00-a"'), forward.index('"c-00-n"'))
        self.assertLess(reverse.index('"c-00-n"'), reverse.index('"c-00-a"'))
        self.assertNotIn("FORWARD", forward)
        self.assertNotIn("REVERSE", reverse)
        self.assertNotIn("cell-a", forward)
        self.assertNotIn("GISMo", forward)

    def test_unicode_user_fields_are_valid_json_literals(self):
        case = PoolJudgeCase(
            "case-unicode", 'Crème "cake"', ["café", "water"], "café",
            [CandidateItem("opaque-1", "rice flour")],
        )
        prompt = format_user_prompt(case)
        self.assertIn('"Crème \\\"cake\\\""', prompt)
        self.assertIn('"café"', prompt)

    def test_exact_response_is_valid_and_records_request_metadata(self):
        labels = {candidate.opaque_id: "U" for candidate in self.case.candidates}
        result = make_response(self.case, "FORWARD", labels)
        self.assertTrue(result.is_valid)
        self.assertEqual(set(result.judgments), set(labels))
        self.assertEqual(result.judgments["c-00-a"].label, "U")
        self.assertEqual(result.model_requested, PRIMARY_JUDGE_MODEL)
        self.assertEqual(result.model_returned, PRIMARY_JUDGE_MODEL)
        self.assertEqual(result.temperature, 0.0)
        self.assertEqual(result.attempt_number, 1)

    def test_parser_rejects_duplicate_json_keys_extra_fields_and_markdown(self):
        duplicate_key = (
            '{"judgments":{"c-00-a":{"label":"A","reason":"x"},'
            '"c-00-a":{"label":"A","reason":"y"},'
            '"c-00-n":{"label":"N","reason":"x"}}}'
        )
        extra_top = '{"judgments":{},"note":"outside schema"}'
        extra_nested = json.dumps({"judgments": {
            "c-00-a": {"label": "A", "reason": "x", "score": 1},
            "c-00-n": {"label": "N", "reason": "x"},
        }})
        fenced = "```json\n{}\n```"
        for raw in (duplicate_key, extra_top, extra_nested, fenced):
            with self.subTest(raw=raw[:20]):
                self.assertFalse(parse_and_validate_pool_response(raw, self.case, "FORWARD").is_valid)

    def test_parser_rejects_missing_foreign_invalid_and_overlong_items(self):
        valid_item = {"label": "A", "reason": "works"}
        cases = [
            {"judgments": {"c-00-a": valid_item}},
            {"judgments": {"c-00-a": valid_item, "foreign": valid_item}},
            {"judgments": {"c-00-a": {"label": "positive", "reason": "x"}, "c-00-n": valid_item}},
            {"judgments": {"c-00-a": {"label": "A", "reason": " "}, "c-00-n": valid_item}},
            {"judgments": {"c-00-a": {"label": "A", "reason": "word " * 21}, "c-00-n": valid_item}},
        ]
        for value in cases:
            with self.subTest(value=value):
                self.assertFalse(parse_and_validate_pool_response(json.dumps(value), self.case, "FORWARD").is_valid)

    def test_duplicate_cross_arm_identity_is_deduplicated_and_memberships_preserved(self):
        case = PoolJudgeCase(
            "case-dup", "Recipe", ["flour"], "flour",
            [
                CandidateItem("same-id", "oat flour", "cell", arm="GISMo", identity_key="semantic-1"),
                CandidateItem("same-id", "oat flour", "cell", arm="lookup", identity_key="semantic-1"),
                CandidateItem("other", "water", "cell", arm="lookup"),
            ],
        )
        normalized = normalize_case_candidates(case)
        self.assertEqual([item.opaque_id for item in normalized.candidates], ["same-id", "other"])
        self.assertEqual(normalized.candidates[0].arm_memberships, ("GISMo", "lookup"))

    def test_duplicate_candidate_within_same_arm_is_not_silently_collapsed(self):
        case = PoolJudgeCase(
            "case-repeat", "Recipe", ["flour"], "flour",
            [
                CandidateItem("same", "oat flour", "cell", arm="GISMo", identity_key="same"),
                CandidateItem("same", "oat flour", "cell", arm="GISMo", identity_key="same"),
            ],
        )
        with self.assertRaisesRegex(ValueError, "duplicate_candidate_within_arm"):
            normalize_case_candidates(case)

    def test_parser_does_not_invent_observed_prompt_hashes(self):
        labels = {candidate.opaque_id: "A" for candidate in self.case.candidates}
        raw = json.dumps({
            "judgments": {
                opaque_id: {"label": label, "reason": "Valid."}
                for opaque_id, label in labels.items()
            }
        })
        unknown = parse_and_validate_pool_response(
            raw, self.case, "FORWARD", model_requested=PRIMARY_JUDGE_MODEL,
            model_returned=PRIMARY_JUDGE_MODEL, temperature=0.0,
        )
        self.assertTrue(unknown.is_valid)
        self.assertIsNone(unknown.system_prompt_sha256)
        self.assertIsNone(unknown.user_template_sha256)


class StatisticsContractTests(unittest.TestCase):
    def test_nominal_four_label_kappa_accepts_unknown_as_a_real_category(self):
        pairs = [(label, label) for label in ("A", "C", "N", "U") for _ in range(3)]
        self.assertEqual(compute_unweighted_cohens_kappa(pairs), 1.0)
        with self.assertRaisesRegex(ValueError, "exact_A_C_N_U"):
            compute_unweighted_cohens_kappa([("A", "MISSING")])

    def test_cluster_bootstrap_reports_requested_valid_and_degenerate_draws(self):
        varying_cases = {
            f"case-{index:02d}": [("A", "A"), ("N", "N")]
            for index in range(20)
        }
        first = compute_case_cluster_bootstrap_result(varying_cases, draws=200, seed=BOOTSTRAP_SEED)
        second = compute_case_cluster_bootstrap_result(varying_cases, draws=200, seed=BOOTSTRAP_SEED)
        self.assertEqual(first, second)
        self.assertEqual(first.requested_draws, 200)
        self.assertEqual(first.valid_draws + first.degenerate_draws, 200)
        self.assertGreaterEqual(first.valid_draws, 20)
        self.assertEqual(first.lower, 1.0)

    def test_cluster_bootstrap_reports_unavailable_for_degenerate_resamples(self):
        constant_cases = {f"case-{index:02d}": [("A", "A")] for index in range(20)}
        result = compute_case_cluster_bootstrap_result(constant_cases, draws=50, seed=7)
        self.assertEqual(result.requested_draws, 50)
        self.assertEqual(result.valid_draws, 0)
        self.assertEqual(result.degenerate_draws, 50)
        self.assertIsNone(result.lower)


class OperationalGateTests(unittest.TestCase):
    def setUp(self) -> None:
        self.cases = [make_case(index) for index in range(20)]
        self.forward, self.reverse, self.human = perfect_responses(self.cases)

    def evaluate(self, **kwargs):
        return evaluate_c4_operational_gates(
            self.cases,
            self.forward,
            self.reverse,
            self.human,
            generator_family_by_arm={"GISMo": "non_llm"},
            **kwargs,
        )

    def test_arithmetic_passes_but_source_free_development_state_cannot_be_accepted(self):
        report = self.evaluate()
        self.assertEqual(report.assigned_cases, 20)
        self.assertEqual(report.assigned_candidate_units, 40)
        self.assertEqual(report.reference_present_candidate_units, 40)
        self.assertEqual(report.judge_valid_candidate_units, 40)
        self.assertEqual(report.paired_human_reference_units, 40)
        self.assertEqual(report.agreement_kappa_gate.status, "PASS")
        self.assertEqual(report.kappa_ci_lower_gate.status, "PASS")
        self.assertEqual(report.invalid_output_gate.status, "PASS")
        self.assertEqual(report.order_instability_gate.status, "PASS")
        self.assertEqual(report.generator_family_gate.status, "PASS")
        self.assertEqual(report.overall_status, "INCOMPLETE_GATES_UNAVAILABLE")
        self.assertEqual(report.source_input_hash_status, "MISSING_OR_UNVERIFIED")
        self.assertEqual(report.human_reference_gate.status, "UNAVAILABLE")
        self.assertEqual(report.prompt_freeze_gate.status, "UNAVAILABLE")
        self.assertEqual(report.kappa_ci_lower_gate.details["requested_draws"], BOOTSTRAP_DRAWS)
        self.assertEqual(report.kappa_ci_lower_gate.details["seed"], BOOTSTRAP_SEED)
        self.assertTrue(report.input_hashes["case_slate_canonical_sha256"])
        self.assertTrue(all(gate.pass_identity != "unspecified" for gate in [
            report.invalid_output_gate,
            report.order_instability_gate,
            report.agreement_kappa_gate,
            report.kappa_ci_lower_gate,
            report.judge_model_identity_gate,
            report.source_input_gate,
            report.human_reference_gate,
            report.prompt_freeze_gate,
        ]))

    def test_source_pins_are_derived_from_and_compared_to_raw_manifest_bytes(self):
        manifests = {
            "assignment_manifest": b"synthetic-assignment-manifest",
            "candidate_pool_manifest": b"synthetic-pool-manifest",
            "human_adjudication_manifest": b"synthetic-human-manifest",
        }
        pins = {f"{name}_sha256": hashlib.sha256(raw).hexdigest() for name, raw in manifests.items()}
        report = self.evaluate(
            source_manifest_bytes=manifests,
            expected_source_file_hashes=pins,
        )
        self.assertEqual(report.source_input_hash_status, "PINNED")
        self.assertEqual(report.source_input_gate.status, "PASS")
        self.assertEqual(
            report.input_hashes["source_human_adjudication_manifest_sha256"],
            pins["human_adjudication_manifest_sha256"],
        )
        changed = dict(pins)
        changed["assignment_manifest_sha256"] = "0" * 64
        report = self.evaluate(
            source_manifest_bytes=manifests,
            expected_source_file_hashes=changed,
        )
        self.assertEqual(report.source_input_hash_status, "MISSING_OR_UNVERIFIED")
        self.assertEqual(report.source_input_gate.status, "FAIL")

    def test_primary_kappa_uses_forward_only_and_rejects_systematic_disagreement(self):
        self.human = {
            case.case_id: {
                case.candidates[0].opaque_id: "N",
                case.candidates[1].opaque_id: "A",
            }
            for case in self.cases
        }
        report = self.evaluate()
        self.assertEqual(report.agreement_kappa_gate.status, "FAIL")
        self.assertEqual(report.agreement_kappa_gate.observed_value, -1.0)

    def test_order_gate_uses_conservative_missingness_upper_bound(self):
        self.reverse = []
        report = self.evaluate()
        self.assertEqual(report.order_instability_gate.details["assigned_unique_units_D"], 40)
        self.assertEqual(report.order_instability_gate.details["missing_either_M"], 40)
        self.assertEqual(report.order_instability_gate.details["full_assignment_lower_bound"], 0.0)
        self.assertEqual(report.order_instability_gate.details["full_assignment_upper_bound"], 1.0)
        self.assertEqual(report.order_instability_gate.status, "FAIL")

    def test_order_gate_uses_unique_unit_difference_and_assignment_bounds(self):
        flipped = []
        for case in self.cases:
            labels = {
                case.candidates[0].opaque_id: "N",
                case.candidates[1].opaque_id: "A",
            }
            flipped.append(make_response(case, "REVERSE", labels))
        self.reverse = flipped
        report = self.evaluate()
        self.assertEqual(report.order_instability_gate.details["different_valid_labels_F"], 40)
        self.assertEqual(report.order_instability_gate.details["missing_either_M"], 0)
        self.assertEqual(report.order_instability_gate.details["conditional_instability"], 1.0)
        self.assertEqual(report.order_instability_gate.details["full_assignment_lower_bound"], 1.0)
        self.assertEqual(report.order_instability_gate.status, "FAIL")

    def test_cell_leniency_reports_signed_difference_and_counts(self):
        self.human = {
            case.case_id: {candidate.opaque_id: "N" for candidate in case.candidates}
            for case in self.cases
        }
        report = self.evaluate()
        gate = report.leniency_cell_gates[0]
        self.assertEqual(gate.status, "FAIL")
        self.assertEqual(gate.details["signed_delta_c"], 0.5)
        self.assertEqual(gate.numerator, 20)
        self.assertEqual(gate.denominator, 40)

    def test_invalid_initial_attempt_stays_in_fixed_denominator_after_retry_success(self):
        case = self.cases[0]
        labels = {candidate.opaque_id: ("A" if candidate.opaque_id.endswith("-a") else "N") for candidate in case.candidates}
        retry = make_response(case, "FORWARD", labels, attempt=2)
        invalid_first = parse_and_validate_pool_response(
            "not-json", case, "FORWARD", model_requested=PRIMARY_JUDGE_MODEL,
            model_returned=PRIMARY_JUDGE_MODEL, temperature=0.0,
            system_prompt_sha256=SYSTEM_PROMPT_SHA256,
            user_template_sha256=USER_TEMPLATE_SHA256,
            attempt_number=1,
        )
        self.forward[0:1] = [invalid_first, retry]
        report = self.evaluate()
        self.assertEqual(report.actual_attempt_count, 41)
        self.assertEqual(report.invalid_attempt_count, 1)
        self.assertEqual(report.invalid_output_gate.numerator, 1)
        self.assertEqual(report.invalid_output_gate.denominator, 40)
        self.assertAlmostEqual(report.invalid_output_gate.observed_value, 1 / 40)
        self.assertEqual(report.invalid_output_gate.status, "FAIL")
        self.assertEqual(report.paired_human_reference_units, 40)

    def test_response_object_valid_flag_cannot_override_invalid_raw_json(self):
        case = self.cases[0]
        fake = PoolResponse(
            case.case_id, "FORWARD", "{}", True,
            judgments={candidate.opaque_id: SingleJudgment(candidate.opaque_id, "A", "ok")
                       for candidate in case.candidates},
            model_requested=PRIMARY_JUDGE_MODEL,
            model_returned=PRIMARY_JUDGE_MODEL,
            temperature=0.0,
            system_prompt_sha256=self.forward[0].system_prompt_sha256,
            user_template_sha256=self.forward[0].user_template_sha256,
        )
        self.forward[0] = fake
        report = self.evaluate()
        self.assertEqual(report.invalid_output_gate.numerator, 1)
        self.assertEqual(report.judge_valid_candidate_units, 38)
        self.assertEqual(report.order_instability_gate.details["missing_either_M"], 2)

    def test_wrong_or_unknown_provider_identity_is_a_method_failure(self):
        case = self.cases[0]
        labels = {candidate.opaque_id: ("A" if candidate.opaque_id.endswith("-a") else "N") for candidate in case.candidates}
        self.forward[0] = make_response(
            case, "FORWARD", labels, returned="gemini-fallback", temperature=0.2
        )
        report = self.evaluate()
        self.assertEqual(report.judge_model_identity_gate.status, "FAIL")
        self.assertEqual(report.judge_model_identity_gate.details["response_identity_failures"], 1)
        self.assertEqual(report.judge_valid_candidate_units, 38)

    def test_missing_observed_prompt_hashes_fail_model_identity_gate(self):
        case = self.cases[0]
        self.forward[0] = parse_and_validate_pool_response(
            self.forward[0].raw_text, case, "FORWARD",
            model_requested=PRIMARY_JUDGE_MODEL,
            model_returned=PRIMARY_JUDGE_MODEL,
            temperature=0.0,
        )
        report = self.evaluate()
        self.assertEqual(report.judge_model_identity_gate.status, "FAIL")
        self.assertEqual(report.judge_model_identity_gate.details["response_identity_failures"], 1)
        self.assertEqual(report.judge_valid_candidate_units, 38)

    def test_human_missing_invalid_and_u_are_reported_separately(self):
        case = self.cases[0]
        self.human[case.case_id][case.candidates[0].opaque_id] = "invalid"
        del self.human[case.case_id][case.candidates[1].opaque_id]
        report = self.evaluate()
        self.assertEqual(report.invalid_human_reference_units, 1)
        self.assertEqual(report.missing_human_reference_units, 1)
        self.assertEqual(report.human_reference_gate.status, "FAIL")
        self.human[case.case_id][case.candidates[0].opaque_id] = "U"
        self.human[case.case_id][case.candidates[1].opaque_id] = "U"
        report = self.evaluate()
        self.assertEqual(report.human_u_count, 2)

    def test_unmatched_output_fails_fixed_denominator_gate_and_is_reported(self):
        orphan = PoolResponse("unassigned-case", "FORWARD", "{}", False)
        self.forward.append(orphan)
        report = self.evaluate()
        self.assertEqual(report.unmatched_response_count, 1)
        self.assertEqual(report.invalid_output_gate.numerator, 0)
        self.assertEqual(report.invalid_output_gate.denominator, 40)
        self.assertEqual(report.invalid_output_gate.status, "FAIL")

    def test_cross_arm_duplicate_counts_once_in_gate_and_once_per_arm_summary(self):
        case = PoolJudgeCase(
            "case-cross-arm", "Recipe", ["flour"], "flour",
            [
                CandidateItem("shared", "oat flour", "cell-a", arm="GISMo", identity_key="same"),
                CandidateItem("shared", "oat flour", "cell-a", arm="lookup", identity_key="same"),
            ],
        )
        response = make_response(case, "FORWARD", {"shared": "A"})
        reverse = make_response(case, "REVERSE", {"shared": "A"})
        report = evaluate_c4_operational_gates(
            [case], [response], [reverse], {"case-cross-arm": {"shared": "A"}},
            generator_family_by_arm={"GISMo": "non_llm", "lookup": "non_llm"},
        )
        self.assertEqual(report.assigned_candidate_units, 1)
        self.assertEqual(report.reference_present_candidate_units, 1)
        self.assertEqual(report.paired_human_reference_units, 1)
        self.assertEqual(report.arm_summaries["GISMo"]["assigned_candidate_units"], 1)
        self.assertEqual(report.arm_summaries["lookup"]["assigned_candidate_units"], 1)


class CommonSlateTests(unittest.TestCase):
    def test_common_slate_excludes_gpt4o_and_admits_mistral_only_after_dr04(self):
        case = PoolJudgeCase(
            "case-common", "Recipe", ["flour"], "flour",
            [
                CandidateItem("gismo", "gismo option", "cell", arm="GISMo"),
                CandidateItem("lookup", "lookup option", "cell", arm="lookup"),
                CandidateItem("mistral", "mistral option", "cell", arm="Mistral"),
                CandidateItem("gpt", "gpt option", "cell", arm="GPT-4o"),
                CandidateItem("shared", "shared option", "cell", identity_key="shared", arm_memberships=("lookup", "GPT-4o")),
            ],
        )
        base = build_gpt4o_common_slate(
            [case], eligible_arm_ids=("GISMo", "lookup"),
            gpt4o_generator_arm_ids=("GPT-4o",),
        )
        self.assertEqual([candidate.opaque_id for candidate in base[0].candidates], ["gismo", "lookup"])
        with_mistral = build_gpt4o_common_slate(
            [case], eligible_arm_ids=("GISMo", "lookup"),
            gpt4o_generator_arm_ids=("GPT-4o",),
            mistral_arm_id="Mistral", mistral_dr04_eligible=True,
        )
        self.assertEqual(
            [candidate.opaque_id for candidate in with_mistral[0].candidates],
            ["gismo", "lookup", "mistral"],
        )
        self.assertEqual(common_slate_fingerprint(base), common_slate_fingerprint(base))
        self.assertNotEqual(common_slate_fingerprint(base), common_slate_fingerprint(with_mistral))

    def test_common_slate_refuses_to_drop_any_preselected_case(self):
        case = PoolJudgeCase(
            "case-none", "Recipe", ["flour"], "flour",
            [CandidateItem("gpt", "gpt option", "cell", arm="GPT-4o")],
        )
        with self.assertRaisesRegex(ValueError, "would_drop_preselected_case"):
            build_gpt4o_common_slate(
                [case], eligible_arm_ids=("GISMo", "lookup"),
                gpt4o_generator_arm_ids=("GPT-4o",),
            )

    def test_common_slate_rejects_unapproved_arm_expansion(self):
        case = PoolJudgeCase(
            "case-gate", "Recipe", ["flour"], "flour",
            [CandidateItem("lookup", "lookup option", "cell", arm="lookup")],
        )
        with self.assertRaisesRegex(ValueError, "exact_gismo_lookup"):
            build_gpt4o_common_slate(
                [case], eligible_arm_ids=("GISMo", "lookup", "new-arm"),
                gpt4o_generator_arm_ids=("GPT-4o",),
            )

    def test_gpt4o_diagnostic_model_identity_is_checked_per_response(self):
        case = PoolJudgeCase(
            "case-openai", "Recipe", ["flour"], "flour",
            [CandidateItem("item", "option", "cell", arm="GISMo")],
        )
        response = make_response(case, "FORWARD", {"item": "A"}, model=DIAGNOSTIC_JUDGE_MODEL)
        reverse = make_response(case, "REVERSE", {"item": "A"}, model=DIAGNOSTIC_JUDGE_MODEL)
        report = evaluate_c4_operational_gates(
            [case], [response], [reverse], {case.case_id: {"item": "A"}},
            judge_model_requested=DIAGNOSTIC_JUDGE_MODEL,
            judge_model_returned=DIAGNOSTIC_JUDGE_MODEL,
            generator_family_by_arm={"GISMo": "non_llm"},
        )
        self.assertEqual(report.judge_model_identity_gate.status, "PASS")
        self.assertEqual(report.generator_family_gate.status, "PASS")
        self.assertEqual(report.judge_model_requested, DIAGNOSTIC_JUDGE_MODEL)
        self.assertEqual(report.judge_model_returned, DIAGNOSTIC_JUDGE_MODEL)

    def test_review_report_renders_denominators_and_omits_sensitive_rows(self):
        cases = [make_case(index) for index in range(20)]
        forward, reverse, human = perfect_responses(cases)
        report = evaluate_c4_operational_gates(
            cases, forward, reverse, human,
            generator_family_by_arm={"GISMo": "non_llm"},
        )
        rendered = render_c4_report_markdown(report)
        self.assertIn("# C4 pool-judge gate report", rendered)
        self.assertIn("INCOMPLETE_GATES_UNAVAILABLE", rendered)
        self.assertIn("40 / 40", rendered)
        self.assertIn("bootstrap requested=2000", rendered)
        self.assertNotIn("case-00", rendered)
        self.assertNotIn("oat flour", rendered)


if __name__ == "__main__":
    unittest.main()
