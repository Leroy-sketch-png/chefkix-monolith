# L44 — Saved-output showcase regression recheck (2026-10-21)

- Re-ran from the research checkout at `D:\code\chefkix\.git-worktrees\root-master-segment10-20261014`, HEAD `42f11119ced56cd81034c957ee2c1c511fd5a280`:
  `python -m pytest ai/tools/test_senath_validation_benchmark.py ai/tools/test_midterm_evidence_exhibit.py -q`
- Result: **30 passed in 21.06 s**. The suite includes a saved-receipt-to-rendered-cell-order regression for both benchmark arms. This is automated structural evidence, not a visual or human-presentation review.
- Renderer commit: research `eda6ddf` (`Fix Senath showcase metric column ordering`). Renderer SHA-256 `4d6eabe8cc873a00d075c291bf7865f1a80b7af9eabf749ce402b4cc72b3c545`; benchmark test SHA-256 `cf81bb76f1e47c3c1e6cd71b98783c3cd8ef90a590ce0eb733ab2c7639f782b`; midterm-exhibit test SHA-256 `7305909c6262871ff2661db9549b60a87fc85b4c910ff59ddae07d59b4ded5e2`.
- Corrected local-only showcase SHA-256 `2f88fcbcc8512e44908f0136fbf5e43d7c25ef59a826f1962bc8318eaec5bcb3`. The generated HTML and prediction rows were not copied into this monolith commit.
- **Boundary:** Browser visual QA and timed listener rehearsal remain unperformed; do not describe the showcase as presentation/accessibility approved. No new inference, GPU request, Kaggle transfer, or quality claim occurred. L44 remains open.
