# L36 operational addendum: adjudicated strict-A@5 with unknown bounds

**Specification ID:** `iron-chef-l36-case-rule-v1`
**Date/provenance:** 2026-11-06; adopted by the HUMAN's explicit response in this session.
**State:** Versioned operational clarification for preparation; no Protocol v1 byte change, human labels, score, milestone, or C1 acceptance.

## Adopted scope

The HUMAN adopts the primary case-level rule: a case is a strict-A@5 success when at least one of the five candidates in the frozen primary GISMo-arm slate has final adjudicated label `A`. Unresolved adjudicated judgments remain unknown rather than being counted as failures. The HUMAN's description of this as “done and usable” is not a finding that the study, system, or model is complete, good, safe, or useful; no result exists yet.

This resolves only the operational meaning of `V` and the treatment of `U` for L36. Protocol v1 remains byte-frozen at SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`. The primary estimand, unseen-GISMo population, primary arm, `k=5`, 150-case allocation, frame weights, 2,000 case-cluster resamples, seed `20261021`, 0.15 decision boundary, gold blinding, and PR10 agreement gates are unchanged.

## Case-level observation

For every sampled `H=0` case, use the five candidate-level **final adjudicated** labels from the primary GISMo-arm top-five slate; raw rater labels and the adjudication record remain separate and immutable.

| Final adjudicated labels on frozen top five | Case value |
|---|---|
| At least one `A` | `V=1`, even if another candidate is `U` |
| No `A`; all five are `C` or `N` | `V=0` |
| No `A`; one or more are `U`, unresolved, or lack a final adjudication | `V=unknown` |

`U` is not negative and is never silently imputed as observed failure. If the frozen candidate slate is missing, duplicated, altered, or cannot be joined unambiguously, this is an input-integrity failure: retain the case and denominator, record its failure, and do not compute a primary estimate from a malformed slate. A missing/withdrawn human record is handled under the applicable consent/ethics rule; required deletion takes precedence, with only an allowed non-identifying flow count retained.

## Full-denominator partial-identification estimator

Let `h` range only over the two frozen unseen-identity strata: unseen-profiled and unseen-unprofiled. Let `N_h` be each verified frame size, `n_h` its frozen sample size (50 each), and `W_h=N_h/n_h`. Define case endpoints `L_i=1[V_i=1]` and `U_i=1[V_i∈{1,unknown}]`. The fixed-denominator point bounds for `P(V=1 | H=0)` are:

```text
p_lower = (Σ_h W_h Σ_{i∈sample(h)} L_i) / (Σ_h W_h n_h)
p_upper = (Σ_h W_h Σ_{i∈sample(h)} U_i) / (Σ_h W_h n_h)
```

The denominator is the weighted unseen-frame size and includes every frozen `H=0` sample case, regardless of `A/C/N/U`, missing adjudication, or method failure, subject only to an ethics-required withdrawal/deletion rule. Do not use complete-case analysis, reweight after seeing labels, move cases between strata, or substitute the seen strata. Report unknown-case counts and weighted unknown mass alongside both endpoint estimates. A+C is a separately named sensitivity and cannot replace the strict-A primary.

## Bootstrap and decision rule

Use the frozen **2,000-draw case-cluster bootstrap**, seed `20261021`, on the two unseen strata only. Resample independent source-recipe clusters with replacement within each frozen unseen stratum, drawing the observed number of distinct clusters in that stratum; keep every sampled case and all nested candidate/rater/adjudication records for a selected cluster together. In each replicate, recompute the stratum-specific endpoint ratios over the replicated case rows and combine them using the unchanged frame weights `N_h` (equivalently, the weighted ratio above). Never resample candidate or rater rows as independent units. If a source-recipe cluster crosses the two unseen strata, the strata cannot be resampled independently: record the affected cluster structure and hold the interval until a joint-cluster implementation is specified and reviewed; do not split or duplicate the cluster to force a result.

For each endpoint, report the percentile 95% interval using the 2.5th and 97.5th percentiles of valid bootstrap estimates, with linear interpolation (`numpy.quantile(method="linear")` or an exactly equivalent implementation). If fewer than 20 valid draws remain, report the interval unavailable under Protocol v1. Support H1 only when the lower 95% bound for `p_lower` is at least `0.15`; refute only when the upper 95% bound for `p_upper` is below `0.15`; otherwise report inconclusive. Missing intervals, inadequate clusters, or a failed integrity gate cannot become support or refutation.

This specifies the encoding of the already-frozen stratified estimator and interval decision; it does not select a new scientific population, arm, cutoff, allocation, or identity rule. A methods reviewer may identify a genuine incompatibility with the frozen case-cluster procedure; if so, preserve this version and document the exact incompatibility rather than editing Protocol v1 or improvising a different resampling unit after outcomes.

## Evidence ceiling and next owners

This rule authorizes no scoring or human activity by itself. Two independent qualified raters, raw labels, separate adjudication provenance, PR13 institutional determination/consent, rights, pre-access records, valid case/candidate coverage, and resource gates remain required before the analysis. The HUMAN confirmed on 2026-11-06 that applicable permission correspondence and human-evaluation consent evidence are held internally, and grants standing authority to read/use already-accessible project artifacts for this existing LEAD mission. JARVIS/ARCHITECT must not access or expose those records or search confidential stores the HUMAN excluded. This is a HUMAN attestation only, not agent-inspected documentation or independently verified institutional determination, participant enrollment, labels, or evaluation. Do not request copies or repeat general permission questions. This authority does not override explicit artifact-specific restrictions or extend to external publication, redistribution, spending, destructive operations, rights-transfer, or actual-participant requirements. The current evidence contains no L36 human labels, adjudication, agreement statistics, primary estimate, interval, H1 decision, or acceptance credit.

M-B's scope remains sample/frame/blind-pack work; M-I owns estimator implementation and computation when inputs pass. Their next implementation must use the named case rule and fixed denominator above without changing source identity, sample allocation, or raw labels. The schema crosswalk for the distinct Vietnamese pilot is in [`iron-chef-lead-member-handoff-20261105/l40-vietnamese-pilot-contract-20261106.md`](iron-chef-lead-member-handoff-20261105/l40-vietnamese-pilot-contract-20261106.md); it does not share this 150-case method or its weights.
