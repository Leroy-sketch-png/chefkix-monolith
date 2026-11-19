# M-A accepted-state package receipt — 2026-11-05

## Delivery state

`READY_FOR_LEAD` — Member reverified the held graph bundle against the existing M-A receipt and issued a separate accepted-state package. The Lead's T01/T02 decision remains narrow to the original candidate manifest; the new manifest digest is presented for review before any product-route use.

## Verification and package

- Original local bundle: `C:\Users\LENOVO\ResearchData\member-ma-graph-eval-v4-clean-build-20261104-run3`.
- Original manifest SHA-256: `460de87d4eb6834142f99c31866ef16b15b4e3ebe5ce2afe1bea228f601ee060` — matches the two-build/T01 receipt.
- New local package: `C:\Users\LENOVO\ResearchData\member-ma-graph-eval-v4-accepted-state-20261105`.
- New `manifest.json` state: `ACCEPTED`; SHA-256: `5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e`.
- The state field is the only changed package content. All four data outputs were copied byte-for-byte and rechecked against the original receipt:

| Output | SHA-256 | Bytes |
| --- | --- | ---: |
| `nodes.jsonl` | `89b8ba22ab73ccad1c4de4d55706ff6efdf45f071ec5929934be7b73b0f74e75` | 1,110,852 |
| `edges.jsonl` | `7a434ef79d39e0be54e0f234c24180cb13757e68780a12b189eb1a1ef8a6f1e1` | 7,446,458 |
| `edge_evidence.jsonl` | `07d4db1d34877447d51af0761158eef4229389c49f18277c8f5473cf005ef60c` | 30,755,862 |
| `row_errors.jsonl` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | 0 |

Counts retained: 6,632 nodes, 23,185 directed edges, 49,044 train evidence records, 2,214 isolates, 0 identity-error rows, 948 ambiguous source-slot rows, and two unmapped canonical labels. No graph rows were regenerated. Both local bundles remain outside Git; no graph data was uploaded or committed.

## Acceptance boundary and next owner

The Lead accepted T01/T02 narrowly for the original candidate manifest only. This package receipt does not itself extend that decision to the new manifest digest, close T03/T52/C5, establish equivalence to historical R38, or authorize publication, redistribution, product use, completeness, or safety claims. The new manifest digest is `READY_FOR_LEAD` for review; after that exact package is accepted for the intended route, M-C/M-E still need the real endpoint → Next/Explorer → browser served-hash evidence and remaining T52 criteria. The Graph Candidate Preview endpoint in the coordinated PR continues to accept only the provisional-preview state and is not evidence for that product path.
