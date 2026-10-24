# Member checklist reconciliation against LEAD v525

Date: 2026-10-24. This is an audit of the current local checkouts and the recorded evidence. After the Sprint 2 draft reconciliation and M-F inventory, the member backlog has **21 checked** and **34 unchecked** top-level checklist lines. A checked line records only the scope stated on that line; it does not complete its work package.

## Completed checklist lines already recorded

| Package | Checked member lines | Evidence boundary |
|---|---|---|
| M-A | 682–684, 686–687 | Seed-source location, source-only graph census and directed degree invariant, historical claim location, `GRAPH_TRUTH.csv`. Runtime graph and Round-0 acceptance are open. |
| M-D | 722–726 | Local written four-arm output package and byte-identical replay, per LEAD v469. The output files are absent from the synced checkouts; this does not close L44. |
| M-F | 710–713 | Inventory count and per-recipe/source table delivered under the recorded operational definition. 44 DB-held records; 0 private-Western; 44 licenses `UNKNOWN`. Internal-use attestation does not assert authorship or redistribution rights. |
| M-C | 733–734 | Default-off shadow endpoint and returned-provenance FE display. Neither establishes an accepted artifact or calibrated show decision. |
| M-E | 759 | T-12 passes for the recorded affected suites, as bounded in the member T-12 row and LEAD v525. |
| Thesis workspace | 819–821 | Fourteen chapter mapping, chapter handoffs, and pending artifact cards. Pending cards are not captures. |
| Sprint drafts | 825 | The six Word documents and six PDF counterparts were reconciled as historical v3 drafts in `output/sprints/2026-10-11/STATUS_2026-10-24.md`; original document bytes remain unchanged. |

## All unchecked lines and the remaining gate

| Package | Member line(s) | Current state and completion requirement |
|---|---|---|
| M-A | 685 | **Blocked:** the registered AI-service `models/substitution_subgraph.pt` is absent. The new inspection tool and `evidence/member-ma-runtime-graph-inventory-20261024.json` establish path and missing-file status, not node/edge counts. Obtain the eligible bundle and run the inspector. |
| M-A | 688 | **Blocked:** no authoritative query cohort and denominator are available for `COVERAGE_CURVES.csv`. |
| M-A | 689 | **Partial:** the source-only degree sum passes; Round-0 values have not all been reproduced or struck. LEAD v457–v456 explicitly leave lineage/convention unresolved. |
| M-B | 694–695 | **Partial:** all three official Recipe1MSubs pickle payloads have now been downloaded outside Git and audited for hashes, actual row counts, schema, repeated IDs/pairs, and cross-split Recipe1M-ID overlap; the pinned GISMo split-index files are also hashed. Those GISMo files contain indices into preprocessed arrays, not direct recipe IDs, so the exact Recipe1M ID-to-split/order join, denominator reconciliation, requested `build_strata.py` deliverables, and byte-identical regeneration remain open. See `evidence/member-mb-mf-recipe1msubs-source-license-audit-20261024.json`. |
| M-B | 696 | **Open:** Appendix A pool-function reuse and its protocol/PR15 boundary are unresolved; no accepted `build_pools.py` delivery here. |
| M-B | 697 | **Partial:** LEAD v483 records a 40-case/three-arm synthetic mechanics fixture and 13 focused tests. It is not the required blinded human fixture under a frozen protocol. |
| M-B | 704–705 | **Open:** full real val/test execution, recorded hashes, independent bit-for-bit replay and package acceptance require the research inputs, protocol/rights gates, and accessible outputs. |
| M-F | 710–713 | **Complete under the recorded operational definition:** user attests the ChefKix-owned DB is for internal recipe use; first-party here means records held in that DB and authorized for internal use, not an authorship or redistribution claim. Authenticated read-only inventory: 44 total, 2 PRIVATE (Filipino/Japanese), 0 private-Western under the recorded cuisine-label rule. Per-recipe source/license table has 44 stable SHA-256 ID references; all licenses are `UNKNOWN` because source/license metadata is absent. Receipt/table: `evidence/member-mf-recipe-license-inventory-20261024.json` and `.csv` (CSV SHA-256 `eed30600a6e54bf07fa1c17b11a1436d81e46b4b426aa87c8be4ea0e6db86c41`). Internal-use attestation does not establish authorship or external redistribution rights. |
| M-C | 731 | **Partial:** LEAD v516 describes a private provisional graph candidate with ten preparation errors, missing authoritative split provenance, and unresolved rights. It is not the accepted `graph_eval_v4`/`graph_product_v4` pair. |
| M-C | 732 | **Partial:** source-only seed statistics exist. Accepted-artifact type/component/source and occurrence-weighted test coverage statistics are missing. |
| M-C | 735 | **Partial:** the feature flag, bounded legacy-API Explorer, unknown-identity path, layout checks and fixture tamper refusal exist. A real accepted artifact, hash-pinned product route, measured render time and real-artifact tamper proof remain absent. |
| M-C | 736–737 | **Open:** T-01–T-12 are not all passing at their full acceptance scope; see test matrix below. |
| M-I | 744–747 | **Partial, not locally verifiable:** LEAD v442–v489 records a research-local independent runner, manifests, refusals and progressively stronger recovery controls. The cited `D:/code/chefkix` research checkout does not exist on this machine. The same Lead entries leave provider hard caps, forced-crash recovery, sandbox/hostile-child containment and independent closure open. Do not check the four lines from summary receipts alone. |
| M-E | 756 | **Blocked:** no frozen accepted raw-vs-filtered/calibrated Act 3 output pair or recorded Act 3 is available here. |
| M-E | 757 | **Partial:** fixture-backed network gating and a separate isolated legacy-graph browser run exist; no recorded accepted-artifact Act 3 with fallback (member T-10). |
| M-E | 758 | **Partial:** bounded sentinel/log and static-exhibit scans exist; no complete live demo output/log/response/artifact inventory scan (member T-11; LEAD v524 covers only two static files). |
| M-E | 760 | **Open:** Act 3, T-10 and T-11 still prevent acceptance. |
| M-G | 765–769 | **Partial, not locally verifiable:** LEAD v515 identifies reviewed research-local register v12 (45 rows; five narrowly VERIFIED, two PARTIAL, 38 UNVERIFIED) and a structural/local-byte validator pass. The register, mismatch ledger, validator and primary research artifacts are not in these synced checkouts, and L48 remains open. Do not promote statuses or mark the five lines complete from the summary alone. |
| M-H | 774–777 | **Open:** a clean-checkout one-command figure build, third-person clean-machine Table 1 reproduction, and complete source/license/freeze README require the accepted data and research pipeline. No such verified kit is present here. |
| G9 | 822 | **Open:** canonical ledger, real-source figures/screenshots and audited captions require source access and review. |
| G9 | 823 | **Open:** deployed regions/URLs, source fingerprints, auth/quota/recovery observations and measured costs need deployment records. |
| G9 | 824 | **Human/Lead gate:** ethics/recruitment/study, independent safety/image review, and a recorded/scored rehearsal are not evidenced as completed. |

