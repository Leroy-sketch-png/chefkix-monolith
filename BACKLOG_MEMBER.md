# IRON CHEF v4: MEMBER BACKLOG

### L48 / LEAD v474 original Segment-9 checkpoint recheck (2026-10-21)

The original `checkpoint-561/` was found in the dirty root checkout and read-only checked at 12/12 files against the Segment-9 manifest; the separate retained-best adapter matches 8/8 manifest hashes. Updated R26 evidence note SHA-256 `b6feea712b8441eeeda026c90088441da017dd1aed662b16b3ed105c3607ca90`; register SHA-256 `28c12b8a81755d497114ae9b233076ecdfda2b567da342af9e12985656f6a988`; mismatch ledger SHA-256 `4c4ebd4db785ffd7958a3dbb007d05da57d0d859dbe7be0ae960abec7d2b5a91`. This hash-bound local evidence does not establish R26's downstream outcome, inference quality, or human value. All 45 rows remain UNVERIFIED; M-G/L48 remain OPEN.

R26 now references the tracked local recheck note (SHA-256 `e1b40fddfb586c402e23febb8048768e5cfa13f9c1205c3f6366cc8ca63b9f52`) binding the Segment-9 receipt/manifest and eight retained-best adapter files. The full 12-file checkpoint is not locally present and remains receipt-attested; R26 stays UNVERIFIED. Register SHA-256 `09ef8466ff35e4a76cbbf25cde8361ac2aee42f9c1469735a5e16ab525d44cdd`; mismatch ledger `0103e1e832746e8b23d3635d74a67222b83851f5c7d46f38b463601a7cc988a0`. M-G/L48 remain OPEN.

### L48 / LEAD v472 bounded historical-register search (2026-10-21)

The updated crosswalk-review note (SHA-256 `0cce3de5ee6643b4952db2ab376aec6bd57898c695dc30c7be332a8c838c3bd4`; research commit `f989a5f52356c8c3bff36001ac03b958ac36eeeb`) records that the available worktree file inventory/Git history has no v171/v197 register bytes. This does not exclude an external/archive copy. All 45 statuses remain UNVERIFIED; M-G/L48 remain OPEN.

### L48 / LEAD v471 row-level crosswalk review (2026-10-21)

The 45 mapping dispositions were checked against the pinned §15.1 wording and current Lead/Member package IDs; review note SHA-256 `ca4e1f891009215e5293b12ca9dd3fb5c6a76a8c662c97a6086dcae7f5bac72e`. All current referenced IDs resolve except L29/L30 in R30/R31, which are explicitly historical pointers rather than active owners. Fifteen claims remain unmapped; all 45 remain UNVERIFIED with no reproductions or backlog-status promotion. M-G/L48 remain OPEN.

### L48 / LEAD v470 conservative R01–R45 register draft (2026-10-21)

Research `master` commit `f5a14dc` creates a deterministic, source-pinned 45-row inventory at `ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v1.csv` (SHA-256 `7e8bc939cb7954f28ff6072ba515fadb42218dacde56da4c32822fcbd38727db`) with `EVIDENCE_VS_CLAIM.csv` (SHA-256 `2ae2bf2c8ef72eb90869d76b8f52790c1862201418d3432ea3e85c36dbba4476`). All rows remain UNVERIFIED, with no outcomes or reproduction status; 30 have explicit/bounded crosswalks and 15 remain unmapped. Builder/validator checks establish structure and local-byte integrity only. Historical v171/v197 files were unavailable and not imported. The research commit is local-only because that checkout has no configured remote. This is a first reviewable draft, not claim reconciliation or a scoreboard; **M-G and L48 remain OPEN**. See LEAD v470 for full provenance and limits.

### M-D / LEAD v469 written-artifact acceptance met locally (2026-10-21)

The literal §17.2 M-D criterion—four deterministic full-split GISMo/lookup files—is now **met locally**: every validation/test case is present, all four outputs and the package manifest replay byte-identically, and 36 focused tests pass. This does not resolve external handoff, rights/release, unspecified historical lookup tie equivalence, model comparison, or L44. Those remain separately open; no labels were read or scored for this packaging work. Receipts: `evidence/lead-md-arm-output-package-20261021.json` and `evidence/lead-md-arm-output-validator-hardening-20261021.json`.

### M-D / LEAD v468 package validator AoE hardening (2026-10-21)

The package validator now checks label-like keys recursively and rejects boolean/invalid graph IDs and ranks. The three new boundary regressions pass within the 36-test focused suite; a fresh package replay retains identical output/manifest hashes. This is integrity hardening only. Rights remain UNKNOWN, lookup tie equivalence remains unresolved, and there is no inference/quality result. M-D/L44 remain open. Receipt: `evidence/lead-md-arm-output-validator-hardening-20261021.json`.

### M-D / LEAD v467 local full-split arm package and replay (2026-10-21)

The four expected `arm_outputs/{gismo,lookup}_{val,test}.jsonl` files are assembled with a hash-bound package manifest. Full validation/test coverage is 10,729/10,747 cases; cross-arm case identity and candidate uniqueness/ranks/scores are checked; a fresh package replay is byte-identical; 33 focused preparation tests pass. This is a local-only, label-free output-preparation artifact, not inference or evaluation. Dataset rights remain UNKNOWN, and historical equivalence of the deterministic lookup tie rule is unestablished; M-D/L44 stay open pending remaining authorization and downstream acceptance. Receipt: `evidence/lead-md-arm-output-package-20261021.json`.

### M-D / LEAD v466 deterministic lookup-frequency top-20 preparation (2026-10-21)

The second arm is now materialized for every source-only validation/test case using the released GISMo LTFreq training-pair count rule: 10,729/10,747 cases, 20 unique ranked graph IDs each. The lookup files and manifest replay byte-identically; the combined preparation suite passes 27/27. GISMo's already-generated full-split arm remains unchanged. No labels were opened, and no scoring/evaluation or method selection occurred. The lookup tie rule (count descending, graph ID ascending) is an explicit deterministic transcription; historical `torch.topk` tie equivalence is unknown. Output files are still local-only and not rights-cleared or in final shared packaging. **M-D and L44 remain open.** Receipt: `evidence/lead-md-lookup-top20-preparation-20261021.json`.

### M-D / LEAD v465 GISMo full-split top-20 materialization (2026-10-21)

Research `master` commit `f0282c4` materializes the deterministic GISMo arm by taking the first 20 candidates from each pinned, verified source-only top-50 row: 10,729 validation and 10,747 test cases. A fresh build replay is byte-identical; all output rows match their source-pool top-20 prefix with exact case order/ranks and no label fields. The 21-test focused source-pool/builder suite passes. This does not rerun GISMo, read labels, score/evaluate, establish rights, or select a method. Lookup-frequency is still missing a pinned executable contract; **M-D and L44 remain open**. Details and hashes: `evidence/lead-md-gismo-top20-preparation-20261021.json`.

### L44 / LEAD v464 showcase regression suite rerun (2026-10-21)

Fresh rerun of the source-bound benchmark and midterm-exhibit test suites passes 30/30, including receipt-to-table-order assertions for both arms. The test/source hashes and renderer commit are in `evidence/lead-l44-showcase-regression-rerun-20261021.md`. The corrected HTML remains a local-only artifact; no browser visual review or listener rehearsal was performed, so L44 stays open.

### L44 / LEAD v463 recovered validation-benchmark diagnostics (2026-10-21)

The existing one-shot Kaggle version-4 recovery receipt is bound to a 14-file inventory that was rechecked against disk with zero missing, extra, or mismatched files. Saved-output validation covers 200 fixed validation cases with zero method failures. GISMo identity Hit@1/5/10 is 1.5%/7.5%/14.5% for GISMo top-50 + Senath reranking and 3.0%/11.5%/14.0% for Senath top-20 generation; raw-string sensitivity and per-case latency are separately recorded in `evidence/lead-l44-validation-benchmark-results-20261021.md`. This is descriptive fixed-validation diagnostic evidence only, not an independent test, human-usefulness result, superiority claim, or L44 completion. No new Kaggle operation was performed.

### L48 / LEAD v462 R04/R05 source-reproduction crosswalk (2026-10-21)

The local full-split audit now maps to the two matching seed claims: R04 identity counts and R05 split row counts/raw CSV-to-GISMo-pickle joins. The note `evidence/lead-l48-r04-r05-reproduction-20261021.md` records exact values and limitations, linked to the hash-bound M-B receipt. The migration seed's provenance remains checklist-only; no register row is promoted or verified, and L48 remains open.

