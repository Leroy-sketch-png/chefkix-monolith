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

## Supplemental L36 correction and method disposition — 2026-11-11 (v1.1)

This section supersedes only the point-estimator denominator wording above. It does not rewrite the 2026-11-06 record, alter frozen Protocol v1, or authorize outcome recomputation or H1 acceptance.

### Estimand-correct point bounds

Protocol v1's primary is conditional rescue `P(V=1 | H=0)`, not an unconditional `V` rate across every unseen case. The checked source-free aggregate confirms that the assigned unseen sample is not H=0-only: 97 of 100 cases are gold misses (`H=0`), while 3 are gold hits. Retain all 100 assigned cases in sample-flow and missingness reporting; exclude `H=1` only from the conditional target ratio.

With frozen unseen strata `h`, fixed `W_h=N_h/n_h`, `A_i=I(H_i=0)`, `L_i=I(V_i=1)`, and `U_i=I(V_i∈{1,unknown})`, the conditional partial-identification endpoints are:

```text
lower = Σ_h W_h Σ_{i∈sample(h)} A_i L_i / Σ_h W_h Σ_{i∈sample(h)} A_i
upper = Σ_h W_h Σ_{i∈sample(h)} A_i U_i / Σ_h W_h Σ_{i∈sample(h)} A_i
```

The denominator is the weighted eligible H=0 mass, not the full unseen frame size. The existing all-100 bounds `[0.702768, 0.741845]` remain a dated unconditional descriptive output only; they are not the frozen conditional H1 estimate, interval, or support evidence. No corrected bound has been computed in this supplement. M-I may compute a clearly labeled corrected descriptive point bound once its H input is carried through the implementation; this does not wait on interval resolution. The primary interval/H1 decision remains held until the uncertainty method is accepted.

### Joint-cluster uncertainty remains held

