# C4 Pool-Level LLM Judge Contract, Runner, and Operational Gates

This package implements and verifies the pool-level, candidate-order-blind C4 judge contract and Section 3.3 / L43 operational gates specified in:
- `research/protocols/IRON-CHEF-PROTOCOL-v1.md` §3.3
- `evidence/lead-l43-judge-family-prompt-decision-20261106.md`
- `BACKLOG_LEAD.md` L43

## Implemented Invariants

1. **Pinned Prompt Assets:**
   - System Prompt SHA-256: `96e24ae1f629d9c5f4a58457ae0c148253be2571ca55eb8d4560b1f1281f5db7`
   - User Template SHA-256: `713997acc2715d327fda254d3a34a4904999f5806c43b3e990e8dfb98782539b`
2. **Opaque-ID Candidate Blinding:**
   - Slates are submitted as unordered opaque IDs without revealing arm, rank, source, gold, model family, or presentation position.
   - Forward pass is the primary comparison pass; reverse pass inverts candidate list order for stability checking.
3. **Operational Gates Evaluated:**
   - **Gate 1 (Agreement Kappa):** Unweighted nominal 4-label (A/C/N/U) Cohen's $\kappa \ge 0.60$ against human adjudications.
   - **Gate 2 (Kappa Bootstrap CI):** 2,000 whole-case cluster bootstrap draws (seed `20261021`) with order-statistic percentile lower bound $\ge 0.50$.
   - **Gate 3 (Leniency Delta):** Per-cell signed $|\Delta_c| = |\text{mean}(\text{judge} = A) - \text{mean}(\text{human} = A)| \le 0.10$ for each nonempty cell $c$.
   - **Gate 4 (Order Instability):** Conservative upper bound $(F + M) / D \le 0.10$, where $F$ is differing valid labels and $M$ is missing labels.
   - **Gate 5 (Invalid Output):** Proportion of failed requests across $2 \times N_{\text{cases}}$ planned pool calls $\le 0.02$.
4. **Fail-Closed Disposition:**
   - Any `FAIL` rejects the judge (`overall_status = "REJECTED"`).
   - Any `UNAVAILABLE` prevents acceptance (`overall_status = "INCOMPLETE_GATES_UNAVAILABLE"`).
   - All gates `PASS` $\to$ `overall_status = "ACCEPTED"`.

## Execution

```bash
python -m pytest tools/research/c4/test_c4_pool_judge_runner.py -v
```