### L45 / LEAD v461 all-chapter structural link sweep (2026-10-21)

Across 14 active chapter drafts, a read-only scan found 60 local file links, zero missing targets, and zero duplicate headings; all source-file hashes and scan limits are recorded in `evidence/lead-l45-chapter-structure-sweep-20261021.json`. External links/fragments, citations, claims, content, rendering and advisor acceptance were not reviewed. L45 remains open; the research checkout has no remote.

### M-B / LEAD v460 Chapter 3 and local receipt synchronized (2026-10-21)

Research `master` commit `42f1111` commits the full-split receipt (SHA-256 `c2429fbad69d5ffc83d62fbd118c9c26bed6868511cb71c49c5dae4e9165d6420`) and corrects Chapter 3 (SHA-256 `57b2086f607e8163bdc238f783bfdc60175bf44593d3012c00103d7bc6b04bc`) to distinguish the old clean-room-directory observation from the later wider-archive source discovery. The research checkout has no remote; the matching receipt and backlog update are shared in this monolith. M-B remains open; R04/R05 remain unverified.

### M-B / LEAD v459 complete raw-split join and identity-frame rerun (2026-10-21)

The exact pinned test CSV was found outside the previously designated clean-room directory, and all pinned GISMo assets were assembled and verified. Full train/validation/test CSV↔pickle joins pass; validation/test identity counts are 10,729/10,747 with GISMo-primary and Senath-normalized strata recorded separately. The machine receipt `evidence/lead-mb-full-split-source-join-20261021.json` binds inputs, outputs, hashes, and limitations. This resolves the scoped join gap only: M-B acceptance and R04/R05 remain open/unverified; no rights, human-label, independent-evaluation, or model-quality claim is made.

### L45 / LEAD v458 Chapter 3 provenance-bound update (2026-10-21)

Research `master` commit `43d4a2f` updates the internal Chapter 3 draft with the direct edge-count derivation and candidate legacy producer trace, preserving missing producer/output binding, random-feature fallback, and no-runtime/no-quality boundaries. Chapter SHA-256 `49db7b05a497d1f1f391907a99b975fe9b04e957708817a37a14c372115c5be2`. This is local-only, advisor-unreviewed draft progress; the research checkout has no remote and L45 remains open.

### M-A / LEAD v457 legacy producer source trace (2026-10-21)

Found the committed legacy subgraph extractor (`59e8560`): it emits only features and directed edges, with a random-feature fallback if optional feature inputs are absent. The local production inputs and any output-binding receipt are missing; the current source fail-fast retirement guard was not bypassed. Candidate producer identified, lineage still unknown. M-A stays OPEN; R39/R40 stay UNVERIFIED. Receipt: `evidence/lead-ma-legacy-producer-trace-20261021.json`.

### M-A / LEAD v456 direct serialized-edge count recheck (2026-10-21)

The pinned graph edge tensor was rechecked with the restricted, hash-pinned inspector; direct set construction confirms 4,591 unordered pairs including four self-loops (4,587 excluding them), alongside 4,883 unique directed pairs and 7,247 arc entries. This supports a count-convention hypothesis only; historical convention and provenance remain unknown. M-A stays OPEN; R39/R40 stay UNVERIFIED. Receipt: `evidence/lead-ma-undirected-count-convention-20261021.json`.

### M-A / LEAD v455 undirected-count convention check (2026-10-21)

The local artifact's 4,883 distinct directed pairs, 292 reciprocal non-self pairs, and four self-loops yield exactly 4,591 unique unordered endpoint pairs including loops (4,587 excluding loops). This is a plausible count-convention explanation only; it does not establish Round-0 method, artifact lineage, runtime use, or quality. M-A stays OPEN; R39/R40 stay UNVERIFIED. Receipt: `evidence/lead-ma-undirected-count-convention-20261021.json`.

### M-A / LEAD v454 source-family coverage revalidation (2026-10-21)

The shared receipt binds the GISMo-v23 matrix and records an independent 35/35 arithmetic reconstruction; the focused test was rerun and passed 2/2. The ≥2-family tier covers 31,974/45,988 top-50 occurrences and 57,641/141,040 full-vocabulary occurrences. This is source-linkage support only, not runtime graph coverage, semantic equivalence, rights, or safety. Generator/test/CSV remain local-only. **M-A stays OPEN; R39/R40 stay UNVERIFIED.** Receipt: `evidence/lead-ma-source-family-coverage-curve-20261021.json`.

### L45 / LEAD v453 Chapter 14 internal draft (2026-10-21)

Chapter 14 now exists as an internal v0.1 evidence-bound conclusion/future-work draft on research `master` (`94a0f253`; SHA-256 `bc786827…ef8583395fa`). All 14 §21 chapter slots have draft files, but this does not imply advisor acceptance or L45 completion. The research repository has no remote; the chapter is local-only. See `evidence/lead-l45-chapter14-draft-20261021.json`.

### M-A / LEAD v452 preserved Round-0 evidence search (2026-10-21)

The v4.2 and preserved pre-migration strategy documents contain attributed Round-0 assertions, not the source report or raw measurements. A bounded local text/history search found no underlying report path; its scope excludes unavailable remote history and unsearched ignored/hidden content. The result does not change acceptance: M-A stays OPEN and R39/R40 stay UNVERIFIED. See `evidence/lead-ma-round0-preserved-source-search-20261021.json`.

### M-A / LEAD v451 source-seed directed-graph reconstruction (2026-10-21)

The current monolith seeder statically parses to 25 canonical ingredient source records and 59 directed substitution statements. With exact unique canonical/alias resolution and unmatched targets retained as literal vertices, the interpreted graph has 79 vertices, 59 arcs, and max total degree 4; in/out sums are 59/59. Seven parser/graph fixtures pass 7/7 three consecutive runs. This partially reconciles source numbers, not the original Round-0 artifact/definitions or runtime lineage. **M-A remains OPEN; R39/R40 remain UNVERIFIED.** Report/test hashes and limits: `evidence/lead-ma-seed-graph-source-reconstruction-20261021.json`.

### M-G / LEAD v450 fail-closed claims-register validator (2026-10-21)

Added the missing structural/local-byte claims-register validator in the research checkout. Its 21 tests and compile check pass; it rejects the migration seed's checklist-only provenance with no success summary. This is a validator/tooling increment only: no register was instantiated, no claim verified, and no scoreboard generated. M-G and L48 remain open; the research commit is local-only because that checkout has no remote. Receipt: `evidence/lead-mg-validator-implementation-20261021.json`.

### M-I / LEAD v449 operator-interrupt recovery (2026-10-21)

Ctrl+C during the research runner's child-poll loop now terminates and reaps the direct child, records `OPERATOR_INTERRUPTED`, finalizes cooperative usage, and preserves a blocked receipt/ledger with the idempotency reservation consumed. Regression verifies a second execution is refused. The 36-test focused suite passes three consecutive runs; compile, CLI help, and diff checks pass. This does not handle forced process/machine crashes or descendant processes; no paid/API job ran, the research source has no remote, and M-I remains open. Receipt: `evidence/lead-mi-operator-interrupt-recovery-20261021.json`.

### M-I / LEAD v448 missing-case and diagnostic stops (2026-10-21)

Jobs must now emit a standard `job_diagnostics.json`; missing/malformed/incomplete reports, missing cases, and unresolved diagnostics fail closed. The runner contract documents the schema and operator sequence, and 35 fixtures pass three consecutive runs. Counts are child-reported and not independently audited. M-I remains open: the runner is local-only, unsandboxed, has no provider hard cap, and needs independent/platform/recovery review. Receipt: `evidence/lead-mi-diagnostics-completion-contract-20261021.json`.

### M-I / LEAD v447 explicit validation-only preflight (2026-10-21)

Added a no-side-effect preflight mode so manifests can be checked before a run: it verifies the script/data identities, parameter-derived idempotency, environment requirements, limits, and freeze record but neither launches nor creates a ledger/output folder. Thirty-one tests pass three consecutive runs. M-I remains open because the implementation is local-only, usage accounting is cooperative, execution is unsandboxed, and broader review/platform work remains. Receipt: `evidence/lead-mi-runner-preflight-mode-20261021.json`.

### M-I / LEAD v446 child environment isolation (2026-10-21)