The actual frozen sampler (`ai/tools/c1_freeze_a_sampler.py`, SHA-256 `6062ce45b94a22c9f7c64b432571c5e71b86d965a96e60770319f8d048090cfb`) draws the fixed case allocation independently within each stratum without replacement; recipe clusters are dependence blocks observed after case sampling, not sampled PSUs. The Protocol v1 hash remains exactly `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.

Two source-free independent methods reviews examined (a) pooled union-cluster resampling with one shared cluster multiplicity and (b) that same multiplicity with stratum case-mass calibration. Both were **PASS WITH CONDITIONS**, not accepted design-based procedures. Reviewers found the conditional ratio formula coherent but neither candidate is shown to reproduce uncertainty for this fixed-stratum, without-replacement case-sampling design; calibration alone does not establish design-based coverage, finite-population correction, or adequacy under unequal cluster sizes/sparse eligible denominators. The percentile intervals for lower and upper endpoints would also be marginal summaries, not automatically a simultaneous 95% confidence region; the 20-valid-draw floor is an availability rule, not a coverage guarantee. The final delta review confirmed the distinction and corrected one overstatement: the sampler does not establish the assumptions needed to call either candidate a design-based interval; it does not prove that a superpopulation model is impossible.

Accordingly retain `JOINT_CLUSTER_HOLD`; no corrected conditional point result, percentile interval, H1 support/refutation decision, or C1/milestone credit is claimed here. The point estimator can be corrected and computed separately from an interval; interval uncertainty is not a prerequisite for producing a clearly labeled descriptive point-bound result. Keep Protocol v1 unchanged. A model-based joint-cluster interval may be prepared under the existing bounded local authority only as an explicitly exploratory sensitivity, with its assumptions and claim ceiling stated; it cannot satisfy the frozen primary 0.15 H1 decision rule. A design-matched primary interval must be specified and independently reviewed against the actual sample design; if that specification changes a frozen protocol choice, use the protocol amendment process. Do not retrofit a confirmatory method after seeing adjudication outcomes.

### Exact M-I implementation handoff

1. Preserve/derive the frozen primary-arm `H` indicator from the hash-bound case manifest and recorded-gold/top-five identity linkage; do not assume all assigned unseen cases are H=0.
2. Correct L36 lower/upper point bounds to the H=0 weighted ratios above. Preserve all 100 assigned unseen cases in flow/missingness counts and report the H=0 denominator separately. Do not report a corrected value until the inputs and method are reviewed.
3. Historical state at this handoff: the C1 integrator and L36 evaluator carried no `H` field through their case types and emitted all-unseen bounds. The later source-free implementation checkpoint below adds an optional exact-cohort `h0_by_case` input and a separate v2 descriptive endpoint. It does not authenticate the caller's source receipt or supply the real H map; verify those before any real-data invocation.
4. Do not substitute L37 (`tools/research/l37/strata_cluster_comparator.py`, SHA-256 `965460f15b199932fe34123a32cae01d9237be3827bd8fb626f92e2d81c59dd9`)'s `evaluate_conditional_rescue`: it filters to H=0, then uses an unweighted case ratio and a bootstrap over the filtered cohort. That is not the frozen L36 weighted ratio over the assigned unseen stratified sample and cannot supply its H1 interval. The inspected sampler is `ai/tools/c1_freeze_a_sampler.py`, SHA-256 `6062ce45b94a22c9f7c64b432571c5e71b86d965a96e60770319f8d048090cfb`.
5. Keep bootstrap code/interval computation disabled for the primary decision until a reviewed design-matched procedure is adopted. Do not split a cross-stratum recipe cluster, duplicate it, drop H=1 cases from the assigned-flow denominator, treat candidate/rater rows as independent, or silently label an exploratory interval as design-based.

No code, tests, labels, source bytes, resampling, outcome computation, or protocol edits occurred in this supplement. External methodological background reviewed: Rao & Wu (1988), “Resampling Inference with Complex Survey Data,” *JASA* 83(401), 231–241, [doi:10.1080/01621459.1988.10478591](https://doi.org/10.1080/01621459.1988.10478591). The classic stratified procedure does not itself validate a shared post-sampling cluster crossing strata; therefore it is not cited as approval of either candidate here.

## Supplemental implementation checkpoint — 2026-11-11 (source-free interface only)

The conditional point-estimator path is now implemented as an explicit optional interface in the existing L36/C1 tooling. `compute_conditional_partial_bounds` requires an H=0 boolean for exactly all 100 assigned unseen cases, validates the frozen 50/50 stratum allocation and SHA-256 pin shapes, applies the fixed `N_h/n_h` case weights to both eligible numerator and denominator, and returns the assigned/H=0 counts plus a digest of the private membership map. A zero eligible denominator returns unavailable (`None`) endpoints, not imputed zeros. C1 receipt v2 names the corrected conditional result `DESCRIPTIVE_POINT_BOUNDS_ONLY_NO_INTERVAL_OR_H1_DECISION` and labels the former all-unseen output as unconditional descriptive bounds. Existing v1 receipts remain unchanged.

This is preparation, not an empirical result: no source-backed H map, gold identities, adjudication outcomes, or labels were loaded for this change; no corrected point bounds were calculated. The API records caller-supplied source/receipt hashes and computes a membership-map digest, but it does not authenticate the source receipt; the caller must validate the source/receipt binding before invoking it. Therefore the function is not yet an accepted real-data integration, C1 acceptance, H1 evidence, or interval method. The primary `JOINT_CLUSTER_HOLD` remains.

The v2 receipt also includes `membership_provenance_status=CALLER_VERIFICATION_REQUIRED`, added after independent review flagged that digest shape validation is not source authentication. Review found the weighted ratio and failure boundaries correct; no other finding. This is one bounded code review, not scientific acceptance of an actual H map or interval.

Changed files and SHA-256: `tools/research/l36/audit_estimand_evaluator.py` `ee6c52b30cc6a0d91b52ae99da18bbe4a3f34f8039bc620287e5a1318e818163`; `tools/research/c1/c1_adjudication_integrator.py` `c1a2f6a2c5f83d2487690681d8cb2b8eca86ec93b0877560c18ce316f261837f`; `tools/research/c1/test_c1_adjudication_integrator.py` `289a55b3e9ef5b92c24b321e02334457d65abbb224d632e2fae40cf4876deae2`. Existing L36+C1 test modules passed 28/28. A separate generated-data check verified the weighted formula with a 100-case/30-H0 cohort, zero-H0 unavailability, and exact-cohort fail-closed behavior. No real-data execution, interval, H1 decision, or milestone credit follows.

## Supplemental independent call-path review — 2026-11-11 (read-only)

Reviewed the conditional-estimator change at monolith commit `f6abef86b5b2d235bd84cce4fb226875b8c3f9fd` against the adopted `325eebe` method handoff. **PASS WITH CONDITIONS for the source-free point-estimator interface only.** In `compute_conditional_partial_bounds`, the endpoint math matches the frozen conditional target: for each unseen stratum it uses the fixed `W_h=N_h/n_h`, multiplies both endpoint numerators and the eligible denominator by `I(H=0)`, and divides by the weighted H=0 mass. It requires explicit booleans for exactly the 100 unseen case IDs, enforces the 50/50 unseen allocation, retains assigned-unseen and H=0 counts, rejects malformed H=0 case values, and returns unavailable endpoints when the weighted H=0 denominator is zero. The receipt labels the result descriptive-only and explicitly disclaims an interval/H1 decision. This does not authenticate the map or its provenance: SHA-256 format checks plus caller-supplied pins are not source verification, as the `CALLER_VERIFICATION_REQUIRED` field correctly states.

**Material execution gap:** the optional inputs stop at `evaluate_integrated_audit`. `evaluate_full_audit` invokes that function without forwarding an H map or provenance, and `main`/`parse_args` expose no membership-map or receipt inputs. The normal CLI therefore continues to produce the legacy unconditional receipt; it cannot emit the corrected conditional endpoint. Tracked-artifact and existing handoff text inspection found no source-backed H=0 map, verified source/receipt binding, or corrected real-data receipt. This means “not evidenced in the shared repository,” not proof that a private artifact does not exist. No raw gold, label, return, or case-level data was opened in this review.

**Exact M-I completion handoff:** use the frozen case manifest and recorded-gold/top-five identity linkage to supply one `H=0` boolean for each of the exact 100 unseen case IDs; verify the source artifact and its receipt against their actual bytes and binding before invoking the conditional path. Either connect those verified inputs through `evaluate_full_audit` and the CLI or document an equivalent reproducible invocation. The saved corrected receipt must bind the case-manifest, H-source/receipt, and membership-map digests; state the 100 assigned cases and each 50-case stratum allocation; report stratum H=0 counts, weighted H=0 denominator, lower/upper descriptive endpoints and unknown mass; retain the all-100 assigned-flow/missingness accounting; and carry the no-interval/no-H1 ceiling. Independent review of that receipt and provenance is still required. No value is computed or inferred here; `JOINT_CLUSTER_HOLD`, H1 status, C1 acceptance, and milestone credit remain unchanged.

## Supplemental method decision — case-SRSWOR RWYB sensitivity (2026-11-11)

**Decision:** adopt Beaumont–Émond Equation 20 as a separately labeled, design-based *secondary sensitivity* for the already-frozen case sample. Do not substitute it for Protocol v1's frozen case-cluster bootstrap, call it the primary interval, or use it to support/refute primary H1. Protocol v1 remains byte-identical at SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`. This choice is made after adjudicated outcomes exist; any resulting interval is exploratory and cannot be represented as prospective confirmatory evidence. `JOINT_CLUSTER_HOLD` therefore remains on the primary H1 decision.

