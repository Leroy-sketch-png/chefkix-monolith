# Lead claim-evidence follow-up: R09, R11, R18, R19 — 2026-11-05

This is a bounded Lead-root follow-up to the Member crosswalk snapshot. The source files were found at Lead research checkout HEAD `74a1e7a98dd556b674e7a207ba64459d50f88cdc`; each copied file was SHA-256 checked against that checkout before staging. The scoped `.gitattributes` keeps the transferred bytes stable across Windows checkouts.

## Shareable scope

The handoff contains only existing aggregate receipts, bounded reports, and verifier source code. It contains no source CSVs, corpus rows, parquet snapshots, historical audit payloads, labels, case IDs, recipe IDs, prompts, images, model weights, credentials, or private account material. The R19 protocol JSON is intentionally excluded because it contains 208 case prompts/identifiers. None of the verifiers was run during this transfer; this package establishes artifact availability and byte identity only.

| Claim | Transferred aggregate/report | Transferred verifier source | Boundary |
|---|---|---|---|
| R09 | `claims/r09-global-flavor-test-reproduction-20261102.json` — SHA-256 `b2c80426fb51713f4ccdcda9bec9fbac98c4df8649d89f97d938b6019fe45005` | `tools/reproduce_r09_global_flavor_pairing.py` — SHA-256 `8301cfccfd42f9dcacbb4ff203945750173b5ee1036b805246c69c26e9100c13` | Aggregate negative result only. The hash-pinned `flavor_pairing_report.json`/corpus inputs are not included; no fresh run or broader inference is claimed. |
| R11 | `claims/r11-smolvlm-withdrawal-reproduction-20261103.json` — SHA-256 `7e8c164e0fda2d4c7c1f24a17ea94a89fc7027dfbfbfee4d152192644fe7dea1` | `tools/reproduce_r11_smolvlm_withdrawal.py` — SHA-256 `d21fa066c0ecb4b29e984f58db315feda326e1de23104fbd78f17bd03171ce18` | Existing withdrawal summary only. The historical audit files, example payload, images and trainer artifacts are not included; no new artifact inspection was performed. |
| R18 | `claims/r18-bounded-reproduction-20261021.md` — SHA-256 `0055df06e2af4299b64ef882de79cf14d6e4d7eae32dbec719d5e605c52aec32`; `claims/r18-taxonomy-partition-validation-20261021.json` — SHA-256 `a5f97d1caacec550178ab2dd94c03aa7fd3847e7626893398e1f19798185cdb7` | `tools/reproduce_off_allergen_snapshot_tag_counts.py` — SHA-256 `3126dc90834403db76f4f24f3e7d3429af6dc26d5729944658bb65115eb4a350` | Exact report/partition receipt and source are available, but the report-referenced count-output digest `df4845a7109603ba2e07b56646aa086353948a951b6f5aba67d00281db6afed7` is not present: the same-named local JSON hashes to `90be80bc32946342f80cd198fe5d23baf057bb4c0544f8a7264de0e6ac1d7d33` and is excluded. The report-pinned taxonomy validator/builder digests (`101683c0…`, `759dedae…`) also do not match the currently present source files (`64e1a2fb…`, `59d8d4da…`); those sources are excluded. No inputs were re-read and no output was regenerated. |
| R19 | `claims/r19-benchmark-protocol-reproduction-20261102.json` — SHA-256 `4c4c97f37a3a7265d93d6c7f5e55ad12e974acb0918b1dd5529434cc6a47efe2` | `tools/reproduce_r19_frozen_allergen_protocol.py` — SHA-256 `e05076bc28b1097093de74950206d94e5cd16095424a8c6cf0cd9ae9ad094741` | Aggregate 208-query/13-group receipt only. No prompts, query/case IDs, or protocol bytes are transferred; no protocol verification was run here. |

## Review and claim-status limits

This is an artifact-availability and publication-scope review, not an independent scientific review. A standalone independent review receipt for R09, R11 or R19 was not located in the scoped files. The R19 row in the multi-claim T42b candidate report was not copied: it is a later v10 register assessment, not a standalone review, and would mix a different register snapshot into this v20 handoff. The broader T128 review and R18 source data were also not copied.

The original Member crosswalk remains an immutable snapshot of its searched roots. This separate Lead-root discovery does not correct that snapshot, alter any R01–R45 status, apply an evidence record, or establish protocol equivalence, model execution, identity, performance, generalization, rights, clinical/allergen safety, human validity, or L48 acceptance. Existing register status remains governed by its pinned register and existing Lead adjudication; availability of a copied receipt is not new adjudication.

## Byte manifest

| Destination path | SHA-256 |
|---|---|
| `claims/r09-global-flavor-test-reproduction-20261102.json` | `b2c80426fb51713f4ccdcda9bec9fbac98c4df8649d89f97d938b6019fe45005` |
| `tools/reproduce_r09_global_flavor_pairing.py` | `8301cfccfd42f9dcacbb4ff203945750173b5ee1036b805246c69c26e9100c13` |
| `claims/r11-smolvlm-withdrawal-reproduction-20261103.json` | `7e8c164e0fda2d4c7c1f24a17ea94a89fc7027dfbfbfee4d152192644fe7dea1` |
| `tools/reproduce_r11_smolvlm_withdrawal.py` | `d21fa066c0ecb4b29e984f58db315feda326e1de23104fbd78f17bd03171ce18` |
| `claims/r18-bounded-reproduction-20261021.md` | `0055df06e2af4299b64ef882de79cf14d6e4d7eae32dbec719d5e605c52aec32` |
| `claims/r18-taxonomy-partition-validation-20261021.json` | `a5f97d1caacec550178ab2dd94c03aa7fd3847e7626893398e1f19798185cdb7` |
| `tools/reproduce_off_allergen_snapshot_tag_counts.py` | `3126dc90834403db76f4f24f3e7d3429af6dc26d5729944658bb65115eb4a350` |
| `claims/r19-benchmark-protocol-reproduction-20261102.json` | `4c4c97f37a3a7265d93d6c7f5e55ad12e974acb0918b1dd5529434cc6a47efe2` |
| `tools/reproduce_r19_frozen_allergen_protocol.py` | `e05076bc28b1097093de74950206d94e5cd16095424a8c6cf0cd9ae9ad094741` |