The runner no longer forwards every parent environment variable to arbitrary child jobs. A small OS baseline is combined only with manifest-declared, required variable names; names are included in the execution identity and receipt, values are not. The 29-test suite passes three consecutive runs, including an explicit secret-noninheritance fixture. Allowlisted secrets are still available to the child and this is not a sandbox; no credentialed job was run. M-I remains open and the source repository has no remote. Receipt: `evidence/lead-mi-child-environment-isolation-20261021.json`.

### M-I / LEAD v445 enforced stop-policy ceilings (2026-10-21)

The runner now refuses any manifest that weakens the strategy's 5% API-error or 2% invalid-output maximum stop thresholds; exact-threshold fixtures confirm strict `>` behavior. The 27-test suite passes twice consecutively. M-I remains open; usage remains child-reported/cooperative, the runner has no OS sandbox, and the implementation is still local to a research checkout without a remote. Receipt: `evidence/lead-mi-stop-threshold-contract-20261021.json`.

### M-I / LEAD v444 strict manifest parsing (2026-10-21)

Third-pass parser review removed last-value-wins duplicate JSON keys and Python's `true == 1` schema-version ambiguity; the usage monitor now reads appended journal bytes rather than rescanning the full file every 50 ms. The 24-test suite passed twice consecutively. M-I remains open: local-only implementation, provider-hard budgeting, sandboxing, full recovery/platform coverage and independent review remain outstanding. Receipt: `evidence/lead-mi-strict-manifest-parse-20261021.json`.

### M-I / LEAD v443 idempotency binding and collision controls (2026-10-21)

Follow-up review found that merely accepting a hash-shaped idempotency key did not meet the active §13.1 identity contract. The research-local runner now derives the key from canonical script hash, sorted input identities, and runtime parameters; it refuses mismatches before launch. Regression coverage also exercises concurrent same-key execution, preflight not consuming the key, prior-ledger preservation, and output-collision non-overwrite. The 22-test suite passed three consecutive runs. M-I remains open: the source is local to research `master` without a remote, and provider hard caps, sandboxing, broader platform/recovery tests, and independent review remain. Receipt: `evidence/lead-mi-idempotency-hardening-20261021.json`.

### M-I / LEAD v442 local runner prototype and refusal fixtures (2026-10-21)

An independently implemented Python runner on the research repository's local `master` now passes 18 focused fixtures for hash-bound manifests/inputs, fresh outputs, idempotency, time/call/cooperatively reported spend limits, stop thresholds, receipts, freeze-record shape, and failure preservation. The runner explicitly does not provide an OS sandbox or provider-side hard budget, and a local unsigned approval record grants no confirmatory-claim permission. The implementation commits are local-only because that repository has no configured remote; no real API job ran. M-I remains open for broader invalid-manifest, concurrency, recovery, platform and independent-review coverage. Receipt: `evidence/lead-mi-runner-prototype-20261021.json`.

### M-B / LEAD v436 split and identity recount (2026-10-21)

The research-default-branch local recheck reproduced validation/test case-frame counts (10,729/10,747) and GISMo-primary/Senath-sensitivity partitions from the hash-pinned identity map. Available training/validation CSV row counts and hashes matched the audit manifest. The raw test CSV is not in the designated clean-room folder, and the original GISMo joins were not rerun. Evidence note: `ai/evidence/claims-register/r04-r05-split-identity-recheck-20261021.md`, research commit `03ab020`, SHA-256 `57bb6d4ca2f22485f2a8926fcaecc643144ecf14c93e3db21c91b430d5011974`. This is interim local count/integrity evidence; M-B acceptance and R04/R05 claim verification remain open. Research checkout has no remote.

### L48 / LEAD v433 synchronized active register status (2026-10-21)

The active LEAD summary now points to the v4.2 R01–R45 inventory (research commit `d6341cf`, SHA-256 `871ED9937D5C366744B5759F51238A944AEC6FCC8F3002D59E53AD74821EAA36`), with all 45 rows still `UNVERIFIED`. The old 11/6/28 overlay and v171/v197 pointers remain historical only. M-G's evidence contract was corrected in LEAD v432; no claim-by-claim mapping or reproduction has been accepted. L48 remains open; the research checkout has no remote.

### L48 / LEAD v431 unverified R01–R45 inventory (2026-10-21)

Created and locally committed `ai/evidence/claims-register/claims-register-r01-r45-unverified-seed-20261021.csv` (research commit `d6341cf`, inventory SHA-256 `871ED9937D5C366744B5759F51238A944AEC6FCC8F3002D59E53AD74821EAA36`). It contains 45 unique checklist rows, all `UNVERIFIED`, with migration-checklist-only provenance and no reproduction values. Builder tests pass 4/4; validator tests pass 8/8; the produced file validates structurally but this is not verification of claims, source findings, or reproduction. L48 remains open for artifact/provenance reconciliation; no scoreboard or progress percentage is authorized by this artifact. Research `master` has no configured remote, so the file is not yet fetchable by collaborators.

### Q124 / LEAD v430 browser-rendered exhibit inspection (2026-10-21)

Read-only browser inspection of the local offline exhibit (source SHA-256 `47D9AA1D7F5E1F3F704D43EA32AF5773EF2CF94AA906D94AFF232457E11C15FF`) found the desktop hierarchy legible and metric cards stacked/readable at a narrow-width zoom stress approximation (~320 CSS px; not device emulation). The evidence table scrolls horizontally and exposes a named region/instruction. No screenshot was saved. Keyboard/focus validation remains inconclusive; actual handset/breakpoint checks and timed human rehearsal/listener responses remain NOT RUN. Q124 stays open; no accessibility, comprehension, or demo-acceptance claim. This later observation supplements, but does not erase, the historical v416 source-contract record.

### L45 / LEAD v429 evidence-bounded introduction draft (2026-10-21)

Added `research/chapters/01-introduction.md` to the research checkout's default `master`, commit `6eeac1a77e99e45a1df5a830c2cb7de898bbfcdf`, SHA-256 `3a9e9402b593d54de46d9e2ac35642a681cdeed5afd78580386b6cb09ee829a`. The 76-line v0.1 draft follows active §21 and §§2–3: it defines the question and H1–H5 boundaries, separates the intended C1–C5 contributions, and explicitly rejects unsupported safety, usefulness, superiority, and novelty claims. It uses 8 resolving Markdown links, has no duplicate headings, and passes `git diff --check`. No new source verification or empirical result is claimed; chapter acceptance remains open, and the research checkout has no remote.

### L45 / LEAD v428 archive-boundary correction (2026-10-21)

Rechecked the active chapter map against the canonical document's archive delimiters: the older `§8 — Thesis Chapters` table is inside fenced Appendix Y, explicitly marked superseded v3 text, while §21 is the active chapter map. Updated the Chapter 6, 7, and 13 draft cautions to use §21 without implying an unresolved conflict. Research-default-branch commit `ebb1a1404e1823c37ed42b4c6cc2b08b397a0ef2`; hashes: `1b62b33df4330bd57e4135188e09703005e723c53564c1805e1c0cbaac778c7b`, `bed6756f73265ef246f3ff2c2addfbb9760a2be0abd6df3ed5880e9738f31bfb2`, `3135cf5c9bf8a68f11b7e84d8192814d518876328ffa612b23fd68e8072213ca`. Research checkout has no remote; chapter acceptance and overall L45 completion remain open.

### L45 / LEAD v427 evidence-bound discussion chapter scaffold (2026-10-21)

A twelfth internal v0.1 thesis draft now exists on the separate local research `master`: `research/chapters/13-discussion-and-threats.md`, commit `9a19f6d`, SHA-256 `2144a89d948bfd12ee046e08618e70e8cedfcb4f862b30746922f7264812383a`. It synthesizes the canonical result ledger while preserving the validation-only, partial, negative, and unresolved boundaries; it introduces no independent analysis or new claim. All nine linked sources resolve. The source checkout has no remote; L45 review, final evidence reconstruction, and chapter acceptance remain open.

### L45 / LEAD v426 bounded guardrail chapter scaffold (2026-10-21)

An eleventh internal v0.1 thesis draft now exists on the separate local research `master`: `research/chapters/07-guardrail-design-and-assumptions.md`, commit `fdd7caa`, SHA-256 `b1b0db055607c8a1766292fdcd19a8ebffbac10a1363793f55ef200d7fca0e05`. It records the proposed rank-conformal construction and failure semantics while leaving PR02/04–08/15 unresolved and treating Appendix A as non-executable reference only. No calibration result, coverage guarantee, safety claim, or deployment readiness is claimed. The source checkout has no remote; L45 acceptance, independent protocol/code validation, and thesis review remain open.