### Frozen design and inclusion probabilities

Sampled units are cases selected by independent fixed-size SRS without replacement within each unseen stratum; recipe IDs are post-sampling dependence descriptors, not the selection unit. For stratum `h` with frame size `N_h` and sample size `n_h=50`:

```text
π_hi = n_h/N_h
π_hij = n_h(n_h−1) / (N_h(N_h−1)) for distinct sampled cases i,j in stratum h
π_ij = π_hi π_gj for cases in independently sampled strata h ≠ g
```

For unseen-profiled (`N=176`): `π_i=25/88`; `π_ij=49/616`. For unseen-unprofiled (`N=3639`): `π_i=50/3639`; `π_ij=2450/(3639·3638)`. Across strata, `π_ij=(25/88)(50/3639)=625/160116`. Distinct-case inclusion indicators have negative covariance within a stratum and zero covariance across strata. The normalized design variance factors `Δ_ij=(π_ij−π_iπ_j)/π_ij` are `Δ_ii=1−f_h` and `Δ_ij=−(1−f_h)/(n_h−1)` for distinct within-stratum cases; cross-stratum `Δ_ij=0`.

A single common random multiplier per recipe gives cases sharing that recipe a common resampling perturbation and positive covariance (and can couple strata when a recipe appears in both). That does not match the case-SRSWOR covariance above. This rejects that *common recipe-multiplier construction as design-matched for this finite-frame design*; it does not prove every possible cluster or superpopulation construction is invalid for another estimand.

### Selected secondary replicate construction

For each stratum `h`, let `f_h=n_h/N_h`, `m_h=n_h−1=49`, and independently draw `K_h=(K_hi)` from `Multinomial(m_h; 1/n_h,…,1/n_h)`. For each sampled case `i` in `h`, use the Beaumont–Émond SRSWOR factor:

