# R38 handoff reconciliation — 2026-11-05

## 2026-11-24 Lead adjudication

**Decision: ADOPT the clean graph artifact subclaim narrowly; DEFER the full live graph/Explorer proof. Preserve the R38 v20 row and status unchanged.** The Lead-local `ai/out/graph_eval_v4_clean/manifest.json` has SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1` and declares 6,633 nodes, 23,185 edges, 49,044 evidence records, and zero identity-error rows. On 2026-11-24, the four exact output files named by that manifest were SHA-256 checked without opening their contents; all four hashes match. This supports those counts and integrity for that named export only.

The separate M-A candidate is not the same artifact: the shared accepted-state package manifest (`5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e`) declares 6,632 nodes, 23,185 edges, 49,044 evidence records, and zero identity-error rows; its four output hashes also match its own manifest, and its node/edge/evidence output hashes differ from the 6,633-node export. The earlier M-A candidate receipt identifies source manifest `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060`. Thus the inventory resolves the apparent contradiction as counts for two separately pinned outputs, not two readings of one manifest. It does **not** explain why the candidate has one fewer node; that would require a content-level comparison, which was not performed or authorized here. R39's `eligible_classes=6,632` is a different measure and is not a node count for R38.

The v20 R38 `Source_provenance` field contains `ade85c3312b87ff04f5e7178c772e0220641df1c7d24a919aa8e38d72605f63d`, which does not match the report's `Reproduced_value` or the actual `graph_eval_v4_clean/manifest.json`; the latter two agree on `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`. This is recorded as a citation inconsistency, not silently repaired in the byte-preserved v20 register. The cited report says four browser captures exist, but neither their filenames/hashes nor the cited `C5-G4-runtime-walkthrough.md` were present in the bounded checked handoff paths. The existing 2026-11-05 reconciliation likewise records those captures as not found or revalidated. Therefore the browser-serving, live-DOM, allergen-interaction, responsive-reflow, and refusal-replay portions remain **unverified by this adjudication**; report prose is not runtime evidence.

**Remaining owner operation:** M-E, with M-C providing serving identity, must attach the four existing capture files with hashes and metadata binding the walkthrough to the `ade85c33…` manifest (or provide an equivalent already-existing runtime receipt with the exact served-manifest identity). Do not substitute the 6,632-node candidate or infer equivalence. No graph rows, source records, or private captures were opened, copied, or published.

## Recorded conflict

The byte-preserved R01–R45 v20 register still records R38 as `VERIFIED / POSITIVE`: 6,633 nodes, 23,185 edges, 49,044 evidence records, zero identity errors, manifest SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`, and four browser captures.

The current member M-A receipt instead describes a distinct 6,632-node provisional candidate, manifest SHA-256 `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060`, with 23,185 directed edges, 49,044 evidence records, and zero identity-error rows. It also reports 2,214 isolates and notes two vocabulary labels without catalog identity. The member checklist says the historical R38 bundle is unavailable for rerun and requires an explicit lead reconciliation before treating the candidate as its replacement.

## Historical disposition as of 2026-11-05

At the time of this 2026-11-05 check, the original 6,633-node bytes and browser receipt were not present in the checked repositories/paths. The bounded 2026-11-24 checks above supersede that availability statement for the graph manifest/output hashes only; they do not resolve runtime evidence or claim equivalence. No private graph bundle, source row, pool, label, or output was copied.

## Pinned evidence already fetchable in this monolith

- Original register: [`claims-register-r01-r45-v20.csv`](claims-register-r01-r45-v20.csv), source commit `0c42206e6bdd08d42700e3846bffe73212d97cd7`, SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`.
- Member candidate receipt: [`../member-ma-graph-eval-v4-clean-build-20261104.md`](../member-ma-graph-eval-v4-clean-build-20261104.md), source commit `3e11b80a7efefec75cca1646e7ed584e937d3cf5`, SHA-256 `8d34bb3fa8d58903eee9d2c2192294d956bc6e0540c5f593d95e71028653f91a`.
- Member checklist and historical-bundle availability statement: [`../member-backlog-checklist-20261104.md`](../member-backlog-checklist-20261104.md), source commit `f27eaecff0fc049bbd1937dee5c4c83880a388e5`, SHA-256 `225f8ff5eebba3f69fccc0eb51428271ee40e9e4d5815822b8e9bf557dcc2cc9`.

This is a provenance reconciliation, not independent reproduction, scientific acceptance, or permission to enable/serve the candidate.
