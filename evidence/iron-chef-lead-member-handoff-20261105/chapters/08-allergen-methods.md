# Chapter 8 — Matched Allergen Evaluation: Method and Freeze Boundaries

**Working draft v0.3 — 2026-11-04**
**Evidence state:** Frozen Open Food Facts allergen taxonomy (23 positive tags, 99.1% coverage), pinned $n=208$ allergen-benchmark protocol v2 dataset (`allergen_benchmark_protocol_v2.json`), scoring rubric, run state (208 observed cells, 0 errors), DOID-FoodOn crosswalk, and 10-role evidence-intake declaration verified under `validate_c3_evidence_intake.py` (103/103 tests passed). The separate campaign Protocol v1 is frozen at SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`; that freeze does not establish execution or acceptance of this allergen-specific comparison.
**Use:** canonical methods account for the allergen evaluation dimension of Contribution C3 (Selective Guardrails). It defines the frozen taxonomy mappings, benchmark case frame, and fail-closed filtering policy; it does not report clinical safety outcomes, which remain strictly out of scope.

## 8.1 Research question and boundary

The proposed study asks whether a tri-state allergen filter changes the constraint-violation profile of substitution candidates produced for the same cases, and whether model outputs can be evaluated transparently when source identity or allergen evidence is unknown. It is not a clinical safety study, a claim that an ingredient is safe for a person, or proof that a recommendation is safe to prepare or consume.

The comparison is paired: preserve the original raw outputs and apply the frozen filter to those same outputs. Do not issue new model calls for the filtered arm. This holds the generated candidates fixed and isolates the filter's transformation as specified; it does not compare the quality of independently generated systems or establish real-world harm reduction.

## 8.2 Case frame and intended arms

The strategy records an existing 208-case, 13-group frame and a Mistral raw-output collection of 208/208 cases. A previous 115-query unmatched classifier/Mistral/Cohere pilot is quarantined; it is not a matched baseline and must not be pooled into this study. Source-group concordance and trace-label rates are separate historical evidence with community-label and taxonomy limitations, not gold safety labels.

The proposed matched generation arms are the pinned model identities listed in the active instrument specification, with requested and returned identities captured separately. The current recorded runner contract names `gpt-4o-2024-08-06` and `gemini-2.5-flash`; actual account eligibility, quotas, credentials, and provider model identity remain dependencies until verified for a separately authorized run. A model substitution, missing arm, or mismatched task does not become a comparable result. One provider attempt per cell is the recorded protocol; any ambiguity or provider failure must be preserved and resolved under the frozen attempt rule, never hidden by an unlogged retry.

The authoritative allergen taxonomy and benchmark mappings have been formally pinned and verified under Lead Decision T350:
1. **Open Food Facts Official Allergen Taxonomy:** Bound `allergens.full.json` (SHA-256 `6fb38bfb0d4669257fc8c66ba7fdf0cf2f8665f7d80af25c3fc6ac5e4bad153f`), enumerating 23 official positive allergen tags covering 99.1% of global product occurrences, with the negative `en:none` sentinel strictly isolated and 8/8 semantic mutations rejected.
2. **Pinned $n=208$ Benchmark Dataset:** Bound `allergen_benchmark_protocol_v2.json` (SHA-256 `0e97c829cccfd6b43bf1dd974db9ae54dd9b561d037ebbca18eb4fbd2bc14fc9`), scoring rubric (`allergen_scoring_rubric_v2.json`, SHA-256 `517c85f8...`), and observed run state (`allergen_benchmark_v2_run_state.json`, SHA-256 `4855239a...`) across all 208 cases with zero execution errors. Both the OFF taxonomy partition counts (Claim R18, SHA-256 `0055df06...`) and the frozen 208-case/13-group allergen protocol specification (Claim R19, SHA-256 `4c4c97f3...`) are formally adjudicated as VERIFIED in Claims Register v15 (`claims-register-r01-r45-v15.csv`).
3. **DOID-FoodOn Crosswalk:** Bound `doid-foodon-relations-v2026-09-30.json` (SHA-256 `ea874bd4b0ce31e34a585cda0cbc02af9e8ef513942720c902553c49b3670691`).
4. **Fail-Closed Filtering Invariants:** Implements `hard_filter_policy: WITHHOLD_CANDIDATE`, `allergen_relation: PRESERVE_UNKNOWN`, and `case_profiles: EXPLICIT_UNKNOWN`.

## 8.3 Output and label schema

For each case, preserve the exact prompt/context, requested and returned model identifiers, provider response ID, raw response bytes/hash, latency, token counts, finish reason, parse status, and each candidate in original order. Every response has explicit parse/format and abstention fields. Review labels are candidate-level and retain uncertainty:

- declared-group violation: `yes`, `no`, or `unknown`;
- trace-policy violation: `yes`, `no`, `unknown`, or `not_applicable`;
- culinary label: `A`, `C`, `N`, or `U` under the recorded rubric;
- reviewer identity code, independent label, rationale/notes, and adjudication record.

Unknown is never converted to no. Missing, unresolved, malformed, or ambiguous identity remains explicit. An empty filtered candidate list is an abstention, not a safe response. Response-level “any violation,” allowed-candidate, withholding, and abstention metrics are derived from candidate-level records using the frozen rule; do not treat candidates as independent cases.

Two independent reviewers and a separately documented adjudication step are proposed; qualified dietetic, clinical, or food-science expertise should be sought, and actual qualifications must be disclosed. The present record does not establish that independent labels or adjudication were collected. An LLM-judge label is not interchangeable with human review or an authoritative allergen fact.

## 8.4 Paired raw/filtered analysis

The filter operates on the recorded raw candidates using the versioned source and identity policy. For every case, retain raw and filtered outputs, removed candidates and reasons, unknown-name withholding, remaining pool size, parse failures, and final abstention state. Report the case denominator, number of cases/candidates with known, unknown, or unresolved evidence, and candidate-level and response-level results separately. Keep all failures and missingness in the denominator under the frozen rule.

The evidence source matters: the recorded policy uses bounded source/taxonomy information and tri-state decisions; it is not a sensitivity/specificity oracle. Community-label concordance and derived trace judgments are not clinical ground truth. If no authoritative product-level evidence resolves an ingredient, the correct output may remain unknown. Do not count an unknown filtered item as a correctly detected violation or a safe negative.

## 8.5 Sampling, intervals, and dependencies

The existing 208-case frame is preserved unchanged. The frozen campaign Protocol v1 retains this $n=208$ allergen route; $n\ge299$ is stretch only and does not authorize expanding or replacing this frame. The older proposal for at least 300 cases is not an adopted change to the frozen cohort. Any separately authorized expansion would require rights-cleared recipes, hidden-source coverage, logged seed, duplicate/template-cluster review, and a new freeze before model outputs. Candidate count does not multiply independent sample size.

Report observed `n`, events `k`, rate, a Wilson 95% interval, and a one-sided exact upper bound for zero events where applicable as distinct quantities; do not call the Wilson interval exact. The strategy's illustrative values—about 1.43% for zero events among 208 independent cases and about 299 cases for a one-sided bound at or below 1%—are conditional binomial arithmetic, not an agreed risk threshold or safety guarantee. State the target population and independence assumptions; case clustering and missing cases limit interpretation.

## 8.6 Frozen campaign rules and allergen-route gates

The campaign Protocol v1 is frozen and governs the campaign's named Freeze-A/Freeze-B procedures, data envelope, resource caps, and result language. It does not retroactively modify the separate `allergen_benchmark_protocol_v2.json`, turn the 208-case route into a completed experiment, or supply missing outputs and human evidence. Conversely, do not reopen a v1 decision by copying a historical PR flag into this chapter when that flag concerns a different procedure.

For the allergen route, the campaign-level decisions that apply here are bounded: retain the existing 208-case scope; treat $n\ge299$ as stretch only; report list-based allergen violations as mechanical policy checks rather than clinical truth; and use the stated interval definitions without calling Wilson exact. Keep the Mistral raw arm as recorded; no further Mistral training is authorized. These rules do not claim matched model outputs, reviewed labels, or reduced risk.

The route-specific prerequisites still open under the strategy's §§9, 14, and 16 are:

- Verify the exact 13-group names and authoritative taxonomy correspondences; the cited reference does not provide those names, so do not invent them.
- Verify current provider/model identity, account capacity, and conservative cost before any separately authorized call; preserve the one-attempt-per-cell rule and reconcile uncertain calls before retry.
- Obtain the applicable institutional classification/authorization and consent before human annotation or participant activity. Independent desk preparation may continue; participant evidence is not inferred from the LLM judge.
- Acquire and validate the missing matched-arm outputs and independent adjudication before reporting a comparison. The existing Mistral collection alone is not a matched result.

The campaign's PR04–PR08 guardrail/calibration rules remain distinct: do not derive a deployable cell from benchmark gold, reuse test labels to choose thresholds, or claim conditional coverage from this allergen study. Raw/filtered violation counts alone establish no safety guarantee or set-valued calibration result.

## 8.7 Claim boundary and reporting template

If the protocol later passes its gates, the permitted report is a bounded comparison for the named case frame, source identities, models, taxonomy version, and adjudication process. It must distinguish model output behavior from the filter's source coverage; report unknowns and all failures; provide denominators and intervals; and state that the study does not establish clinical safety or generalization to other recipes, products, people, or preparation conditions.

At present, the allergen-specific 208-case protocol and Mistral raw collection are partial artifacts. No matched GPT-4o/Gemini outputs, independent adjudication, expanded case set, comparative score, or clinical result is established by this draft. Campaign Protocol v1 is frozen, but that is not a proxy for these missing artifacts or gates; this chapter records method boundaries, not execution evidence.

## 8.8 Frozen intake resolution and taxonomy verification

Under the Task T350 metadata-only intake contract, all ten required allergen component roles were verified without opening or contaminating proprietary source data (`ai/out/c3_evidence_intake/declaration.json`, SHA-256 `a25925a0...`):
- Validator `validate_c3_evidence_intake.py` returned `structural_valid: true`, 0 schema issues, and 0 blocked components, advancing the intake state to `DECLARATION_COMPLETE_REQUIRES_INDEPENDENT_EVIDENCE_REVIEW`.
- Full C3 automated test suite passes 103/103 tests (`pytest ai/tools/test_c3*`).
- **Clinical disclaimer preserved:** Pinned taxonomy matching and tri-state filtering do not constitute a medical or clinical safety guarantee. The system withholds candidates with declared or unknown allergen risks; it does not certify food as allergy-safe for human consumption.

## Provenance map

- Protocol, 13-group inventory boundary, source labels, reviewer fields, and freeze conflicts: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§5–9, 14, and 16.
- Recorded Mistral raw-arm boundary and review-pack state: shared LEAD v195–v196 and v223; these are protocol/readiness summaries, not completed adjudication.
- Earlier frozen-frame history and all status changes: active strategy Appendix X; historical source claims are not live observations.
- Underlying protocol, case, prompt, raw-response, label, and receipt artifacts must be reassembled and hash-verified before publication. Rights and institutional approval remain distinct gates.
