# L36 Adjudicated Strict-A@5 Audit Estimand & Partial-Identification Bounds Evaluator

## Purpose

Implements the Lead L36 operational addendum under frozen Protocol v1 (`iron-chef-l36-case-rule-v1`):
1. **Case-Level Adjudication Rule:** Evaluates frozen primary GISMo top-5 candidates.
   - `V = 1` if at least one candidate has final adjudicated label `A` (even if another is `U`).
   - `V = 0` if no `A` and all 5 are `C` or `N`.
   - `V = unknown` if no `A` and at least one is `U` or unresolved (never silently imputed as failure).
   - Operational failures and malformed slates are retained in the fixed denominator ($V=0$).
2. **Fixed Strata & Frame Denominators:**
   - Evaluates over the two frozen unseen-identity strata ($H=0$):
     - `unseen_profiled`: $N = 176, n = 50, W = 3.52$.
     - `unseen_unprofiled`: $N = 3,639, n = 50, W = 72.78$.
     - Total unseen frame denominator = $3,815$ (fixed sample $n = 100$).
   - Rejects complete-case analysis; preserves fixed denominator verbatim.
3. **Partial Identification Point Bounds:**
   - $p_{\text{lower}} = \frac{\sum_h W_h \sum_{i \in h} L_i}{3,815}$ where $L_i = \mathbb{I}[V_i = 1]$.
   - $p_{\text{upper}} = \frac{\sum_h W_h \sum_{i \in h} U_i}{3,815}$ where $U_i = \mathbb{I}[V_i \in \{1, \text{unknown}\}]$.
   - Weighted unknown mass: $\Delta_{\text{unknown}} = p_{\text{upper}} - p_{\text{lower}}$.
   - Sensitivity analysis: $A+C$ sensitivity report.
4. **2,000-Draw Case-Cluster Bootstrap:**
   - Seed: `20261021`.
   - Resamples independent source-recipe clusters with replacement within each stratum.
   - Guard: If any recipe cluster crosses both unseen strata, triggers `JOINT_CLUSTER_HOLD` and halts independent resampling.
   - Percentile 95% confidence interval via linear interpolation (`numpy.quantile(method="linear")` convention).
5. **H1 Decision Rule:**
   - `SUPPORT_H1`: 95% CI lower bound of $p_{\text{lower}} \ge 0.15$.
   - `REFUTE_H1`: 95% CI upper bound of $p_{\text{upper}} < 0.15$.
   - Otherwise: `INCONCLUSIVE`.
6. **PR10 Rater Agreement Verification:**
   - Evaluates unweighted nominal Cohen's $\kappa$ across $\{A, C, N, U\}$ against PR10 cutoff $\ge 0.60$.
   - Computes Wilson 95% score interval for percent agreement.
   - Tracks reference-dispute diagnostic (both raters mark recorded-gold as $N$, e.g. 58 cases in baseline), keeping benchmark Hit unchanged.

## Pinned Hashes & Evidence

- **Specification ID:** `iron-chef-l36-case-rule-v1`
- **Protocol v1 SHA-256:** `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`
- **SSoT Operational Document:** `evidence/lead-l36-adjudicated-strict-a5-operational-v1-20261106.md`

## Testing

From `chefkix-monolith`:
```bash
python -m pytest tools/research/l36/
```
