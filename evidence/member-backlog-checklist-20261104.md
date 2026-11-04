# Member backlog M-A–M-I shared checklist

**Observed:** 2026-11-04. **Status owner:** Member records delivery state; Lead's scientific/protocol acceptance is a separate field and is never inferred from a commit, test count, or artifact count. Use only `TODO`, `IN_PROGRESS`, `READY_FOR_LEAD`, `ACCEPTED`, or `BLOCKED`; every `BLOCKED` row names the exact missing input or decision.

This checklist is the shared checkpoint record for [`BACKLOG_MEMBER.md`](../BACKLOG_MEMBER.md). No contribution percentage is calculated. Inputs and outputs under `C:\Users\LENOVO\ResearchData` are local-only and are not made available to other machines by this file.

## Checkpoint 0 — baseline before more experiments

The member/source baseline was inspected before the new M-A build. The original dirty monolith worktree was preserved; no changes were made there.

| Repository | Worktree/branch and source commit | Default branch commit observed | MR/PR inspection |
| --- | --- | --- | --- |
| Monolith, clean `-latest` checkout | `codex/member-sync-all-20261024`; `4c87b6decaf417da7a6f6d65ba431fdde1409d24` | `master` at the same `4c87b6d…`; this is merged PR #48 | PR #48 is merged. Local branch contains the default commit and is one commit ahead of its old sync-branch remote ref. Test-created untracked `tools/research/t535/ai/tools/__pycache__/` remains; removal was blocked by the local command policy. |
| Monolith, original checkout | `codex/member-backlog-image-evidence-20261024`; `daa30f5…` | Separate checkout, many pre-existing local modifications/untracked files | Preserved; not used for edits. |
| AI service | local `codex/member-graph-evidence-20261104`; `601419a0765d593ebb96a53b37831831a8e4f379` (base `f6c5651…`) | `main` at `cc6acb0ca5256225d3d3736ba8aa3c83624c2e09`; local branch is four commits ahead | No push or MR was created. Private-repository open-MR state could not be queried with installed tools (no `gh`; anonymous GitHub API returned 404). |
| Frontend, clean `-latest` checkout | `codex/member-sync-all-20261024`; `dfcb6ae3efc988837e85584487807e238b6d174c` | `main` at the same `dfcb6ae…`; this is merged PR #86 | PR #86 is merged. Local branch contains the default commit and is one commit ahead of its old sync-branch remote ref. |
| Infrastructure | `master`; `62951ed85f97bd9491c8d3fba02338046994c9f3` | `master` at the same commit | Private-repository open-MR state could not be queried with installed tools (no `gh`; anonymous GitHub API returned 404). No changes made. |

### Baseline discrepancies and artifact inventory