## T-01–T-12 acceptance matrix

| Status | Tests | Scope |
|---|---|---|
| Open | T-01, T-02, T-03, T-06, T-07, T-08 | Authoritative split/source rebuild, accepted product graph, calibration/show rule and allergen evidence are missing. |
| Partial | T-04, T-05, T-10, T-11 | Local loader/fixture or legacy-path checks exist; accepted-artifact, recorded Act 3 and full inventory checks are outstanding. |
| Narrow pass | T-09, T-12 | Route response isolation and recorded affected suites only; LEAD v523/v525 state these limits. |

## Local availability and next dependency

- The latest leader entry in this checkout is LEAD v525 (2026-10-23). It prioritizes an eligible real graph artifact and its source/owner gates rather than more unchanged synthetic tests.
- The official Recipe1MSubs experiment-code checkout is available at upstream commit `14014ad501afd0c7da95ed90de19096c7f12ff75`; the audited code repository is Apache-2.0, while GISMo's README says most ingredient-substitution material is CC-BY-NC 4.0. The precise coverage of the split pickle files and underlying Recipe1M comment/recipe rights remains unresolved; see `evidence/member-mb-mf-recipe1msubs-source-license-audit-20261024.json`.
- The three official split pickle files are now present in a local research-data directory outside Git; hashes, bytes, actual row counts, schema, duplicate counts, and zero cross-split Recipe1M-ID overlap are recorded in the audit receipt. The pinned GISMo split files are preprocessed-array indices; the authorized Recipe1M source/order needed to join these indices to recipe IDs is still unavailable, so source joins and retained-denominator results are not claimed.
- A bounded filename search across the four synced repositories found no `graph_product_v4`, `graph_eval_v4`, or `substitution_subgraph.pt`. This is a local inventory, not global absence. The historical `D:/code/chefkix/.git-worktrees/root-master-segment10-20261014` path is unavailable here.
- First unblockers: provide the eligible graph artifact plus authoritative split/source and rights records; make the research output/validator checkout accessible; provide the authorized Recipe1MSubs split payloads and exact Recipe1M release/split metadata for M-B, plus source permission/terms evidence for M-F. The ChefKix DB recipe inventory and its recorded private-Western rule are already delivered; do not repeat that scan. Human/Lead ethics, review and rehearsal remain separate gates.
- The Sprint draft reconciliation and M-F checklist status were updated from delivered evidence. All other unchecked statuses remain unchanged; the leader backlog was read only.


