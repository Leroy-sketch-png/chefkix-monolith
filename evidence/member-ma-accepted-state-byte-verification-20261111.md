# M-A accepted-state package byte verification — 2026-11-11

## Result

On 2026-11-11, Member directly rehashed the five files in the owner-private accepted-state package. The manifest digest matches the shared M-A receipt, and each output digest matches both the package manifest and that receipt. The JSONL output contents were not opened or copied.

| File | SHA-256 | Bytes |
| --- | --- | ---: |
| `manifest.json` | `5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e` | 2,804 |
| `nodes.jsonl` | `89b8ba22ab73ccad1c4de4d55706ff6efdf45f071ec5929934be7b73b0f74e75` | 1,110,852 |
| `edges.jsonl` | `7a434ef7b39e0be54e0f234c24180cb13757e68780a12b189eb1a1ef8a6f1e1` | 7,446,458 |
| `edge_evidence.jsonl` | `07d4db1d34877447d51af0761158eef4229389c49f18277c8f5473cf005ef60c` | 30,755,862 |
| `row_errors.jsonl` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | 0 |

The verification used SHA-256 hashes from the local files, compared the manifest's `outputs_sha256` map, and matched all five hashes to [`member-ma-accepted-state-package-20261105.md`](member-ma-accepted-state-package-20261105.md). The package remains owner-private and outside Git; no graph payload was added to this repository.

## Acceptance boundary

This confirms byte identity with the receipt only. The manifest records `ACCEPTED`, but this Member verification is not Lead approval of the new state-only manifest, does not extend the narrow T01/T02 decision from the original manifest, and does not establish R38 equivalence, completeness, serving, product use, or T03/T52/C5 acceptance. The separate 6,633-node R38 bundle and browser captures remain outstanding.

## Next operation

After Lead reviews the new manifest for the intended accepted-artifact consumer, make the exact verified package available to the runtime workspace through an authorized path. M-C/M-E must bind the intended provider to the exact bundle path and manifest hash, then record a real endpoint/Next/Explorer/browser served-hash result. Keep the provisional preview consumer separate.
