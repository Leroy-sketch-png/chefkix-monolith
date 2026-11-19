# L45 Scientific Argument and Methods Rationale Evaluator

Evaluates and verifies the complete 14-chapter scientific argument, methods rationale,
and conclusion boundaries specified in Lead Backlog L45, Academic Vision & Strategy
§§3, 6–8, 17, and 21, and the shared authored chapters at
`evidence/iron-chef-lead-member-handoff-20261105/chapters/`.

## Invariants and Verification Gates

1. **Chapter Hash Parity (14/14 chapters):**
   - Strictly verifies that every chapter matches its exact destination SHA-256
     pinned in `SOURCE.json`.
   - Chapters 01 through 14 integrity locked.

2. **Chapter 12 Bounded Negative Results Audit:**
   - 7 distinct negative/withdrawn/unavailable threads:
     * SAG fusion ablation rejection (Hit@1 0.1171% vs naive fusion 0.3333%)
     * True-context dual encoder rejection (Recipe1MSubs Hit@1 16.12% vs GISMo 20.69%)
     * Global flavor-pairing non-significance ($p = 0.238095 > \alpha = 0.05$)
     * Behavioral exact-pair overlay rejection ($-0.0021$ unseen MRR loss)
     * Recipes5k frozen-feature CLIP projection rejection ($+1.32$ pp vs $+2.00$ pp gate)
     * Historical SmolVLM adapter withdrawal (`7e8c164e0fda2d4c7c1f24a17ea94a89fc7027dfbfbfee4d152192644fe7dea1`)
     * Voice-vision loopback unavailability (100/100 unavailable)
   - Descriptive calibration boundaries:
     * $\hat{t} = 7$ (`ok` on seen pairs, $n=4,801$, $\alpha=0.30$)
     * $\hat{t} = 12$ (`uninformative` on seen pairs, $\alpha=0.20$)
     * $\hat{t} = \infty$ (honest vacuousness on unconditioned/unseen cells)
     * Formal statistical coverage guarantees remain strictly disabled under unproven exchangeability.

3. **Chapter 13 Discussion & Threats Audit:**
   - Frozen core protocol rules acknowledged (PR01-03/05/09/10).
   - PR13 ethics/consent preserved as an uncompromised human-subject gate.
   - L36 operational mapping acknowledged without replacing human evidence.
   - Campaign progress strictly reconciled to 19/25 milestones (76.0%).
   - C5 vertical integration proof verified: 6,633 nodes, 23,185 directed edges, 0 identity errors, manifest prefix `ade85c3312b8`.

## Verification

Run the test suite:
```bash
python -m pytest tools/research/l45/ -v
```
