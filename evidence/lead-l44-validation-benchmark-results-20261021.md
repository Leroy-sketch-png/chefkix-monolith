# L44 — Recovered 200-case validation benchmark results (2026-10-21)

## Evidence identity

- Kaggle kernel: `phanphutho/chefkix-senath-validation-benchmark-v1`, version 4; terminal status `COMPLETE`.
- One-shot recovery receipt: `evidence/senath-inference/validation-benchmark-v1-recovered/recovery_receipt.json`, SHA-256 `90da69d9157203bef144ef6cf191d75398c593212fc6255fb6ca02a4e6686b81`.
- The receipt records exactly one output-download invocation and `LOCAL_VALIDATION_PASSED`. A fresh local inventory comparison found 14 expected/14 actual files, with zero missing, extra, or hash-mismatched files.
- Frozen evaluation: `senath-vs-gismo-validation-identity-v2`; benchmark: `senath-vs-gismo-stratified-validation-200-v1`; 200 fixed validation cases, 100/100 Senath-pair seen/unseen and 105/95 GISMo-identity seen/unseen. Test split was not used.
- Stage-3 final manifest SHA-256: `7a3c3768b7b83ae2d922dfb2275808be08c02142e670b3607b20f69b1c71a2d3`; adapter model SHA-256: `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`.
- Candidate-pool SHA-256: `5d4fe8ab4b6d190be8cb1d8bcf483dbe994869cc1c639bddf60391ea9eafb96a`; identity-map SHA-256: `eb612db7a46b9813d3295e99c0bf8edfb03be8cda03e34c05cafa90ddc44e9fa`; output-manifest SHA-256: `2bc2e500c2cc244d5683ed0a5a593f0dd40b8400344fb00fbe50d6f98959b9ea`; raw-output SHA-256: `75024e51d73b7379fdcb006af9308306f72d24ea8ad4e6b2efd48e75ddfdea96`; phase-log SHA-256: `3d688fa9a7eb91cac08b99c4d76bb3216c1cab38f95c85998a4dcaeb4fdf6cef`.

## Recomputed saved-output diagnostics

Hit@k is shown as hits/200 (rate). Identity scoring and raw-string sensitivity are separate; neither is human-judged usefulness.

| Arm | GISMo identity Hit@1 / @5 / @10 | Raw-string sensitivity Hit@1 / @5 / @10 | Unmapped candidate predictions | Median / p90 case latency |
|---|---|---|---:|---:|
| GISMo top-50 candidates + Senath likelihood reranking | 3/200 (1.5%) / 15/200 (7.5%) / 29/200 (14.5%) | 3/200 (1.5%) / 14/200 (7.0%) / 28/200 (14.0%) | 0 | 54.914 / 56.850 s |
| Senath top-20 generation | 6/200 (3.0%) / 23/200 (11.5%) / 28/200 (14.0%) | 7/200 (3.5%) / 24/200 (12.0%) / 29/200 (14.5%) | 898 | 30.860 / 31.515 s |

Both arms retained denominator 200 and had zero method-failure records. The observed process wall time was 17,407.021 s (about 4 h 50 min); startup was 210.244 s. The receipt's stratified per-case p90 cost scenario totals 17,892.784 s and is not a statistical p90 for total job duration.

## Interpretation boundary and remaining work

This is recovered execution plus local saved-output validation and identity-based diagnostic scoring on a fixed 200-case validation sample. It does not establish independent test performance, statistical significance, human usefulness, superiority, generalization, or overall L44 completion. The arms differ in candidate construction and output cardinality; treat the table as descriptive, not a causal or fairness-controlled comparison. Unmapped generated strings remain visible in the denominator and are not silently discarded. Full-split inference, independent test, human evaluation, and showcase review remain separate gates. Source: the hash-bound recovery receipt above; no new Kaggle invocation or download was performed for this note.
