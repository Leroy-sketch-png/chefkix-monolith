# L44: Scientific Arm and Method Selection Evaluator

## Purpose & Scope

This package implements the **Lead-owned L44 scientific method selection and operational feasibility evaluator** under canonical SSoT §§4, 6, 15, Protocol v1 (`b2952599...`), and `BACKLOG_LEAD.md`.
It provides an executable, testable, source-free framework for:
1. **Validation Diagnostic Evaluation (200 cases):** Evaluates Senath generation vs GISMo top-50 reranking, proves ranking inversion, and enforces that no method winner is declared.
2. **Mistral GPU Resource Feasibility Gate:** Verifies whether full-split Mistral expansion is feasible under the 40-GPU-hour cap, enforcing fail-closed rejection.
3. **C2 API Route Selection Gate:** Evaluates conservative preflight costs against remaining OpenAI budget under the 80% spending ceiling, selecting Full Matched Cohort (10,747 cases) or Stratified Fallback (2,000 cases).
4. **Matched-Cohort Integrity:** Ensures arms are evaluated on identical case IDs.

---

## Core Invariants

### 1. No Method Winner Under Frozen Protocol v1
- In the recovered 200-case validation diagnostic:
  - **Generation:** Hit@1 = 6 (3.0%), Hit@5 = 23 (11.5%), Hit@10 = 28 (14.0%)
  - **Reranking:** Hit@1 = 3 (1.5%), Hit@5 = 15 (7.5%), Hit@10 = 29 (14.5%)
- Because Generation leads at Hit@1/5 while Reranking leads at Hit@10, there is a **ranking inversion**.
- Mixed rankings do NOT select a winner. The diagnostic cannot be enlarged or relabeled as full-split test evidence.

### 2. Mistral Widening is Not Feasible under 40 GPU-Hours
- Pinned projections: 255.91 h validation / 256.33 h test mean ($512.24$ h total) and $527.25$ h summed-p90 scenario.
- Required hours exceed the total 40.0-hour cap by $>470$ hours.
- Rule: **`NO_NEW_MISTRAL_TRAINING`**. Keep the historical 200-case ceiling until HUMAN explicitly amends global resource ceiling.

### 3. C2 API Arm Preflight Routing Rule
- If conservative estimated cost $\le 80\%$ of remaining budget: Route to **`FULL_MATCHED_TEST_COHORT_10747`**.
- If conservative estimated cost $> 80\%$ of remaining budget: Route to **`STRATIFIED_TEST_FALLBACK_2000`** (`seed=20261021`, cost-only fallback).
- If remaining budget $\le 0$: Route to **`BLOCKED_BUDGET_EXHAUSTED`**.

---

## Usage

```python
from tools.research.l44.arm_selection_evaluator import (
    run_l44_evaluation,
    route_api_arm,
)

report = run_l44_evaluation(spent_usd=0.0, reserved_usd=0.0, estimated_cost_usd=45.0)
print(f"Method winner: {report.method_winner}")
print(f"GPU Feasibility: {report.gpu_feasibility.status}")
print(f"API Route: {report.api_routing.route_selected.value}")
```

## Running Tests

From `chefkix-monolith`:
```bash
python -m pytest tools/research/l44/
```
