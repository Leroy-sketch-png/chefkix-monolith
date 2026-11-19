# L13′ Shift-Aware Selector Interface & Leakage Guard

## Role & Scope Boundary

Implements the Lead-owned scope decision:
`evidence/lead-l13-shift-aware-selector-scope-20261106.md`
governed by Campaign Protocol v1 (`SHA-256 b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`)
and Strategy Sections 2–3, 5, 8, and 17.

### Core Invariants

1. **Frozen Scorer Selection Only**:
   - The selector operates strictly over the outputs of already frozen, calibrated scorer arms (e.g. baseline, train-graph, profile-augmented).
   - It **does not** retrain models, blend raw logits, alter relative ranks, or synthesize new candidate entities. Joint dual-encoder / SAG architectures were explicitly rejected.

2. **Strict Request-Time Feature Leakage Guard**:
   - Permitted features: request-time, permissioned, source-versioned features available before substitution scoring occurs (e.g. recipe token count, ingredient count, catalog membership state, train source presence, FooDB profile presence).
   - Forbidden features: gold/reference substitutions, human or LLM adjudication labels, test metrics, oracle strata, realized candidate ranks, post-hoc scores.
   - Any presence of forbidden features raises an immediate, fail-closed `ValueError`.

3. **Denominator Preservation**:
   - Fixed evaluation denominator is strictly maintained across all arms and the selector.
   - When a chosen scorer yields an empty-pool abstention (`EMPTY_POOL_ABSTAIN`) or execution failure (`SCORING_FAILURE`), the selector preserves that state verbatim. It never silently swaps to another scorer to pad hit rates.

4. **H5 Exploratory Boundary**:
   - Emits `status = "EXPLORATORY_DESCRIPTIVE_ONLY"` and `guarantee_status = "no_guarantee"` with `reason = "unproven_exchangeability_and_exploratory_h5"`.
   - Never claims formal coverage guarantees under covariate shift without proven calibration exchangeability.
