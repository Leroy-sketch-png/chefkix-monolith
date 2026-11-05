# Chapter 5 — Senath Clean-Room Training and Bounded Inference Evidence

**Working draft v0.4 — 2026-11-02**
**Evidence state:** verified local Segment-9 export, recovered 200-case validation outputs, independently recomputed matched measurement benchmark, and verified 10,747 Option 2 batch transport candidate (53 partitions, zero leakage).
**Use:** internal chapter draft. This is a matched validation diagnostic; it is not an independent test result, method-selection decision, model-quality claim, or completed L12/L44 thesis contribution.

## 5.1 Research question and comparison boundary

This chapter documents what the clean-room Senath training pipeline produced and what a bounded inference run has measured so far. The immediate comparison is between (1) deterministic top-20 generation from the retained Mistral adapter and (2) likelihood reranking of GISMo's frozen top-50 candidate pool using that adapter. It is a validation diagnostic intended to prove and inspect the inference path; it is not a replication of the authors' reported 22.04% Hit@1 and is not a fresh estimate of generalization.

The distinction matters because the same official Recipe1MSubs train/validation/test split is used by Senath and GISMo, while the retained adapter was selected using validation loss. These 200 validation cases therefore cannot serve as an independent test of the selected model or settle a method comparison. The test split has not been evaluated by this benchmark (`test_split_used: false`). Any future test analysis remains subject to the frozen protocol, rights, and authorization gates in the active strategy.

## 5.2 Training artifact and evaluation checkpoint

The checkpoint lineage records the three-stage clean-room SFT → preference-generation/DPO → post-DPO SFT path, with the segmented Stage-3 chain ending at global step 561. Its frozen evaluation history is exactly step 187 (loss `0.19342221319675446`), step 374 (`0.20272061228752136`), and step 561 (`0.224910706281662`). The selected adapter is the retained best from step 187, not the last training step. Segment-9 manifest SHA-256 is `46f5b0f403f9840e07854ca4c1ce179e49d136b65cc5f319c56f3b2e626eb969`; final Stage-3 manifest SHA-256 is `7a3c3768b7b83ae2d922dfb2275808be08c02142e670b3607b20f69b1c71a2d3`; adapter-model SHA-256 is `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`.

The verified Segment-10 record reaches step 631 without a new evaluation at that boundary. A later primary-wrapper monitor record positively acknowledges Segment-11 version 1 (script SHA-256 `1926e2b2e8bf785f12d4d40a8fd9a481d8f9b43a06a709b32b7d06d5a3410f2b`; metadata SHA-256 `7b89c5528a50b7aeb007d71347e97b7888961e3a12d792afcc9f10815304b2d3`; sole predecessor Segment-10 pinned to manifest `24b297105fda76720d9d080e4f0cb102dc08fb151acaf39aa7356609acc6a581`). The launch acknowledgement alone does not establish terminal status, a validated step-701 checkpoint, or an evaluation at that step; the monitor specifies no new evaluation at 701 and preserves the frozen 187/374/561 evaluation history. This later history provides no measured basis for replacing the selected adapter. The inference experiment described here binds to the verified Segment-9 export and retained-best adapter. Training completion, loss values, and longer optimizer schedules are not themselves substitution-quality evidence.

## 5.3 Frozen validation execution and independent recomputation

The frozen validation-benchmark v4 contains 200 cases with a hash-bound case frame, identity map, GISMo top-50 pools, adapter, runner, and scoring specification. Kaggle produced execution outputs only; labels remained local. The one-shot recovery receipt records one output-transfer invocation for expected kernel version 4 and validation before promotion. A fresh local inventory check matched all 14 expected output paths and hashes to the retained bundle exactly (no missing, extra, or mismatched files); the helper was not invoked again.

The validator payload nested in the recovery receipt and the recovered state file record diagnostic-only validation of the fixed denominator before the saved-output showcase was rendered. Both arms have 200/200 records and zero method failures. The standalone ingestion receipt is present at `ai/out/senath-validation-benchmark-v1-recovery-20261021/ingestion_receipt.json` (SHA-256 `89fa10c939d073ee10414742c1af0f01dfab4255cae318c6fc9c216c3bb60600`).

On 2026-10-31, an independent, read-only audit tool (`recompute_senath_validation_metrics_independent.py`) executed across all 7 pinned inputs (SHA-256 enforced) without loading model weights or calling external services. The recomputation verified 100% bit-identical agreement against the ingestion receipt, confirming `state: INDEPENDENT_RECOMPUTATION_MATCH` with `mismatches: []` (receipt output `ai/out/milestone-reconciliation/c2-independent-recompute-20261031.json`, SHA-256 `99f53930c76798bb1bb64d7576e781263df3f37d4165710679a6a7a9af39f818`).

