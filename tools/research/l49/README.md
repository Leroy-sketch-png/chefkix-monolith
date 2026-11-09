# L49 / H4 Counterbalance Schedule, Scoring, and Estimator

This package implements and verifies the mathematical counterbalance design and statistical estimators specified in `BACKLOG_LEAD.md` L49 and `evidence/iron-chef-lead-member-handoff-20261105/l49-h4-measurement-instrument-v1-20261108.md`.

## Implemented Invariants

1. **Cohort Specification:** Fixed target $N = 24$ randomized adult participants arranged in 12 counterbalance pairs ($j = 0 \dots 11$).
2. **Case Ordering:** In pair $j$, position $i \in \{0 \dots 11\}$ presents case $k = (i + j) \bmod 12$. No participant sees any case twice.
3. **Condition Assignment:** Member A sees `DISPLAYED` iff $(i + 2j) \bmod 12 < 6$; Member B sees `WITHHELD`.
4. **Exact Orthogonal Balancing:**
   - Each participant evaluates exactly 6 `DISPLAYED` and 6 `WITHHELD` cases.
   - Each case is presented to 12 participants `DISPLAYED` and 12 participants `WITHHELD`.
   - At each serial position $i \in \{0 \dots 11\}$, exactly 12 participants see `DISPLAYED` and 12 see `WITHHELD`.
   - In each pair, for every case, exactly one member sees `DISPLAYED` and the other `WITHHELD`.
   - Each case-position pair $(k, i)$ occurs exactly twice across the entire cohort.
5. **Scoring Key & Codes:**
   - Displayed key: `PRESENCE_ONLY` (Option 1) $\to 1$, all others $0$.
   - Withheld key: `UNKNOWN_NOT_SHOWN` (Option 8) $\to 1$, all others $0$.
   - Unanswered / declined / unavailable responses are missing ($M$), never imputed as incorrect.
6. **Full-Assigned Cohort Missingness Bounds ($N = 24$):**
   $$L[p] = \frac{c[p, s]}{6} - \frac{c[p, w] + m[p, w]}{6}$$
   $$U[p] = \frac{c[p, s] + m[p, s]}{6} - \frac{c[p, w]}{6}$$
7. **Harmful Overreading & Misread Contradiction Detection:**
   - Common overread: Options 3–7 (`SUITABILITY`, `SENSORY_OR_FUNCTION_EQUIVALENCE`, `NUTRITION_BENEFIT`, `ALLERGEN_STATUS`, `SAFETY`) in either condition.
   - Withheld extra overread: Options 1 (`PRESENCE_ONLY`) and 2 (`ABSENCE`).
   - Displayed contradiction: Option 2 (`ABSENCE`) is a misread/contradiction, not an overread.
   - Exact one-sided 95% binomial upper bound for zero observed events.
8. **Planned Floor:** Requires $\ge 20$ complete participants; shortfalls fail closed as `BELOW_FLOOR_INCONCLUSIVE`.

## Execution

```bash
python -m pytest tools/research/l49/test_h4_counterbalance_schedule.py -v
```