```text
a_hi = 1 − sqrt(1−f_h) + sqrt(1−f_h) · n_h/(n_h−1) · K_hi
replicate weight = W_h · a_hi, where W_h=N_h/n_h
```

The multinomial moments give `E(a_hi)=1`, `Var(a_hi)=1−f_h`, and `Cov(a_hi,a_hj)=−(1−f_h)/(n_h−1)` for distinct cases in a stratum; independently generated strata have zero cross-covariance. These are the normalized SRSWOR design-variance `Δ` factors above, not the raw covariance of inclusion indicators. Since `Σ_i K_hi=n_h−1`, `Σ_i a_hi=n_h`, preserving each replicate's stratum total weight `N_h`. Here `0<f_h<1`, so every factor has positive baseline `1−sqrt(1−f_h)>0`. This reproduces the usual stratified SRSWOR plug-in variance for linear totals, including the finite-population correction. It does not make a nonlinear ratio interval exact.

For each `q∈{L,U}`, retain the existing conditional endpoint, with `A_i=I(H_i=0)` and `q_i` the strict-A lower or unknown-inclusive upper indicator:

```text
θ_q = Σ_h W_h Σ_i A_i q_i / Σ_h W_h Σ_i A_i
θ*_q,b = Σ_h W_h Σ_i a*_hi,b A_i q_i / Σ_h W_h Σ_i a*_hi,b A_i
```

Keep all 100 assigned unseen cases in flow/missingness reporting and report each stratum's H=0 count and the weighted H=0 denominator. If the observed eligible denominator is zero, the endpoint and uncertainty are unavailable; do not redraw, impute, or omit assigned cases. With at least one observed H=0 case, positive factors keep every replicate denominator positive.

If later executed under a separate authorized, reviewed task, use 2,000 replicates and seed `20261021`, both fixed before that execution; pin the exact `svrep`/RNG runtime version and check generated factors against Equation 20 before outcome evaluation. For each endpoint, the replicate variance approximation is `B⁻¹ Σ_b (θ*_q,b−θ_q)²`, with `B=2000` and `θ_q` the full-sample weighted ratio. Report the 2.5th and 97.5th empirical replicate quantiles with **linear interpolation**, separately for lower/upper endpoints as **marginal exploratory bootstrap interval summaries**. They are not a simultaneous 95% region, not validated finite-sample coverage for this ratio, and not an H1 decision rule. Preserve undefined-input/run state and Monte Carlo limitations; do not tune the method to observed labels/results.

### Independent review and analytical cross-check

An independent source-free methods reviewer checked the inclusion probabilities, Equation 20 moments, factor positivity/calibration, linear-total variance match, ratio-denominator condition, and primary/secondary boundary. Result: accept only as this secondary design-based sensitivity; finite-sample nonlinear-ratio bias/skew, sparse eligible denominators and coverage remain unvalidated. The official [`svrep` RWYB documentation](https://bschneidr.github.io/svrep/reference/make_rwyb_bootstrap_weights.html) maps SRSWOR to Equation 20 and says factors are generated independently by stratum; the primary method source is Beaumont & Émond (2022), [§3.3 Eq. 20](https://www.mdpi.com/2571-905X/5/2/19#sec3dot3), with the nonlinear-estimator qualification in [§8](https://www.mdpi.com/2571-905X/5/2/19#sec8). These references establish method provenance, not an empirical result or coverage validation for ChefKix.

The requested Taylor expression is retained only as an analytical cross-check, not as an adopted estimator or another interval route. For endpoint `q`, let `x_i=A_i`, `y_qi=A_i q_i`, `D=Σ_h W_hΣ_i x_i`, `θhat_q=Σ_h W_hΣ_i y_qi/D`, and `e_qi=y_qi−θhat_q x_i`. The plug-in design-linearization variance reference is:

```text
Vhat_Taylor(θhat_q) = [Σ_h N_h²(1−n_h/N_h) s²_(e_q,h)/n_h] / D²
```

where `s²` uses denominator `n_h−1`. A later comparison with RWYB replicate variance is diagnostic only; no data values were read and no comparison was computed here. A discrepancy should trigger input/method review, not outcome-tuned adjustment.

No source rows, case IDs, labels, outcomes, maps, estimates, intervals, data transfer, code, tests, protocol change, C1 acceptance, or milestone credit were produced by this decision. Real H-map/source binding and corrected conditional point-receipt gates remain separate and uncleared.
