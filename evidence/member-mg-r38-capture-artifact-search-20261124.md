# R38 capture artifact search — 2026-11-24

## Decision boundary

The Lead's 2026-11-24 decision adopts the structural integrity subclaim for `graph_eval_v4_clean` narrowly: manifest `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1` and its four declared output hashes were verified. It does not establish that the clean graph was served or shown in the live Explorer. The separate M-A preview has 6,632 nodes and different hashes.

## Local search

Searched the current monolith, AI-service, frontend, and infrastructure repositories for the cited `C5-G4-runtime-walkthrough.md`, the named R38 reproduction report, capture/screenshot files, and references binding browser images to the exact clean manifest. The walkthrough and R38 reproduction report were not present by path in these four roots. The following four graph-related local screenshots were inspected and hashed; their visible content is for different graph states, and none displays or binds the accepted `ade85c…` manifest.

| Local screenshot | Bytes | SHA-256 | Why it does not satisfy R38 |
| --- | ---: | --- | --- |
| `output/sprints/backlog-2026-11/evidence/sprint4_live_graph_20261105.png` | 149,624 | `ace6b45aeb65a7a3b84dee55e4453e83eecec7bd77f93621ed562d8ce9282e5f` | Shows the 25-item bounded legacy Knowledge API preview, which says it is not the accepted study graph. |
| `output/sprints/backlog-2026-11/evidence/sprint4_live_unknown_search_20261105.png` | 127,895 | `df391325897b4c4cad56385db7cfa04b54d6d4a02deba84d460879d491ce390c` | Same 25-item legacy API state with an unmatched search; no accepted graph or manifest identity. |
| `output/sprints/backlog-2026-11/evidence/graph_candidate_preview_via_monolith_20261105.png` | 205,010 | `b5d699863d51617236ede38635aa9e9f6b00275689efb0f11ba0e150d88b6b69` | Explicitly labels the 6,632-node private candidate as not accepted for study claims or safety decisions. |
| `output/sprints/backlog-2026-11/evidence/graph_candidate_preview_butter_search_via_monolith_20261105.png` | 207,024 | `76d7596a50d3fdbaa4b8af8f77d89f7fd8e3149666b0b1e7c98c8b31c3d9101b` | Same unaccepted 6,632-node candidate with a search; it is not the 6,633-node clean artifact. |

The older `evidence/member-mc-live-graph-20261023/graph-live-desktop.png` is from a scoped local smoke whose receipt explicitly says it is not production acceptance; it also has no binding to the clean manifest. The local screenshots above remain in their existing private/untracked locations and are not included in this receipt.

## Remaining operation

No exact four-capture set or served-manifest binding was established by this search. M-E must identify the four precise existing captures and provide hashes/metadata for the walkthrough, and M-C must bind that evidence to the exact served `graph_eval_v4_clean` manifest. Keep the byte-preserved R38 v20 status unchanged. Do not substitute the separate M-A candidate or infer why it has one fewer node.
