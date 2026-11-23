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
   - Checks the historical 19/25 (76.0%) wording in the pinned Chapter 13 snapshot. Current campaign accounting comes from the later C1-G3 acceptance.
   - C5 vertical integration proof verified: 6,633 nodes, 23,185 directed edges, 0 identity errors, manifest prefix `ade85c3312b8`.

## Verification

From the repository root, check the current public handoff:

~~~bash
python tools/research/l45/scientific_argument_evaluator.py evidence/iron-chef-lead-member-handoff-20261105/chapters
~~~

The output verifies authored destination bytes and selected text boundaries. Scientific approval, current cross-chapter reconciliation, and accepted-result reproduction remain separate.

Run the test suite:
```bash
python -m pytest tools/research/l45/ -v
```

## Offline manuscript review copy

The HTML builder assembles all 14 Lead-authored chapters from a specific Git revision. It verifies each chapter against the shared handoff manifest, renders equations and tables, includes the Chapter 6 aggregate figure as a separate appendix, and lists citations whose shared destinations remain unavailable. It keeps the dated chapter text and annotations as published. The review copy runs offline.

Use Node.js 24 with Marked 17.0.5 and KaTeX 0.19.1. From the repository root:

```powershell
$reviewDeps = Join-Path $env:TEMP 'chefkix-manuscript-review'
npm install --prefix $reviewDeps --no-save --ignore-scripts marked@17.0.5 katex@0.19.1
$env:NODE_PATH = Join-Path $reviewDeps 'node_modules'
node tools/research/l45/build_manuscript.cjs ./out/iron-chef-manuscript-review.html b52e5b646ce174604aa57ee95a005bd61c91904c
```

The final argument selects the source revision. The builder uses `HEAD` when the argument is omitted. For citations moved into the shared repository, [the reference pins](shared_reference_pins.json) record the original path, verified copy, source SHA-256, and provenance. The builder links a copy only when the selected Git revision contains those exact bytes. The console prints the output SHA-256 and counts of chapters, equations, tables, and references. See the [Member review receipt](../../../evidence/member-mh-thesis-evidence-update-20261122.md#offline-manuscript-review-copy) for the verified build.
