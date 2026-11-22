# Member implementation of the 2026-11-22 Lead handoffs

| Handoff | Delivered | Verification and remaining gate |
| --- | --- | --- |
| M-I / C4 | The existing saved-response runner binds the Lead-frozen system and user prompt pins, prompt version, and planned request hashes for both orders. The distinct reverse summary reports assigned cases/units, terminal valid/missing/invalid coverage, and A/C/N/U counts. | 32 source-free tests passed. Forward remains primary. Independent review of the delta, exact private adjudications/pools, transfer and dispatch are still open. |
| M-A / native FooDB parser | Corrected publicly handed-off candidate revision 1e1f8d0. Selected and skipped JSON values reject nonstandard constants, and equivalent decoded top-level profile keys are rejected as duplicates. | 20 source-free tests passed. The private ID-bearing input and historical producer f2fbd52e remain separate reproduction dependencies. Independent Lead review remains open. |
| M-C / accepted graph | AI-service f74db77 registers the existing artifact provider and canonical-ID-only index for the authenticated research preflight, with an explicit default-off flag, absolute bundle path and exact pin. Providers consume the same bytes whose output hashes passed. | 42 focused tests passed, one historical external bundle test skipped. The selected real M-A package check passed in local HTTP integration with synthetic service and research credentials. Independent acceptance, deployment and the separate T395 staging/Explorer proof are open. |

## Accepted graph consumer receipt

- Source revision: AI-service f74db77. Configured bundle: local checkout of [the shared five-file M-A bundle](member-ma-accepted-state-package-20261105/).
- Expected and observed manifest SHA-256: 5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e. The existing provider verifies the output hashes and the accepted-state predicate before constructing the local index.
- Existing protected POST /api/v1/suggest_substitutions returned HTTP 200, ready graph provenance with the observed hash and train split, abstention, empty exposed candidate list, and null calibration tHat. An unrecognized source was not evaluated. Invalid service credentials returned 401. No actual IDs, candidate rows, credentials or raw payloads are included in this receipt.
- The check used no external provider or model calls, graph generation, downstream browser, or deployment. It is local consumer integration evidence. T03/T04, T52/C5, ranking, safety and milestone acceptance retain their existing gates.
