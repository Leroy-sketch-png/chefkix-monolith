# M-A accepted-state hash correction and Git delivery — 2026-11-14

## Scope

The owner-local five-file package at `C:\Users\LENOVO\ResearchData\member-ma-graph-eval-v4-accepted-state-20261105` was rehashed and copied byte-for-byte to `evidence/member-ma-accepted-state-package-20261105/`. This follows the package-specific HUMAN sharing authorization and shared-repository direction recorded in [`BACKLOG_LEAD.md`](../BACKLOG_LEAD.md) on 2026-11-12. It applies only to this package and is not a general rights determination.

No source corpus was copied. No package file was edited or regenerated. The path-specific `-text` rule in `.gitattributes` prevents Windows line-ending conversion.

## Corrected hashes

| File | SHA-256 | Bytes |
| --- | --- | ---: |
| `manifest.json` | `5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e` | 2,804 |
| `nodes.jsonl` | `89b8ba22ab73ccad1c4de4d55706ff6efdf45f071ec5929934be7b73b0f74e75` | 1,110,852 |
| `edges.jsonl` | `7a434ef79d39e0be54e0f234c24180cb13757e68780a12b189eb1a1ef8a6f1e1` | 7,446,458 |
| `edge_evidence.jsonl` | `07d4db1d34877447d51af0761158eef4229389c49f18277c8f5473cf005ef60c` | 30,755,862 |
| `row_errors.jsonl` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | 0 |

The 2026-11-11 byte-verification receipt contains a transcription error for `edges.jsonl`: it records `7a434ef7b39e0be54e0f234c24180cb13757e68780a12b189eb1a1ef8a6f1e1`. The correct digest is `7a434ef79d39e0be54e0f234c24180cb13757e68780a12b189eb1a1ef8a6f1e1`; the owner-local file, the package manifest's `outputs_sha256` entry, and the 2026-11-05 package receipt agree. The earlier receipts remain unchanged.

## Acceptance boundary

This corrects the hash record and delivers the exact five files to the shared repository. LEAD must independently fetch and rehash the published files before deciding whether to accept the state-only manifest. T01/T02 remain accepted only for the original manifest; this delivery does not establish T03/T04/T52/C5, R38 equivalence, completeness, identity truth, safety, or product use.