- The Leader summary says C2/C3/C5 each earned 5/5 and R38 is VERIFIED. The operational rows say C2 has no paid output, C3 has no accepted export, and T01–T12/C5 acceptance is incomplete. The cited graph bundle (`graph_eval_v4_clean`, manifest `ade85c33…`, 6,633 nodes) and its browser proof are absent from every checked-out repository and the searched local research workspace. No campaign ledger, T52 map, frozen Protocol v1 bytes, R01–R45 CSV, or consolidated Q20 receipt was located. The contradiction remains for Lead to reconcile; no credit is assigned to an artifact whose bytes cannot be checked.
- Recipe1M metadata is present locally (`layer1.json`, `layer2.json`, `det_ingrs.json`); the image archive is not present and was not downloaded. Recipe1MSubs train/validation/test pickles are present with the hashes in the source audit; the 70,520-row ID-to-source-partition join is already verified (0 missing, 0 wrong split), so it was not repeated. GISMo runtime, vocabulary and FlavorGraph catalog are locally pinned. The exact historical positional-index-to-recipe-ID mapping is still unverified: replay gaps are train 1,044, validation 230 and test 220.
- The new M-A candidate bundles are local `...run3` and `...run4` and match all hashes in the linked receipt. Superseded `...run1`/`...run2` include all 8,298 catalog nodes and are excluded (manifest SHA-256 `7b9986f66c83863d23b31efefb6eb85137996aa7fd8930116737bdd921097e55`); the local command policy blocked removal, so they remain. The original monolith worktree is still separate and dirty; no edits were made there. Monolith `-latest` also has a test-created untracked `tools/research/t535/ai/tools/__pycache__/`, retained after the same removal block and not staged.
- The local FooDB 2020-04-07 dump is present. Current local key artifacts contain 797 keys (SHA-256 `605a79c510242a4ef459f51a711142783dbbc38066a227503a795d24e6e804c1`) and 791 content-backed keys (SHA-256 `4aac2058e3beb996322acaeb77488a6b6b5805604d4c9f53db64b5ceb5d53c02`), not the required 971-profile artifact. Existing exact-primary-name coverage receipt reports 427/10,729 validation and 410/10,747 test cases with both endpoints covered; alias-expanded coverage remains a separate sensitivity only. No alias crosswalk was used to assign primary strata.
- GISMo source-free exporter/package tools exist only in the T535 handoff. They are not the C2 request-package, forecast, Batch transport, attempt journal, or ingestion pipeline. The handoff's focused suite was run once: 9 failed, 74 passed; failures trace to a stale MKL notice hash (`SOURCE.json` and tools pin `bfda9286e31251fdc9eaacddb1856c5e73342126efcf6e34a53f1d55ddd54f4b`; bundled bytes hash to `203fdbf14e4e7ff8fc8c2e2bde99d88f4ece1fe3e6841a672fdfaf2c96cf7821`, including after LF normalization). The handoff was not silently re-pinned. Runtime archive, C3 export, original graph, acceptance map, campaign ledger, claim register, and defense-pack inputs were not found locally at the cited paths.
- Browser/runtime material found is fixture/legacy graph evidence plus the narrow T503 synthetic direct-loopback isolation receipt (accepted by Lead only for T09 in that configuration). It is not an accepted-graph browser/Next run. A prior C3 CPU probe is environment evidence, not a C3 export. The 44 ChefKix database rows are covered by an internal-use metadata inventory; all 44 record-level licenses are UNKNOWN.

## Rights and operation matrix

`UNKNOWN` means no basis found for that operation. The table is deliberately about the exact operation; code access or local research scope does not grant data transfer, redistribution, or product rights.

| Source | Local/offline processing | Kaggle/processor transfer | External API | Publish metrics/results | Distribute artifact / product use | Evidence or exact request |
| --- | --- | --- | --- | --- | --- | --- |
| Experiment code | Apache-2.0 applies to the pinned reproduction code only | Code license permits use subject to its terms; this does not carry dataset rights | Same code-only scope | Cite and retain notices | Code may be reused subject to Apache-2.0; no data/model implication | Upstream code commit `14014ad…`; member source/license audit. |
| Recipe1M metadata | Supported for the stated non-commercial university thesis, conditional on authorized recipient and accepted access terms | UNKNOWN; supplied terms do not address third-party cloud transfer | UNKNOWN | UNKNOWN for derived publication absent terms review | Raw redistribution not cleared; product/commercial use not cleared | User-provided rights-holder email summary and official research-access notes in `evidence/member-recipe1m-rights-holder-email-terms-20261025.md`. No images were needed for M-A. |
| Recipe1MSubs pickle/comment payload | Local non-commercial thesis analysis is supported by the existing terms review with an explicit caveat; exact file-level grant and underlying comment rights remain UNKNOWN | UNKNOWN; no Kaggle upload authorized | UNKNOWN | UNKNOWN until exact file/source terms and publication scope are resolved | Redistribution/product use UNKNOWN/not cleared | `evidence/member-recipe1msubs-use-terms-review-20261025.md` and `evidence/member-mb-mf-recipe1msubs-source-license-audit-20261024.json`. A specific request must ask whether the three named pickle files and comment-derived contents are covered, and whether the proposed derived output/transfer is permitted. |
| FooDB 2020-04-07 | Non-commercial local analysis is supported by FooDB's published CC BY-NC 4.0 notice, with attribution and license conditions | UNKNOWN for this processing setup; get confirmation before upload | UNKNOWN for API processing/transfer | Aggregate disclosure still needs exact source attribution and scope review | Redistribution and product/commercial use are not cleared by this checklist | Official [FooDB compliance page](https://foodb.ca/compliance) states data/metadata are CC BY-NC 4.0; [downloads page](https://foodb.ca/downloads) points users to use/distribution terms. The pages returned 403 to direct open in this environment; source archive is the locally pinned 2020-04-07 dump. |
| Derived graph, strata, model, request/output packages | Keep within the local, scoped research work described above; use conditions inherit from all inputs | UNKNOWN; no transfer | UNKNOWN; no paid API | UNKNOWN until input rights and exact output are reviewed | UNKNOWN; no product/redistribution authorization | Derived artifacts do not gain broader rights than source inputs. M-A bundles remain local-only. |
| 44 internal ChefKix records | Internal project metadata inventory only, under owner attestation | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN; not cleared for external/product publication | `evidence/member-mf-recipe-license-inventory-20261024.json`; 44 rows, no content/user IDs exported, all record licenses UNKNOWN. |