### L45 / LEAD v425 human-validity audit chapter scaffold (2026-10-21)

A tenth internal v0.1 thesis draft now exists on the separate local research `master`: `research/chapters/06-human-validity-audit.md`, commit `b3163a8`, SHA-256 `88ee3f1ed0f209a9467908724bb17a6bef1f19b96226af6c0ad06924e0448314`. It integrates the proposed audit question, identity/rubric/sampling/reliability boundaries, and evidence needs while preserving PR01–03/05/09/10/13 and the §21-versus-legacy-§8 chapter-number conflict. No human audit, protocol approval, or result is claimed. The research checkout has no remote, so the source remains local-only; L45 review, citations, evidence gates, and chapter acceptance remain open.

### M-I / LEAD v424 regression expansion across both showcase methods (2026-10-21)

Extended the v423 cell-order regression to verify both method rows—not only generation—against the receipt's grouped GISMo-primary and raw-string-sensitivity metrics. The combined Senath renderer and existing midterm-exhibit suites pass 30/30. This is static/contract evidence; it does not complete browser visual QA, timed listener rehearsal, L44, or any quality/usefulness gate. See LEAD v424 and the research-local evidence note `ai/evidence/senath-inference/showcase-column-order-fix-20261021.md` (note SHA-256 `e71fe95cceb2f03035e2d8829c58e5f77d2b4d786e7593a38db66e7b34d40269`). The research checkout remains local-only.

### M-I / LEAD v423 saved-output showcase metric-column correction (2026-10-21)

AoE review found that the generated HTML interleaved primary GISMo-identity and raw-string-sensitivity metrics beneath grouped headers. The renderer now places the two Hit@1/5/10 groups in header order, and the 15-test focused suite compares rendered values with the validated receipt. The 200-case outputs and metrics did not change; this repairs their presentation only. The corrected local HTML is structurally checked, but browser visual QA and timed human rehearsal remain NOT RUN. This is not a quality claim, human-usefulness result, or L44 completion. See LEAD v423 and `ai/evidence/senath-inference/showcase-column-order-fix-20261021.md` in the research checkout; that checkout remains local-only with no remote.

### L45 / LEAD v419 manuscript drafting progress (2026-10-21)

Nine internal v0.1 drafts now exist on the separate local research `master`: Chapters 12, 5, 2, 10, 3, 8, 4, 11, and 9. Chapter 4 leaves PR01–03/05/09/10 unresolved and approves no human protocol. Chapter 8 preserves allergen holds and claims no comparison or safety result. Chapter 11 now explicitly distinguishes an earlier successful local restart from a later route 404 caused by a different loaded artifact; lineage, Explorer behavior, public deployment, and user value remain open. Chapter 9 is only a Vietnamese-pilot planning scaffold; it creates no data, labels, or authorization. These are working drafts, not accepted chapters or evidence of thesis completion; full L45 writing, review, citation reconstruction, evidence gates, and defense work remain open. The research checkout has no configured remote, so these files are not yet shared/fetchable. See LEAD v417–v422 for exact commit IDs, hashes, paths, and per-chapter limitations. This derived status note changes no member-package acceptance criteria and does not claim a member package complete.

### Q124 / LEAD v416 offline exhibit source-contract recheck (2026-10-21)

Focused suite passes 15/15. Static assertions and source inspection confirm the exhibit has no external URL/remote asset or fetch/XHR patterns and includes visible focus styling plus a named, keyboard-focusable table region. The browser-level navigation/focus check did not run: the computer-use policy stopped before navigation because the Windows browser URL was not sufficiently determined. The timed human rehearsal and listener answers remain NOT RUN. This does not close Q124 or prove rendered accessibility/runtime offline behavior. See LEAD v416 and `evidence/lead-q124-offline-contract-check-20261021.json`.

### M-I / LEAD v410 recovered validation benchmark and replayable showcase (2026-10-21)

This monitor's v4 output recovery and local validator passed for all 200 fixed validation cases with 0 method failures per arm; ingestion recomputed identity-separated Hit@1/5/10 and latency, and a saved-output HTML showcase was rendered. A previous validated recovery already existed; both 14-file hash inventories match, so this retransfer was redundant and adds no independent benchmark result. Measured process wall time was about 4h50m. These are validation diagnostics, not product-quality evidence. Outputs remain local; a compact hash receipt is committed. Do not recover/download again. See LEAD v410.

### M-A / LEAD v409 live runtime/default-master artifact drift (2026-10-21)

The existing port-8080 service returns 404 for the graph endpoint and loads a culinary JAR byte-identical to the dirty nested checkout's target, not the clean default-master artifact whose controller contains `/graph`. Its loaded controller bytecode lacks the mapping. No process was changed; producer lineage remains unknown and M-A stays open. See LEAD v409 and its evidence receipt.

### M-A / LEAD v408 source-family coverage curve (2026-10-21)

Recomputed the frozen GISMo-v23 top-N source-family support curve and independently matched all 35 emitted family/tier records; focused tests pass 2/2. The 2+ independent-family source-linkage tier reaches 57,641/141,040 endpoint occurrences at top-5,000 (40.87%). This is not live graph coverage, semantic equivalence, rights clearance, safety, or M-A completion; R39/R40 remain open/quarantined. The generator and aggregate stay local-only. See LEAD v408 and its receipt.

### M-I / LEAD v407 one-shot recovery failure-control tests (2026-10-21)

Six mocked/temporary-directory tests pass for the no-retry recovery helper: no download while RUNNING, exactly one download after COMPLETE, preserved partial/invalid bundles, and refusal to overwrite an existing destination. No remote call occurred. This validates helper control flow only; the already-recovered real bundle remains separate evidence. See LEAD v407 and the shared evidence JSON.

### M-I / LEAD v406 CPU benchmark-contract tests (2026-10-21)

The 15-test benchmark suite passes on CPU, including both inference arms on a tiny Mistral and the score/parser/failure/logging contracts. This validates harness behavior only; it is not the real GPU smoke, another validation run, or quality evidence. No retry or new Kaggle launch occurred. See LEAD v406 and its shared evidence receipt.

### M-B / LEAD v405 wider data-preparation test sweep (2026-10-21)

Six focused local modules covering identity/join audit, source-only pools, partition packaging, output collection, and scoring contracts pass 44/44. These are CPU contract tests on the dirty research checkout, not GPU inference, benchmark quality, rights clearance, or M-B acceptance. See LEAD v405 and its shared evidence JSON.

### M-B / LEAD v404 focused pool-builder and verifier tests (2026-10-21)

The focused pytest suite for source-only case construction, GISMo candidate ranking, and fail-closed pool verification passes 15/15, covering label-key exclusion, identity/hash checks, candidate validation, and graph catalog mapping. This supplements LEAD v403's full-split artifact verification but is still synthetic-contract/input-preparation evidence, not inference, evaluation, rights clearance, or M-B completion. M-B/M-D/L44 remain open. See LEAD v404 and the shared evidence JSON.

### M-B / L44 full-split pool replay verification (2026-10-21; derived from LEAD v403)

Independent verification confirms deterministic full validation and test source-only GISMo top-50 pools: 10,729/10,747 cases and 536,450/537,350 candidates, with matching pool and manifest hashes across each original/replay pair. All candidate IDs match the pinned graph catalog; target labels were not read and no evaluation or selection occurred. This is input-preparation evidence, not M-B completion, Senath inference, or model-quality evidence. Rights/protocol and blinded-fixture gates remain open. See LEAD v403 and its evidence JSON.

### M-A / LEAD v402 current HGAT kernel metadata (2026-10-21)

The official SDK reports current saved kernel version 10, last-run time Oct 17, and attached source `paultimothymooney/recipenlg`. It provides no dataset version/hash or binding between version 10 and the listed outputs, so this narrows input identity only. M-A remains open; see LEAD v402 and evidence JSON.

### M-A / LEAD v401 Kaggle output-list schema check (2026-10-21)

The official SDK output-list response exposes filenames and signed URLs, but no file hashes or explicit run/version binding. URLs were not printed or fetched. This confirms the queried inventory endpoint cannot close M-A provenance; it does not rule out another run-history source. See LEAD v401 and evidence JSON.

### M-A / LEAD v400 current Kaggle HGAT inventory (2026-10-21)

