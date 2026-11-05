# Iron Chef Evaluation Protocol v1

Status: HUMAN-selected for prospective use on 2026-10-21. The active mission records the SHA-256 of these exact bytes before any new test-label access or scored run. This does not relabel prior runs or make exposed test results confirmatory.

Authority and decision provenance: [`research/authority/IRON-CHEF-AUTHORITY-ADOPTION-20261021.md`](../authority/IRON-CHEF-AUTHORITY-ADOPTION-20261021.md) and the preserved consultant v2 source cited there.

## 1. Purpose and evidence boundary

The midterm target is accepted C2, C3, and C5 floor outcomes plus an evidence-bound working showcase. The ideal C1–C5 destination remains. Benchmark claims describe the recorded dataset and protocol; they do not establish food safety, clinical validity, human usefulness, or product desirability without the corresponding evidence.

The test partition is `EXPOSED`. Log every test-label access. Use validation for method choice, thresholds, and tuning. Do not change the frozen method based on test outcomes and then report confirmatory test evidence. Prior runs retain the labels and exposure status under which they were produced.

## 2. Freeze A: acceptability audit

| Decision | Frozen rule |
|---|---|
| Primary estimand | Conditional rescue `P(V=1 | H=0)` for the unseen GISMo-identity stratum, primary GISMo arm, strict valid@5. Joint `P(V=1, H=0)` is secondary. |
| Decision threshold | Support H1 only when the stratified-design 95% lower bound is at least 0.15; refute only when the upper bound is below 0.15; otherwise inconclusive. |
| Gold judgments | Label gold blind like every candidate. Preserve raw A/C/N/U judgments immutably; adjudication is a separate versioned field. If both raters label gold N, report “reference disputed” separately. Retain benchmark Hit unchanged. |
| Sample | `k=5`; allocation 50/50/25/25 across unseen-profiled, unseen-unprofiled, seen-profiled, seen-unprofiled. Use verified frame weights `W_h=N_h/n_h`, stratified ratio estimation, and case-cluster bootstrap with 2,000 resamples and seed `20261021`. Refuse undersupplied strata. Freeze frame counts, seed, and weights before drawing the sample. |
| Primary identity | Pinned GISMo vocabulary, exact match, raw strings retained. Senath normalization and reviewed aliases are named sensitivity analyses only. Report unmatched rate per arm and flag rates above 5%. |
| Human audit | Proceed at T1 kappa >= 0.5. Claim “reliable agreement” only if strict-kappa cluster lower bound >= 0.6; otherwise call the audit exploratory. Use two-sided Wilson intervals for proportions and a one-sided exact bound for zero events, with n stated. If fewer than 20 valid bootstrap draws remain, report the interval unavailable. |

## 3. Freeze B: calibration and judge

### 3.1 Partitions and leakage control

Descriptive oracle strata are seen/unseen by both-profiled/not-profiled. They depend on gold and are descriptive only.

Deployable calibration uses request-time features only:

- Baseline-0: one all-input cell.
- Partition-1: `(target ingredient occurs as a source in train: yes/no) x (target has a FooDB profile: yes/no)`.
- Count cell support from the frozen validation frame before viewing outcomes. A cell below 200 validation cases falls back to Baseline-0 under this fixed rule. This fallback supports a marginal baseline statement, not a conditional guarantee for that cell.

For any new fit or method choice, partition validation recipes deterministically by recipe identity into disjoint development and calibration sets before inspecting outcomes (seed `20261021`, 30% development / 70% calibration). Keep every case for a recipe in one set. Record the assignment manifest and hash. Require at least 200 calibration cases for a reported cell guarantee; otherwise report `no_guarantee` for that cell. Existing historical outputs may be described under their original procedure, not relabeled as satisfying this split.

Validation may support judge development, calibration, and method choice only on their assigned portions. Freeze judge family, version, and prompt before test scoring. Test remains exposed benchmark evaluation only. Future private authored cases are confirmatory only after ethics clearance, consent, protocol freeze, and evidence that no model saw them earlier.

### 3.2 Set construction and status

Pipeline order: candidate pool -> hard allergen filter -> rank -> truncate to M -> top-t-hat set. The score is the rank of the first acceptable candidate within M; otherwise infinity. Infinity is never covered. Missing calibration entry means `no_guarantee`; an empty filtered pool means `abstain`. Denominator includes all cases and failures.

KS and gold-rank comparisons are diagnostics only. No test-driven cell merging or reweighting. Weighted conformal is excluded from v1.

Use alpha `{0.10, 0.20, 0.30}`, primary `0.20`; M=20 primary for arm parity; M=50 is GISMo sensitivity only. Status truth table:

| Status | Rule |
|---|---|
| `ok` | n >= 200 and finite t-hat <= 10 |
| `uninformative` | finite t-hat > 10 |
| `vacuous` | t-hat is infinity |
| `no_guarantee` | n < 200 or calibration artifact unavailable/mismatched |
| `abstain` | filtered candidate pool is empty |