## Checkpoint ledger

| Checkpoint | Status | Owner | Source commit / data or output hash | Evidence | Exact missing input or condition | Lead decision |
| --- | --- | --- | --- | --- | --- | --- |
| 0 — baseline and evidence reconciliation | `BLOCKED` | Member; Lead supplies missing campaign evidence | Monolith baseline `4c87b6d…`; source join report SHA `5d7b8789…`; split-join manifest `9b67b250…` | Repo states and local inventories above; existing evidence paths in `BACKLOG_MEMBER.md` | Authenticated read-only PR state for private AI and infrastructure repositories; bytes/path for campaign ledger, T52, frozen Protocol v1, R38 graph/browser package, R01–R45 CSV, and consolidated Q20 receipt. | Reconcile the 5/5 C2/C3/C5 summary with operational rows. Confirm R38 after inspecting the cited bundle and T52 map. |
| 1 — M-B identity, split, FooDB reference | `BLOCKED` | Member; Lead owns semantic/policy adjudication | Source join evidence SHA `5d7b8789…`; GISMo identity inputs `e08b82b7…`, `ad8c62de…`, `ea59d185…`; Recipe1MSubs train `b4d58e6c…` | 70,520 IDs joined to correct Recipe1M partitions with 0 missing/0 wrong split; no re-run needed while hashes stay fixed. Pre-access log and exposure disclosure remain linked from backlog. | Exact historical GISMo preprocessing/vocabulary/cache/order sufficient to explain 1,044/230/220 replay gaps and prove index→recipe ID; exact 971-profile FooDB artifact/hash/denominator. Do not use candidate mappings or alias crosswalk as primary strata. | T504 keeps canonical-name baseline and alias sensitivity separate; no new alias decision is requested. Lead must supply/identify 971-profile pin and accept exact reference/filter/inference unit before C3 claims. Test remains `EXPOSED`. |
| 2 — M-F rights by operation | `READY_FOR_LEAD` | Member prepares; Lead/Human owns actual permission/access decisions | Monolith `4c87b6d…`; rights evidence hashes recorded in referenced files | Operation matrix above; 2020 FooDB CC BY-NC scope checked against official compliance/download pages. | Exact terms/permission only for operations marked UNKNOWN; no transfer, external API, publication, derived-artifact distribution, or product use may be inferred. | Lead decides whether the scoped local research boundary is adequate for each planned analysis. Human/data steward must answer the specific Recipe1MSubs file/comment and external-processing requests if those operations are needed. |
| 3 — M-A graph source and serving bundle | `READY_FOR_LEAD` | Member builds; Lead accepts artifact and T52 | AI service `601419a0765d593ebb96a53b37831831a8e4f379`; train `b4d58e6c…`; manifest `460de87d…`; outputs in M-A receipt | [`member-ma-graph-eval-v4-clean-build-20261104.md`](member-ma-graph-eval-v4-clean-build-20261104.md); two independent builds match all five hashes; provider smoke returns train/ready. | Original `ade85c33…` artifact and T52 map are unavailable; new candidate has 6,632 nodes, two pinned vocabulary labels unmapped, and 948 ambiguous ingredient slots. | Decide whether candidate replaces the 6,633-node R38 artifact or supply the exact historical bytes/source and acceptance map. Until then do not call it accepted or serve it. |
| 4 — M-D GISMo/lookup baseline | `BLOCKED` | Member reproduces/hands off; Lead accepts use/interpretation | Existing referenced evidence: AI `tools/build_member_strata.py` at `aaba0fc…`; local prior output hashes/path not in current checkout | BACKLOG_MEMBER records deterministic val/test generation as locally accepted; no duplicate baseline run was made. | Provide existing output directory and file hashes plus matched-arm case IDs/filter manifest; resolve applicable sharing rights and historical tie/missing/error behavior. | Confirm frozen arm set/filter and permitted handoff; no validation ranker result becomes test evidence. |
| 5 — M-I offline C2 release and cost/recovery preflight | `BLOCKED` | Member packages; Lead authorizes protocol gates; Human owns account/secret facts | T84/T538 decision in Lead backlog; T535 handoff `4906f0e…` is not C2 tooling | Tool inventory found no C2 package/forecast/Batch transport/journal/ingestion tools in the checked-out AI/monolith repos; no package or paid output is present. T535 focused suite: 9 failed, 74 passed due MKL notice hash mismatch. | Exact missing C2 toolchain or source commit/path; pinned prompt/model/schema/transport and output destination; release cases/hash; conservative input-inclusive cost/reserve, remaining capacity, active account, secure secret destination, transfer authority, exposure log, retry/recovery receipt. Preserve 10,747 = 10,499 READY + 248 errors and three-field payload. | GPT-4o Batch retained only for frozen v1 per T538; this is not spend or transfer approval. No live call until every listed receipt is present. DeepSeek remains separate future method. |
| 6 — M-I calibration / comparisons with M-B and M-D | `BLOCKED` | Member runs frozen implementation; Lead adjudicates inference and claims | Protocol v1 digest is cited as `b2952599…`; its bytes were not found in checked-out paths; frame/profile outputs are incomplete | No accepted frozen calibration output or full-cell report located. No test selection or ranker claim made in this turn. | Frozen Protocol v1 bytes plus exact reference/filter/rank/set, inference unit, accepted source mapping, 971-profile denominator, matched case IDs, and independent expected-result fixture. | Lead confirms estimand and permitted exposed-test diagnostics; Lead retains interpretation/claim decisions. |
| 7 — M-C/M-E graph through endpoint/browser and T01–T12 | `BLOCKED` | Member integrates; Lead accepts; Human performs rehearsal | Candidate graph manifest `460de87d…`; FE default `dfcb6ae…`; narrow T503 report `46a92830…` | T09 accepted narrowly for synthetic direct-loopback isolation. T04/T05/T10/T11 are partial in the existing backlog; T01/T02/T03/T06/T07/T08/T12 remain open at accepted-artifact scope. | Lead acceptance of M-A bundle and T52 map; enabled test environment/auth/session; runtime request/response/log/screenshot/video and privacy inventory; impacted suites on final commits; actual presenter/listener rehearsal availability. | Keep graph feature default-off and HTTP 503 on missing/invalid config. T503 does not accept C5. Lead/Human perform the actual walkthrough. |
| 8 — M-G R01–R45 register | `BLOCKED` | Member reconstructs; Lead adjudicates | Leader cites v17 CSV SHA `50014fff…`; local CSV absent | Only the reported start state (10 VERIFIED / 2 PARTIAL / 33 UNVERIFIED) and narrative citations were found. No row was re-verified from absent bytes. | Provide exact v17 CSV, original claim/citation ledger, source commits and paths for each row, reproduction receipts, and mismatch ledger. | Lead makes the final disposition for each R-ID; specifically reconcile R38 after M-A/T52 inspection. |
| 9 — M-H clean reproduction and thesis materials | `BLOCKED` | Member prepares reproducible inputs/drafts; Lead owns authorship and claims | M-A builder source `601419a…`; graph candidate manifest `460de87d…`; Q20 receipt unavailable | M-A receipt contains a repeatable local build command and pinned source/output hashes. No 14-chapter manuscript or consolidated Q20 receipt found. | Accepted inputs/rights and output artifacts; actual manuscript/source document; complete Q01–Q28 pack and Q20 revalidation receipt; third-person clean-checkout run. | Lead owns scientific wording, open/future labels, final interpretation, authorship, venue and publication decisions. |
| 10 — close M-A–M-I row by row | `BLOCKED` | Member reconciles; Lead/Human close their decisions | Final commits remain local: monolith baseline `4c87b6d…`; AI builder `601419a…`; FE default `dfcb6ae…`; infrastructure `62951ed…` | This ledger and linked receipts provide the current baseline. No remote publish/MR was created for the new graph builder. | Every row must reach `ACCEPTED` or retain `BLOCKED` with the exact missing input/owner; fetch final commits before any integration; obtain Lead acceptance and any human-only approvals/demonstration. | No package or contribution is `ACCEPTED` without its real gate. No “100%” completion statement. |

