# R38 handoff reconciliation — 2026-11-05

## Recorded conflict

The byte-preserved R01–R45 v20 register still records R38 as `VERIFIED / POSITIVE`: 6,633 nodes, 23,185 edges, 49,044 evidence records, zero identity errors, manifest SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`, and four browser captures.

The current member M-A receipt instead describes a distinct 6,632-node provisional candidate, manifest SHA-256 `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060`, with 23,185 directed edges, 49,044 evidence records, and zero identity-error rows. It also reports 2,214 isolates and notes two vocabulary labels without catalog identity. The member checklist says the historical R38 bundle is unavailable for rerun and requires an explicit lead reconciliation before treating the candidate as its replacement.

## Disposition

Keep the original R38 row and its status/history unchanged. Keep the member candidate and its status distinct. Matching edge/evidence counts do not prove artifact identity; the node-count difference and manifest hashes are material. The original 6,633-node bytes and browser receipt were not present in the checked repositories/paths, so this handoff cannot adjudicate equivalence or upgrade/downgrade R38. No private graph bundle, source row, pool, label, or output was copied.

## Pinned evidence already fetchable in this monolith

- Original register: [`claims-register-r01-r45-v20.csv`](claims-register-r01-r45-v20.csv), source commit `0c42206e6bdd08d42700e3846bffe73212d97cd7`, SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`.
- Member candidate receipt: [`../member-ma-graph-eval-v4-clean-build-20261104.md`](../member-ma-graph-eval-v4-clean-build-20261104.md), source commit `3e11b80a7efefec75cca1646e7ed584e937d3cf5`, SHA-256 `8d34bb3fa8d58903eee9d2c2192294d956bc6e0540c5f593d95e71028653f91a`.
- Member checklist and historical-bundle availability statement: [`../member-backlog-checklist-20261104.md`](../member-backlog-checklist-20261104.md), source commit `f27eaecff0fc049bbd1937dee5c4c83880a388e5`, SHA-256 `225f8ff5eebba3f69fccc0eb51428271ee40e9e4d5815822b8e9bf557dcc2cc9`.

This is a provenance reconciliation, not independent reproduction, scientific acceptance, or permission to enable/serve the candidate.
