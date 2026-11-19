# Member validation of Lead handoff — 2026-11-05

## Scope

This receipt records a mechanical integrity and availability audit of the Lead handoff in `evidence/iron-chef-lead-member-handoff-20261105/`. It does not re-run scientific claims, change a claim status, approve data use, accept T52/C5, or substitute the 6,632-node candidate for R38.

- Source manifest SHA-256: `d08547e67fcd7a0337e3fb2a907b2c5c13239ad00d22c56671461bd91146d858`; 21 manifest-listed destination files checked, 21 matched and 0 mismatched/missing.
- Frozen Protocol v1 SHA-256: `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.
- T52 acceptance map SHA-256: `cfffe7bf94aa6b376b88f98cfb11d756fb750f9c811d280a751e66624864b208`.
- T54 runtime receipt SHA-256: `366e31179d5cef470280f3782c4ee2c26c8acc748be992983aba127569ee2690`; it reports a bounded local synthetic HTTP boundary result, not deployed behavior or calibration.
- Claims register v20 SHA-256: `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`.
- Defense dossier SHA-256: `7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2`.

## M-G — register integrity and crosswalk

The v20 CSV has 45 rows and 45 unique IDs; IDs are sequential R01–R45. Recorded statuses are 10 VERIFIED, 7 PARTIAL, and 28 UNVERIFIED. Recorded outcomes are MIXED=1, NA=32, NEGATIVE=4, POSITIVE=1, REPRODUCTION=7. The v20 counts differ from the older v17 baseline and are reported as recorded; no claim status was changed.

The generated row-by-row crosswalk is [`member-mg-register-crosswalk-20261105.csv`](member-mg-register-crosswalk-20261105.csv). It preserves each claim, citation, source provenance, recorded status/outcome, referenced path and expected digest, then reports whether the cited bytes were found under the checked roots: monolith checkout, AI-service checkout and AI-service `-latest` checkout, and `C:\Users\LENOVO\ResearchData`.

The citation/source-file path index is [`member-mg-citation-source-paths-20261105.csv`](member-mg-citation-source-paths-20261105.csv). It contains 22 distinct file references; 4 are present or available as a hash-pinned handoff copy, while 18 are not present in the checked roots (monolith, AI service, AI-service `-latest`, frontend `-latest`, `ResearchData`, and this handoff). The current `BACKLOG_LEAD.md` exists but has SHA-256 `9ede8b3c09e6fa093174871fb5edc8e504a90fb993815f6e3668a4bf4278d7ff`, which differs from the historical digest `8b025dd6…` cited in v20. The Protocol v1 reference is available under the handoff name `protocol-v1.md` and matches its cited SHA; the chapter 12 draft is available as a hash-pinned handoff copy. These file-path checks do not establish the referenced claims.

- Distinct cited artifact paths: 18; found in checked roots: 0.
- Distinct reproduction-tool paths: 10; found in checked roots: 0.
- The CSV and manifest establish what the Lead recorded and that the handoff bytes match their pins; they do not establish that the cited underlying claim artifacts or reproduction tools are correct.
- R38 remains recorded as VERIFIED/POSITIVE in v20, while its separate reconciliation documents a distinct 6,633-node manifest (`ade85c33…`) from the Member 6,632-node candidate (`460de87d…`). The referenced R38 reproduction artifact/tool is not present in the checked roots. No equivalence, reproduction, or status change is made.

## M-H — manuscript and defense handoff

All 14 destination chapter files listed in `SOURCE.json` were checked against their destination hashes. 10 are byte-identical to the pinned source baseline. Four intentional destination differences are recorded in `SOURCE.json`: `chapters/04-instrument-and-identity-contract.md`, `chapters/06-human-validity-audit.md`, `chapters/08-allergen-methods.md`, and `chapters/13-discussion-and-threats.md`. All 14 match their declared destination hashes; Protocol v1 remains unchanged. The defense dossier contains 28 distinct IDs among Q01–Q28; its hash matches the manifest.

This establishes the presence and integrity of draft text only. It does not supply accepted results, final scientific argument, a clean-checkout pipeline producing accepted figures/tables, third-person reproduction, Q20 revalidation receipt, or an observed presenter/listener rehearsal. Keep those gates open.

## Repository baseline observed

- Monolith worktree: branch `codex/graph-sample-preview-20261105`, HEAD `fbdbe848b667bf35917d94700d538aa02e86697f`, same as its task branch remote-tracking ref. Three unrelated untracked files/directories were present and preserved.
- AI service worktree: branch `codex/graph-sample-preview-20261105`, HEAD `d93839868cd5f9ce3c7abcf0f70a58f894daee1c`, same as task branch remote-tracking ref.
- Frontend worktree: branch `codex/graph-sample-preview-20261105`, HEAD `e04c1c2b7859f10e36dbe04519a6b1354419b1c5`, same as task branch remote-tracking ref.
- Infrastructure worktree: `master`, HEAD `62951ed85f97bd9491c8d3fba02338046994c9f3`, same as `origin/master`.

## Exact remaining inputs

1. For M-G: the source wording ledger and per-claim evidence/reproduction files named in the crosswalk, including the referenced R38 report/tool and underlying graph/browser captures, or a Lead-issued complete evidence package with access paths. Lead adjudication remains separate.
2. For M-H: accepted/source-authorized outputs for the clean reproduction package, the consolidated Q20 revalidation receipt, and a third person to perform and sign the reproduction/rehearsal steps.
3. For M-B: the historical GISMo positional cache/map and exact 971-profile FooDB input remain absent. The existing private Kaggle index-replay resource metadata names `tnvtrung/recipe1m-gismo-index-replay-20261104`; a read-only `datasets files` query returned HTTP 403, so no remote files were listed or downloaded. The recorded user authorization is operation-scoped and does not grant this account access to that private resource.
