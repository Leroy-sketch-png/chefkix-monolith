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

Additional historical context: the clean upstream Recipe1MSubs code checkout at `14014ad501afd0c7da95ed90de19096c7f12ff75` contains mapping code whose commented run output reports 6,632 unique GISMo-to-FlavorGraph ingredient mappings. That script points to `recipe1m/preprocessed_flavorgraph_substitutions_fixed_3/final_recipe1m_vocab_ingrs.pkl`, which is absent from the checkout. This supports the candidate count as a plausible historical mapping size, but does not reproduce the missing cache, establish positional recipe-index identity, or reconcile R38's 6,633-node artifact.

## T01 train-row provenance verification

The independent T01 verifier is AI service commit `650713d4ca947678f55e977cfd85c362b729045f`; script SHA-256 `a86b3892e75d4677226ed074f31354017df3daaca8042471475f34c66ce9f2c8`; test SHA-256 `5f2316e0219276aafa8a167db9bacf8bf905d2252746cefbe47e06314bb2dbd1`.

Command:

```powershell
python tools/verify_graph_eval_v4_t01.py --bundle-dir C:\Users\LENOVO\ResearchData\member-ma-graph-eval-v4-clean-build-20261104-run3 --expected-manifest-sha256 460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060 --subs-dir C:\Users\LENOVO\ResearchData\Recipe1MSubs --gismo-runtime-dir C:\Users\LENOVO\ResearchData\gismo-runtime-20261024 --gismo-source-dir C:\Users\LENOVO\ResearchData\gismo-source-20261024
```

Result: `PASS` for the provisional candidate. The verifier matched all 49,044 `edge_evidence.jsonl` row keys and source/target/context hashes to the pinned train pickle, checked all 23,185 edge evidence-reference sets and pair counts, found 0 identity-error rows, and confirmed 6,632 nodes / 2,214 isolates. Every evidence row says `train`; no validation or test split was opened by this check. Focused builder and verifier tests: 6 passed; both scripts compiled; `git diff --check` passed.

This proves candidate-level T01 source tracing only. The manifest remains `PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED`; the Lead's T52 decision, original R38 reconciliation, T02 served-byte proof, and product/browser acceptance are still required.

## Checks performed

- `python -m pytest -q -o addopts= tests/test_build_graph_eval_v4.py`: 3 passed.
- `python -m compileall -q tools/build_graph_eval_v4.py tests/test_build_graph_eval_v4.py`: passed.
- `git diff --check`: passed.
- The earlier in-process smoke loaded run 3 and returned `ready` before the acceptance-state guard was added. That result is superseded: the same provisional manifest is now intentionally rejected by the provider.

## Provider acceptance and tamper gate

AI service commit `bac6673af95228f5c396c53ab3d0925001a1fe6c` makes the provider reject any manifest whose state is not exactly `ACCEPTED`, in addition to its existing caller-pinned manifest hash and output-hash checks. This prevents a matching hash alone from making the current `PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED` bundle serveable. Test coverage was completed in `f699c77d5c258e790a3ecadd7b35c8b56d89f247`; provider source SHA-256 `91718259867886704b320edcf6ef27bfe04519ca07ef36303dc0e6e066bed381`, test SHA-256 `145e63bfd625e4e2c3c8e06eb6ec4cb56c39745654b6902efc53bbe94b659f1e`.

`python -m pytest -q -o addopts= tests/test_artifact_backed_graph_provider.py tests/test_graph_preflight_app_composition.py` completed with 11 passed and 1 skipped. Unit fixtures prove provisional-state refusal, accepted/hash-pinned fixture loading, and output-byte tamper refusal. A read-only call against actual run 3 with manifest SHA-256 `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060` returned `status=invalid; provenance=None; candidates=0`, as required for its provisional state. The skipped test targets the unavailable historical `D:/code/chefkix/ai/out/graph_eval_v4_clean` bundle. `py_compile` and `git diff --check` passed. The runtime dependencies needed for collection were installed into a temporary directory outside the repository.

This is provider-level guard evidence only. The app gateway remains default-off, and the actual 6,632-node candidate remains provisional. T02 served-byte proof, the original T52 acceptance map, R38 reconciliation, an accepted real artifact load, and HTTP endpoint/browser rendering have not been demonstrated.

## Unresolved acceptance boundary

The leader's R38 record cites a different, unavailable bundle: manifest prefix `ade85c33…`, 6,633 nodes, 23,185 edges, 0 identity errors, plus browser proof. Neither that bundle's bytes nor `T52-c5-acceptance-map.md` were found in the checked-out repositories or local research workspace. This candidate has 6,632 nodes because two pinned vocabulary labels have no catalog identity. The matching edge count does not reconcile the one-node mismatch or reproduce the historical manifest.

Required Lead input/decision: provide the exact original bundle and T52 map with their source pins/hashes, or explicitly decide whether this 6,632-node candidate supersedes the R38 artifact and specify the claim/register disposition. Until then M-A stays `READY_FOR_LEAD`; M-C/M-E must not serve it as accepted evidence. No test, validation, or test partition was used to select or tune this graph. Bundles remain local under `C:\Users\LENOVO\ResearchData`; no source or artifact was uploaded or published.

Two superseded first-pass bundles (`...run1` and `...run2`) remain in that local directory. They included every FlavorGraph catalog node (8,298 nodes; manifest SHA-256 `7b9986f66c83863d23b31efefb6eb85137996aa7fd8930116737bdd921097e55`) and are explicitly excluded from this receipt. The local command policy blocked removal of those generated directories, so they were retained rather than removed through another path.