## T52 runtime acceptance rows

These statuses describe the narrow evidence scope actually inspected. `READY_FOR_LEAD` means Member has evidence to review, not that the Lead acceptance gate passed.

| Criterion | Status | Owner | Evidence currently available | Exact remaining condition | Lead decision |
| --- | --- | --- | --- | --- | --- |
| T01 — train provenance | `READY_FOR_LEAD` | Member; Lead accepts | New candidate contains 49,044 train-only evidence rows with stable `train:<pickle-sha>:<index>` keys; manifest binds the train projection SHA; 0 identity-error rows. M-A receipt records source hashes. | Inspect original T52 rule and accept candidate's exact row/evidence binding; later serving test must prove no held-out path. | Pending for this candidate. |
| T02 — reproducible rebuild | `READY_FOR_LEAD` | Member; Lead accepts | Two independent local builds have identical manifest and all output hashes (`460de87d…`, plus four output hashes in receipt). | Tie reproducibility to the exact artifact actually loaded by the endpoint and original acceptance contract. | Pending. |
| T03 — real artifact render | `BLOCKED` | M-C/M-E; Lead accepts | Only fixture/legacy browser evidence is available; candidate provider smoke is in-process only. | Accepted artifact/T52 plus real endpoint → Next/Explorer → browser run, visible or recomputed hash, screenshots/logs and timing. | Pending. |
| T04 — tamper/hash refusal | `IN_PROGRESS` | M-C/M-E | Existing loader/mock/browser and provisional-bundle refusal evidence is recorded in backlog; provider validates the candidate manifest/output hashes in an in-process smoke. | Runtime product-path mutation test against the accepted artifact, recorded refusal and response/log evidence. | Prior proof is partial; no T52 acceptance. |
| T05 — unknown identity | `IN_PROGRESS` | M-A/M-C | Candidate keeps all 49,044 input rows, emits zero identity-error rows, and has no fabricated IDs for the two vocabulary labels missing from catalog; previous tests preserve unresolved errors. | Exercise unknown identity through accepted graph/product path and show it remains reported without an invented node. | Prior proof is partial; no T52 acceptance. |
| T06 — empty/vacuous calibration cell | `BLOCKED` | M-I/M-C; Lead owns inference | No accepted calibration artifact/cell report found. | Frozen calibration output bound to the accepted graph and proof of abstention for missing/vacuous support. | Pending; no confidence claim. |
| T07 — supported show threshold | `BLOCKED` | M-C/M-I; Lead owns threshold | Consumer/endpoint guards exist, but no accepted calibration or real show path. | Exact frozen status/cutoff wired into the runtime and tested with supported and unsupported cases. | Pending. |
| T08 — allergen unknowns/cross-contact | `BLOCKED` | M-C/M-I; Lead owns policy | Contract/test evidence is not a real accepted graph-backed request. | Accepted boundary proof that unknown/cross-contact and unsupported candidates are withheld or labeled within the frozen limits. | Pending. |
| T09 — isolation | `ACCEPTED` | Lead accepted narrow scope; Member supplied evidence | T503 synthetic direct-loopback report SHA-256 `46a92830e5302a2939bba01dca00faeb44c26e39f88800133dbb724a8225f8a6`; identical suggestion responses bracketed Explorer calls and disposable domain-store fingerprint was unchanged. | Extend only if the actual browser/Next proxy and real artifact require a broader isolation claim. | Accepted only for T503's synthetic configuration; no T52/C5 acceptance. |
| T10 — demo/network/failure/recovery | `IN_PROGRESS` | M-E; Human rehearses | Fixture/legacy-data network/runtime proof exists. | Accepted-artifact Act 3 path, real network failure/recovery, recorded backup and actual presenter/listener rehearsal. | Pending. |
| T11 — privacy inventory | `IN_PROGRESS` | M-E | Sentinel/log/response fixes and bounded scans exist. | Inventory actual demo bundle, browser responses, telemetry, logs, artifacts and sensitive fields; verify FE patch on target runtime. | Prior scope is partial; no T52 acceptance. |
| T12 — affected regression suites | `READY_FOR_LEAD` | Member | Existing backlog records PASS for previously affected suites. This turn also ran the new M-A builder tests (3 passed), compilation and diff check. | Rerun only suites affected by future M-C/M-E runtime changes and record final source commits. | Prior PASS is limited to its recorded suites; no new T52 acceptance was recorded. |