The primary scoring identity is GISMo's canonical vocabulary (`candidate_normalized_name_to_unique_id`). Senath raw/normalized string matching is reported separately as sensitivity, never substituted for primary identity. The frozen 200-case denominator partitions into 5 audited cohorts:
- `all`: 200 cases (case IDs SHA-256 `147ee2f21e7b11a9e7e64ca63494f9a0d0818c2b6910957225a6b06fec41df43`)
- `GISMo_identity:seen`: 105 cases (SHA-256 `49fc4d84402f59da5a9e1fd61b990011f51c342c38071ecd7845b5cac2a4693a`)
- `GISMo_identity:unseen`: 95 cases (SHA-256 `4077e782243e29af59d336408190b72e07970095f5ca41e94716117db759cb17`)
- `Senath_pair_identity:seen`: 100 cases (SHA-256 `320fb8051c93aba72f1d490db383ab3e84401bf9c4ff8a00eb61d475ec384007`)
- `Senath_pair_identity:unseen`: 100 cases (SHA-256 `c5ee9ebd9e9f90503dbbaa838ea5826efb2784b46f945147bd9cd504c43b396e`)

### Full Matched Measurement Benchmark Table (200 Validation Cases)

| Arm | Cohort | Denom ($N$) | Hit@1 | Hit@5 | Hit@10 | Raw Hit@1/5/10 | Failures | Unmapped Predictions | Unresolved Targets |
|---|---|---:|---:|---:|---:|---|---:|---:|---:|
| **Senath top-20 generation** | all | 200 | 6 (3.0%) | 23 (11.5%) | 28 (14.0%) | 7 (3.5%) / 24 (12.0%) / 29 (14.5%) | 0 | 898 | 0 |
| | GISMo_identity:seen | 105 | 5 (4.8%) | 18 (17.1%) | 23 (21.9%) | 6 (5.7%) / 19 (18.1%) / 24 (22.9%) | 0 | 478 | 0 |
| | GISMo_identity:unseen | 95 | 1 (1.1%) | 5 (5.3%) | 5 (5.3%) | 1 (1.1%) / 5 (5.3%) / 5 (5.3%) | 0 | 420 | 0 |
| | Senath_pair_identity:seen | 100 | 5 (5.0%) | 18 (18.0%) | 23 (23.0%) | 5 (5.0%) / 18 (18.0%) / 23 (23.0%) | 0 | 446 | 0 |
| | Senath_pair_identity:unseen | 100 | 1 (1.0%) | 5 (5.0%) | 5 (5.0%) | 2 (2.0%) / 6 (6.0%) / 6 (6.0%) | 0 | 452 | 0 |
| **GISMo top-50 + Senath rerank** | all | 200 | 3 (1.5%) | 15 (7.5%) | 29 (14.5%) | 3 (1.5%) / 14 (7.0%) / 28 (14.0%) | 0 | 0 | 0 |
| | GISMo_identity:seen | 105 | 2 (1.9%) | 9 (8.6%) | 20 (19.0%) | 2 (1.9%) / 8 (7.6%) / 19 (18.1%) | 0 | 0 | 0 |
| | GISMo_identity:unseen | 95 | 1 (1.1%) | 6 (6.3%) | 9 (9.5%) | 1 (1.1%) / 6 (6.3%) / 9 (9.5%) | 0 | 0 | 0 |
| | Senath_pair_identity:seen | 100 | 1 (1.0%) | 7 (7.0%) | 18 (18.0%) | 1 (1.0%) / 6 (6.0%) / 17 (17.0%) | 0 | 0 | 0 |
| | Senath_pair_identity:unseen | 100 | 2 (2.0%) | 8 (8.0%) | 11 (11.0%) | 2 (2.0%) / 8 (8.0%) / 11 (11.0%) | 0 | 0 | 0 |

### Latency and Throughput Characteristics

Under the saved-output timing contract, execution latencies measured on Kaggle GPU instances are:
- **Senath top-20 generation:** median per-case latency 30.860 s; 90th percentile (p90) 31.515 s.
- **GISMo top-50 reranking:** median per-case latency 54.914 s; 90th percentile (p90) 56.850 s.

A separately recorded full-split projection estimates a two-arm pass at 255.91 mean hours for 10,729 validation rows and 256.33 mean hours for 10,747 test rows; its summed-stratum-p90-cost scenarios (263.41 / 263.84 hours) are planning projections, not executed durations. They imply that any full-split campaign requires a newly frozen, validated partition schedule rather than monolithic job dispatch under a 6-hour cap.

## 5.4 Interpretation and Boundary Truth

