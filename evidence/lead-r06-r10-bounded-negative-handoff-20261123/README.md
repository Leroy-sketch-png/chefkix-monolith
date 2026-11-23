# R06 and R10 safe evidence handoff — 2026-11-23

This handoff provides sanitized, aggregate-only derivatives of the current v20 R06 and R10 forensic receipts. It does not copy either original JSON receipt byte-for-byte: both contain private local workspace paths and references/hashes for primary output artifacts that the receipts say are absent. The v20 claims remain `PARTIAL / NEGATIVE`; no claim state, outcome, or evidence ceiling is changed.

## Source bindings

| Claim | v20 source artifact | Source SHA-256 | Recorded limitation |
| --- | --- | --- | --- |
| R06 | `ai/evidence/claims-register/instantiated-r01-r45-v1/r06-sag-rejection-reproduction-20261103.json` | `b199ff3812efe10a804447fd65e33d9f1bf37b5b4d2990111cbe9944d969f4b2` | The primary experiment output is absent; this is corroborative forensic evidence, not an independent arithmetic reconstruction from primary bytes. |
| R10 | `ai/evidence/claims-register/instantiated-r01-r45-v1/r10-clip-projection-missed-reproduction-20261103.json` | `70887dea79b213b9eb3a8961c1288a2775a8d6d5396fc65fae3d130f7b1d4df6` | The primary validation output is absent; this is corroborative forensic evidence, not an independent arithmetic reconstruction from primary bytes. |

The aggregates below are transcribed from those pinned v20 receipts. Private machine paths, primary-output hashes, and other local receipt metadata are deliberately omitted. No primary outputs or source data were recovered or re-read.

## Tool-reference boundary

The R06 and R10 v20 rows have no tool citation or tool digest. The local Lead checkout contains forensic-audit scripts, but those scripts are not bound as tools by these v20 rows and are not copied here. This preserves the crosswalk distinction `NO_REFERENCE`; it does not assert that a tool is absent. No tool was run for this handoff.

## Limits

These summaries do not restore the missing primary experiment outputs, prove the full experiment independently, establish broad model or product quality, or expand the claims beyond their recorded bounded-negative scope. The 2026-11-22 M-G crosswalk remains a dated inventory snapshot; this follow-up adds safe derived copies without rewriting that historical file.
