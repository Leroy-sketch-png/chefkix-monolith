# R05 fixed split-count evidence handoff — 2026-11-05

This is a byte-preserving transfer of the narrow R05 local split-count report, its source-free reproduction tool, and the existing independent review. The sources were found in the separate Lead research checkout at HEAD `74a1e7a98dd556b674e7a207ba64459d50f88cdc`; the Lead/M-G crosswalk in this repository had checked other roots and recorded these paths absent there. Each copied file hash was checked against its source. The scoped `.gitattributes` disables line-ending conversion for these pinned evidence bytes.

The report records only row counts and SHA-256 pins for three fixed local CSV files: train 49,044; validation 10,729; test 10,747. It includes relative input paths, file hashes and the selective-manifest hash. No CSV, row, case, label, candidate, model, key, or source-data bytes are included. The tool reads three caller-provided files and emits only hashes/counts; the inputs themselves are not part of this handoff, so it cannot run from this checkout alone. No data was re-read or re-counted during this transfer.

The existing v20 register already records R05 `VERIFIED / REPRODUCTION` for this literal local fixed-file count contract. This handoff does not change that status or broaden it: no paper-protocol equivalence, identity join, benchmark performance, generalization, rights, or broader M-B/M-G/L48 acceptance follows. Rights and upstream release identity remain outside this receipt's evidence.

## Byte manifest

| Destination path | SHA-256 |
| --- | --- |
| `claims/r05-split-counts-reproduction-20261021.json` | `e85a635a762095201eb106237098b159249b0ef7ee16160ad4b3b8034a77875e` |
| `tools/reproduce_lead_split_counts.py` | `23a92e0bb6b2d7453e5b2f208690bf61da2fc13c788183463d65c10fcccba3d5` |
| `reviews/T06-r05-reproduction-review.md` | `83f33d4bb3c9f34e3838746a38257e3412a7b0cf2e1bc7f73240aa396786a979` |

The R05 register row is preserved as recorded in v20 (SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`). The original Member crosswalk remains an immutable snapshot; this handoff records the additional Lead-root discovery.