## Contribution and claim crosswalk

| Item | Status | Linked member work | Evidence / exact gap | Lead decision |
| --- | --- | --- | --- | --- |
| C1 | `BLOCKED` | M-B/M-D/M-G | Leader cites a 10,747-case Freeze-A frame and 2/5 earned gates; frozen source/claim inputs are not fully available here. | Reconcile source pin and remaining C1 gates before assigning contribution acceptance. |
| C2 | `BLOCKED` | M-I | Leader summary says 5/5; operational records say no paid output. No hash-bound release/transport receipt or paid output located. | Confirm authoritative campaign ledger and gates; T84/T538 freeze is not execution or dispatch authorization. |
| C3 | `BLOCKED` | M-B/M-F/M-I | CPU probe succeeded, but no accepted C3 export or 971-profile input was found. | Accept exact source/reference/filter and all-cell report only after the missing artifacts are reviewed. |
| C4 | `BLOCKED` | M-I/M-E | Frozen judge contract/runner may exist in campaign, but no real human labels/consent/institutional receipt were located in the available checkout. | Human study and validity gates require their actual consent/institutional evidence; synthetic agreement is not human validity. |
| C5 | `BLOCKED` | M-A/M-C/M-E | Summary says 5/5 and R38 verified; current operational T52 rows remain open. Original graph/browser package is absent. | Reconcile R38/T52 and accept the exact served artifact before C5 claims. |
| R01–R45 | `BLOCKED` | M-G | Leader reports v17 SHA `50014fff3b8562b38854e0a5e7f3c93721983337f2de16b600421e9c9e0bfa24`, 10 VERIFIED / 2 PARTIAL / 33 UNVERIFIED; CSV and per-row receipt set are absent here. | Supply register bytes; Lead adjudicates each actual claim and its evidence ceiling. R38 is the known mismatch to resolve. |

