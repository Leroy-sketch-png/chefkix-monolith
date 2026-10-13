# IRON CHEF v4: LEAD BACKLOG
> **AUTHORITY:** `academic_vision_and_strategy.md` (canonical SSoT) is the single source of truth. This backlog is derived from §3 (C1–C5), §17 (Work Packages), §18 (Schedule), §19 (Risks), and §16 (Protocol Resolutions). When they conflict, the strategy file wins.
> **MIGRATION:** Updated from v3 on 2026-10-12 (LEAD v113). v3 "four trained models" and "nine contributions" structure retired. See Appendix Y of SSoT for superseded v3 text.

## Segment 8 status revalidation — 2026-10-13

- At `2026-10-13T05:59:11.437826Z`, the separately vaulted `tnvtrung` wrapper invoked the official Kaggle CLI and returned Segment 8 version 1 `COMPLETE`. The already downloaded canonical output was revalidated locally at exact step 514, `STAGE3_SEGMENT_8_COMPLETE`, with the 12-file checkpoint inventory, eight-file retained-best inventory and manifest SHA-256 `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`; no new output download was attempted.
- The only evaluation records remain the unchanged step-187 and step-374 evaluations. This observation establishes status and artifact/provenance continuity only—not inference, scoring, model quality or overall Stage-3 completion. Segment 9 remains unprepared and unauthorized pending human direction.
- Durable receipt: [`segment8_status_revalidated.json`](../ai/out/iron-chef-lead-v99/action-receipts/20261013T055911.437826Z-segment8_status_revalidated.json). Canonical details: [`SESSION.md`](../SESSION.md) and [`academic_vision_and_strategy.md`](../academic_vision_and_strategy.md).

## LEAD v115 — Segment 8 artifact boundary verified (2026-10-13)

- One official secondary-account Kaggle output transaction validated Segment 8 at exact step 514 and terminal state `STAGE3_SEGMENT_8_COMPLETE`: the exact 12-file checkpoint inventory/hashes, pinned Segment-7→1 and Stage-2 ancestry, frozen source and selection identities, manifest integrity, and retained-best continuity all passed.
- Segment-8 manifest SHA-256: `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`; normalized launched-source SHA-256: `92c25e0daa6886e75ffd0360e10a543fa0a6467fc6a7b0cb71c5dc5a611bd176`. Manifest and trainer state contain only the unchanged step-187 (`0.19342221319675446`) and step-374 (`0.20272061228752136`) evaluations; step 514 adds no evaluation. The eight-file retained-best inventory matches Segment 7, including adapter model SHA-256 `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814` and best loss `0.19342221319675446`.
- Root evidence: `ai/out/iron-chef-lead-v99/secondary_stage3_segment8_advancement.json`; verified output: `ai/out/iron-chef-lead-v99/segment-8-output/`. Artifact/provenance validation does not establish inference, scoring, model quality, or overall Stage-3 completion. Segment 9 is not prepared or launched; its identity/package contract awaits human direction.

## LEAD v114 — Segment 7 verified; Segment 8 launched (2026-10-12)

- **L44 progress boundary:** one official secondary-account Kaggle transaction downloaded and validated Segment 7 at exact step 444, terminal state `STAGE3_SEGMENT_7_COMPLETE`, exact 12-file checkpoint inventory/hashes, recursively pinned Segment-6→1 and Stage-2 ancestry, frozen source/data selections, and manifest integrity. Segment-7 manifest SHA-256: `d3406733ca38209df9d25b7dd1883cc20a2659611573588f7c32d27690d15481`.
- The manifest preserves exactly the unchanged step-187 (`0.19342221319675446`) and step-374 (`0.20272061228752136`) evaluation entries. The eight-file retained-best adapter and all hashes remain identical to Segment 6; model SHA-256 is `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`.
- Only after those gates passed, Segment 8 was built for `tnvtrung` with the exact Segment-7 manifest pin, the same private bridge dataset, and Segment 7 as sole kernel predecessor. Package validation passed; one official-CLI launch was positively acknowledged as version 1. Script SHA-256 `2c97fce2424b992cd76a65c1c40de283847a805ff896145b23d14d782d825af7`; metadata SHA-256 `375a941232b12fa343a4e89eb34ac32c9f27884ac62b41ee39c8d56d8c840f88`. Latest secondary status observed `RUNNING` between `2026-10-12T11:36:38Z` and `11:36:40Z` toward step 514; no Segment-8 output has been validated.
- Evidence: root [`secondary_stage3_segment7_advancement.json`](../ai/out/iron-chef-lead-v99/secondary_stage3_segment7_advancement.json), transactional-ingestion and launch receipts under `ai/out/iron-chef-lead-v99/action-receipts/`. Stage-3 completion, inference, scoring, and model quality remain unproven; loss/evaluation/status are not quality evidence.

## Verified execution update — 2026-10-12

