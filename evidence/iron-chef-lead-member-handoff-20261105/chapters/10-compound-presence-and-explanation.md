# Chapter 10 — Bounded Compound-Presence Explanations

**Working draft v0.2 — 2026-11-02**
**Evidence state:** verified FooDB presence runtime (`chefkix-ai-service/models/foodb_compounds.json`, 971 food profiles, 206 named compound dimensions, 125,992 links) and empirically recomputed full-split endpoint coverage across frozen validation (10,729 cases, 1,121 both-profiled) and test (10,747 cases, 1,162 both-profiled) splits (`ai/evidence/senath-inference/foodb-split-coverage-v1.json`).
**Use:** canonical methods account for the compound-presence explanation dimension. This establishes database presence-only factuality; it does not claim sensory similarity, functional equivalence, or live product usefulness.

## 10.1 Question and permitted answer

Can a substitution system expose a small, traceable piece of evidence about two ingredients without implying that the evidence proves the substitution works? The current system answers a narrow factual question: which selected compound identifiers are recorded for each mapped food profile, and which identifiers are shared by the two profiles? It may report the shared set and a set-overlap statistic under the named identity and profile contract.

This is descriptive database evidence. A shared compound does not establish concentration, bioavailability, sensory similarity, causal flavor compatibility, cooking-function equivalence, allergen absence, or that one ingredient can replace another. A compound absent from this bounded profile is not thereby absent from the food. “No profile,” “unresolved name,” and “no shared recorded compounds” are distinct states and must not collapse into a reassuring zero.

## 10.2 Recorded source and service boundary

The active strategy's inventory records an installed artifact with 971 food profiles, 206 named compound dimensions, and 125,992 binary links. Its lineage is tied to official FooDB source evidence and an explicit identity/provenance sidecar; 49 high-frequency source Content identifiers that do not resolve to Compound rows are excluded from named dimensions, and four fingerprinted profiles have no selected named compounds. The artifact is a bounded projection, not a complete chemical analysis of foods. Rights are separately recorded as non-commercial, and the source data must not be treated as freely redistributable.

The strategy's historical LEAD v5 entry reports exhaustive identity/arithmetic checks across 471,906 profile-pair responses, covering the 125,992 links and 206 identities with zero mismatches, plus a deterministic 512-pair response audit. That sample checked shared-compound completeness, separately labeled examples, omission of a chemistry-confidence field, explicit `technique_evaluated=false`, and missing-profile failure. These are recorded historical claims from the active ledger; the draft has not independently rebuilt the source conversion, runtime, or receipts. They support only the bounded response-contract/factuality boundary when their cited producer artifacts are reconstructed.

The endpoint-profile presence has now been empirically recomputed and verified across all $10,729$ validation cases and $10,747$ test cases under the pinned FooDB runtime (`audit_senath_foodb_split_coverage.py` and `foodb-split-coverage-v1.json`):
- **Test Split ($N=10,747$):** exactly $1,162$ cases ($10.81232\%$) have FooDB compound profiles for both endpoints (`both_profiled`), formally adjudicated as VERIFIED in Claims Register v17 (Claim R14, SHA-256 `50014fff3b8562b38854e0a5e7f3c93721983337f2de16b600421e9c9e0bfa24`); $4,099$ have a profile for exactly one endpoint (`partial`); and $5,486$ have neither (`none`). Across all $21,494$ endpoint occurrences, $6,423$ occurrences ($29.88\%$) map to a documented profile, spanning $252$ distinct profiled food strings out of $2,762$ distinct raw endpoint strings.
- **Validation Split ($N=10,729$):** exactly $1,121$ cases ($10.44832\%$) are `both_profiled`; $4,017$ are `partial`; and $5,591$ are `none`. Across $21,458$ endpoint occurrences, $6,259$ ($29.17\%$) map to a profile, spanning $264$ distinct profiled strings out of $2,767$ raw endpoint strings.
- **Invariance across tasks:** Even when both endpoints resolve to profiles, shared compounds indicate catalogued chemical presence, not culinary interchangeability, cooking functionality, or taste equivalence. These empirical counts are verified under SHA-256 hashes binding `cases_test.jsonl` (`60448a9f...`) and `cases_val.jsonl` (`db33e767...`).

