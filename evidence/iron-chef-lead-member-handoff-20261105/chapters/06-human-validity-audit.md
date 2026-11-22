# Chapter 6 — Human Validity Audit: Questions, Measurement, and Holds

**Status correction (2026-11-20):** Two complete, issued-pack-bound reviewer returns and the complete chef/adjudication return now exist for the frozen 150-case sample. HUMAN reports institutional ethics/consent coverage for this activity; no determination document was independently inspected. A private, hash-bound PR10 analysis found exact four-category agreement of 717/867 (82.7%; Wilson 95% CI 80.0–85.1%) and unweighted nominal Cohen’s κ=0.746567 (case-cluster percentile-bootstrap 95% CI 0.700901–0.786907; 2,000/2,000 valid draws, seed `20261021`). Both §2 thresholds pass under the predeclared order-statistic percentile implementation; its lower bound exceeds 0.60 by 0.100901. Protocol v1 does not specify a κ quantile convention; the L36 addendum's linear quantile applies to H1 endpoint bounds, so there is no conflict or protocol change. A separate hash-bound integration receipt records the complete 150-case adjudication output with denominator preservation and `JOINT_CLUSTER_HOLD`; the primary conditional-H1 result remains unaccepted. Parent review accepted C1-G3 narrowly as complete, real, fixed-cohort collected-and-adjudicated output under the existing human-attested activity authority: mission measure **20/25 (80%)**, C1 **3/5**, with fully accepted contributions still **3/5 (C2/C3/C5)**. This is not a primary H1 result, generalization claim, or full C1 acceptance. C1-G4 remains open pending independent acceptance. HUMAN's adopted L36 addendum resolves the `V/U` mapping; reviewer qualification/training/assignment evidence, independent-acceptance evidence, and a design-matched H1 interval/decision remain distinct gaps. The uninspected institutional record is a provenance limitation, not an additional G3 hold.

**Result update (2026-11-22):** A reviewed, hash-bound descriptive conditional H=0 point result is now available for the same fixed audit cohort. It does not resolve the primary H1 uncertainty method, C1-G4, or any broader claim. The result and interpretation are in §6.8; the sanitized shared receipt is [`L36 conditional H=0 point result`](../../../evidence/l36-conditional-h0-descriptive-point-20261122.md). No case-level identifiers, ratings, or private inputs are reproduced here.

**Interval-method qualification:** Protocol v1 does not specify a bootstrap quantile convention for κ. The pre-access analysis receipt explicitly bound an order-statistic percentile interval; its lower bound exceeds 0.60 under that implementation. A same-input linear-interpolation sensitivity shifts the lower bound by −0.00000742, leaving both above 0.60; retain the predeclared interval as primary. This is an implementation qualification, not a protocol amendment. The candidate-level Wilson interval is descriptive and is not cluster-adjusted.

**Working draft v0.4 — 2026-11-07; status reconciled 2026-11-20**

The evidence-state paragraph below preserves its original pre-adjudication snapshot; its “remaining evidence” list is historical and is superseded by the dated status correction above and the current return-status section in §6.5.

