# L45 Chapter 5 receipt-to-text reconciliation — 2026-10-21

## Scope and method

Read the current Chapter 5 draft and the already-produced local ingestion receipt; compared its stated cohort counts, primary/sensitivity Hit@k, method failures, unmapped predictions, and latency/runtime fields with the corresponding receipt fields. Rehashed the chapter, ingestion receipt, one-shot recovery receipt, recovered output manifest, and recovered raw JSONL. This was a receipt-to-text consistency check—not an independent recomputation from rows, rerun of the ingester, or a new Kaggle transfer. No label contents, model weights, or raw prediction lines were opened during this comparison.

## Reconciled values

| Field | Ingestion receipt | Chapter 5 | Result |
|---|---|---|---|
| Fixed validation cohort / per-arm denominator | 200; each arm 200 | 200 cases; both arms 200/200 | Match |
| Method errors | 0 / 0 | Zero method failures | Match |
| GISMo strata | 105 seen / 95 unseen | 105 / 95 | Match |
| Senath-pair strata | 100 seen / 100 unseen | 100 / 100 | Match |
| Generation GISMo Hit@1/5/10 | 6 / 23 / 28 (3.0 / 11.5 / 14.0%) | Same | Match |
| Reranking GISMo Hit@1/5/10 | 3 / 15 / 29 (1.5 / 7.5 / 14.5%) | Same | Match |
| Generation raw sensitivity Hit@1/5/10 | 7 / 24 / 29 (3.5 / 12.0 / 14.5%) | Same | Match |
| Reranking raw sensitivity Hit@1/5/10 | 3 / 14 / 28 (1.5 / 7.0 / 14.0%) | Same | Match |
| Unmapped generation candidates | 898 | 898 | Match |
| Median / p90, generation | 30.860 / 31.515 s | 30.860 / 31.515 s | Match |
| Median / p90, reranking | 54.914 / 56.850 s | 54.914 / 56.850 s | Match |
| Observed wall / full-200 mean projection / summed-stratum-p90-cost scenario | 17,407.02 / 17,385.94 / 17,892.78 s | Same, with p90 caveat | Match |
| Test split used | `false` | Explicitly validation-only; no test evaluation claim | Match |

## Pins and conclusion

- Chapter SHA-256: `9049f5ec8299cd994bbe2a30727eee0347f3849639d0767afb879456706e20d9`.
- Ingestion receipt SHA-256: `89fa10c939d073ee10414742c1af0f01dfabf4255cae318c6fc9c216c3bb60600`.
- One-shot recovery receipt SHA-256: `90da69d9157203bef144ef6cf191d75398c593212fc6255fb6ca02a4e6686b81`.
- Recovered manifest SHA-256: `2bc2e500c2cc244d5683ed0a5a593f0dd40b8400344fb00fbe50d6f98959b9ea`.
- Recovered raw output SHA-256: `75024e51d73b7379fdcb006af9308306f72d24ea8ad4e6b2efd48e75ddfdea96`.

All checked Chapter 5 values match the ingestion receipt at the displayed precision. No Chapter 5 edit is warranted. This does not validate the receipt independently, provide a fresh remote status or model inference, establish test performance, select a method, or change L44/M-D/claim/thesis acceptance. L44 and M-D remain open.
