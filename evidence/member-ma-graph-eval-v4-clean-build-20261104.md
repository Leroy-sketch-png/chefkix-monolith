# M-A graph_eval_v4 clean-build receipt — 2026-11-04

## Delivery state

`READY_FOR_LEAD` — member build and byte-replay evidence are complete for a private candidate. This is not the historical `graph_eval_v4_clean`, not a T52 acceptance, and not permission to enable serving. The bundle manifest itself is marked `PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED`.

## Pinned inputs and source

- AI service source commit: `601419a0765d593ebb96a53b37831831a8e4f379` (`fix(research): emit graph vocabulary nodes only`); initial builder commit: `edea730a32ce4b257557d343a4a7ed3941f30c8a`.
- Builder: `tools/build_graph_eval_v4.py`; script SHA-256 `bbfd80c6fe06b6336061cf85ff3bbc3ef54c0d7abfac3d1127cda00b63a1b511`.
- Recipe1MSubs train pickle: 49,044 rows, SHA-256 `b4d58e6c6c6747671e531812393397acf75c02d2a9ce00c2edcc7794bf567c81`.
- GISMo runtime manifest: SHA-256 `e08b82b7ff138d2523e077406b1a66e6c238557aead8faa83c04a1421fc3fd97`; GISMo commit `f5d2394c7ade897987ce584d2a75d8bbbbae058e`; FlavorGraph commit `8d3472d0823f4542b2ce47f83e28046c41d2f06`.
- Pinned vocabulary SHA-256 `ad8c62dee7d1314be6e173c3f3bbde473c2c79352bd6e3ccf656a6ac9a488ed4`; node catalog SHA-256 `ea59d1858ae3a41de14aeabda32e6274693ea7897eb6ae1a3b7262bac5526d5a`.
- Identity resolution uses exact substitution endpoint → pinned `word2idx` → first `idx2word` canonical label → pinned node catalog, with the catalog's last-row-wins duplicate-name behavior. No positional GISMo recipe-index mapping is used.

## Rebuilds

Command, run independently twice from the AI service checkout:

```powershell
python tools/build_graph_eval_v4.py --subs-dir C:\Users\LENOVO\ResearchData\Recipe1MSubs --gismo-runtime-dir C:\Users\LENOVO\ResearchData\gismo-runtime-20261024 --gismo-source-dir C:\Users\LENOVO\ResearchData\gismo-source-20261024 --output-dir C:\Users\LENOVO\ResearchData\member-ma-graph-eval-v4-clean-build-20261104-run3
python tools/build_graph_eval_v4.py --subs-dir C:\Users\LENOVO\ResearchData\Recipe1MSubs --gismo-runtime-dir C:\Users\LENOVO\ResearchData\gismo-runtime-20261024 --gismo-source-dir C:\Users\LENOVO\ResearchData\gismo-source-20261024 --output-dir C:\Users\LENOVO\ResearchData\member-ma-graph-eval-v4-clean-build-20261104-run4
```

Both bundles produced manifest SHA-256 `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060`; all five output hashes matched byte-for-byte:

| Output | SHA-256 |
| --- | --- |
| `manifest.json` | `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060` |
| `nodes.jsonl` | `89b8ba22ab73ccad1c4de4d55706ff6efdf45f071ec5929934be7b73b0f74e75` |
| `edges.jsonl` | `7a434ef79d39e0be54e0f234c24180cb13757e68780a12b189eb1a1ef8a6f1e1` |
| `edge_evidence.jsonl` | `07d4db1d34877447d51af0761158eef4229389c49f18277c8f5473cf005ef60c` |
| `row_errors.jsonl` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |

The train projection is 49,044 rows, SHA-256 `a0b050b28ae2ede67d40badc97d69b26fc2c1e777b74000f899aa790d658bcfa`. Counts: 6,632 vocabulary-backed nodes, 2,214 isolates, 23,185 deduplicated directed edges, 49,044 evidence records, and 0 identity-error rows. Of the train rows, 48,096 have exactly one source-ingredient slot and 948 have ambiguous source slots; 0 are missing a source slot. The ambiguity count remains visible and is not counted as an identity error.

The pinned vocabulary has 6,634 distinct first-canonical labels; 2 are absent from the pinned node catalog. The manifest records the count and a hash of the sorted absent-label list (`20be016b8b882132004ca93ff7dfb2adb4042280555e91388a5c0ae5e8c9ad32`). No alias or fabricated node ID was added.

## Checks performed

- `python -m pytest -q -o addopts= tests/test_build_graph_eval_v4.py`: 3 passed.
- `python -m compileall -q tools/build_graph_eval_v4.py tests/test_build_graph_eval_v4.py`: passed.
- `git diff --check`: passed.
- Existing `ArtifactBackedGraphCandidatePoolProvider` loaded run 3 when pinned to its manifest hash and returned `ready`, source split `train`, and one candidate for node `1`. This was an in-process provider smoke, not an HTTP endpoint or browser test. Normal package import in the checkout is currently missing the `limits` Python dependency; the smoke loaded the provider module directly.

## Unresolved acceptance boundary

The leader's R38 record cites a different, unavailable bundle: manifest prefix `ade85c33…`, 6,633 nodes, 23,185 edges, 0 identity errors, plus browser proof. Neither that bundle's bytes nor `T52-c5-acceptance-map.md` were found in the checked-out repositories or local research workspace. This candidate has 6,632 nodes because two pinned vocabulary labels have no catalog identity. The matching edge count does not reconcile the one-node mismatch or reproduce the historical manifest.

Required Lead input/decision: provide the exact original bundle and T52 map with their source pins/hashes, or explicitly decide whether this 6,632-node candidate supersedes the R38 artifact and specify the claim/register disposition. Until then M-A stays `READY_FOR_LEAD`; M-C/M-E must not serve it as accepted evidence. No test, validation, or test partition was used to select or tune this graph. Bundles remain local under `C:\Users\LENOVO\ResearchData`; no source or artifact was uploaded or published.

Two superseded first-pass bundles (`...run1` and `...run2`) remain in that local directory. They included every FlavorGraph catalog node (8,298 nodes; manifest SHA-256 `7b9986f66c83863d23b31efefb6eb85137996aa7fd8930116737bdd921097e55`) and are explicitly excluded from this receipt. The local command policy blocked removal of those generated directories, so they were retained rather than removed through another path.