The official CLI reports the separate HGAT kernel COMPLETE and lists expected producer outputs. Its currently saved notebook source hashes to `45e44b7e01b02bb4593e5bf88e49abab8b00d356014a6dcc6eb88cc73e8e9e0b` and shows a RecipeNLG-based graph/HGAT pipeline. The interface did not bind that source to a completed run version or expose output hashes, so provenance remains partial and these files are not Senath artifacts. No outputs were downloaded; see LEAD v400 and evidence JSON.

### M-A / LEAD v399 installer retrieval trace and scope correction (2026-10-21)

The broader workspace contains a tracked installer for a separate HGAT Kaggle kernel, and its ignored local mirror is byte-identical to the current service files. The installer records no output version, manifest, or expected digests and does no post-download hash check, so it does not establish Senath provenance or runtime readiness. This narrows LEAD v398's “not found” observation to the AI-service subtree; the unsafe deserialization finding remains. M-A stays open. See LEAD v399 and its evidence JSON.

### M-I / LEAD v385–v386 recovered validation execution and replayable showcase (2026-10-20)

The one-shot v4 output recovery and local ingester validated 200/200 records for each arm with no method failures; the showcase is regenerated from saved outputs and receipt-bound. This advances M-I's execution slice but does not settle its separately listed PR15 reference-code defects or complete M-I's acceptance. Full-split validation/test, protocol, rights, and human-evidence gates remain separate. L44 and M-I stay open; no method choice or product-quality claim follows. See LEAD v385–v386 and the canonical strategy's L44 boundary.

### M-A / LEAD v398 graph artifact deserialization and provenance blocker (2026-10-21)

The tracked AI-service loader reads the ignored, untracked PyG subgraph with `weights_only=False`; a replacement artifact could execute pickle code. The local bytes were inspected only with a restricted loader, but provenance is missing and runtime serving is not ready. Preserve the files; M-A remains open until artifact provenance and safe loading are established. See LEAD v398 and its evidence JSON.

### M-A / LEAD v397 AI-service graph artifact and readiness boundary (2026-10-21)

The registry marks `graph-rag` active but not serving-ready; a restricted local inspection counted 195,600 nodes and 7,247 serialized arcs without establishing that the artifact is loaded or usable. The 4,591-arc historical claim remains unreconciled. M-A stays open; R39/R40 stay quarantined. See LEAD v397 and its evidence JSON.

### M-A / LEAD v396 live route on source-built artifact (2026-10-21)

After a backed-up local Maven install and guarded monolith restart, the graph route returns 200 from the exact source-built JAR. It exposes 92 nodes/25 canonical edges, not all 109 embedded statements (3 alias-only, 81 unresolved). Local runtime binding improved; producer lineage, AI-service artifact, and Round-0 reconciliation remain open. See LEAD v396 and evidence JSON.

### M-A / LEAD v395 full Culinary reactor suite (2026-10-21)

Offline reactor tests pass 140/140 (including graph controller 2/2 and graph service 6/6). This validates current source tests, not live artifact lineage or runtime graph truth; M-A remains open. See LEAD v395 and evidence JSON.

### M-A / LEAD v394 reproducible source build (2026-10-21)

Current monolith `master` builds offline with the graph route present; the focused controller test passes 2/2. This does not prove the running process uses that artifact; no install/restart occurred. See LEAD v394 and its evidence JSON.

### M-A / LEAD v393 artifact provenance metadata (2026-10-21)

The loaded Maven artifact has no sources JAR, SCM/commit metadata, or Maven build timestamp/number in the inspected POM/metadata. Its source revision remains unknown; see LEAD v393 and its evidence JSON.

### M-A / LEAD v392 loaded graph-controller artifact (2026-10-21)

The live Java classpath resolves a local Maven `culinary:1.0.0-SNAPSHOT` jar whose controller bytecode has no graph route, explaining the 404 despite the current worktree source declaring it. The artifact’s build-source commit remains unknown; M-A stays OPEN. See LEAD v392 and its evidence JSON.

### M-A / LEAD v391 substitution endpoint spot check (2026-10-21)

For `butter`, the dedicated substitutions endpoint returns 2 items, matching the ingredient record’s embedded count. This is a one-record API contract cross-check only; M-A remains OPEN. See LEAD v391 and the compact evidence JSON.

### M-A / LEAD v390 graph route check (2026-10-21)

Local ingredient and technique collection GETs return 200, while the source-declared graph GET returns 404. This is evidence of route availability mismatch, not proof of the running artifact’s identity or of any graph source. M-A remains OPEN; see LEAD v390 and `evidence/lead-ma-graph-route-check-20261021.json`.

### M-A / LEAD v389 local knowledge API snapshot (2026-10-21)

Read-only local knowledge endpoints returned HTTP 200 and yielded 92 ingredient and 25 technique records. Seed-name overlap is 17/25 ingredients and 3/16 techniques, not lineage proof. In the analyst interpretation, all 109 embedded substitution statements are retained as arcs (28 exact-name resolutions; 81 literal unmatched targets). This does not prove persisted graph edges, seeder provenance, deployment state, or reconcile the quarantined Round-0/HGAT claims. M-A stays OPEN and R39/R40 stay quarantined. See LEAD v389 and `evidence/lead-ma-local-runtime-snapshot-20261021.json` for hashes and scope limits.

### M-A / LEAD v388 static graph/coverage recheck (2026-10-20)

The source-family coverage curve regenerated byte-identically and the focused graph-truth/coverage suite passes 8/8. The current local graph-truth builder emits a new schema/output hash versus the historical snapshot, adding an explicitly source-only seed-literal interpretation; this is not runtime graph evidence. The builder is still untracked in the dirty root worktree, so no implementation is promoted. M-A remains open pending runtime lineage/access and Round-0 claim reconciliation. See root SCOPE LEAD v388.

### M-D / L44 full-split source-only candidate pools (2026-10-20; derived from LEAD v387)

Existing GISMo top-50 candidate pools are freshly verified for all validation/test source-only cases (10,729/10,747; 50 candidates each; exact pinned graph ID/name identity). The verifier confirms target labels were not read and no evaluation or method selection occurred. These are local input-preparation artifacts, not model outputs; rights, protocol, partition and execution gates remain open. See canonical SSoT §17, SCOPE/SESSION v387 and the LEAD evidence note.

### M-D / L44 recovered 200-case validation comparison (2026-10-20; derived from LEAD v385–v386)

The frozen 200-case validation run is recovered and locally ingested: both arms have 200/200 records and zero method failures. GISMo-identity Hit@1/5/10 is 3.0/11.5/14.0% for generation and 1.5/7.5/14.5% for top-50 likelihood reranking; raw-string sensitivity is reported separately. Latency and case-level outputs are preserved in the local recovery bundle. LEAD v386 verifies its replayable 200-case saved-output showcase (hash-pinned HTML; focused renderer/ingester suite 15/15). This is a bounded validation diagnostic, not full deterministic GISMo/lookup outputs for every case, an independent test, method choice, quality, or human-usefulness evidence. M-D and L44 remain open. See canonical SSoT §§1.2/17 and LEAD v385–v386.

### M-D / L44 scorer join-integrity update (2026-10-20; derived from LEAD v384)

AoE review tightened the local evaluator to require label and identity-map rows to agree on split, row index, recipe ID, and case ID, in addition to the target graph ID check. A resealed adversarial identity row is rejected; the focused scorer suite passes 10/10. This only strengthens future local analysis; no collected full-split outputs were scored and M-D/L44 remain open. Evidence: root `ai/evidence/senath-inference/full-split-local-score-recomputation-preparation-20261020.md`.

### M-D / L44 analysis-preparation update (2026-10-20; derived from LEAD v383)

A new label-local evaluator is ready for future collected validation/test outputs. It validates independent provenance pins, exact source/label/identity joins and full case coverage; verifies GISMo reranker candidates against each frozen top-50 pool; and recomputes Hit@1/5/10 under GISMo graph identity and a separately reported Senath-normalized raw-string sensitivity. Explicit method failures remain in the denominator and ambiguous generated names do not receive a GISMo identity. Nine synthetic tests pass, and the real local-only source-only shard packages load at their frozen 10,729/10,747 case denominators. No collected output has been scored, no inference is claimed, and this does not complete M-D/L44. Rights/protocol gates remain open. Evidence: root `ai/evidence/senath-inference/full-split-local-score-recomputation-preparation-20261020.md`.