## M-A–M-I delivery status

### M-A

- **Status:** `READY_FOR_LEAD`; **owner:** Member.
- **Source commit / hash:** AI service `601419a…`; candidate manifest `460de87d…`.
- **Member evidence:** Two-build receipt above; 0 identity errors; in-process candidate-provider smoke returns train/ready.
- **Remaining condition:** Reconcile 6,632 nodes against R38's 6,633 and provide T52/original bundle bytes.
- **Lead decision:** Pending; candidate is not accepted.

### M-B

- **Status:** `BLOCKED`; **owner:** Member.
- **Source commit / hash:** Existing source-join report `5d7b8789…`; GISMo runtime `e08b82b7…`.
- **Member evidence:** Verified ID/split join retained; positional and FooDB inventories recorded.
- **Remaining condition:** Exact historical preprocessing cache/order and the 971-profile input/hash.
- **Lead decision:** No alias policy change; Lead supplies source pins and accepts the reference/inference unit.

### M-C

- **Status:** `BLOCKED`; **owner:** Member.
- **Source commit / hash:** Monolith `4c87b6d…`; candidate manifest `460de87d…`.
- **Member evidence:** Default-off endpoint/Explorer and narrow synthetic T09 evidence exist.
- **Remaining condition:** Accepted artifact and T52, followed by real endpoint/Next/browser runtime proof.
- **Lead decision:** T09 is accepted only for its synthetic configuration.

