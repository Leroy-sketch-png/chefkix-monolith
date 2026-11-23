# R04 safe evidence handoff — 2026-11-23

This is a **sanitized derived handoff**, not an exact copy of the R04 machine receipt and not a closed, runnable reproduction package. The source register and its claim meaning are unchanged.

## Source binding and transformation

- The current v20 R04 row identifies the source artifact `ai/evidence/claims-register/r04-full-split-reproduction-20261021.json`, SHA-256 `99469392617d18ba2786e92901ac346ee6c46e9c77bdc77161ac3e1ba631eef9`.
- Its source-only builder is `ai/tools/audit_senath_full_split_identity.py`, SHA-256 `dd692e077ad5eea44c2749641f8b1dcc66a3491707efe98e34c71976f2bceba9`. The file in `tools/` below is a byte-preserving copy of that source.
- The aggregate source report is `ai/evidence/claims-register/r04-full-split-reproduction-20261021.md`, SHA-256 `afe9bde36c1b8e80611c228f1552e5ed1ccfce6fc16445dd0c072a02a247c615`. It is the provenance for the aggregate summary in `r04-aggregate-summary.md`; that summary deliberately omits local paths and per-output hashes.
- The v20 row cites the independent T07 review at SHA-256 `74df06e2cf6026009c83624c98a52b0bae8ea1b9daa8d7901dfb67fd1d18daa7`. Its scope and limitations are recorded in the v20 row; this handoff does not represent a new review.
- The R04 receipt binds the upstream FlavorGraph commit `8d3472d0823f4542b2ce47f83e28046c41d2f06a`, input tree `c8b13e0608752805f39d120e47eb7c4f76f9e040`, nodes blob `5135807f8da62108127b89a81afb101e0d8c8507`, and edges blob `f8fc6a544fddd4230188e91b3a17436a4b750ed3`. It also pins the three split inputs at SHA-256 `65d143c3bacd21555e5f16abdddd961585fce7dcbfdabfcbad0c5ccf8d60557c`, `002588cb1d7db2d1644cb16079542df641c0ace4807490be380e150973796a0b`, and `7d239b405ef71a4dea3513afe2a22b11b9e0e15256428b40896a31cae0ddd2a6`.
- The output manifest is pinned by the original receipt at SHA-256 `a5d1fafa4b6f0078330693a433487f69f66eb771de7804699caeed5101036bca`; the v20 claim says all six output hashes matched the prior source-join receipt. The output file names and individual hashes are intentionally not reproduced here because the local-only output set includes identity-map and label material.

## Safe aggregate result

The deterministic identity-strata reconstruction recorded:

| Split | Rows | GISMo graph-ID seen / unseen | Senath-normalized sensitivity seen / unseen |
| --- | ---: | ---: | ---: |
| Train | 49,044 | 23,185 unique graph-ID pairs | 23,421 unique normalized pairs |
| Validation | 10,729 | 6,827 / 3,902 | 6,771 / 3,958 |
| Test | 10,747 | 6,932 / 3,815 | 6,866 / 3,881 |

GISMo graph-ID identity and Senath-normalized string identity are separate contracts; their partitions are not interchangeable or averaged. The v20 R04 status remains `VERIFIED / REPRODUCTION` at its stated narrow boundary. This does not establish paper-protocol equivalence, rights, semantic correctness, evidence coverage, human adjudication, inference, ranking, scoring, performance, generalization, or utility.

## Included code and reproduction boundary

`tools/audit_senath_full_split_identity.py` is source only. It writes case frames, identity maps, split counts, and label outputs when run; none of those outputs or source datasets are included here. The tool imports `build_senath_candidate_pools` directly. The original R04 receipt does **not** pin the exact run-time bytes of that imported wrapper, so this handoff is not independently reproducible end-to-end from its contents alone. The current root checkout had wrapper bytes SHA-256 `2df4d4a03124410f255b8e82c7474d9505acb119dcef5603e3b3bd85fd6a5960` during this handoff, but that is an observation of current bytes, not run-time provenance; the wrapper is not copied here.

No input data, source mirrors, model/checkpoint/pickle files, case rows, labels, identity maps, output files, or private filesystem paths are included. No reproduction was run for this handoff. The older `evidence/lead-l48-r04-r05-reproduction-20261021.md` remains an unchanged historical note and predates the later R04 v20 verification.
