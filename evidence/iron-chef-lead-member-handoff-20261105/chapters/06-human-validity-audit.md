# Chapter 6 — Human Validity Audit: Questions, Measurement, and Holds

**Working draft v0.3 — 2026-11-04**

**Evidence state:** the Freeze-A frame (10,747 cases), freeze plan, deterministic 150-case sample and executable audit path are recorded under C1-G1/G2. [Protocol v1](../protocols/IRON-CHEF-PROTOCOL-v1.md) (SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`) now freezes the prospective estimand, identity, sample, rubric, and analysis rules described below. No human judgments have been collected; the human audit and its acceptance evidence remain open under the applicable ethics and consent process.

**Use:** canonical chapter specifying the validity audit instrument, sampling frame, blinding protocol, agreement metrics, and fail-closed human execution boundaries.

**Chapter-slot note:** canonical §21 assigns Chapter 6 to the validity audit. A similarly numbered table in Appendix Y is preserved verbatim inside the explicitly superseded v3 archive; it is provenance, not a competing active chapter map. Use §21 for current chapter placement.

## 6.1 Question and scope

This chapter asks whether a single recorded reference ingredient is an adequate proxy for culinary validity among a system's ranked substitutes, especially when the reference is absent from the candidate list and when the source–target pair is unseen in the training frame. The human audit is intended to measure candidate-level judgments in recipe context. It is not a user-preference study, a safety assessment, a clinical evaluation, or evidence that a recommendation will be useful in a live product.

The primary motivation is an identification limit: failure to retrieve one recorded reference does not, by itself, establish that every retrieved candidate is invalid. Conversely, a fluent or plausible string match does not establish that a substitute performs the needed culinary function. These are hypotheses motivating measurement, not findings. The prospective audit rules are frozen in Protocol v1; their adoption is not evidence that the human audit has run or that H1 is supported.

The current frozen 200-case Senath comparison does not answer this question. It reports model-output Hit@k under GISMo graph identity and a separately named raw-string sensitivity. Its outputs have no independent human-validity labels. The low observed Hit@5 values—11.5% for top-20 generation and 7.5% for top-50 likelihood reranking under the primary GISMo identity—describe recovery of the recorded target, not the proportion of human-acceptable alternatives. They cannot be relabeled as invalidity or “rescue.” See the canonical result boundary in [§1.2](../../academic_vision_and_strategy.md) and the bounded inference draft in [Chapter 5](05-senath-clean-room-inference.md).

## 6.2 Unit of judgment and candidate identity

The audit unit is a candidate considered for a particular source-to-target substitution in a specific recipe context. Case identity, recipe identity, split, row index, source/target strings, and candidate provenance remain attached through sampling, review, and analysis. Under Protocol v1, the shared slate contains each eligible arm's top five candidates and the recorded reference where defined. The frozen identity and deduplication rules preserve raw forms and originating arm/rank associations.

Candidate identity is a measurement concern, not cosmetic preprocessing. Protocol v1 uses the pinned GISMo vocabulary with exact identity matching; raw generated strings remain available, and Senath normalization or reviewed aliases are separately named sensitivity analyses. Unmatched or ambiguous mappings are not silently promoted to a unique GISMo identity. Reviewers judge the presented candidate without scores, rank, model, arm, or gold identity; the sealed analysis key retains its provenance. An unmatched candidate remains visible for human review and is flagged in arm-level reporting; it is not discarded from the audit denominator.

## 6.3 Frozen rubric and gold-label treatment

Protocol v1 freezes four candidate-level labels:

| Code | Frozen meaning |
|---|---|
| A | Acceptable as-is under ordinary quantity logic and recipe context. |
| C | Acceptable only with a bounded quantity, technique, or companion-ingredient change while preserving the dish's intended function. |
| N | Not acceptable under the case because it fails function or identity, is nonsensical or too vague to act on, or is effectively the unchanged target. |
| U | The reviewer cannot judge from the supplied evidence; this is unknown, not a negative label. |

The reviewer view includes the recipe title/context, ingredients, source/target role, and one candidate, but withholds scores, rank, model name, arm, and recorded-gold identity. Reviewers label candidates independently rather than selecting a single winner. If cooking instructions are needed to make a judgment, that need is recorded; title-and-ingredient evidence is not silently treated as sufficient. The recorded gold is blinded and judged like every other candidate. Raw A/C/N/U judgments are immutable; adjudication, when applicable, is stored separately and does not overwrite them. If both raters judge the gold `N`, report “reference disputed” separately and retain the benchmark Hit result unchanged.

For the primary estimand, `A` is strict validity; `A+C` is a separately reported sensitivity. `U` is unknown, not a negative. Gold inclusion does not force an `A` label: the blinded judgment may produce a separately reported disputed-reference outcome without changing the recorded benchmark target. This resolves the earlier protocol ambiguity prospectively under v1; it does not revise historical labels or results.

Allergen or other dietary-constraint judgments are separate fields and evidence tracks. Culinary acceptability does not certify constraint compatibility or safety. Unknown evidence remains unknown; no negative source flag becomes a safety certificate. This chapter makes no claim that its proposed culinary rubric covers all relevant cultural, dietary, religious, medical, or accessibility contexts.

## 6.4 Frozen Freeze-A sampling frame and mathematical estimand

Under Contribution C1 (C1-G1 and C1-G2 earned), the sampling frame and estimand were formally resolved, built, and verified:
- **Canonical 10,747-case sampling frame (`c1_freeze_a_real_frame.jsonl`, SHA-256 `e5d065228c20...`):** Reconstructed from pinned Recipe1M test split data, authoritative GISMo vocabulary mapping (`vocab_ingrs.pkl`), and FooDB compound profile presence. The public frame exposes only `case_id`, `recipe_id`, and `stratum`; zero candidate strings or gold labels leak into the public frame.
- **Stratum allocation & non-undersupply arithmetic:** Population counts $N_h$ across all four strata substantially exceed sample allocations $n_h$ ($N_h \ge n_h$), defining mathematically exact verified frame weights $W_h = N_h / n_h$:
  - `unseen_profiled`: $N_h = 176, n_h = 50, W_h = 3.52$
  - `unseen_unprofiled`: $N_h = 3,639, n_h = 50, W_h = 72.78$
  - `seen_profiled`: $N_h = 986, n_h = 25, W_h = 39.44$
  - `seen_unprofiled`: $N_h = 5,946, n_h = 25, W_h = 237.84$
  - Total population $N = 10,747$; sample $n = 150$.
- **Frozen sample draw:** Deterministic sampler `c1_freeze_a_sampler.py` bound under seed `20261021` and Protocol SHA-256 `b2952599...` drew `sample.jsonl` (SHA-256 `e195126b...`) and generated `sample_manifest.json` (SHA-256 `8d5d3756...`). Dual clean rebuilds confirmed 100.0% bit-identical hash parity.
- **Frozen primary estimand (PR01):** Conditional rescue $P(V=1 \mid H=0)$ on the unseen GISMo-identity stratum for the primary GISMo arm under strict valid@5 ($k=5$, label `A`). Joint rescue $P(V=1, H=0)$ and lenient valid@5 (`A+C`) are secondary sensitivity estimands. Support H1 only when the stratified-design 95% lower bound is at least 0.15; refute only when its upper bound is below 0.15; otherwise report inconclusive.
- The frozen design uses the stated stratified ratio estimator and case-cluster bootstrap (2,000 resamples, seed `20261021`). The deterministic frame, sample and weights are preparation evidence, not human outcomes.

## 6.5 Blinding, reliability, and disagreement

The frozen design requires two independent raters and preserves a separately documented adjudication field; it does not itself verify reviewer qualifications, training exposure, sample assignment, or adjudicator independence. No recruitment, annotation, or participant contact is represented as having occurred here. Apply the institution's required ethics determination, authorization, and consent before any human-subject activity.

The report preserves each rater's original labels, uncertainty and notes; adjudicated values in a separate field; per-rater `U` rates; percent agreement; Cohen's kappa; and case-cluster uncertainty intervals. Under Protocol v1, kappa >= 0.5 is the threshold to proceed; call agreement “reliable” only when the strict-kappa cluster-bootstrap lower 95% bound is >= 0.6, otherwise characterize the audit as exploratory. Report two-sided Wilson intervals for proportions and a one-sided exact bound for zero events, with denominators. If fewer than 20 valid bootstrap draws remain, report the interval unavailable. These rules are frozen prospectively and cannot be selected after observing audit outcomes. Candidate judgments nested within recipes do not create extra independent cases.

Disagreement is a result about the instrument and domain, not a nuisance to erase. Preserve raw judgments and distinguish adjudication from agreement. A high acceptance rate with poor reliability, high `U`, reviewer fatigue, identity confusion, or a disputed gold may make the audit inconclusive. It is not permission to change the rubric after results are seen or to report only a convenient consensus value.

## 6.6 Prior evidence and claim discipline

The related-work record already contains human-evaluated substitution and recipe-adaptation systems. It therefore rules out broad novelty claims such as “first human evaluation of ingredient substitution” or “first human-rated recipe adaptation.” The remaining research question is narrower: whether a frozen, blinded, shared candidate slate and per-candidate recipe-context judgments provide evidence about the specific single-reference Hit@k limitation under seen/unseen pair strata. That distinction is a research question, not an accepted novelty claim; source lineage, case overlap, and exact protocol novelty remain open in the canonical source register.

The existing 200-case Senath validation diagnostic is useful execution evidence and may motivate where to look. It is not a replacement for the human audit, not independent test performance, and not evidence that a model's missed-reference candidates are acceptable. Any later inference must keep retrieval metrics, human-validity judgments, allergen evidence, and human usefulness as separate outcomes.

## 6.7 Protocol resolutions and fail-closed ethics boundary

Protocol v1 freezes the prospective instrument and analysis rules; the completed frame and sample artifacts establish preparation, not human-subject execution. The operational boundary is:

- **PR01 (FROZEN IN V1):** Conditional rescue is primary; joint rescue is secondary. The unseen-GISMo primary arm, strict valid@5, stratified-design interval, and 0.15 lower/upper-bound decision rule are fixed above.
- **PR02 (FROZEN IN V1):** Blind every candidate including the recorded gold; preserve raw A/C/N/U labels and separate adjudication; both-rater `N` on gold is “reference disputed,” not a rewrite of Hit.
- **PR03 (FROZEN IN V1):** `k=5`, allocation 50/50/25/25, verified frame weights `N_h/n_h`, stratified ratio estimator, and 2,000 case-cluster bootstrap draws with seed `20261021`; refuse undersupplied strata.
- **PR09 (FROZEN IN V1):** Pinned GISMo vocabulary exact identity is primary; retain raw strings, and report normalization/alias alternatives only as named sensitivities with unmatched rates.
- **PR10 (FROZEN IN V1):** Apply the distinct proceed and “reliable agreement” kappa gates and the interval/missing-draw rules in §6.5; do not conflate them.
- **PR13 (HUMAN-SUBJECT GATE):** Apply institutional ethics determination and consent requirements before recruitment, contact, or data collection. No human judgments are fabricated or inferred from model/LLM-judge outputs. C1-G3/G4 remain open until their evidence criteria are actually met.

## 6.8 Evidence needed to advance this chapter

The design, frame, deterministic sample, weights, rubric, identity rule, and executable schema/validator are already frozen or prepared under Protocol v1 and C1-G1/G2. Remaining evidence includes the institution-specific ethics determination and required consent/authorization, qualified raters and assignment records, a tested blinded reviewer view and handbook, complete case/candidate coverage, preserved raw judgments and missingness, separate adjudication provenance, and the pre-specified agreement and H1 analyses. Any package must fail closed on altered inputs, missing/duplicated cases, leakage, and post-freeze protocol changes.

The actual human audit and independent acceptance remain open. This chapter records a frozen prospective method and prepared tooling, not human-validity results, protocol-independent confirmation, novelty, or completion. Acceptance still requires the actual audit evidence, complete denominator, raw judgments, adjudication provenance, reliability/disagreement diagnostics, and stated limitations.

## Provenance map

- Canonical hypotheses and the pre-v1 design rationale: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§2, 4–6, 15–17, 21, and Appendix A; the prospective operating rules are Protocol v1.
- Candidate identity, label separation, failure handling, and audit-data boundary: [`04-instrument-and-identity-contract.md`](04-instrument-and-identity-contract.md).
- Frozen two-rater/case-cluster acceptance requirement and C1/C4 boundaries: Protocol v1 §§2–3 and canonical §§3, 17.
- Senath inference metrics are recorded in canonical §1.2 and LEAD v376; the HTML display grouping correction is documented in LEAD v423–v424. Neither source supplies human-validity labels.
- Related-work and source-lineage statuses remain in canonical §4 / Appendix C and Chapter 2; this chapter does not independently re-review external literature.