> **AUTHORITY:** `academic_vision_and_strategy.md` (canonical SSoT) is the single source of truth. This backlog is derived from §17.2 (Member Packages), §12 (Product Proof), §13 (Compute), §25 (Bounded Task Recipes), and the critical dependency path in §17.4. When they conflict, the strategy file wins.
> **MIGRATION:** Updated from v3 on 2026-10-12 (LEAD v113). v3 "four trained models" UI work retired. New focus: graph v4, Explorer, evidence endpoint, demo, reproducibility.

> **Aligned 2026-10-04 with LEAD v101**, monolith `origin/master` commit `e2e0dfc` (latest update dated 2026-10-02).
> **Authority:** `academic_vision_and_strategy.md`, then `ai/out/iron-chef-lead-v2/BACKLOG_LEDGER.md`, then current updates in [BACKLOG_LEAD.md](BACKLOG_LEAD.md). The strategy and atomic ledger are absent from these four checkouts. Lead scientific claims below are attributed summaries, not independently reconstructed results.
> This replaces the historical member backlog, preserved in Git history. A checked engineering task means code exists with the cited checks; it does **not** certify a model, deployment, clinical safety or thesis result.

### M-I / L44 preparation update (2026-10-20; derived from LEAD v382)

A local-only structural collector now prepares deterministic recombination of future full-split partition output records. It independently pins the source-input and execution manifests, verifies every case appears once in its scheduled shard, checks source joins and rerank candidate identities against the frozen pool, rejects target-derived fields and invalid scores/ranks, and preserves explicit method failures as denominator misses. Eight synthetic collector tests and the combined source-shard/pool/catalog/benchmark suite pass 46/46. This does not create inference outputs, evaluate labels, authorize upload or complete M-I/L44; rights and protocol gates remain open. Evidence: root `ai/evidence/senath-inference/full-split-output-collector-v1-20261020.md`.

### M-I / L44 input-preparation update (2026-10-20; derived from LEAD v381)

The full validation and test source-only candidate inputs are now reproducibly partitioned into 54 ≤200-case local-only shards per split. All 108 shards passed independent hash, coverage, join, firewall, and pinned GISMo catalog-identity checks; focused preparation tests pass 35/35. This prepares an input-materialization subtask only; protocol/rights gates remain, shards are not uploadable/runnable packages, and no new inference/evaluation or M-I/L44 acceptance is claimed. Evidence: root `ai/evidence/senath-inference/full-split-source-only-partitions-v1-20261020.md`.

### M-B / L44 preparation update (2026-10-20; derived from LEAD v380)

The local-only full validation/test GISMo top-50 pools now pass an independent candidate ID/name check against the pinned 6,653-item ingredient catalog, using the released helper's 1-based row-position ID semantics. All 1,073,800 candidate entries passed; focused source-pool and benchmark tests pass 30/30. This closes a structural input-integrity subtask only. M-B acceptance remains open (including its blinded adversarial fixture and protocol prerequisites), and no inference or evaluation result is claimed. Evidence: root `ai/evidence/senath-inference/full-validation-source-only-gismo-pools-v1-20261020.md`.

## Current handoff

The v4 thesis demonstrates **measurement, not models**. Your work makes the measurement visible:

| Contribution | What You Make Visible | SSoT Reference |
|--------------|----------------------|----------------|
| **C1 Instrument** | Strata breakdown, audit pool construction, blinded labeling UI | §5, §6, §25.2–25.3 |
| **C2 Measurement** | Per-cell Hit@k / valid@k tables with intervals, collapse map figure | §5.4, §6.9, §12.8 Act 2 |
| **C3 Guardrail** | Cost-of-trust curves, cell status badges (ok/vacuous/no_guarantee), abstention panel | §8.7–8.8, §12.4, §12.6 |
| **C4 Human Evidence** | Rater agreement dashboard (κ with cluster CI), judge calibration report | §6.8, §7.5, §14.6 |
| **C5 Product Proof** | `/explore/graph` (real artifact), `/api/v1/graph/evidence` (shadow), 5-min recorded demo | §12.4–12.8, §25.7 |

**Progress boundary (2026-10-20; derived, not a completion score):** the L44-supporting M-B slice now has full local joins/identity strata for 49,044 train, 10,729 validation, and 10,747 test rows; source-only val/test frames with local-only labels; and byte-identical rebuild evidence. A hash-pinned FooDB endpoint-presence audit reports both-profiled pairs of 1,121/10,729 validation and 1,162/10,747 test, with partial/none and occurrence denominators separate; focused test suites pass. The earlier frozen 200-case/10,000-candidate package and label-free preflight remain distinct evidence. This is presence-only, not chemistry or quality, and does **not** satisfy PR03/PR09 prerequisites or M-B's blinded 40-case fixture; M-B remains open. See LEAD v369/v370 and root `SCOPE.md` for receipts and limits.

**M-I / L44 readiness update (2026-10-20):** a read-only preflight rechecked the prepared 200-case validation package's payload hashes, closed file inventory, frozen case/pool identities, and label firewall; six preflight tests and 21 benchmark/recovery tests pass. No network action was made, and this does not identify/verify a remote run or produce benchmark outputs. See LEAD v372 and the root package-preflight evidence note.

**M-B / M-D / L44 preparation update (2026-10-20):** source-only GISMo top-50 candidate pools now cover all 10,729 frozen validation source cases (536,450 candidates). The independent verifier checks exact case/source joins, split and manifest hashes, candidate count/uniqueness/finite scores/order, closed inventory, and target-key exclusion; a separate full replay reproduces pool and manifest hashes byte-for-byte. The generalized verifier's unchanged default also accepts the preserved full-test pool at its original hashes; the combined focused suite passes 28/28. Artifacts and pinned model/source assets remain local-only; labels were not read or uploaded. This is input preparation only—not Senath inference, an evaluation, a method choice, or quality evidence. M-B still has rights, protocol and blinded-fixture gates; M-D/L44 still need inference outputs and all downstream acceptance. See LEAD v379 and root `ai/evidence/senath-inference/full-validation-source-only-gismo-pools-v1-20261020.md`.

**M-B / M-D preparation update (2026-10-20; neither package is complete):** a new deterministic CPU builder produced 50 GISMo-ranked candidates for all 10,747 frozen, source-only test cases (537,350 candidates total), and a second complete build was byte-identical. The test-label pickle and local label frames were not opened; hashes bind the case frame, GISMo model/vocabulary/helper, and pinned FlavorGraph graph inputs. Exact row joins, 50 unique candidate IDs per case, finite logits, tie order, output inventory, and label-key exclusion verify; 8 focused tests pass. This is a source-only candidate-pool preparation artifact, not Senath inference, a benchmark result, method selection, or model-quality evidence. M-B still requires its blinded fixture and remaining rights/protocol gates; M-D/L44 still require arm outputs and inference. Evidence: LEAD v373–v374 and root `ai/evidence/senath-inference/test-source-only-gismo-pools-20261020.md`.

**No VLM/CLIP camera integration.** No "four trained models" UI. No behavioral learning dashboard. The bounded negatives chapter covers what didn't work.

**Rights boundary (2026-10-20; M-F remains open):** official Recipe1M/GISMo pages inspected so far do not establish dataset-specific or per-case Recipe1MSubs reuse/redistribution permission. GISMo's code-license statement is not a dataset grant; per-case rights remain `UNKNOWN`. Preserve the actual registered access terms for the dataset/version used and map them to research, derived data, storage, and release—or keep case-level content out of release. See LEAD v371 and the bounded audit note in root `ai/evidence/senath-inference/`.

## EPIC 1: Graph explorer infrastructure — IMPLEMENTED, coverage partial

## Member Work Packages (from SSoT §17.2)