### M-D

- **Status:** `BLOCKED`; **owner:** Member.
- **Source commit / hash:** Historical AI source `aaba0fc…`; prior output hashes are not available in this checkout.
- **Member evidence:** Existing deterministic-generation acceptance is preserved; no duplicate baseline run was made.
- **Remaining condition:** Locate output files/hashes and materialize matched arms under the accepted filter and rights.
- **Lead decision:** Accept frozen use and interpretation.

### M-E

- **Status:** `BLOCKED`; **owner:** Member; Human performs the rehearsal.
- **Source commit / hash:** FE `dfcb6ae…`; T503 report `46a92830…`.
- **Member evidence:** Fixture/legacy browser evidence and privacy work remain narrow.
- **Remaining condition:** Accepted artifact, authorized runtime/session, full inventory/backup, and presenter/listener rehearsal.
- **Lead decision:** Lead/Human must observe the real walkthrough.

### M-F

- **Status:** `READY_FOR_LEAD`; **owner:** Member prepares; Lead/Human own permission decisions.
- **Source commit / hash:** Monolith `4c87b6d…`; rights evidence linked in the matrix.
- **Member evidence:** Operation matrix separates code, dataset, derived-artifact, and internal-record scope.
- **Remaining condition:** Permission/access answer for each specifically requested `UNKNOWN` operation.
- **Lead decision:** Pending for any transfer, external API use, publication, derived-artifact distribution, or product use.

### M-G

- **Status:** `BLOCKED`; **owner:** Member reconstructs; Lead adjudicates.
- **Source commit / hash:** Reported register SHA `50014fff…`; CSV bytes absent.
- **Member evidence:** Starting claim counts/status recorded; no absent claim was treated as verified.
- **Remaining condition:** Actual register bytes, per-claim evidence and source references.
- **Lead decision:** Lead adjudicates every R-ID; R38 remains unresolved.

### M-H

- **Status:** `BLOCKED`; **owner:** Member prepares; Lead owns scientific authorship.
- **Source commit / hash:** AI `601419a…`; candidate manifest `460de87d…`.
- **Member evidence:** Rebuild command and M-A output hashes documented.
- **Remaining condition:** Accepted results/rights, manuscript and Q20 receipt, and a third-person clean reproduction.
- **Lead decision:** Lead owns claims, conclusions, authorship, and publication.

### M-I

- **Status:** `BLOCKED`; **owner:** Member prepares; Lead/Human own dispatch gates.
- **Source commit / hash:** T84/T538 in Lead backlog; T535 handoff `4906f0e…`.
- **Member evidence:** Source-free tool inventory complete; no C2 release or paid result exists.
- **Remaining condition:** Locate the existing frozen C2 package/runner at an accessible source commit/path or hand off those tools; then pin its inputs and output destination and obtain cost/capacity/account/secret, transfer, and recovery receipts.
- **Lead decision:** GPT-4o Batch remains the frozen-v1 comparator; no dispatch is approved until every gate is evidenced.

## Reopen rule

Resume only the affected blocked row when its named input/decision changes. Do not rerun the 70,520-row ID join, repeat T503, rebuild strata with candidate GISMo positions or alias keys, download Recipe1M images, or repeat baseline generation solely to create duplicate receipts. New source hashes or a new Lead protocol decision may reopen the relevant row. Never enable upload, paid API, participant study, product use, or public distribution based only on this checklist.