The training and inference chain has produced a hash-bound adapter and a recoverable, inspectable validation output bundle. This is meaningful pipeline and execution evidence. On the fixed 200-case validation sample, both arms returned outputs successfully (zero failures), but exact-reference hit rates were modest and the arms trade places across $k$:

1. **No unsupported winner claim:** The observed metrics do not establish one method as superior. Generation yields higher exact-reference hits at $k=1$ (3.0% vs 1.5%) and $k=5$ (11.5% vs 7.5%), while reranking yields higher Hit@10 across the full cohort (14.5% vs 14.0%) and specifically in the unseen cohort (9.5% vs 5.3%). Neither method dominates across all strata and cutoffs.
2. **Vocabulary boundary effect:** The 898 unmapped generation strings (out of 4,000 total candidate slots generated across 200 cases) reflect open-vocabulary generation by the underlying language model. Reranking operates strictly within GISMo's frozen candidate pool (zero unmapped strings). These unmapped strings are preserved honestly as diagnostic non-matches under the primary GISMo identity; they are never artificially normalized or promoted.
3. **No confirmatory test claim:** Because the adapter was selected on validation loss from the same split, these 200 validation cases serve as an internal diagnostic, not an independent test of model generalization. The test partition remains unexposed (`test_split_used: false`).
4. **Single-reference versus culinary validity:** Exact Hit@k measures whether the top-$k$ outputs match the single ground-truth target recorded in the dataset. It does not measure whether generated alternatives are culinarily sound, safe, or acceptable substitutes.

## 5.5 Threats to validity and unresolved gates

1. **Validation reuse / selection optimism:** the adapter was selected on validation loss using the same official split. The 200-case validation comparison is diagnostic, not an independent test.
2. **Single-reference scoring:** exact identity against one recorded target can mark plausible alternatives as misses; this benchmark does not adjudicate culinary validity.
3. **Identity contract:** GISMo canonical identity is primary, while Senath identity is sensitivity only. The two sample-stratum definitions remain separate.
4. **Unmapped generation:** 898 strings lack GISMo identity mapping and affect interpretation; they are preserved, not promoted through fuzzy matching.
5. **Method asymmetry:** generation returns up to 20 beam candidates, while reranking begins from GISMo's top 50. Filtering is frozen, but the arms have different candidate-generation mechanisms and should not be described as identical interventions.
6. **Scale and runtime:** only 200 validation cases ran. Full validation/test jobs have not run; projections depend on sample timing and audited strata and include assumptions described in the runtime report.
7. **Rights and protocol:** Recipe1MSubs redistribution rights remain unresolved in the active record, and full-split execution is gated on protocol/rights/partition authorization. Local availability does not imply permission to upload or redistribute.
8. **Human outcomes:** no blinded human validity, usefulness, trust, preference, or safety result follows from the model metrics or the showcase.
9. **Offline Batch preparation boundary (C2-G2 EARNED):** following Lead Decision T84 (Option 2 canonical-first projection `list[str]` preserving `metadata.raw_aliases`) and Task T510 schedule requirements, the complete 10,747 headline test package was partitioned deterministically into 53 groups (52 batches of 200 plus 1 batch of 99 items). All 10,499 READY cases were serialized into provider-facing OpenAI Batch envelope lines (`POST /v1/responses`) using deterministic, opaque correlation tokens (`c2_[A-Za-z0-9_-]{43}`). All 248 preparation error cases (`TARGET_SLOT_MATCH_MULTIPLE`) were preserved honestly in the denominator with zero provider calls. Mapping from correlation tokens to case identities is strictly preserved in `local_only_mapping.jsonl`. Dual-build verification confirmed 100.0% bit-identical reproduction across all files (`batch_requests.jsonl`, `local_only_mapping.jsonl`, `partition_schedule.json`, `manifest.json`), with all dispatch gates defaulting to fail-closed (`provider_called: false`, `dispatch_authorized: false`, `status: 'PREPARATION_ONLY_NOT_DISPATCHABLE'`). Milestone C2-G2 is EARNED, bringing Contribution C2 to 5/5 complete (100%).

## 5.6 Claim boundary and next evidence

