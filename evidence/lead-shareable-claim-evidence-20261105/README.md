# Lead source-evidence handoff — 2026-11-05

This handoff publishes existing, aggregate-only R12/R13 evidence and its reproduction code for shared review. The source files were copied byte-for-byte from the Lead research checkout at `D:/code/chefkix`, HEAD `74a1e7a98dd556b674e7a207ba64459d50f88cdc`; every destination SHA-256 is listed below and was checked against its source before staging.

The source register wording snapshot is provenance only, not evidence for any claim. R12 is limited to a local FooDB inventory; R13 is limited to profile-pair arithmetic. The records state the declared FooDB CC BY-NC 4.0 terms, but that sidecar statement is not independent rights clearance. This handoff contains no FooDB profile/source bytes, raw records, case rows, labels, graph data, pools, credentials, or model weights. It does not authorize new processing or redistribution of the underlying artifact.

The member crosswalk remains an accurate snapshot of the roots it checked. A later bounded check found these source files in the separate Lead checkout, which that crosswalk did not include. The transfer closes only the accessible R12/R13 path gaps; it does not change the v20 register's recorded statuses or independently re-adjudicate them. Recipe1M-dependent R04 artifacts were not published under the recorded non-publication boundary. The exact R38 report/tool were also found locally at their v20-pinned hashes, and its local manifest digest matches the report; those Recipe1M-dependent materials were not transferred. The underlying graph output bytes were not rehashed and the browser captures were not found or revalidated, so no R38 equivalence or fresh reproduction is inferred.

## Byte manifest

| Destination path | SHA-256 |
|---|---|
| `claims/ssot-15-1-claim-wording-source.md` | `2adebe9b039e26eaa9609b60fde5c40b5c9f2bfb1f3ec1e7ffff0d077925be6a` |
| `claims/r12-foodb-service-inventory-reproduction-20261021.json` | `14a3bf7f66a74e303f09d61d3329b0d308b077aa1916de33e5017bf74b6dbd2d` |
| `claims/r12-foodb-service-inventory-reproduction-20261021.md` | `6079a124f10ca81b5c144ff3559cdd73f4d172581845f48967a445a6003bd92d` |
| `claims/r12-evidence-application-20261021.json` | `84509a8a89f9527c6db0be0174a1c6383c103ed98281b060c9befb85a2598a4b` |
| `claims/r12-register-validation-20261021.json` | `052ad0f2c76f3773e714ff42cd8ccb2468f580f656340a810531adf4bdec8bc9` |
| `claims/r13-profile-pair-arithmetic-reproduction-20261021-v2.json` | `c63db935d919d5c87c3bbffe4aafb2a1ff476ef66a3a1d7a5db34d9c8d6482fa` |
| `claims/r13-profile-pair-arithmetic-reproduction-20261021-v2.md` | `7c9f62e3148e22af99c0c06b178b732ca3ab1d463b4441ed10cb2f95e11d79af` |
| `claims/r13-evidence-application-20261021.json` | `d43bd003056f18e517884d5fa00c89cf7b49e8d86651019e1825487609860197` |
| `claims/r13-register-validation-20261021.json` | `b7cbf2f2de8193b458c997c01d987ccaaa4b0a32ed5cfdb7a54e99fc471bce83` |
| `reviews/T22-r12-independent-review.md` | `7ab40403b1a45014a9a1e9e7f24ab88d860a7d406305224b894c351c81c3795a` |
| `reviews/T11-r13-reproduction-independent-review.md` | `684c5dc19a736a59042aa3fae9d5548f23e34f08cf5c79fdc2f32c7c42a309cf` |
| `tools/reproduce_foodb_service_inventory.py` | `77bde878ec1f8636ff3760ea5ab269b02dd08c5a7121838d4bc54bded803d029` |
| `tools/test_reproduce_foodb_service_inventory.py` | `b866fdc1a863049057a5f1d5507ec696fa95dc07613aa3ce6a25dd17e61b8d3e` |
| `tools/reproduce_foodb_profile_pair_arithmetic.py` | `bbf10b9f0f5aa3dfdf559fa61555973c90550c633673df56c337c5269bfa9006` |
| `tools/test_reproduce_foodb_profile_pair_arithmetic.py` | `f46ca34dd9e3f03d8b1d3de823d553f7e589e0e0524ed93a414e19dd1e7ae962` |
| `claims/r41-reference-statistics-selftest-audit-20261104.json` | `a6f6a14a5f7427b9c25c72e1406563e9d01f0e68de833459a028fb438cbb367d` |
| `tools/audit_ssot_appendix_a1.py` | `7695473f00ce77696b0ed9e1034d14cddb598426b4fadfaf214c105f3a563b9a` |
| `tools/test_audit_ssot_appendix_a1.py` | `2f496965ddf0b425113b1fdbfee50d4152f88ef279b86b70869867e3ee3ee619` |
| `reviews/T132-independent-r42-partial-status-review-report.md` | `96c9b0889fe66852f2d7a381c0cfede574efa684dd669202fce2018c4ea65425` |

## Citation/source-path follow-up — 2026-11-05

The Member citation/source-path CSV is preserved as its observation-time snapshot; it is not edited here. Its cited §15.1 claim-wording source is already available as [`claims/ssot-15-1-claim-wording-source.md`](claims/ssot-15-1-claim-wording-source.md), byte hash `2adebe9b039e26eaa9609b60fde5c40b5c9f2bfb1f3ec1e7ffff0d077925be6a`, pinned to source SSoT hash `a52601b7d6d8ebbbf86a6dd1dfbe85cfa51413ab4cea4be06d8b17f8144cc6b9`. That closes the exact wording-source path recorded by the CSV; it is not the entire active strategy. The current Lead file is a modified local working copy (SHA-256 `b5ad66201eeed44e201b471eb895aaaff0f6f3c2c774a1d1cdf978d6deacddce`) and is deliberately not copied wholesale or represented by the older excerpt.

Two eligible authored/source-free gaps are now available at handoff-relative paths. The R41 Appendix A.1 audit tool, its synthetic test source, and its existing JSON receipt are under `tools/` and `claims/`; source code/tests are pinned to Lead commit `0c42206e6bdd08d42700e3846bffe73212d97cd7`. The receipt remains historical: it audited SSoT SHA-256 `51ce14610a0c2ed1259846d077c2d3d2eb83b4a92ea3e2304ef71ba2e129e1ef`, is a line-anchored text audit plus bounded diagnostic self-test, and does not validate protocol behavior or the current modified SSoT. No audit or tests were rerun for this handoff.

The authored T132 R42 review is available at [`reviews/T132-independent-r42-partial-status-review-report.md`](reviews/T132-independent-r42-partial-status-review-report.md), source SHA-256 `96c9b0889fe66852f2d7a381c0cfede574efa684dd669202fce2018c4ea65425`, from Lead commit `d831804f36edc97a043d8ceb52c05e15ffdb735c`. Its cited publisher PDF hash is preserved in the report, but the PDF was absent at its cited Lead-local path during this follow-up and is not included; this is a historical review record, not a fresh source verification or status application. The report's embedded absolute paths remain provenance strings only; use the relative handoff path above to open the shared copy.

Remaining path dispositions: the existing R04 T07 review/materials stay excluded under the recorded non-publication boundary; the R40 cross-source matrix and R11 dataset are not transferred; the R10/R39 generated outputs were not verified as safe authored-source dependencies; and `C5-G4-runtime-walkthrough.md` remains missing from the checked roots. No claim status, score, register, protocol, contribution acceptance, or milestone changed. See the L48 addendum in monolith `BACKLOG_LEAD.md` for the same dependency map.