## 10.3 Data contract and failure states

The evidence record must bind the exact source artifact, conversion, service version, identity mapping, and response. For each input pair, retain the original names, resolved profile identifiers or an explicit unresolved/ambiguous state, profile-presence flags, the complete shared identifier set, the set-operation denominator, and the bounded examples shown to a reader. Missing inputs fail closed; they are not replaced by guessed aliases, hand-authored profiles, random features, or fabricated compound names. Ambiguous mappings remain visible and are excluded from canonical claims unless separately reviewed under an approved identity policy.

The UI and narration must say “documented compounds present in these mapped profiles,” not “chemically similar,” “same flavor,” “safe,” or “equivalent.” A displayed Jaccard or overlap value describes the selected sets only. It is not a calibrated probability, ranking confidence, or threshold for suitability. The service must not imply a technique was evaluated when the explicit record says `technique_evaluated=false`.

## 10.4 Coverage, missingness, and interpretation

Coverage is sparse and identity-dependent. The recorded test split has profiles for both endpoints in approximately one substitution out of nine; long-tail and out-of-vocabulary cases therefore cannot be hidden behind the covered subset. Report the full denominator, both-profiled/partial/unprofiled counts, identity ambiguities, and no-profile outcomes. If explanation is shown only when both profiles exist, that selection itself changes which cases receive an explanation; any usefulness study must retain coverage and missingness in the analysis.

Database support is not independent of ingredient popularity or source curation. High-frequency identities are more likely to resolve, while the unresolved and unsupported cases are likely to be precisely those where a user could most need guidance. The existing accounting identifies this availability problem; it does not establish a correction for it. Do not silently fuzzy-match unknown ingredients or impute a presence record.

## 10.5 Separate factuality from usefulness

The recorded factuality checks address whether the bounded response matches the pinned source rows and whether failure states remain explicit. They do not ask whether people understand the explanation, whether it helps them choose, whether it causes overconfidence, or whether it improves a recipe. The proposed H4 study compares substitution understanding with and without the bounded presence record, including usefulness, comprehension of the presence-only meaning, confidence, and harmful over-reading. Its target sample is at least 20 adults, subject to institutional review, consent, and the finalized protocol. No such human result is claimed here.

The strongest alternative is that this is merely an infrastructure wrapper around a chemistry database, not a scientific contribution. That interpretation remains plausible until a preregistered, appropriately reviewed study shows a meaningful user outcome and the comparison is evaluated against simpler explanations. A null or harmful-overreading result would be a valid negative finding; it would not invalidate the narrower source-factuality work. Conversely, exact source reproduction alone cannot be promoted as evidence of usefulness.

## 10.6 Limitations and claims boundary

Supported at the recorded ledger boundary: a source-pinned, presence-only representation; a historical exhaustive response-contract audit; and sparse, identity-dependent test-endpoint coverage. Before external publication, reconstruct and hash the original data, converter, runtime, validation reports, and claims-register row, and verify the applicable license and citation against primary records.

Not supported: sensory or functional equivalence; chemistry-based substitute validity; causal flavor claims; nutrition benefit; allergen safety; calibrated confidence; human comprehension or usefulness; product readiness; or a general quality claim. These require different data and evidence. The chapter's organizing rule is therefore simple: report only what the source records, expose when it records nothing, and measure human interpretation separately.

## Provenance and next gates

- Canonical definitions, coverage denominators, rights, and H4 protocol: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§1, 2, 14, 15, and 21.
- Historical compound-contract and blast-radius record: active strategy Appendix X, LEAD v4/v5 entries (source artifacts still require reconstruction for publication-grade citation).
- Split-specific endpoint coverage: the strategy's dated LEAD v370 entry and shared backlog evidence; report SHA-256 `f086fa3e195b85924475a1484536adf281719dd47886e60ea113611aaceb9a4d`.
- The project's research sequence classifies Chapter 10 as factuality recorded, usefulness open. Do not upgrade that status from this synthesis draft.