**Supported now:** 
- A locally verified Segment-9 model export is bound into the v4 execution package (`46f5b0f4...`).
- The one-shot recovery receipt and its nested validator record bind the recovered outputs.
- Ingestion receipt `ai/out/senath-validation-benchmark-v1-recovery-20261021/ingestion_receipt.json` (SHA-256 `89fa10c939d073ee10414742c1af0f01dfab4255cae318c6fc9c216c3bb60600`) records diagnostic-only validation of all 200 cases per arm with no method failures.
- Independent recomputation tool `recompute_senath_validation_metrics_independent.py` achieved 100% bit-identical reproduction across all 5 cohorts and both arms (`INDEPENDENT_RECOMPUTATION_MATCH`), recorded in `c2-independent-recompute-20261031.json` (SHA-256 `99f53930c76798bb1bb64d7576e781263df3f37d4165710679a6a7a9af39f818`).
- Primary and sensitivity metrics, unmapped strings, and execution latency are fully documented in matched tables.
- Executable batch transport path verified: Option 2 canonical projection package (10,747 cases = 10,499 READY + 248 PREPARATION_ERROR) partitioned into 53 deterministic groups with zero metadata leakage, verified dual-build bit-identical agreement, and 188 passing tests (C2-G2 EARNED). Contribution C2 reaches 5/5 milestones earned (100% COMPLETE).

**Not supported now:** 
- Reproduction of Senath et al.'s 22.04% result;
- Independent test performance;
- Superiority or final method choice (arms trade places across $k$);
- A general model-quality claim;
- Human validity, usefulness, safety, or product readiness;
- Provider-side acceptance/execution or exact cost/capacity for the T253 candidate;
- Source-transfer permission; or
- Overall Stage-3 completion.

Next evidence must follow the active strategy: resolve rights and protocol gates, freeze the intended method/analysis boundary, prepare and validate a partitioned full-split execution plan within actual resource limits, and only then collect authorized outputs. Preserve validation/test separation, labels locally, fixed denominators, identity sensitivity, unmatched strings, and all failures. A human-validity study and the separate claims audit remain necessary for broader thesis conclusions.

## Provenance and source map

- Frozen model lineage and step-187/374/561 evaluations: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), L44/L12 history and Stage-3 recorded evidence; exact Segment-9 and final manifests cited above.
- Recovered validation manifest: `../../ai/evidence/senath-inference/validation-benchmark-v1-recovered/outputs/senath_validation_benchmark_manifest.json` (SHA-256 `2bc2e500c2cc244d5683ed0a5a593f0dd40b8400344fb00fbe50d6f98959b9ea`).
- Recovered benchmark state: `../../ai/evidence/senath-inference/validation-benchmark-v1-recovered/outputs/senath_validation_benchmark_state.json` (SHA-256 `99cb76adc6f4b6e59ae7c6760e5f05e1358177c896b8d7f5247e47365482d3b6`).
- Raw recovered outputs: `../../ai/evidence/senath-inference/validation-benchmark-v1-recovered/outputs/senath_validation_benchmark_raw.jsonl` (SHA-256 `75024e51d73b7379fdcb006af9308306f72d24ea8ad4e6b2efd48e75ddfdea96`).
- One-shot recovery receipt: `../../ai/evidence/senath-inference/validation-benchmark-v1-recovered/recovery_receipt.json` (SHA-256 `90da69d9157203bef144ef6cf191d75398c593212fc6255fb6ca02a4e6686b81`).
- Standalone ingestion receipt: `../../ai/out/senath-validation-benchmark-v1-recovery-20261021/ingestion_receipt.json` (SHA-256 `89fa10c939d073ee10414742c1af0f01dfab4255cae318c6fc9c216c3bb60600`).
- Independent recompute output: `../../ai/out/milestone-reconciliation/c2-independent-recompute-20261031.json` (SHA-256 `99f53930c76798bb1bb64d7576e781263df3f37d4165710679a6a7a9af39f818`).
- C2 milestone reconciliation brief: `../../ai/out/milestone-reconciliation/brief-c2-validation-benchmark-20261031.md` (SHA-256 `4921f6c73f3444477f60987db75c2b142fbd9ffdfe42307451a66be7a8793a78`).
- C2 milestone reconciliation report: `../../ai/out/milestone-reconciliation/c2-report-v1.md` (SHA-256 `f363d941a8bb9c69e00df6d4c3cc7ea6417e613e5fdedbb3b9d171d95281ed66`).
- Local inventory recheck: `../../ai/evidence/senath-inference/validation-benchmark-v1-recovery-inventory-recheck-20261021.md` (SHA-256 `66f71acf620b5ac7ac7948b3e3a9f6ef8cffce4bda0e1ed64bbd4ff8131f0ad3`).
- Runtime method and assumptions: [`full-split-runtime-projection-v1-20261020.md`](../../ai/evidence/senath-inference/full-split-runtime-projection-v1-20261020.md).
- T253 local Batch transport-candidate report: `../../ai/out/T253/report.md` (SHA-256 `eb109be72775956c6505e0c95125c3928c09aa8a02a414cd5decbfdb01aff23b`).
- Current limitations and larger L12/L44 status: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§13, 17, 21.