| ID | Deliverable / Acceptance | Dependencies | Effort `[P]` |
|----|--------------------------|--------------|--------------|
| **M-A** | **Graph & coverage truth:** `GRAPH_TRUTH.csv`, `COVERAGE_CURVES.csv` with provenance labels. Seed-graph degree-sum check passes; every Round 0 number reproduced or struck. | Real seed arrays, runtime artifact access | 10h |
| **M-B** | **Deterministic strata, identity map, candidate pool builder, validation-score check:** Files in §5.5; pool builder using Appendix A functions. Interim v3 split/identity recount exists (LEAD v436; note in research commit `03ab020`), but raw test CSV/source-join reproduction and remaining M-B acceptance are open. Regenerate bit-for-bit; pool builder deterministic; fixture-tested adversarially. | PR03/PR09 resolved; official splits | 12h |
| **M-C** | **Explorer & evidence endpoint (flagged, shadow):** §12.4–12.5 implementation. T-03 to T-09 pass. Feature flag OFF by default. Real artifact only; tampered artifact refuses render. | M-A graph artifacts; PR04/PR06/PR11/PR12 resolved | 25h |
| **M-D** | **Complete deterministic GISMo & lookup outputs:** `arm_outputs/{gismo,lookup}_{val,test}.jsonl`. Every case present; deterministic across two runs. **Artifact acceptance met locally (LEAD v469)**; recursive label firewall and strict ID/rank typing are regression-tested. Keep handoff/rights, historical tie-equivalence, and downstream L44 separate and open; they do not undo the local file-generation result. | M-B case frames; official GISMo checkpoint | 8h |
| **M-E** | **Demo build, recorded Act 3, test suite:** §12.7–12.8. T-10, T-11, T-12 pass. Act 3 recorded from frozen files (not live API). Clean checkout, network disabled except API arms. | M-C Explorer; L38/L39 results | 12h |
| **M-F** | **First-party recipe inventory & license check:** Count and license table. Number stated; unclear licenses marked `UNKNOWN`. | App access; legal review | 4h |
| **M-G** | **Reconstruct the R01–R45 claims register from §15.1:** reconcile claim provenance and current Lead-item mappings; preserve unsupported rows as UNVERIFIED/OPEN/PARTIAL. No statuses inherit from the migration seed. | SSoT §15; source artifacts and reproductions | 6h |
| **M-H** | **README & reproducibility kit:** One command from clean checkout to a main figure. Third person reproduces Table 1 on their machine. | All evidence + scripts | 8h |
| **M-I** | **Job manifests & runner (fixture-tested):** independent implementation of the active §13.1–13.2 contract plus refusal/happy-path fixtures. The Appendix A reference code is not reusable unless PR15 is separately resolved. | Active §13.1–13.2 contract; no dependency on copying Appendix A | 6h |

## EPIC 2: Evaluation dashboard shell — IMPLEMENTED, evidence contract partial

## Critical Dependency Path (from SSoT §17.4)

```
L41 (raters/ethics) → L36 (audit) → L43 (judge) → L38 (calibration) → L45 (writing) → L46 (defense)
         ↑                              ↑
    M-B (pools)                   M-D (arm outputs) ← L44 (inference)
M-A (graph truth) → M-C (Explorer) → M-E (demo)
L42 (novelty) → L45 (claim wording)
L39 (allergen) → L45
```

**Your blocking items:** M-A (unblocks M-C), M-B (unblocks L36/L38), M-D (unblocks L38/L44). Prioritize M-A → M-B → M-D → M-C → M-E.

## EPIC 4: Camera scaffold — IMPLEMENTED, model capability open

## Phase 1: Truth & Infrastructure (Weeks 0–2)

### M-A: Graph & Coverage Truth (10h) — **START WEEK 0**
> SSoT §12.1–12.3, §25.1 (P1)

- [ ] Locate actual seed arrays (`KnowledgeGraphSeeder.java`, `18-knowledge-graph.js` or equivalents)
- [ ] Compute by-type nodes/edges, degree min/median/mean/max, isolates
- [ ] Verify degree-sum invariant appropriate to graph type (not blindly undirected)
- [ ] Load runtime AI service artifact: path, SHA-256, node/edge counts by type, isolated nodes
- [ ] Locate hardcoded claims for node/ingredient counts in docs/README/pitch (file+line)
- [ ] Emit `GRAPH_TRUTH.csv` and `COVERAGE_CURVES.csv` with provenance labels
- [ ] **Acceptance:** Seed-graph degree-sum check passes; every Round 0 number reproduced or struck

### M-B: Deterministic Strata, Identity Map, Pool Builder (12h) — **START WEEK 1**
> SSoT §5.5, §25.2 (P2), Appendix A functions (review only, not mandatory)

- [ ] Implement `build_strata.py`: GISMo primary identity, Senath secondary, seen/unseen (one frozen function on train/val/test), evidence_coverage (FooDB both-endpoints), cell key (seen\|unseen × profiled\|not)
- [ ] Emit: `cases_val.jsonl`, `cases_test.jsonl`, `strata_counts.csv`, `identity_map.csv`, `manifest.json` (hashes + script git SHA)
- [ ] Implement `build_pools.py` using `stratified_sample()` and `blinded_pool()` from Appendix A (do not reimplement)
- [ ] **FIXTURE FIRST:** 40-case / 3-arm fixture → tests proving:
  - (a) Rerun with same seed = identical
  - (b) Rater view contains no arm name/rank
  - (c) Gold appears in pool
  - (d) Missing arm output fails closed
  - (e) Per-stratum minimums honored
  - Test duplicate candidates, insufficient strata, identity collisions, missing metadata
- [ ] After fixture passes: run on real val/test case files; report actual counts/hashes
- [ ] **Acceptance:** Regenerate bit-for-bit; pool builder deterministic; fixture tests pass

### M-F: First-Party Recipe Inventory & License Check (4h) — **START WEEK 1**
> SSoT §17.2 M-F, §10.8, U8

- [ ] Count first-party ChefKix recipes available for private-Western cell
- [ ] Record license per recipe/source
- [ ] Mark unclear licenses `UNKNOWN`
- [ ] **Acceptance:** Count + license table delivered; numbers stated

## EPIC 6: Allergen safety UI — IMPLEMENTED surface, comparison open

## Phase 2: Arms & Explorer (Weeks 2–4)

### M-D: GISMo & Lookup Arm Outputs (8h) — **START WEEK 2**
> SSoT §5.7, §13.3

- [x] Generate ranked top-M (M=20) lists for GISMo released checkpoint on all val (10,729) + test (10,747) cases — local-only outputs in `ai/out/senath-m-d-gismo-top20-v1`; provenance in LEAD v465
- [ ] Generate lookup-frequency baseline ranked lists on same cases
- [ ] Emit `arm_outputs/gismo_val.jsonl`, `gismo_test.jsonl`, `lookup_val.jsonl`, `lookup_test.jsonl`
- [x] Verify determinism: two runs produce identical outputs — fresh replay and hashes in LEAD v465
- [ ] **Acceptance:** Every case present; deterministic across runs

### M-C: Explorer & Evidence Endpoint (25h) — **START WEEK 2 (after M-A artifacts)**
> SSoT §12.2–12.7, §25.7 (P6)

- [ ] Build `graph_eval_v4` (train-only substitution edges; test asserts no val/test pair) and `graph_product_v4` (all rights-permitted) with provenance fields (§12.2)
- [ ] Emit statistics: nodes/edges by type, degree distribution, components, source families per ingredient, occurrence-weighted coverage (top-N: 50/100/250/500/1000/2000/5000), both-endpoint coverage on test
- [ ] Implement shadow endpoint `GET /api/v1/graph/evidence` (§12.5) — **additive, read-only, does not change `/suggest_substitutions`**
- [ ] Implement `/explore/graph` (§12.4): feature flag OFF by default; real artifact only; search → 1–2 hop ego network (1-2 hop ego network); unknown identity explicit; provenance footer; <1s laptop render; tampered artifact refuses render
- [ ] Run acceptance tests T-01 through T-12 (§12.7) + existing suites covering touched code
- [ ] **Acceptance:** T-01–T-12 pass; existing suites pass; feature flag OFF by default

### M-I: Job Manifests & Runner (6h) — **START WEEK 2**
> SSoT §13.1–13.2 and §25.1. Appendix A/reference-code reuse is conditional on a separate PR15 resolution; an independently implemented runner may proceed without copying that code.

- [ ] Implement the §13.1 manifest fields: job ID, script and hash, input paths and hashes, fresh output directory, maximum USD/calls/minutes, stop conditions, idempotency key, and freeze eligibility.
- [ ] Implement an independent fail-closed runner: verify script/input hashes and eligibility before launch; enforce budgets/stop conditions; create a unique output directory; bind identities, outputs, exit status, spend/call/time counts, and logs in a receipt.
- [ ] **FIXTURE FIRST:** Happy path plus refusal/stop fixtures for script or input hash mismatch, missing inputs, placeholder/invalid manifest fields, ineligible freeze, zero/exceeded budget, repeated idempotency key, output collision, nonzero child exit, API error rate >5%, judge invalid-output rate >2%, and missing required outputs.
- [ ] **Acceptance:** Fixture tests pass; runner refuses bad manifests

