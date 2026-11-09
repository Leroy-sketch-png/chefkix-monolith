# L42: §25.4 Novelty Evidence Funnel & Defensible Contribution Evaluator

## Purpose & Scope

This package implements the **Lead-owned L42 literature evaluation tool** under canonical SSoT §§4, 25.4, 25.5 and `BACKLOG_LEAD.md`.
It provides an executable, testable, source-free framework for:
1. Deduplication and multi-stage filtering of literature candidates.
2. Refutation of broad novelty claims against published prior art.
3. Verification of pinned hashes and lineage discrepancies across historical reports.
4. Fail-closed bounding of the candidate differentiator design without claiming unevidenced global novelty.
5. Strict alignment with Claims Register v20 (`72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`).

---

## Core Invariants

### 1. Broad Priority Claims are Strictly Forbidden
The tool proves and enforces the refutation of four broad priority claims:
- **`FIRST_SUBSTITUTION_SYSTEM`:** Refuted by Yamanishi et al. (2015, S55), Shidochi et al. (2011, S56), and AdaptaFood (2025, S43).
- **`FIRST_HUMAN_EVALUATED_SUBSTITUTION`:** Refuted by Pellegrini et al. (2021, S46), Li & Zaki (2022, S47), Vandeputte et al. (2023, S48), Achananuparp & Weber (2016, S49), Yamanishi et al. (2015, S55), and Morales-Garzón et al. (2021, S57).
- **`FIRST_CONTEXT_AWARE_RANKING`:** Refuted by AdaptaFood (2025, S43) and Li & Zaki (2022, S47).
- **`FIRST_FOOD_SWAP_ACCEPTANCE`:** Refuted by Vandeputte et al. (2023, S48).

### 2. Defensible Contribution Framing
The surviving framing is strictly a **research question**:
> *"Does exact-match retrieval against a single recorded substitute undercount independently judged, context-valid alternatives within a recipe-context candidate set, and does that gap differ across the frozen identity strata?"*

### 3. Pinned Source Lineage & Discrepancies
- **S43 / S46:** Pinned PDF SHA-256 for S43 (`8dcfab91...`) and S46 (`d3a0a023...`). S43 cites S46 as having 13,056 pairs in ground truth, whereas S46 distinguishes 708 ground-truth pairs (over 42 source ingredients) from a separate 13,056-row prediction export. Discrepancy is classified as `PREDICTION_COUNT_CONFLATION_PLAUSIBLE_UNPROVEN`; editorial cause is not asserted as fact.
- **S47:** Preserves the unresolved count mismatch: reported 150 items vs 50 recipe + 50 ingredient + 100 generalized = 200 items (`UNRESOLVED_CATEGORY_OVERLAP`).
- **Morales-Garzón Lineage (S57/S58/S60):** Reconciles corpus counts (267,071 = 256,392 from 3 corpora + 10,679 BBC Food in S59). S57 PDF hash (`2826ad0c...`) and S60 LFS corpus hash (`221a3257...`) are pinned. Enforces `ONE_LINKED_LINEAGE_NOT_INDEPENDENT_REPLICATIONS` (participant and case overlap remain `UNKNOWN`).

### 4. Claims Register v20 Boundary
- Claims **R28** (bounded prior-art screen) and **R29** (novelty funnel / independent review) remain strictly bounded to **`UNVERIFIED / NA`**.
- Local tool evaluation does not constitute external human expert review and does not authorize claim status upgrades.

---

## Usage

```python
from tools.research.l42.novelty_funnel_evaluator import (
    run_novelty_funnel_evaluation,
    BroadClaimType,
)

report = run_novelty_funnel_evaluation()
print(f"Total retrieved: {report.total_retrieved}")
print(f"Deduplicated count: {report.deduplicated_count}")
print(f"Surviving framing: {report.surviving_research_question}")
for claim, eval_res in report.broad_claim_evaluations.items():
    print(f"[{eval_res.disposition.value}] {eval_res.claim_statement}")
```

## Running Tests

From `chefkix-monolith`:
```bash
python -m pytest tools/research/l42/
```
