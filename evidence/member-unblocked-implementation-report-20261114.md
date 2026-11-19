# Member unblocked backlog implementation report

**Date:** 2026-11-14
**Scope:** source-free M-I implementation slices for T54 and L43 C4
**Disposition:** implementation work is ready for Lead review; no empirical result, acceptance, milestone credit, or protocol change is claimed.

## Delivered

### T54 calibration producer

Implemented the prospective recipe-group split and request-time cell projection in the existing AI-service producer path. The v3 bundle revision stays in the existing writer/loader and binds the full frame, recomputed assignment, projection, typed score rows, source pins, graph files, and role bytes. The four score outcomes remain distinct; raw file SHA-256 values and canonical typed-row digests are separate. Descriptive cutoffs and finite-only sample coverage are retained, while product `tHat` remains null with `no_guarantee / inference_assumptions_unresolved`. Missing or mismatched role bytes fail closed, and the typed response carries a calibration-receipt hash separate from the graph-manifest hash. Route activation was not changed.

### L43 C4 pool judge

Updated the existing runner to check the committed prompt files, parse only strict JSON with exact keys and opaque-ID coverage, and require observed per-response model, temperature, and prompt-hash metadata. The parser no longer invents prompt hashes for saved outputs. Candidate identities are deduplicated only across distinct arms; same-arm duplicates fail. The evaluator reports the fixed initial-request denominator, first and terminal attempt dispositions, A/C/N/U counts, forward-versus-human agreement, cluster-bootstrap requested/valid/degenerate draws, signed cell leniency, conservative order-instability bounds, model/family eligibility, input hashes, and pass identities. The GPT-4o common-slate helper admits only GISMo/lookup and explicitly DR-04-eligible Mistral rows, excludes every GPT-4o-generated identity, and refuses to drop a preselected case. A sanitized Markdown renderer is included.

These changes only verify implementation behavior on synthetic fixtures. No judge/provider calls, spend, human labels, test scoring, C4 acceptance, prompt freeze, or empirical claim was created.

## Verification

| Scope | Result |
|---|---:|
| AI-service full test suite | 649 passed, 12 skipped |
| T54 focused producer/bundle/evidence contracts | 72 passed |
| C4 source-free operational contract suite | 30 passed |
| Existing FooDB-native evaluator contract suite | 13 passed |
| Changed Python modules compile; `git diff --check` | Passed |

The AI-service full run emitted 29 existing deprecation warnings. No failing tests remain in the completed runs.

## Remaining input gates

- **M-A private graph build:** the official FooDB archive is present and matches the recorded archive SHA-256, but the exact ID-bearing `foodb_compounds.json` candidate pinned as `8490cd6a…` is absent from the accessible checkouts and ResearchData. The local name/profile-key artifacts are not substitutes. No top-255 selection or graph output was reconstructed or added to Git.
- **T54 real-input verification:** the source identity map and exact graph/filter/ranker/scorer/score-contract/label-contract bytes are still required for real binding verification. The implementation remains descriptive and fail-closed.
- **C4 execution:** the hash-bound eligible slate, final adjudicated human references, final frozen execution prompt/manifest, and authorized account/cost/capacity evidence remain absent. The development prompt assets do not authorize test scoring or provider calls.

The overall M-I/C2 dispatch and C4 acceptance remain blocked by their independent gates. M-A/M-B/M-C identity and consumer acceptance remain separate. Existing accepted-state manifests and backlog acceptance statuses were not changed.
