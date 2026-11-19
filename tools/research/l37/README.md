# L37: Strata and Cluster-Aware Comparative Metric Evaluator

## Purpose & Scope

This package implements the **Lead-owned L37 comparative metric and strata evaluation tool** under canonical SSoT §§1, 3, 4, 6 and Protocol v1 (`b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`).
It provides an executable, testable, source-free framework for:
1. Deterministic Oracle Strata mapping and denominator validation (10,747 test cases; 1,162 profiled total per Claim R14).
2. Partition-1 deployable request-time cell mapping without gold labels.
3. Standardized metrics calculation with fail-closed denominator preservation:
   - Hit@1, Hit@5, Hit@10, MRR@20 (truncated).
   - Unmatched rate calculation with mandatory `> 5%` flag.
   - Preservation of operational failures and empty-pool abstentions in denominators.
4. Statistical confidence intervals:
   - Exact two-sided Wilson score intervals for proportions.
   - Recipe/Case cluster bootstrap intervals (2,000 resamples, deterministic seed `20261021`, order-statistic percentile convention).
5. Primary Conditional Rescue Estimand $P(V=1 \mid H=0)$:
   - Evaluates rescue under strict valid@5 on unseen GISMo-identity cases.
   - Decision threshold: support H1 if 95% lower bound $\ge 0.15$; refute if 95% upper bound $< 0.15$; otherwise inconclusive.

---

## Core Invariants

### 1. Fixed Denominator Preservation
- Every metric denominator must equal the full assigned cohort ($N = 10,747$ for full test).
- Failures, timeouts, and unranked cases are **never discarded**; they score 0 for Hit and 0 for MRR.
- Unmatched ingredient rates must be disclosed and flagged if $> 5\%$.

### 2. Descriptive Oracle Strata vs Deployable Partition-1 Cells
- **Descriptive Oracle Strata:** Depend on gold annotations and are descriptive only:
  - `seen_unprofiled` (5,946)
  - `unseen_unprofiled` (3,639)
  - `seen_profiled` (986)
  - `unseen_profiled` (176)
  - Total = 10,747 cases; Profiled sum = 1,162 cases (Claim R14).
- **Deployable Partition-1 Cells:** Use request-time features only:
  - `(target in train: yes/no) x (target profiled in FooDB: yes/no)`.

### 3. Primary H1 Conditional Rescue Gate
- Estimand: $P(V=1 \mid H=0)$ among unseen stratum cases.
- Threshold: $\ge 0.15$ lower bound for support; $< 0.15$ upper bound for refutation.
- Both conditional rescue and joint probability $P(V=1, H=0)$ are reported.

---

## Usage

```python
from tools.research.l37.strata_cluster_comparator import (
    analyze_stratified_cohort,
    evaluate_conditional_rescue,
    OracleStratum,
)

# Analyze stratified cohort
report = analyze_stratified_cohort(cases, seed=20261021)
print(f"Total cases: {report.total_cases}")
print(f"Hit@5 point estimate: {report.hit_at_5.point_estimate:.4f}")
print(f"Hit@5 95% CI: [{report.hit_at_5.ci_lower:.4f}, {report.hit_at_5.ci_upper:.4f}]")

# Evaluate primary conditional rescue
rescue = evaluate_conditional_rescue(
    cases,
    target_strata=[OracleStratum.UNSEEN_UNPROFILED, OracleStratum.UNSEEN_PROFILED],
    threshold=0.15,
)
print(f"Conditional rescue P(V=1|H=0): {rescue.conditional_rescue_rate:.4f}")
print(f"Decision: {rescue.decision.value}")
```

## Running Tests

From `chefkix-monolith`:
```bash
python -m pytest tools/research/l37/
```