**Evidence state:** the Freeze-A frame (10,747 cases), freeze plan, deterministic 150-case sample and executable audit path are recorded under C1-G1/G2. [Protocol v1](../protocols/IRON-CHEF-PROTOCOL-v1.md) (SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`) freezes the prospective estimand, identity, sample, rubric, and analysis rules described below. The historical freeze-time statement that no human judgments had been collected is superseded by the two complete returns and agreement result above. Remaining evidence is separate adjudication, H1 endpoint execution/review, and canonical acceptance—not first-round collection.

**Use:** canonical chapter specifying the validity audit instrument, sampling frame, blinding protocol, agreement metrics, and fail-closed human execution boundaries.

**Receipt pointer:** the reviewed aggregate-only conditional result is in the [shared L36 result receipt](../../../evidence/l36-conditional-h0-descriptive-point-20261122.md). Detailed intake/adjudication pins and private artifacts remain in the source-workspace T343 record and are not bundled here.

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

For the primary estimand, `A` is strict validity; `A+C` is a separately reported sensitivity. `U` is unknown, not a negative. Gold inclusion does not force an `A` label: the blinded judgment may produce a separately reported disputed-reference outcome without changing the recorded benchmark target. The HUMAN-adopted L36 operating interpretation uses the final adjudicated field for case-level `V`: any final `A` among the verified primary top five gives `V=1`; with no `A`, five final `C/N` labels give `V=0`; with no `A`, one or more `U` or unresolved/missing final judgments on an otherwise valid verified slate give `V=unknown`. An invalid or unverifiable slate is an input-integrity failure and makes the affected metric unavailable; it is not converted to `V=0` or `V=unknown`. This operational interpretation resolves the implementation mapping for the reported descriptive result without editing byte-frozen Protocol v1 or deciding H1.

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
- **Operational interpretation (adopted separately; Protocol v1 unchanged):** case-level `V` uses final adjudicated labels under the rule in §6.3: any `A` gives `V=1`; no `A` plus all five `C/N` gives `V=0`; no `A` plus any `U` or unresolved/missing final judgment on an otherwise valid slate gives `V=unknown`. The missing-U rule creates explicit lower/upper descriptive bounds. It does not authorize treating an invalid identity/slate join as unknown, and it does not settle the sampling-uncertainty method for H1. See the [shared L36 operational addendum](../../../evidence/lead-l36-adjudicated-strict-a5-operational-v1-20261106.md).
- The frozen design uses the stated stratified ratio estimator and case-cluster bootstrap (2,000 resamples, seed `20261021`). The deterministic frame, sample and weights are preparation evidence, not human outcomes.

## 6.5 Blinding, reliability, and disagreement

**Current return status (updated 2026-11-20):** both raters returned the complete fixed-sample judgments, and the chef/adjudication return was integrated into a complete fixed-denominator output. The output receipt records `JOINT_CLUSTER_HOLD`, not an accepted H1 decision. Reviewer qualification, assignment, and adjudicator independence remain unverified.

The frozen design requires two independent raters and preserves a separately documented adjudication field; it does not itself verify reviewer qualifications, training exposure, sample assignment, or adjudicator independence. No recruitment, annotation, or participant contact is represented as having occurred here. Apply the institution's required ethics determination, authorization, and consent before any human-subject activity.

The report preserves each rater's original labels, uncertainty and notes; adjudicated values in a separate field; per-rater `U` rates; percent agreement; Cohen's kappa; and case-cluster uncertainty intervals. Under Protocol v1, kappa >= 0.5 is the threshold to proceed; call agreement “reliable” only when the strict-kappa cluster-bootstrap lower 95% bound is >= 0.6, otherwise characterize the audit as exploratory. Report two-sided Wilson intervals for proportions and a one-sided exact bound for zero events, with denominators. If fewer than 20 valid bootstrap draws remain, report the interval unavailable. These rules are frozen prospectively and cannot be selected after observing audit outcomes. Candidate judgments nested within recipes do not create extra independent cases.

Disagreement is a result about the instrument and domain, not a nuisance to erase. Preserve raw judgments and distinguish adjudication from agreement. A high acceptance rate with poor reliability, high `U`, reviewer fatigue, identity confusion, or a disputed gold may make the audit inconclusive. It is not permission to change the rubric after results are seen or to report only a convenient consensus value.

## 6.6 Prior evidence and claim discipline

The related-work record already contains human-evaluated substitution and recipe-adaptation systems. It therefore rules out broad novelty claims such as “first human evaluation of ingredient substitution” or “first human-rated recipe adaptation.” The remaining research question is narrower: whether a frozen, blinded, shared candidate slate and per-candidate recipe-context judgments provide evidence about the specific single-reference Hit@k limitation under seen/unseen pair strata. That distinction is a research question, not an accepted novelty claim; source lineage, case overlap, and exact protocol novelty remain open in the canonical source register.

The existing 200-case Senath validation diagnostic is useful execution evidence and may motivate where to look. It is not a replacement for the human audit, not independent test performance, and not evidence that a model's missed-reference candidates are acceptable. Any later inference must keep retrieval metrics, human-validity judgments, allergen evidence, and human usefulness as separate outcomes.

## 6.7 Protocol resolutions and fail-closed ethics boundary

Protocol v1 freezes the prospective instrument and analysis rules. The frame/sample artifacts alone establish preparation; the later hash-bound returns and integration receipt provide evidence of fixed-sample human execution, while ethics documentation and acceptance remain separate gates. The operational boundary is:

- **PR01 (FROZEN IN V1):** Conditional rescue is primary; joint rescue is secondary. The unseen-GISMo primary arm, strict valid@5, stratified-design interval, and 0.15 lower/upper-bound decision rule are fixed above.
- **PR02 (FROZEN IN V1):** Blind every candidate including the recorded gold; preserve raw A/C/N/U labels and separate adjudication; both-rater `N` on gold is “reference disputed,” not a rewrite of Hit.
- **PR03 (FROZEN IN V1):** `k=5`, allocation 50/50/25/25, verified frame weights `N_h/n_h`, stratified ratio estimator, and 2,000 case-cluster bootstrap draws with seed `20261021`; refuse undersupplied strata.
- **PR09 (FROZEN IN V1):** Pinned GISMo vocabulary exact identity is primary; retain raw strings, and report normalization/alias alternatives only as named sensitivities with unmatched rates.
- **PR10 (FROZEN IN V1):** Apply the distinct proceed and “reliable agreement” kappa gates and the interval/missing-draw rules in §6.5; do not conflate them.
- **PR13 (HUMAN-SUBJECT GATE):** Apply institutional ethics determination and consent requirements before recruitment, contact, or data collection. No human judgments are fabricated or inferred from model/LLM-judge outputs. C1-G3 is accepted narrowly as complete output; C1-G4 remains open pending independent checks.

## 6.8 Conditional H=0 descriptive result and interpretation (2026-11-22)

The frozen audit assigned 100 cases in the two unseen GISMo-identity strata (50 per stratum). Under the exact primary GISMo top-five identity rule, the recorded gold appeared in the top five for 3 cases; the remaining 97 cases form the observed H=0 miss cohort. All 100 assigned cases remain in the cohort-flow record. The conditional result below uses only the 97 verified H=0 cases in its numerator and denominator; the 3 hits are not silently dropped from the study flow or relabeled as misses.

Applying the adopted case-level rule in §6.3 to the final adjudications, H=0 cases were `V=1/V=0/unknown` **38/10/1** in unseen-profiled and **34/12/2** in unseen-unprofiled. Thus, among the 97 observed gold misses, 72 had at least one strict-`A` candidate in the primary top five, 22 had five final `C/N` judgments, and 3 remained unknown. This is evidence that, within this frozen audit sample, a single recorded-reference miss did not imply that every retrieved alternative was judged invalid under the stated rubric. It is not evidence that every miss is valid or that the candidates are safe or useful in practice.

The fixed frame weights are `176/50 = 3.52` for unseen-profiled and `3,639/50 = 72.78` for unseen-unprofiled. Applied to the verified H=0 cases, they produce a weighted eligible denominator of **3,665.92**. The strict-success lower numerator is **2,608.28**; the upper numerator, which additionally treats the three H=0 unknown cases as potentially strict-valid, is **2,757.36**. The resulting weighted conditional descriptive bounds are **0.7114939769553073–0.7521604399441341** (reported as **71.1494%–75.2160%**). They are partial-identification bounds caused by unknown adjudications: the lower endpoint counts only observed `V=1`, while the upper endpoint counts `V=1` plus unknown. The 4.0666 percentage-point width is not a standard error, confidence interval, or measure of sampling uncertainty.

Interpretation is deliberately conditional and narrow. The estimate describes the weighted, fixed audit sample's strict-validity outcomes **among cases whose exact recorded GISMo reference was absent from the primary GISMo top five**, under the frozen two-stratum allocation and adopted `V/U` rule. It is not overall top-five accuracy, an all-100-case validity rate, a result for the full unseen population without sampling uncertainty, or a comparison establishing that GISMo or another method is superior. It does not establish the probability that an arbitrary missed target has a usable substitute outside this audited slate, nor live cooking usefulness, dietary/allergen compatibility, medical or cultural safety, or user acceptance. The conditioning on observed `H=0`, the single fixed sample, exact identity rule, candidate slate, adjudication procedure, and unresolved sampling-uncertainty method all constrain transport beyond these cases.

This point result does **not** satisfy Protocol v1's primary H1 decision rule: no design-matched stratified 95% uncertainty interval was accepted, so no support/refute/inconclusive H1 decision is made. The result also does not close C1-G4 independent acceptance or change the public claims register, milestone measure, protocol, or overall C1 status. The source-bound sanitized aggregate receipt and its digest-domain distinctions are in the [shared handoff](../../../evidence/l36-conditional-h0-descriptive-point-20261122.md); protected inputs, case IDs, raw judgments, and per-case outputs remain private.

## 6.9 Evidence still needed to advance this chapter

The design, frame, deterministic sample, weights, rubric, identity rule, executable schema/validator, two complete reviewer returns, agreement output, chef/adjudication return, denominator-preserving integration output, and descriptive conditional H=0 point result are recorded. Remaining acceptance evidence includes reviewer qualification/training/assignment evidence, blinded-view/handbook execution evidence, independent checks for C1-G4, and a design-matched primary H1 interval/decision with required review. Any future human-subject activity remains subject to the applicable institution-specific determination/consent gate. The integration receipt records `JOINT_CLUSTER_HOLD`. Any package must fail closed on altered inputs, missing/duplicated cases, leakage, and post-freeze protocol changes.

Human-audit outputs and the denominator-preserving adjudication integration receipt support narrow C1-G3 complete-output acceptance. The added conditional point result is a bounded descriptive Lead interpretation, not independent C1-G4 acceptance or a primary H1 decision. This chapter claims no protocol-independent confirmation, novelty, generalization, safety, or overall completion; the fixed denominator and stated limitations remain binding.

## Provenance map

- Canonical hypotheses and the pre-v1 design rationale: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§2, 4–6, 15–17, 21, and Appendix A; the prospective operating rules are Protocol v1.
- Candidate identity, label separation, failure handling, and audit-data boundary: [`04-instrument-and-identity-contract.md`](04-instrument-and-identity-contract.md).
- Frozen two-rater/case-cluster acceptance requirement and C1/C4 boundaries: Protocol v1 §§2–3 and canonical §§3, 17.
- HUMAN-adopted case-level `V/U` operating rule and conditional-point method boundary: the [shared L36 operational addendum](../../../evidence/lead-l36-adjudicated-strict-a5-operational-v1-20261106.md); source-bound aggregate hashes and reviewed result: the [2026-11-22 sanitized receipt](../../../evidence/l36-conditional-h0-descriptive-point-20261122.md).
- Senath inference metrics are recorded in canonical §1.2 and LEAD v376; the HTML display grouping correction is documented in LEAD v423–v424. Neither source supplies human-validity labels.
- Related-work and source-lineage statuses remain in canonical §4 / Appendix C and Chapter 2; this chapter does not independently re-review external literature.

## Claims register and defense dossier alignment

- **Active Claims Register v20:** Bound to `claims-register-r01-r45-v20.csv` (SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`) and receipt v20 (`5925cca64442ad0c21b2ca694a89f7a1dd2b23419bbd79f36fae0f341f33c2b5`).
- **Claim R32 Adjudication & PR13 Boundary:**
  - Two complete, issued-pack-bound reviewer returns for the 150-case Freeze-A sample show exact 4-category agreement of 717/867 (82.7%; Wilson 95% CI 80.0–85.1%) and nominal $\kappa = 0.746567$ (case-cluster percentile bootstrap 95% CI 0.700901–0.786907, 2,000 draws).
  - Historical (superseded 2026-11-20): the 150-unit conflict packet was sealed with an independent chef for final adjudication; the return is now recorded in protected receipts, with integration status `JOINT_CLUSTER_HOLD`.
  - Epistemic integrity: Claim R32 remains strictly UNVERIFIED in the public register until the returned independent chef adjudication packet is received and verified with in-repo byte artifacts.
  - Fail-closed ethics gate (PR13): Human validity cannot be imputed from LLM judges, synthetic agents, or automated proxies.

- **Defense Rehearsal Pack:** Bound to `defense-rehearsal-pack-v1.json` (SHA-256 `7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2`), specifically Rubric C3 (Human Validity Protocol) and Challenge Q01 (Agreement vs Adjudication distinction).

**Provenance update (2026-11-20):** The chef return is no longer merely sealed/pending: its existence and hash-bound integration are documented in protected local receipts summarized in T343. Parent review accepted C1-G3 narrowly as complete output; this does not alter the public claim-register state, establish C1-G4 independence, or resolve `JOINT_CLUSTER_HOLD`/H1.
