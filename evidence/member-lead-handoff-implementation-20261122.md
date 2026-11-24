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

## T54 parent manifest follow-up (2026-11-23)

AI-service commit `cc4405a` implements the parent manifest binding correction. Bundle schema v4 inventories the exact validation-frame and identity-source manifest files. The writer derives and checks both raw digests before writing. The loader recomputes both digests from the file bytes and checks them against the independent expected pins. A missing or mismatched parent returns no guarantee with null `tHat` and no receipt. Legacy v3 bundles require a rebuild with both parents.

The focused bundle and receipt suites passed 33 tests. The fixtures cover both parents, including a tampered parent whose inventory entry and outer manifest were resealed. Lead delta review and authorized real calibration inputs remain open. The route remains default-off.

### Lead review disposition — 2026-11-24

**ACCEPT narrowly at source-and-synthetic-fixture scope** for AI-service commit `cc4405abee6ba4f0d183d01a4b668315284144c8` on verified `origin/main`. The v4 writer compares the raw bytes of both supplied parent manifests with caller-provided expected SHA-256 pins before writing; the loader recomputes those hashes from the files it actually reads and compares them with its separately supplied expected pins. Its required-file inventory includes both parents. The focused bundle/receipt suites passed 33 tests; a bounded synthetic check also confirmed fail-closed handling for a pinned but malformed parent manifest and a resealed legacy-v3 manifest. Two existing default-off composition/gateway tests passed. In the key tamper fixture, changing a parent and resealing both its file-inventory entry and the outer bundle manifest still returns `no_guarantee`, null `tHat`, and no receipt because the independent expected parent pin does not match.

This review does not establish where production callers obtain independent pins: a bounded search found the writer/loader call sites only in tests, with no production construction path. It also does not establish real input/role binding, calibration output, a product guarantee, runtime behavior, route activation, or T54/C2/C3 acceptance. **Next owner operation:** M-I/input owners provide the exact private parent manifests, identity map and required role bytes plus the production caller’s independent expected pins; then Lead can review that binding and the authorized runtime evidence. The route remains default-off. No real inputs or calibration were accessed or produced.
