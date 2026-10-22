# C2 provider-independent prompt-semantic freeze — LEAD v518

Date: 2026-10-22
Scope: internal operational semantics and local synthetic contract only; no source rows, labels, provider, API, network, GPU, or spend.

## Decision

HUMAN delegated resolution of remaining internal decisions. The lead froze the exact existing nested-slot prompt semantics as `FROZEN_PROVIDER_INDEPENDENT_PROMPT_SEMANTICS_NOT_DISPATCH_SPEC` in research task T172. Protocol v1 remains byte-identical. The freeze specifies only the provider-independent request/response meaning; it does not select a research/product winner or bind an endpoint/SDK wire schema.

The selected contract is: send only recipe title, nested ordered ingredient alias slots, and the exact target spelling; require an exact unique raw target-slot match; exclude every alias in that slot; require exactly 20 nonempty raw-distinct substitution strings; retain exact target-slot aliases at their returned ranks but mark them invalid for scoring; never normalize, silently remove, replace, reorder, or retry; retain malformed/provider failures as explicit method-failure records for fixed-denominator scoring. Protocol v1's model, temperature, and one-attempt schedule are unchanged.

## Alternatives, tradeoffs, reversal

The strongest alternative was to defer any prompt freeze until source rights and provider endpoint were known. That would avoid freezing semantics that later conflict with a wire implementation, but unnecessarily blocks independent preparation. This freeze is reversible before execution and endpoint-independent. If an authoritative source cannot supply the three permitted fields, or the exact endpoint cannot preserve the frozen response/denominator contract, dispatch stops and any change requires a new version rather than silent edits.

## Verification and pins

- Prompt artifact SHA-256: `63281b23f392920341983d5f992c3ed0482488d263dff4e0dc02137a26c7520d`.
- Local response helper SHA-256: `5df1da4153dcd3a16a03d7648c749d329bcbe9a05958ee9870de3b292c1739c8`.
- Synthetic test SHA-256: `e33076d495f1af8edd90d71837e735894510fe3333c8ed21d571c04d3052efe4`.
- Command: `python -m pytest ai/tools/test_ingest_senath_c2_response.py ai/tools/test_build_senath_c2_source_package.py ai/tools/test_preflight_c2_openai_cost.py -q` — 41 passed, 3 parameterized subtests.
- `python -m py_compile ai/tools/ingest_senath_c2_response.py` and scoped `git diff --check` passed.
- Research decision cards: `tasks/T171-c2-response-self-substitution-enforcement.md` and `tasks/T172-c2-provider-independent-prompt-freeze.md` in the local research worktree. Their full detail remains local-only because that checkout has no configured remote.

## Remaining gates

Rights/source access, source-split provenance, endpoint/SDK/wire schema and output-token headroom, current account/budget evidence, worst-case reservation, exact cohort pins and fresh local preflight remain unresolved. This evidence is not legal clearance, provider compatibility, comparative quality, C2 acceptance, or launch authority.