## EPIC 7: Feedback instrument — IMPLEMENTED, product evidence partial

## Phase 3: Demo & Reproducibility (Weeks 4–6)

### M-E: Demo Build & Test Suite (12h) — **START WEEK 4 (after M-C, L38/L39 results)**
> SSoT §12.7–12.8, §25.7 (P6)

- [ ] Record Act 3 from frozen files (side-by-side raw LLM vs filtered+calibrated on same input) — **not live API**
- [ ] Implement T-10: demo runs with network disabled except API arms (recorded fallback exists)
- [ ] Implement T-11: no secret in any log/response/artifact (scan clean)
- [ ] Implement T-12: all existing suites covering touched code still pass (report verbatim)
- [ ] **Acceptance:** T-10, T-11, T-12 pass; recorded Act 3 loads; abstain panel reachable

### M-G: Claims Register Artifact Reconstruction (6h) — **START WEEK 4**
> SSoT §15, §25.5 (P5), §17.2 M-G

- [ ] Start from the current 45-row §15.1 migration inventory; do not inherit the old seed's statuses, dates, dummy hashes, L-item mappings, or self-test claims.
- [ ] Reconcile each row's claim wording, source provenance, citation, and actual Lead-item boundary. Find the primary local artifact where applicable, hash its actual bytes, and reproduce the claimed value before marking VERIFIED. Keep citations separate from local artifact hashes.
- [ ] Write claim/evidence mismatches to `EVIDENCE_VS_CLAIM.csv`; never silently rewrite the empirical claim to fit a convenient artifact. Preserve unresolved items as UNVERIFIED/OPEN/PARTIAL and leave reproduction fields empty unless the row is VERIFIED under §15.
- [ ] Run `python ai/tools/validate_claims_register.py <register.csv>` and record its output. A pass establishes schema/local-byte integrity only; it does not execute reproductions or prove claim truth. Do not generate a progress scoreboard from the seed or validator output.
- [ ] **Acceptance:** all R01–R45 IDs are unique and complete; required fields, provenance, citations, paths, hashes, and status-specific reproduction requirements validate; every mismatch is documented; no status exceeds its actual evidence. L48 stays open until lead review accepts the reconciled register.

### M-H: README & Reproducibility Kit (8h) — **START WEEK 6**
> SSoT §17.2 M-H, §24.3

- [ ] One-command script from clean checkout → main figure (collapse map or cost-of-trust)
- [ ] Third-person clean-machine reproduction test (documented)
- [ ] README: data sources, licenses, how to run each pipeline, artifact locations, freeze dates
- [ ] **Acceptance:** Third person reproduces Table 1 on their machine

## EPIC 8: Photo intelligence — ADAPTERS IMPLEMENTED, accepted models blocked

## Phase 4: Parallel Support (Ongoing)

### Overnight Factory Jobs (from SSoT §13.5)
> Eligible for unattended runs (idempotent, hash-logged, stop conditions):

| Job | Inputs | Output | Stop Condition |
|-----|--------|--------|----------------|
| Strata/identity regeneration | Official splits | `cases_*.jsonl`, `strata_counts.csv`, manifest | Count mismatch |
| Candidate pool builder | Arm outputs, audit sample | Blinded pools + private key | Missing arm output |
| Arm inference (rank arms) | Cases | Top-M lists | Missing case, nondeterminism |
| Arm inference (API) | Cases, key | Top-M lists + call logs | Cost cap, error rate >5% |
| Judge labeling | Pools | Judgments JSON | Invalid rate >2%, cost cap |
| Calibration + diagnostics | Val labels | `t̂` tables (`t-hat tables`, `t_hat`), diagnostics | Any diagnostic flag (no auto-continue) |
| Coverage curves | Adjudicated identities | `COVERAGE_CURVES.csv` | Identity hash mismatch |
| Graph build + verifier | Sources | Two artifacts + stats | Provenance gap |
| Novelty query runner | Query list | Hashed raw responses | Rate limits hit repeatedly |

**Morning review (10 min daily):** Receipt present, exit status, spend vs cap, stop conditions, diagnostics, row counts, warnings. **You decide; factory does not.** See §13.8 for the canonical review policy.

## EPIC 10: Graph with real data — BOUNDED API IMPLEMENTED, import pending

## Acceptance Test Checklist (from SSoT §12.7 — all must be automated/scripted)

| ID | Test | Pass Condition |
|----|------|----------------|
| T-01 | `graph_eval_v4` contains no val/test pair | 0 leaks |
| T-02 | Rebuild from sources reproduces both artifacts bit-for-bit | Hashes equal |
| T-03 | Explorer loads with real artifact, hash shown | Visible hash |
| T-04 | Explorer with tampered artifact refuses render | Refusal message |
| T-05 | Unknown ingredient → "not in graph", no invented node | Verified |
| T-06 | Evidence endpoint returns `abstain` for `vacuous` cell (abstain for vacuous) | Verified |
| T-07 | Evidence endpoint returns `show` only when `status=ok` (show only when status=ok) and `t_hat ≤ gate` (t_hat <= gate) | Verified |
| T-08 | Allergen: unknown candidate withheld for allergy-sensitive request | Verified |
| T-09 | `/suggest_substitutions` unchanged by feature flag | Diff empty |
| T-10 | Demo runs with network disabled except API arms (fallback recorded) | Verified |
| T-11 | No secret in any log/response/artifact | Scan clean |
| T-12 | All existing suites covering touched code pass | Report verbatim |

- [x] G2: `/thesis` maps all 14 Lead chapters, replacing retired member numbering.
- [x] Compound → 8; safety → 9; photo → 11; architecture → 12; behavioral/evaluation → 13. Remaining chapters have explicit Lead handoff entries.
- [x] Unique artifact IDs and pending source-dependent captures; ready-to-capture is not completed evidence.
- [ ] G9: obtain canonical strategy/atomic ledger, audit captions and capture real-source figures/screenshots.
- [ ] G9: deployed regions/URLs, source fingerprints, auth/quotas/recovery and measured costs; no $0 claim from a plan.
- [ ] G9: ethics/recruitment/user study, independent allergen/image reviews and recorded/scored defense rehearsal are human/Lead dependencies.
- [ ] Reconcile preserved sprint drafts before submission. They are schedule drafts, not current completion evidence.

## Evidence Integrity Rules (same as Lead)

- Never mark COMPLETE because code was written — require executable evidence.
- Never compare cancelled run or paper number to locally executed result.
- Never report metric without dataset, split, protocol version, seed count.
- Negative results are thesis contributions — report honestly.
- Every number carries a claim ID. Unverified = say so.
- Fixture-test before real data. Durable scripts in approved files. Full paths, `-LiteralPath`.
- Label numbers: `COMPUTED_NOW`, `QUOTED_FROM_ARTIFACT` (path/key), or `UNVERIFIED`.

Contract/check: frontend `docs/epic-12-voice-vision-copilot.md`, `src/features/voice-copilot/__tests__/voiceCopilotService.test.ts`.

## Appendix: Mapping from v3 Member Backlog (for reference only)

| v3 Epic | v4 Treatment |
|---------|--------------|
| EPIC 1 (Graph Explorer) | M-C (real-data Explorer, feature-flagged, hash-pinned) |
| EPIC 2 (Eval Dashboard) | M-E (demo) + thesis figures regenerated from scripts |
| EPIC 3 (Allergen Onboarding) | Separate product work; not thesis contribution |
| EPIC 4 (Camera Scaffold) | Retired — no VLM/voice in v4 |
| EPIC 5 (Compound UI) | C5 product proof: evidence endpoint shows compound presence (presence only, not flavor) |
| EPIC 6 (Allergen Killshot UI) | C5: evidence endpoint + Explorer show allergen flags + abstention |
| EPIC 7 (Feedback Instrument) | Separate product work; behavioral learning not a thesis contribution |
| EPIC 8 (Photo Pipeline) | Retired — photo chain is bounded negative (179/783 correct) |
| EPIC 9 (Eval Dashboard Wire) | M-E + thesis figure regeneration |
| EPIC 10 (Graph Explorer Wire) | M-C |
| EPIC 11 (Thesis Screenshots) | M-H reproducibility kit; figures regenerate from scripts |
| EPIC 12 (Voice Copilot) | Retired — Tier C, not in v4 scope |