Report every cell, including empty and fallback cells. Do not claim conditional coverage from Baseline-0 fallback.

### 3.3 Judge acceptance

The judge must differ in model family from every arm it judges. It is usable only if judge-versus-adjudicated kappa point estimate >= 0.6 and lower bound >= 0.5, leniency delta <= 10 percentage points per cell, instability <= 10%, and invalid output <= 2%. Evaluate these gates on the audit set. Inferential claims require the teammate's methods review.

Two independent C3 statistical implementations must agree exactly on synthetic adversarial fixtures and real validation cells. Agreement does not replace source, identity, or independent-oracle checks; no named script is a prerequisite if absent.

## 4. Arms and cohorts

For each headline comparison, use identical case IDs across arms; disclose any fallback subsample and compare all included arms on that same cohort. Report Hit@1/5/10 and MRR overall, by oracle stratum, and by Partition-1 cell, with Wilson and recipe-cluster intervals, unmatched-rate flags, and failures retained in denominators. When only top-20 ranks exist, call the metric `MRR@20` (truncated); call it full MRR only when complete catalogue ranks were actually computed. Include the cost-of-trust figure for C3.

- GISMo released checkpoint top-20.
- Lookup-frequency top-20; ties sort count descending then graph ID ascending. Historical tie equivalence remains unknown.
- GPT `gpt-4o-2024-08-06`, top-20, temperature 0, one recorded attempt per case. Run full test if conservative preflight fits within 80% of remaining authorized OpenAI budget; otherwise use a seeded `20261021` stratified 2,000-case test sample across Partition-1 x oracle strata. This is a cost-only fallback. Report matched-case comparisons; do not headline unmatched cohorts as direct comparisons.
- Run GISMo and lookup on full validation (10,729) and exposed test (10,747), subject to current artifact/receipt verification. Preserve actual cohort counts if inputs differ.
- Retained-best Mistral adapter is diagnostic only, gated by the bounded DR-04 pipeline checks. No further Mistral training.
- Allergen route: existing n=208; exact bounded intervals. n>=299 is stretch only. Label list-based violations as mechanical policy checks, not clinical truth.

## 5. Graph and provenance

Build `graph_eval_v4` edges only from training-row provenance. A pair that independently appears in train and validation/test is legitimate overlap. The invariant is zero graph edges whose source row is validation or test; do not delete legitimate train-supported pairs to force pair-disjointness.

`train_pair_count` is an integer. `seen = train_pair_count > 0`. “Known node” and “seen pair” are distinct. If calibration is unavailable or incompatible, calibration status is null/unavailable and the system abstains; never fabricate node confidence.

For graph construction, verify source, row, split, artifact hash, and transformation identity. Keep product-graph enrichment sources separately attributed from evaluation-train edges. Do not invent edges or hide isolates to meet a node-count target.

## 6. Fixed resource and data envelope

| Resource | Maximum authorization |
|---|---|
| OpenAI | USD 100 total; `gpt-4o-2024-08-06`; at most 25,000 calls |
| Google | USD 30 total; `gemini-2.5-flash`; at most 2,000 calls |
| Kaggle GPU | 40 GPU-hours total across inference and fresh HGAT; through 2026-11-20; max six hours/job, one GPU job/account, max 200 cases/partition |
| Rented GPU | USD 50 total |

Before each paid or remote job, record estimate, reservation, owner, input hashes, and output location. The sum of spent and reserved value cannot exceed a cap. Retain a reservation for uncertain calls until reconciled; never duplicate an uncertain launch. Provider billing controls are not treated as instantaneous hard caps.

Authorized API transfer is limited to recipe title, ingredient list, and target ingredient. Do not transmit user health/allergen profiles, labels, candidate pools, or case frames. Local and private Kaggle processing is authorized within the source/data rights actually accepted by the HUMAN. Publication is limited to allowed aggregates, metrics, figures, and short excerpts; raw rows, labels, pools, FooDB bytes, keys, and weights are excluded.

## 7. Result language and change control

Report coverage of the recorded substitute when labels are recorded gold; reserve human-valid coverage for an accepted human-valid reference. Report all-abstention outcomes honestly, separately from live usefulness. An absent or incompatible artifact, calibration entry, model output, or evidence receipt cannot become success by fallback narration.

For the C5 floor, build `graph_eval_v4` twice byte-identically, pass the redefined T-01 and T-02 through T-12 fixtures, keep the Explorer feature flag off by default, and have the endpoint return the applicable C3 cell status and abstain for non-`ok` calibration states. A graph target of 5,000 real ingredient nodes is reported as achieved only if the source-backed inventory actually reaches it; never pad counts. Capture evidence against the source-built runtime artifact. The Sprint 4 event is a 12-minute live demonstration; the consultant's five-minute recorded walkthrough is a separate evidence artifact. Neither substitutes for the other's acceptance.

No test-informed change modifies v1. Propose a new version and label affected outcomes exploratory. Institutional ethics rulings, participant consent, human review, current account balances, external execution receipts, and live human rehearsal remain separate facts requiring their own evidence.
