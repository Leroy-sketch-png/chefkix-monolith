# R04/R05 local reproduction note

Date: 2026-10-21. Source audit: research `master` commit `43d4a2f293789a51f722df3d6b8c85fc10e50376`; machine receipt: `evidence/lead-mb-full-split-source-join-20261021.json` (manifest SHA-256 `a5d1fafa4b6f0078330693a433487f69f66eb771de7804699caeed5101036bca`).

| Seed row | Locally reproduced value | Identity/measurement contract | Result boundary |
| --- | --- | --- | --- |
| R04 — GISMo and Senath seen/unseen counts | Validation: GISMo 6,827/3,902; Senath-normalized sensitivity 6,771/3,958. Test: GISMo 6,932/3,815; Senath-normalized sensitivity 6,866/3,881. | GISMo identity resolves endpoints through the pinned released vocabulary to exact ordered graph-node pairs and compares with train pairs. Sensitivity normalizes raw source/target strings with the pinned prior normalization function. | Counts reproduced separately. The partitions differ under the two identity contracts; do not average them or treat the contracts as interchangeable. |
| R05 — 49,044/10,729/10,747 split counts | Train 49,044; validation 10,729; test 10,747 rows. Each raw CSV-to-released-GISMo-pickle join passed on the pinned local bytes. | Exact split CSV and pickle inventory, source/recipe/context joins and pinned source/GISMo hashes are in the machine receipt. | Local artifact consistency is reproduced. This does not independently establish publication provenance, data rights, or split independence. |

This is row-level evidence that can inform a future real claims register. It does **not** change the migration seed: its source-provenance label remains `LOCAL_MIGRATION_CHECKLIST_ONLY`, and the seed is not an instantiated or validated register. No `VERIFIED` register status, progress scoreboard, external citation, independent test result, inference, or quality claim is created here. R04/R05 therefore remain `UNVERIFIED` in the current seed until L48 reconciles their provenance and records them in a structurally valid, source-bound register.