- **L13′ COMPLETE — scope edit only.** The former new-GNN backlog wording is replaced by a shift-aware selector over frozen scorers, with its method and guarantee boundary in SSoT [§8](../academic_vision_and_strategy.md#s8). This does **not** close scientific work package L13: no accepted model is established.
- **L37 COMPLETE — deterministic strata and benchmark-conditional top-1 intervals.** The frozen v26 evaluator was independently reconstructed (`result_validation_report.json` PASS); the supplemental 2,000-replicate recipe-cluster bootstrap reports Hit@1 and 95% percentile intervals for overall, repeated/unseen, and four evidence-missingness strata. Test denominator is 10,747; repeated/unseen is 6,866/3,881. This is conditional uncertainty for the fixed benchmark, not a population/exchangeability guarantee. Optional endpoint-frequency split was not run. Evidence: [`l37_cluster_intervals.json`](../ai/out/iron-chef-lead-v102/l37_cluster_intervals.json), SHA-256 `351230465cbf51a41378f42fd1c12e21ea936ebfa2bd616c31d76572460c9283`, deterministically reproduced byte-for-byte; generator SHA-256 `c26eed23b3c11c9bd989c938310150511e518a8ea3f57d4a08fd8610c5464ccb`; frozen evaluation SHA-256 `293f0c9952d98d389bb3b7d23ee4f1f04b6ad675914e361de9102806f5539ea8`; independent reconstruction report SHA-256 `45492d30acdf46ef26fb443140a8bec2adf1c01e12e8e1049112a27029f217d5`.
- **L42 advanced, not closed.** Official CiST program evidence, the hash-recorded full arXiv:2409.08792 text, a citation-chased ingredient-substitution survey plus Shirai et al. DIISH full text, full-text screens of KDD 2025 LPShift and the NeurIPS 2023 link-prediction evaluation paper, and an author-repository screen of conformal LLM recipe-rating work now bound direct versus adjacent prior art more precisely. The HUMMUS rating-interval lead challenges broad “first conformal prediction for LLM recipe recommendation” language, but is not ingredient-substitution selection, unseen-pair Hit@1, or selective abstention; paper acceptance/full text remain unverified. The graph papers support benchmark-rigor concerns, not ingredient or calibrated-selective-prediction evidence; the exact calibrated-selective candidate remains `UNKNOWN`. The survey’s generic query and 28-work snowball are not a complete search, its omission of separately known Senath work demonstrates a coverage limit, and independent adjudication remains absent. IEEE full text for the role-aware paper, complete hashed query/citation funnel, formal coding, and independent rubric adjudication remain missing. See [SCOPE follow-up](../SCOPE.md#l42-role-aware-followup); no novelty claim is accepted.
- A 2026 *Electronics* paper reports allergen-aware multi-objective **recipe ranking** with a substitution bonus (Wang & Wang, DOI `10.3390/electronics15122628`). Its official 25-page publisher PDF is now screened (SHA-256 `98D9550151509D61B28E8A07807361653BD4C449A2CAC15A647BE33B67D83413`): the displayed algorithm ranks recipes and does not specify ingredient-substitute generation/selection or a validated substitute-evidence procedure. Authors report N=20 home cooks from one recipe source, retroactive replay for external baselines, and no clinical validation; reported metrics were not reproduced. This is direct-adjacent ranking/scoring prior art, not the exact selective ingredient-substitution claim. See root SCOPE L42; formal coding/adjudication and L42 remain open.
- The publisher HTML returned HTTP 429, but the official static PDF was retrieved and screened; acquisition hash and sampled-page visual review are recorded in root SCOPE. The author-affiliated Yamaguchi University publication list identifies an earlier IMCOM 2026 version (Wang, Ding & Wang, pp. 1–6); Crossref now identifies DOI `10.1109/imcom69009.2026.11360864` with matching bibliographic metadata. OpenAlex indexes an abstract describing an allergen-safety score involving ingredient substitution, nutrition/NIMA scores, and weighted recipe ranking; it is abstract/catalog evidence only, not a full-text screen. Its record reports closed access/no repository full text, not proof no copy exists. IEEE Xplore returned HTTP 202 with an empty body, so conference full text remains unreviewed. Treat journal/conference versions as one linked prior-work line, not independent reviews.
- **Segment 7 status boundary:** the separately vaulted secondary Kaggle wrapper observed `RUNNING` at `2026-10-12T10:50:21Z`. The earlier default-CLI HTTP 403 is credential-path-specific, not evidence that this secondary path is inaccessible. This is status-only: no Segment-7 output download/validation or step-444 verification, and no Segment-8 build/launch.

## LEAD v107 execution update — secondary ingestion transactionalized (2026-10-08)

Secondary Stage-3 ingestion is now one fail-closed operation. It performs no transport for `QUEUED`/`RUNNING`, receipts terminal blockers, permits exactly one isolated attempt after `COMPLETE`, preserves failed bytes and hashes, refuses invalid canonical evidence, revalidates valid canonical evidence, and promotes only after the full segmented validator passes.

Nine focused tests pass and the combined ingestion/successor regression is 16/16, including post-promotion receipt-failure truth. A live authenticated Segment-5 probe returned `RUNNING`, action `NONE`, zero transport, and zero receipts. This checks off ingestion-boundary hardening only; exact step 327, later evaluation, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v106 execution update — secondary successor construction hardened (2026-10-08)

Secondary Stage-3 successor construction is now transactional and evidence-derived. It recursively validates predecessor artifacts and exact manifest identity, binds prior local source to the runtime manifest, rechecks frozen Stage-2/boundary/evaluation/exact-resume contracts, stages in isolation, validates full script and Kaggle metadata, and promotes only after every gate passes. It refuses existing-package overwrite.

Seven focused tests reject validator identity, runtime source, upstream pin, kernel predecessor, overwrite, and compile-failure/partial-publication paths. The live Segment-5 package independently revalidates exact launched script/metadata hashes and remains `RUNNING` toward step 327. This checks off successor-construction hardening only; remote step 327, later evaluation, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v105 execution update — secondary Stage 3 step 257 verified (2026-10-07)

Secondary Segment 4 is checked off as transactionally verified at exact step 257/561. Terminal state is `STAGE3_SEGMENT_4_COMPLETE`; manifest SHA-256 is `aa9e6f98...0fd9`; all 12 required checkpoint files/hashes reconstruct with exact Segment-3/Segment-2/Segment-1 and Stage-2 ancestry plus frozen data selections. The sole evaluation remains the frozen step-187 record and all eight retained-best files are byte-identical to Segment 3; no evaluation or best adapter was substituted.

Only after those gates passed, Segment 5 was derived with exact step-257 pin `aa9e6f98...0fd9`, the same private bridge dataset, and Segment 4 as its sole kernel predecessor. Prelaunch lookup proved absence; Kaggle acknowledged version 1; pulled source/dependencies reconstruct; authenticated state is `RUNNING` toward step 327. Step 327, the step-374 evaluation, remaining Stage-3 segments, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v104 execution update — secondary Stage 3 step 187 verified (2026-10-07)

New explicit human direction authorized one fresh isolated Segment-3 transfer after v103 preserved the failed bytes. The new transport validated before promotion: terminal `STAGE3_SEGMENT_3_COMPLETE`, exact step 187/561, manifest `dd974c74...efef`, all 12 checkpoint files/hashes, exact Segment-2 and Stage-2 ancestry, frozen selections, the first frozen evaluation, and all eight retained-best files. The prior partial directory remains preserved and rejected.

Segment 4 pins exact manifest `dd974c74...efef`, retains the private verified-input dataset, and names Segment 3 as its sole kernel predecessor. Complete owner inventory proved absence; official Kaggle CLI acknowledged version 1; pulled remote source and dependencies reconstruct; authenticated state is `RUNNING` toward step 257. Eval loss `0.19342221319675446` is telemetry, not model-quality evidence. Step 257, remaining Stage-3 segments, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v103 blocked update — Segment 3 incomplete transport (2026-10-07)

Secondary Segment 3 reports remote `COMPLETE`, but official-CLI transport failed with `IncompleteRead(199249616 bytes read, 169892568 more expected)`. The preserved isolated directory contains 11 partial files and 373,160,325 bytes: eight apparent retained-adapter files, only three checkpoint files, a zero-byte checkpoint adapter, and no segment manifest. Canonical evidence was not created or overwritten.

No retry was made, no Segment-3 validation claim was emitted, and Segment 4 was neither built nor launched. Stage-3 remains verified only through exact step 140/561. Step 187 evaluation, retained-best integrity, Segment-3 ancestry reconstruction, remaining Stage-3 execution, inference, scoring, and model quality remain blocked pending a separately authorized complete transport attempt.

---

## LEAD v102 execution update — secondary Stage 3 step 140 verified (2026-10-06)

Secondary Segment 2 is checked off as transactionally verified at exact step 140/561. Terminal state is `STAGE3_SEGMENT_2_COMPLETE`; manifest SHA-256 is `2324c21e...03bd`; all 12 required checkpoint files and hashes reconstruct with exact Segment-1 manifest `ed1e3db...d991d`, Stage-2 manifest `435f02e7...6b72`, frozen base revision, data hashes, and selection hashes. Pulled remote source matches the launched source after LF normalization.

Only after those gates passed, secondary Segment 3 was built with exact step-140 pin `2324c21e...03bd`, the same private bridge dataset, and Segment 2 as its sole kernel predecessor. Complete owner inventory proved absence; the official current Kaggle CLI positively acknowledged version 1; pulled remote source and dependency metadata reconstruct; authenticated state is `RUNNING` toward the first evaluation boundary at step 187. Step 187, retained-best adapter integrity, remaining Stage-3 segments, inference, scoring, and model quality remain unchecked.

---

## LEAD v101 execution update — OFF taxonomy risk partition (2026-10-02)

The complete OFF snapshot's 3,377-tag union is now partitioned against hash-pinned official taxonomy bytes. Twenty-three exact positive identities cover 760,877/767,780 occurrences (`99.100914%`); official `en:none` is isolated at 564 occurrences; 17 same-language alias candidates cover 32 and remain review-only; 3,336 unrecognized values dominate identity count but cover only 6,307 occurrences (`0.821459%`). The queue is occurrence-ranked and binds snapshot manifest `5caa20cf...1dde`, derived snapshot `807fe58e...9c68`, and taxonomy `6fb38bfb...d153f`.

Every tag and occurrence reconstructs; 8/8 semantic substitutions reject and six focused tests pass. A v100 precursor remains rejected for incomplete Unicode-ligature normalization; v101 repairs it. This checks off deterministic risk partitioning only. Current-taxonomy timing differs from the frozen snapshot, and alias acceptance, qualified adjudication, verified negatives, clinical validity, production serving, legal review, and user outcomes remain unchecked.

---

## LEAD v99 execution update — secondary Stage 3 step 70 verified (2026-10-02)

Secondary Segment 1 is checked off as transactionally verified at exact step 70/561. Terminal state is `STAGE3_SEGMENT_1_COMPLETE`; manifest SHA-256 is `ed1e3db4...d991d`; all 12 required checkpoint files and hashes reconstruct with exact Stage-2 ancestry, frozen base revision, data hashes, and selection hashes. Pulled remote source is byte-identical to launched SHA-256 `d583e92f...a3af`; runtime source identity matches its LF-normalized form.

Only after those gates passed, secondary Segment 2 was pinned to exact manifest `ed1e3db4...d991d`, bound to the same private bridge dataset and Segment 1 as its sole kernel predecessor, and launched once. Complete owner inventory proved absence; Kaggle accepted version 1 with zero invalid sources and authenticated state is `QUEUED` toward step 140. Step 140, remaining Stage-3 segments, retained-best adapter integrity, inference, scoring, and model quality remain unchecked.

---

## LEAD v98 execution update — secondary Kaggle capacity activated (2026-10-01)

The new `tnvtrung` bearer credential is durably isolated from the preserved `phanphutho` legacy key under user-only ACLs and uses official Kaggle CLI 2.2.4. Because `tnvtrung` cannot read private `phanphutho` kernels, a private 11-file bridge dataset was created and remotely inventoried against exact Stage-2 manifest `435f02e7...6b72`, all eight adapter files, and frozen train/validation hashes.

Secondary Segment 1 preserves exact script SHA-256 `d583e92f...a3af`; only owner/input metadata changed. Complete owner inventory proved it absent before Kaggle accepted version 1 with zero invalid sources. Authenticated state is `RUNNING`. This checks off the governed Segment-1 launch only; exact step 70, checkpoint integrity, Stage-3 completion, inference, scoring, and model quality remain unchecked.

---

## LEAD v97 execution update — Stage 3 launch boundary hardened (2026-10-01)

The segmented Stage-3 executable now pins base script SHA-256 `88d79461...c8c6` across builder, validator, and ingester. Only the exact weekly-GPU-quota response is preserved as a retryable non-launch; malformed acknowledgements, authentication/transport failures, and other runtime errors propagate fail closed. Completed segments must reconstruct the exact expected 12-file checkpoint inventory in addition to every hash and exact-resume state.

All nine packages rebuild byte-identically, static validation remains `PASS`, 8/8 semantic attacks reject, and eight focused tests pass. Authenticated Segment 1 remains `NOT_CREATED`; no second push occurred. Stage 3, inference, scoring, and model-quality boxes remain unchecked.

---

## LEAD v96 execution update — segmented Stage 3 ready; GPU quota gate (2026-10-01)

The canceled 561-step monolith is replaced by nine exact-resume packages at `70, 140, 187, 257, 327, 374, 444, 514, 561`. The longest span is 70 steps (about 8.2 hours at the observed rate), while evaluations stay exactly at 187/374/561. Every successor binds the exact predecessor manifest and reconstructs checkpoint, optimizer, scheduler, RNG, trainer, data-selection, Stage-2, and retained-best state. Nine packages compile; 8/8 semantic attacks reject and five focused tests pass.

Kaggle rejected the authenticated Segment-1 push with `Maximum weekly GPU quota of 30.00 hours reached.` Segment 1 remains `NOT_CREATED`; this is not a failed training run or a launch. Recovery is package-ready but quota-blocked, and inference remains blocked.

---

## LEAD v95 execution update — Stage 3 canceled and inference blocked (2026-10-01)

Post-DPO SFT version 1 ended `CANCEL_ACKNOWLEDGED` with no failure message and an empty remote file inventory. The preserved log SHA-256 `a30a54f...105dc` reaches optimizer step 101/561 at `11:49:56`; the last periodic record is epoch `0.53`, loss `0.1519`. At roughly 421 seconds per step, the monolithic 561-step/three-epoch package cannot fit Kaggle's execution window and canceled before the first epoch evaluation/save boundary. No manifest, checkpoint, or Stage-3 adapter exists.

No retry was issued and inference remained cryptographically unmaterialized, so it did not launch. Stage-3 completion stays unchecked. Recovery requires a materially segmented package chain with exact optimizer/scheduler/RNG resume and retained-best semantics; the partial log is failure evidence, not model quality.

---

## LEAD v94 execution update — inference successor made fail-closed (2026-09-30)

Inference can no longer launch against a merely plausible Stage-3 artifact. Its package starts with an unmaterialized identity sentinel and refuses runtime execution. After Stage 3 completes, the transactional ingester must validate exact Stage-2 ancestry, frozen dataset/settings, and every adapter hash, atomically materialize the exact Stage-3 manifest SHA-256, rebuild the downstream manifest, and rerun package validation before one launch. Static gates pass and 17 focused tests cover identity/settings substitution, transactional promotion, remote-state refusal, and launch acknowledgement.

Stage 3 remains running. This clears successor-provenance preparation only; completed post-DPO SFT, inference output, matched scoring, and model quality remain unchecked.

---

## LEAD v93 execution update — exact post-DPO handoff and Stage 3 running (2026-09-30)

The downstream package cluster no longer references retired DPO Segment 6. Builder, validator, Stage-3 runtime, and transactional ingester now bind exact verified step-261 manifest SHA-256 `435f02e7461992a53d1e4e4882ec7d8bb3da5a3b19d571a2164f382e050f6b72`, Segment 9, its Segment-8 dependency, and the verified v69 terminal evidence. Static validation passes seven scientific checks and 7/7 adversarial mutations; 15 focused tests pass. Kaggle accepted post-DPO SFT version 1 with zero invalid sources and authenticated state `QUEUED`.

This checks off the retained-adapter handoff and Stage-3 launch only. Stage-3 completion, adapter integrity, 10,747 frozen predictions, matched scoring, paper reproduction, and M2 model quality remain open.

---

## LEAD v92 execution update — segmented DPO complete and verified (2026-09-30)

Final Segment 9 version 3 completed and transactionally validates exact step 261/261 with 12 checkpoint files, exact Segment-8 predecessor pin `54ea62d6...c645`, final manifest `9c63d9ed...159e`, terminal state `STAGE2_COMPLETE`, and final evaluation loss `1.1670465704938238e-09`. All nine packages now reconstruct end-to-end; canceled long intervals remain preserved and rejected. This checks off segmented DPO execution only. Near-zero loss remains shortcut-confound evidence, not quality proof. Post-DPO SFT, frozen predictions, matched scoring, paper reproduction, safety, rights, and product value remain open.

---

## LEAD v91 execution update — DPO step 240 verified; final Segment 9 running (2026-09-29)

New Segment 8 version 1 completed and transactionally validates exact step 240/261 with 12 checkpoint files, exact Segment-7 predecessor pin `258a5ded...5635`, and manifest `54ea62d6...c645`. Step 240 is transport-only; the final evaluation remains at 261. Final Segment 9 was rebuilt with the exact step-240 pin and Kaggle accepted materially changed version 3 with zero invalid sources, queued toward step 261. Only 240/261 steps are verified; do not check off DPO or begin post-DPO SFT until exact step-261 artifacts, final evaluation, and adapter integrity transactionally validate.

---

## LEAD v90 execution update — shortened final recovery running (2026-09-29)

The canceled 43-step final interval is now split into `218→240→261`, adding only a transport checkpoint while preserving all optimizer steps and evaluations at 87/174/261. Verified packages 1-7 have zero content diff. The nine-package build validates `PASS`, rejects 12/12 semantic corruptions, and passes 13 focused tests. New Segment 8 version 1 was launched once with exact step-218 manifest pin `258a5ded...5635`, zero invalid sources, and authenticated state `RUNNING` toward step 240. Only 218/261 steps are verified; do not check off Segment 8 or launch the final package until exact step-240 artifacts transactionally validate.

---

## LEAD v89 execution update — final Segment 8 canceled (2026-09-29)

Final Segment 8 version 2 ended `CANCEL_ACKNOWLEDGED` with no failure message or remote files. The failed state is preserved and no retry was issued. Its 43-step 218→261 interval repeats the long-duration failure shape of earlier canceled packages, while 22/21/22-step recoveries completed. Only 218/261 steps remain verified. Exact step 261, final evaluation, final adapter, completed DPO, and model quality are absent; post-DPO SFT remains gated. Require a materially shorter validated final recovery boundary before any retry.

---

## LEAD v88 execution update — DPO step 218 verified; final segment running (2026-09-28)

Segment 7 version 1 completed and transactionally validates exact step 218/261 with 12 checkpoint files, exact Segment-6 predecessor pin `ebee8452...309e`, and manifest `258a5ded...5635`. Step 218 is transport-only; evaluations remain frozen at 87/174/261. The fully-pinned validator mutation was corrected to remain genuinely adversarial; 12/12 semantic corruptions reject and 13 focused tests pass. The final Segment 8 package was rebuilt with the exact Segment-7 pin and Kaggle accepted materially changed version 2 with zero invalid sources, queued toward step 261. Only 218/261 steps are verified; do not check off final DPO or begin post-DPO SFT until exact step-261 artifacts and evaluation transactionally validate.

---

## LEAD v87 execution update — DPO step 196 verified; Segment 7 running (2026-09-23)

Shortened Segment 6 version 2 completed and transactionally validates at exact step 196/261 with 12 checkpoint files, exact Segment-5 predecessor pin `7bbf682c...9340d`, and manifest `ebee8452...309e`. The step-196 checkpoint is transport-only; evaluations remain frozen at 87/174/261, and the last boundary loss is not model-quality evidence. Segment 7 version 1 was launched once with the exact Segment-6 manifest pin, zero invalid sources, and authenticated state `RUNNING` toward step 218. Only 196/261 steps are verified; do not check off Segment 7 or launch the final package until exact step-218 artifacts transactionally validate.

---

## LEAD v86 execution update — shortened Segment 6 recovery running (2026-09-22)

DPO Segment 6 version 1 remains preserved as canceled without artifacts. The retry changes transport boundaries only: the chain is now eight packages at `44, 87, 131, 153, 174, 196, 218, 261`, preserving all optimizer steps and evaluations at 87/174/261. Verified packages 1-5 have zero content diff. The package passes validation, rejects 12/12 semantic corruptions, and passes 13 focused tests. Kaggle accepted Segment 6 version 2 with exact step-174 manifest pin `7bbf682c...9340d`, zero invalid sources, and authenticated state `RUNNING` toward step 196. Only 174/261 steps are verified; do not check off Segment 6 or launch Segment 7 until exact step-196 artifacts transactionally validate.

---

## LEAD v85 execution update — Segment 6 canceled; shorter recovery required (2026-09-22)

DPO Segment 5 version 1 remains transactionally verified at exact step 174/261 with 12 checkpoint files, manifest `7bbf682c...9340d`, exact Segment-4 predecessor pin, and boundary evaluation loss `1.4209885490501506e-09`; near-zero loss remains shortcut-confound evidence, not quality proof. Segment 6 version 1 ended `CANCEL_ACKNOWLEDGED` with no failure message or remote files. No retry was issued. Its 44-step interval repeats the duration shape that canceled the earlier long segment; a materially shorter transport boundary is required before retry. The deterministic multi-arm allergen bridge remains ready; Gemini's configured variable is empty and OpenAI is absent, so neither result is claimed.

---

## Current Evidence Boundary (from SSoT §1)

| Area | Verified Fact | Tier |
|------|---------------|------|
| GISMo reproduction | 10,747 test: Hit@1 20.694%, Hit@3 36.010%, Hit@10 54.164%, MRR 0.31636 | E3 |
| Unseen-pair collapse | GISMo unseen Hit@1 1.206%, MRR 0.04308; Lookup overall 17.996%, unseen 0.000% | E3 |
| Corrected SAG | Mean Hit@1 0.1171% vs semantic 0.3964%, naive fusion 0.3333% | E3 NEGATIVE |
| True-context dual encoder | 16.1223% overall Hit@1 vs GISMo 20.6941%; unseen MRR 0.016601 vs 0.043084 | E3 NEGATIVE |
| CLIP projection | R@1 34.10% → 35.42%, missed +2pp gate | E2 NEGATIVE |
| Flavor pairing | 2,231,142 RecipeNLG rows, p=0.238095, not supported | VERIFIED NEGATIVE |
| FooDB service | 971 foods, 206 dimensions, 125,992 links; 471,906 profile pairs, 0 mismatches | VERIFIED |
| FooDB reach | Both endpoints: 1,162/10,747 (10.81%); unique endpoints 260/2,650 (9.81%) | MEASURED |
| OFF concordance | 400/408 declared groups (98.04%, Wilson 96.18–99.00%); trace 22/40 (55%) | PARTIAL E2 |
| OFF partition | 23 identities cover 760,877/767,780 tag occurrences (99.10%) | E3 |
| Allergen protocol | 208 cases, 13 groups, direct/trace/multi/cross-contact; Mistral raw 208/208 | PARTIAL E2 |
| Mistral Stage 3 | Verified through 374/561; Segment 7 launched, RUNNING toward 444 | PARTIAL E3 |
| Novelty reviews | v8/v9: broad claims rejected; exhaustive re-attack open | PARTIAL |

**No accepted IRON CHEF model exists.** No allergen comparison result (GPT-4o/Gemini unrun, no adjudication). No human usefulness evidence. Stage 3 incomplete.

---

## The Five Contributions (C1–C5 from SSoT §3)

| ID | Contribution | Floor | Target | Stretch |
|----|--------------|-------|--------|---------|
| **C1** | **Instrument**: strata × allergen cases × valid@k audit | 208 allergen cases + Recipe1MSubs strata + 150-case 2-rater audit | + Vietnamese pilot (~100 cases, 2 native raters) | Public release with datasheet |
| **C2** | **Measurement campaign** across arms | GISMo + lookup + one API LLM | + Mistral Stage 3 + second API LLM; intervals everywhere | Contamination-aware 2×2 (public/private × Western/Vietnamese) |
| **C3** | **Guardrail**: hard allergen filter + per-cell conformal sets + abstention | Frozen protocol, honest run, cells reported | Informative sets in ≥1 cell; vacuous cells shown | Fewer allergen violations than raw arms with intervals |
| **C4** | **Human evidence** | 2-reviewer pass on audit pilot, κ (kappa) reported with cluster-level interval | LLM judge calibrated to humans (κ ≥ 0.6; kappa ≥ 0.6) | Usefulness study N ≥ 20 (N >= 20; ethics permitting) |
| **C5** | **Product proof** | Real-data Explorer + evidence endpoint + visible abstention | Live side-by-side demo vs raw LLM on same inputs | Reusable component |

**Bounded negatives (no further spend):** SAG, dual encoder, flavor pairing, CLIP projection, VLM artifact, behavioral overlay, voice copilot. One thesis chapter: *What did not work, and what each failure taught about the evaluation.*

**L13 re-scoped:** from "train new GNN" → "shift-aware selector over frozen scorers" (SSoT §3.5).

---

## Acceptance Gates per Contribution (SSoT §3.2)

| ID | Gate (all binary, checkable) |
|----|------------------------------|
| C1 | Instrument schema validated; strata files regenerate bit-for-bit from official splits; audit pilot labels for ≥150 cases exist with two independent raters and adjudication log; κ reported with case-level interval |
| C2 | Every arm evaluated on identical case list, identical normalization, identical scoring script; each number in a table has an interval; arms with unmatched-output rate >5% flagged |
| C3 | Protocol frozen and hashed before test access; calibration only on validation; per-cell status table (ok / vacuous / no_guarantee); cost-of-trust figure from frozen run |
| C4 | Rater agreement computed per §6.8 with cluster-level interval; judge-vs-human agreement per §7.5; if κ/kappa < 0.6 twice (kappa < 0.6 twice), fallback applied and stated |
| C5 | Explorer loads from hash-pinned graph artifact; demo runs from clean checkout without network except API arms; abstention state reachable and tested |

---

## Work Packages (from SSoT §17.1 — Lead Items)

| ID | Deliverable / Acceptance | Dependencies | Effort | Fallback |
|----|--------------------------|--------------|--------|----------|
| **L13′** | Re-scope to shift-aware selector over frozen scorers; edited item text + link to §8 | None | 1h | None |
| **L36** | 150-case audit: all independent labels/adjudication/cluster intervals/H1 table | Raters (L41), pools (M-B), PR01–03 resolved | 30–40h | Shrink to 100 cases |
| **L37** | Deterministic strata and interval-reported metrics; optional endpoint-frequency split | Identity/counts (M-B) | 10h | Drop frequency sub-split |
| **L38** | Frozen calibration, all-cell status, diagnostics, cost-of-trust figure | Audit/judge/arms, PR04–08 resolved | 25h | Report only ok cells; label rest |
| **L39** | Matched adjudicated allergen outputs; expansion to n≥299 or honestly bounded interval | API keys, reviewers (L41), rights | 30h | Report interval at n=208 |
| **L40** | Frozen ~100-case native-reviewed Vietnamese pilot | Authors/raters/rights (L41), U8, U9 | 30h | Qualitative appendix only |
| **L41** | Written ethics outcome; named raters; handbook + T0/T1 results | Human dependencies | 12h | Fallbacks per §14.8 |
| **L42** | Hashed novelty funnel; full-text/citation chase; independent adjudication | Sources, reviewer | 15h | Narrow claims |
| **L43** | Frozen judge prompt + evaluated gates, or explicit unusable outcome | Leak-free human comparison (L36) | 12h | Humans only on smaller set |
| **L44** | Validated Stage 3; 200-case timing; matched ranked outputs for val/test subsamples | Quota, adapter, arm construction | 12h + waiting | Stage-2 adapter arm |
| **L45** | 14 evidence-bound thesis chapters (§21) | All results/claims | 80+h | Cut by cut line (§3.6) |
| **L46** | Source-bound defense answers; timed human rehearsal; zero forbidden claims | Thesis (L45) | 15h | None |
| **L47** | ARR paper draft per §23 | Evidence + venue check | 40h | Thesis only |
| **L48** | Integrated strategy + completed claims register | This migration completes strategy | 10h | None |

**Critical dependency path (SSoT §17.4):**
```
L41 (raters/ethics) → L36 (audit) → L43 (judge) → L38 (calibration) → L45 (writing) → L46 (defense)
         ↑                              ↑
    M-B (pools)                   M-D (arm outputs) ← L44 (inference)
```
**L41 → L36 → L43 → L38 is the critical path.** Everything on it waits on human availability, not compute.

---

## Schedule & Capacity (from SSoT §18)

**Today:** 2026-10-12. Defense assumed on/after Dec 8 (U3). All dates are proposals `[P]`.

| Week | Dates | Lead Focus | Member Focus | Gates / Freezes |
|------|-------|------------|--------------|-----------------|
| **W0** | Oct 8–11 | Answer U1–U3, U7, U11 (hours, advisor, defense, quota, ethics). Back up SSoT. Start asking raters. | Start M-A, M-F | **Decision: compression level** |
| **W1** | Oct 12–18 | L48 first pass. L42 query runs. Read Rita §3 + Fatemi data (U4). Check val scores + unseen share (U5). Draft rater handbook. | M-A recompute; M-B strata/identity; M-D GISMo/lookup outputs; M-F inventory | **Oct 15: two raters named. Ethics answer by Oct 12** |
| **W2** | Oct 19–25 | Obtain API keys; run API LLM #1 on 150 audit cases. Rater T0/T1. Draft allergen expansion. Write Ch2/Ch3 drafts. | Pool builder fixture test → real run. M-I runner fixture test. | **Oct 22: FREEZE A** (audit protocol, Δmin, arms, normalization). **Oct 23: Vietnamese raters/authors named, licenses checked** |
| **W3** | Oct 26–Nov 1 | Audit labeling. Judge prompt dry run. 200-case timing (by Oct 29). Allergen cases frozen/hashed. | M-C Explorer build; M-G register paths/hashes | **Oct 29: allergen reviewers named; timing done. Nov 1: profiles complete** |
| **W4** | Nov 2–8 | Adjudicate audit; κ with cluster CI; first H1 table. Judge calibration (L43). Allergen arms (GPT-4o, Gemini). Stage 3 check. | M-C Explorer T-03–T-09; M-E demo skeleton | **Nov 6: FREEZE B** (conformal protocol, §8.9) before any test coverage. Advisor interim. |
| **W5** | Nov 9–15 | Judge-label val pools; per-cell calibration (rank arms); diagnostics §8.12; test-subsample eval. Vietnamese freeze. H4 prep if ethics. | M-E recorded Act 3 from frozen files | **Nov 10: usefulness participants recruited (or dropped)** |
| **W6** | Nov 16–22 | Mistral inference + calibration (or Stage-2 adapter). Allergen adjudication. Vietnamese labeling. Cost-of-trust. | Explorer polish T-10–T-12 | **Nov 20: EXPERIMENT FREEZE.** No new runs. Analysis/writing only. |
| **W7** | Nov 23–29 | Red-team claims (§24.2). Rebuild defense answers (§22). Chapters Ch5–Ch9. | M-H reproducibility kit; clean-machine reproduction | **Nov 27: register script exits 0; zero forbidden claims** |
| **W8** | Nov 30–Dec 6 | Write Ch1, Ch4, Ch11, Ch13, Ch14. Defense dry run 1. | Demo hardening | **Dec 1: ARR service-contributor decision** |
| **W9** | Dec 7–13 | **Dec 8: thesis freeze.** Start ARR draft from thesis artifacts. | Support | Thesis frozen |
| **W10–12** | Dec 14–Jan 3 | ARR v1; internal review. ARR v2; limitations/datasheet. Final read, hashes. | Figures/tables regenerate. | Service-contributor registration ready (W10, W11, W12) |
| **W13** | **Jan 4, 2027** | **ARR submission (AoE)** | | Registration form open until 48h post-deadline |

**Capacity arithmetic (SSoT §18.1):** 225 MUST + 32 SHOULD + 70 COULD = 327 lead hours; 91 member hours. 8 weeks at 30h/week = 240h (short 87h). At 40h/week = 320h (fits). **Run arithmetic with real hours on Oct 12 (U1), pick compression level before starting.**

**Compression protocol (SSoT §18.4):** Level 0 (full), Level 1 (drop COULD except 20-case Vietnamese appendix), Level 2 (GISMo/lookup only calibration, no judge, n=208 allergen, minimal Explorer), Level 3 (collapse map + 100-case audit + allergen interval + bounded negatives + claims register). Gates never weakened.

---

## Risks & Pre-mortem (from SSoT §19)

| ID | Risk | Likelihood | Impact | Early Warning | Mitigation |
|----|------|------------|--------|---------------|------------|
| K1 | Raters not found | M | H | <2 named by Oct 15 | Ask earlier; shrink to 100; label exploratory |
| K2 | Rater agreement too low (κ/kappa < 0.5 twice; kappa < 0.6 twice review trigger) | M | H | T1 failure | Rewrite handbook; negative anchors; shrink to audited subset |
| K3 | H1 inconclusive (CI straddles Δmin) | M | M | CI straddles 15pp | Extend to 300 only if time; report inconclusive |
| K4 | Judge fails gates (§7.5) | M | M | Gates fail | Humans-only on smaller calibration; state power loss |
| K5 | All cells vacuous | M | M | Early GISMo diagnostics | Report quantified limit; policy = abstain-by-default |
| K6 | Calibration/test not exchangeable | M | H | KS diagnostic on gold rank | Merge cells or weighted conformal; state limit |
| K7 | Validation scores missing | M | H | M-B check fails | Regenerate GISMo val (cheap); LLM arms on subsample |
| K8 | Stage 3 late/fails | M | M | No 327 by Nov 1 | **OBSOLETE — 327 recorded verified.** Stage-2 adapter hedge exists |
| K9 | Inference too slow | M | M | 200-case timing | Stratified subsample (§13.3) |
| K10 | API keys/budget unavailable | M | H | No key by Oct 22 | Use free/open-weight LLM; pin version; say so |
| K11 | Ethics blocks usefulness study | M | L | No answer by Oct 12 | Drop H4 |
| K12 | Allergen n too small to certify | H | M | Arithmetic fixed | Expand to ≥299 or report interval |
| K13 | Claim breaks under review | M | H | Red-team finds forbidden | §24.2 audit; claims register |
| K14 | Novelty duplicate found | L–M | H | L42 search | Re-scope C1 to Vietnamese+allergen+shift combo |
| K15 | Advisor says scope mismatch | L–M | H | U2 reply | Re-scope before more spend |
| K16 | No qualified ARR contributor | M | M | None by Dec 1 | Thesis only or later cycle |
| K17 | Licenses block release | M | L–M | M-F, §10.2 | Release code/aggregates only |
| K18 | Scope creep | H | H | >MUST tier in progress | Cut from bottom, weekly |
| K19 | Overnight job overspend | L | M | Receipt > cap | Per-job caps; refuse-to-start |
| K20 | Teammate unavailable | M | M | Tasks slip 2 weeks | Reassign M-A, M-B to Lead; compress |
| K21 | R0 numbers leak into thesis | M | H | Figure without claim ID | Register rule (§15.2) |
| K22 | Non-thesis workload | H | H | Weekly hours below plan | Compression level (§18.4) |

**Pre-mortem triggers (SSoT §19.3 — reopen architecture only on these):**
- H1 lower Wilson bound ≥ Δmin → lead with benchmark validity
- H1 upper bound < Δmin → lead with collapse map + abstention
- H1 straddles Δmin → report inconclusive; extend only if time
- κ < 0.6 twice → rewrite handbook once; shrink to audited subset
- All cells vacuous → reframe C3 as quantified limit
- Novelty duplicate → re-scope C1
- Advisor mismatch → re-scope before spend
- Exchangeability diagnostic fails → weighted conformal or merge; state limit
- No ARR contributor by Dec 1 → thesis only

---

## Protocol Resolutions (from SSoT §16 — must resolve before affected work)

| Flag | Conflict | Resolution Required |
|------|----------|---------------------|
| PR01 | §6.1 conditional rescue vs §6.9 joint rescue; > vs ≥ Δmin | Choose estimand, denominator, operator prospectively |
| PR02 | Gold forced A yet both-rater N = label noise | Specify raw/adjudicated/gold policy; implications for V≥H |
| PR03 | Equal allocation vs population inference; top-4 vs top-5 | Resolve allocation, weighting, stratum minima, consistent k |
| PR04 | Cell uses gold substitute (unavailable at deployment) | Separate descriptive oracle strata from deployable partition |
| PR05 | Judge chosen on test audit; claim freeze before test | Establish dev/audit/cal/eval separation; disclose exposure |
| PR06 | Filter/rerank/truncation/empty-pool mismatch; infinity handling | Define returned-set success, filter ordering, guarantee population |
| PR07 | KS called exchangeability check; test-driven merging | Diagnostics cannot prove assumption; freeze variants or label exploratory |
| PR08 | Coverage grid .50–.95 vs α={.05,.1,.2,.3,.5} | Freeze single grid and status semantics |
| PR09 | Lossy normalization vs v23 collision findings | Validate identity/food-form behavior before freeze |
| PR10 | Different κ point/lower-bound gates; Wilson vs exact | State each gate's population/statistic/interval method |
| PR11 | Train-only graph vs "no val/test pair" despite seen overlap | Resolve row provenance leakage test without changing splits |
| PR12 | Endpoint example seen=true/count=0/unseen simultaneously | Replace only via validated contract design |
| PR13 | Annotation pending ethics vs clearance-before-study | Obtain institutional authorization; prep continues |
| PR14 | Seed placeholders marked VERIFIED; checker accepts dummy | Implement real artifact validation separately |
| PR15 | Synthetic tests omit bounded-M/infinite/missing-cell | Independent validation before reuse |

---

## Operating Rhythm (from SSoT §24)

- **Daily (10 min):** Morning review of overnight jobs (§13.8)
- **Monday (30 min):** Claims register diff; overnight receipts; plan week against cut line
- **Wednesday (15 min):** Check gates due; chase people
- **Friday (30 min):** Update status block (generated); write one thesis paragraph from week's evidence; decide cuts
- **Biweekly advisor update (5 lines):** Evidence IDs, gates moved, plan changes/trigger, precise request, next action

**Freeze A (Oct 22):** Audit protocol, Δmin, strict/lenient, arms, blinding, normalization, primary arm/stratum, κ bar, timestamp/hash.

**Freeze B (Nov 6):** Deployable cells, n floor, M, α grid, labeling function/selection, filter/rank/set semantics, infinity/missing, informativeness, diagnostics/sensitivities, evaluation access. **Neither freeze completed here — resolve §16 first.**

**Forbidden claims (§24.2 — red-team every sentence):** No "zero/<1% violations" without n≥299; no "safe/safer/clinical"; no "human-valid coverage" from judge; no "first/novel" before 4.6+second reviewer; no Vietnamese guarantee; no "measured contamination"; no R0 figures without reconstruction; no cross-task rankings; no "four trained models/production-ready AI"; no "zero cost/local-only" without runtime proof; no user outcome claims; no untraceable numbers; no "calibrated" without cell/label/assumption (never claim "calibrated without" its cell, label, or assumption); no "hypothesis confirmed" when interval includes null.

---

## Evidence Integrity Rules

- Never mark COMPLETE because code was written — require executable evidence (checkpoint, hash-bound predictions, scored evaluation).
- Never compare cancelled run or paper number to locally executed result.
- Never report metric without dataset, split, protocol version, seed count.
- Negative results (SAG, dual encoder, CLIP, flavor, VLM, behavioral, voice) are thesis contributions — report honestly.
- Do NOT cite: 22.04% Senath (unverified journal abstract); 0.2222 HGAT (synthetic); 0.00% allergen (self-labeled, withdrawn).
- Every number in thesis carries a claim ID from register (§15). Unverified = say so.

---

## Appendix: Mapping from v3 Backlog (for reference only)

| v3 Epic | v4 Treatment |
|---------|--------------|
| EPIC 3/3b/3c (M1 GNN/SAG/dual encoder) | Bounded negatives chapter; L13′ = shift-aware selector |
| EPIC 10 (M2 Mistral Stage 1–3) | L44 (Stage 3 completion + inference); Stage-2 adapter hedge |
| EPIC 11 (M3 VLM) | Bounded negative; no further spend |
| EPIC 12 (M4 CLIP) | Bounded negative; no further spend |
| EPIC 5/5b (Allergen) | L39 (matched arms + expansion + adjudication) |
| EPIC 4 (Compound explanation) | C4/H4 (usefulness study if ethics) |
| EPIC 9 (Thesis 14 chapters) | L45 (14 v4 chapters per §21) |
| L29 (Photo chain) | Bounded negative; §12 product proof replaces |
| L31 (AI runtime cert) | Separate infra work; not thesis contribution |
| L35 (Defense pack) | L46 (rebuilt answers + rehearsal) |
