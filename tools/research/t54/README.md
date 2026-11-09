# T54 Method-v2 Scorer, Group-Split, and Conformal Calibration Processor

This package implements and verifies the Lead-owned prospective calibration producer contract:
`evidence/iron-chef-lead-member-handoff-20261105/t54-method-v2-producer-contract-20261106.md`
governed by Campaign Protocol v1 (SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`)
and Strategy §§3, 6–8, and 17.

## Scope and Boundaries

- **State:** Source-free contract and algorithm implementation. No empirical calibration, clinical claim, formal guarantee, or route activation is claimed.
- **Fail-Closed Exchangeability Gate:** Under current project disposition, case-level exchangeability across recipes and future requests is unproven. Therefore, product `tHat` remains strictly `None` and `guarantee_status` emits `no_guarantee` with reason `inference_assumptions_unresolved`.
- **Descriptive Utility:** Descriptive calibration cutoff and empirical coverage on the calibration sample are computed for research reporting only, without an inferential guarantee.

## Proven Invariants

1. **Recipe-Grouped Split (`c3-recipe-group-sha256-split-v1`):**
   - Distinct recipe groups $G \ge 4$ required; refuses otherwise.
   - Deterministic SHA-256 hash over domain prefix `iron-chef:c3-rank-conformal-receipt-v2:recipe-split:20261021\0` and exact UTF-8 recipe ID.
   - Stable ascending sort on digest bytes with recipe ID tie-break.
   - Exactly $\lfloor 3G / 10 \rfloor$ groups allocated to development, remaining to calibration.
   - Input-order invariant, outcome-blind, and seed-pinned.
2. **Request-Time Cells and Denominators:**
   - Baseline-0 (all-input) and Partition-1 (4 cells based on train-presence $\times$ profile-presence).
   - Denominator $n$ is distinct calibration cases in the cell. If $n < 200$, emits `no_guarantee` with `insufficient_sample_size_below_200`.
3. **Four Distinct Typed Outcome Rows:**
   - Finite rank $1..M$: candidate satisfies contract, conformal score = rank. Retained.
   - $+\infty$ with `outcome_code="no_reference_match"`: valid computation, nonempty top-M, no match. Retained in denominator, not an operational failure.
   - $+\infty$ with `outcome_code="empty_filtered_pool"`: hard filter leaves no candidates. Retained in denominator, serving result is `abstain`.
   - $+\infty$ with `failure=True` (code `scoring_failure` or `method_failure`): operational failure. Retained in denominator and counted separately.
4. **Finite-Sample Order Statistic:**
   - $k = \lceil (n + 1)(1 - \alpha) \rceil$.
   - Sample cutoff is the $k$-th smallest score among calibration cases (treating $+\infty$ as strictly greater than finite ranks).
5. **Nine-Role Provenance Digest Binding:**
   - Binds and verifies: `graph_manifest`, `hard_filter`, `ranker`, `scorer`, `score_contract`, `label_contract`, `split_assignment`, `identity_index`, and `calibration_scores`.
   - Missing or mismatched roles fail closed with `no_guarantee` and null `tHat`.

## Verification

Run the test suite:
```bash
python -m pytest tools/research/t54/test_method_v2_calibration.py -v
```
