# Chapter 4 — Measurement Instrument and Identity Contract

**Working draft v0.4 — 2026-11-04**
**Evidence state:** Frozen Freeze-A validity instrument under [Protocol v1](../protocols/IRON-CHEF-PROTOCOL-v1.md) (SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`), real sampling frame (`c1_freeze_a_real_frame.jsonl`, 10,747 rows), verified frame weights, executable blind audit pack, deterministic sampler, evidence intake, and synthetic adjudication round-trip (35/35 tests passed). No human audit outcome is claimed.
**Use:** canonical methods account for Contribution C1 (Validity Instrument). It defines the frozen mathematical estimand, verified 10,747-case real sampling frame, exact frame weights, identity contract, and blinded adjudication protocol; it does not report confirmatory human audit outcomes, which remain gated on institutional ethics clearance.

## 4.1 Unit of analysis and case record

The instrument treats a recipe-conditioned replacement request as the primary case. Each record carries a stable case identifier derived from source split and original row identity, the source dataset and per-case rights label, recipe title and available context, original ingredient list, target ingredient, and the recorded substitute when the source provides one. It preserves source strings alongside mapped identities so transformations remain auditable.

The same record may also carry train-relative pair novelty, endpoint-frequency category where the definition is frozen, evidence-coverage state, cuisine/source context, and declared constraint metadata. These fields are not interchangeable features. In particular, a gold substitute can support retrospective benchmark strata but is not available to a live request. Gold-dependent `seen/unseen` and profile-presence cells are descriptive analysis dimensions until a justified deployable partition is separately defined.

For missing or rights-uncertain values, the instrument preserves `UNKNOWN` or an explicit null with a reason. It must not infer a license, cuisine label, allergen group, missing recipe step, or semantic identity from a filename or model response. The full contract and required fields remain in the active strategy §5; this draft does not instantiate every planned artifact.

## 4.2 Identity is part of the estimand

The primary identity contract follows the pinned GISMo vocabulary and its candidate-ID mapping. A separate Senath-normalized string identity is retained only as sensitivity analysis. Each mapping must preserve raw spelling, normalized spelling, source catalog ID, mapping version, and ambiguity/unmatched state. Primary and sensitivity results are reported separately; no denominator combines them.

The source-only GISMo pool audit found that candidate IDs follow the released helper's one-based catalog-row index rather than the CSV `node_id`. Treating these as one namespace caused a real validation failure and was corrected before the accepted replay. Therefore the instrument requires the pinned catalog and explicit ID-to-name checks at package and ingestion boundaries; equal-looking integer IDs alone do not establish identity.

Normalization is not a cosmetic cleanup. Lowercasing, punctuation/quantity removal, singularization, aliases, and food-form collapse can merge distinct foods or erase distinctions relevant to cooking. Protocol v1 freezes exact matching against the pinned GISMo vocabulary as primary. Preserve raw strings and unmatched/ambiguous states; Senath normalization and reviewed aliases are separately named sensitivity analyses, not replacements for primary identity. Fuzzy matching cannot silently promote a result into the primary identity.

## 4.3 Candidate and output record

Each method output is bound to one case and records requested/returned method identity, source candidate pool and hash, ordered candidate IDs and original names, raw generated strings, ranks/scores where permitted, parsing state, and failure reason. A missing output is not deletion; every scheduled case requires either a valid method result or an explicit method-failure record. Input labels and target-derived partitions remain local to evaluation and never enter inference prompts or candidate scoring.

For the frozen human-validity audit, the candidate slate contains each eligible arm's top five candidates plus the recorded gold where defined, deduplicated under the frozen identity rule while retaining raw forms and every originating arm/rank association. The slate is deterministically shuffled per case. The private mapping from candidate to arm, rank, and gold is withheld from reviewers. The primary `k=5` is frozen; a post hoc move to top-4 is not allowed.

## 4.4 Human labels and unknowns

The frozen culinary rubric is `A/C/N/U`: acceptable as-is, acceptable with a bounded quantity/technique/companion-ingredient change, not acceptable under the case, or unable to judge (`unknown`, not a negative). The reviewer judges each candidate independently; the instrument does not ask the reviewer to select a single winner or rank candidates. If cooking steps are required to judge a candidate, that requirement is recorded rather than silently treating title/ingredients as sufficient. For the primary analysis, `A` is strict validity and `A+C` is a separately reported sensitivity.

The recorded gold is blinded and judged like any other candidate. Raw A/C/N/U judgments remain immutable and adjudication is stored in a separate versioned field. If both raters label the gold `N`, report “reference disputed” separately without altering benchmark Hit@k. Allergen and trace-policy judgments are separate fields from culinary validity. They retain `yes/no/unknown` and, where defined, `not_applicable`. A candidate can be culinarily acceptable while its constraint evidence is unknown; a negative flag is not a safety certificate. Reviewer judgments, adjudicated values, rationale, handbook version, and disagreement resolution remain distinct records.

## 4.5 Reproducibility and leakage controls

The instrument manifest must bind the parent specification, case-frame and pool hashes, builder and identity-map revisions, random seed, normalization version, arm/task parameters, reviewer pack, sealed key, and outputs. The same inputs and seed must reproduce case IDs, strata counts, candidate membership, and blinded ordering byte-for-byte. A fresh-process validator checks split/row/recipe joins, identity mappings, complete case coverage, unique IDs, and absence of target/gold/strata fields in source-only inference payloads.

Label frames, target identities, gold values, audit strata, private arm/rank keys, and adjudication keys remain separate from model inputs. Test data may not be used to choose a normalization rule, judge, threshold, candidate policy, or cell merge. If prior exposure occurred, it is disclosed and the resulting analysis is exploratory or otherwise bounded according to the frozen decision; it is not erased by a new manifest.

Structural checks establish that a package matches its declared schema and hashes. They do not establish that a label is correct, an identity is semantically valid, a sample is representative, or a proposed protocol is ethically approved.

## 4.6 Resolution of Freeze-A protocol decisions

Under `research/protocols/IRON-CHEF-PROTOCOL-v1.md` §2, prospectively adopted on 2026-10-21 (SHA-256 `B295259909C72A5C3473A117B4711AF829B84949A60004CE21C963721B6D5C12`), all seven protocol decisions were formally resolved and frozen:

1. **PR01 (Primary Estimand):** Defined as conditional rescue P(V=1 | H=0) for the unseen GISMo-identity stratum, primary GISMo arm, strict valid@5. Joint rescue P(V=1, H=0) is preserved as a secondary estimand. Support H1 only when the stratified-design 95% lower bound is >= 0.15; refute only when its upper bound is < 0.15; otherwise report inconclusive.
2. **PR02 (Reference and Gold Policy):** Gold candidates are labeled blind in the randomized candidate slate exactly like model candidates. Raw A/C/N/U judgments are preserved immutably; adjudication is recorded in a separate versioned field. If both raters label gold N, the case is reported as “reference disputed” separately without altering benchmark Hit@k.
3. **PR03 (Allocation and Weighting):** Slate size is fixed at k=5. Stratified sampling allocation is fixed at 50/50/25/25 across four strata: unseen-profiled (n=50), unseen-unprofiled (n=50), seen-profiled (n=25), and seen-unprofiled (n=25) for a total sample of n=150 cases. Stratified ratio estimation uses verified frame weights W_h = N_h / n_h and case-cluster bootstrap with 2,000 resamples and seed `20261021`.
4. **PR04 (Strata Definitions):** Descriptive oracle strata (seen/unseen by both-profiled/not-profiled) depend on gold identity and are retained as descriptive dimensions. Deployable calibration partitions use request-time features only (Partition-1: target in train × FooDB profile present).
5. **PR05 (Partitions and Leakage Control):** Validation recipes are deterministically partitioned by recipe identity into disjoint development (30%) and calibration (70%) sets before inspecting outcomes (seed `20261021`). Test data remains strictly exposed, evaluation-only, and uninspected for method tuning.
6. **PR09 (Primary Identity):** Pinned GISMo vocabulary exact matching is the primary identity contract; raw candidate strings are preserved in parallel. Senath normalization and alias expansion are retained as named sensitivity analyses only.
7. **PR10 (Rater Agreement Thresholds):** Proceed at Tier-1 Cohen's kappa >= 0.5. Claim “reliable agreement” only if the strict-kappa case-cluster-bootstrap 95% lower bound >= 0.6; otherwise characterize the audit as exploratory. Use two-sided Wilson intervals for proportions and a one-sided exact bound for zero events; with fewer than 20 valid bootstrap draws, report the interval unavailable.

## 4.7 Executable verification evidence

The Freeze-A measurement instrument has been implemented and verified in the automated test suite:
- **Real Sampling Frame Builder (`build_c1_real_sampling_frame.py`):** Materializes the canonical 10,747-case Freeze-A sampling frame from pinned test split data, GISMo pair identity, and FooDB compound profile presence. Derives exact verified frame weights ($W_h = 3.52, 72.78, 39.44, 237.84$) across four non-undersupplied strata ($N_h = 176, 3639, 986, 5946$), bound to frozen plan (`freeze_plan.json`, SHA-256 `66453dc3...`) and formal permissions declaration (`declaration.json`, SHA-256 `79aa6069...`). Both-endpoint FooDB profile coverage ($N_{\text{seen,profiled}} + N_{\text{unseen,profiled}} = 986 + 176 = 1,162 / 10,747$ = 10.812%) is formally verified in Claims Register v15 (Claim R14, SHA-256 `92a5b5a1...`).
- **Bit-Identical Dual Build:** Independent clean rebuild from scratch confirmed 100.0% identical SHA-256 digests across all 5 generated frame artifacts.
- **Deterministic Sampler (`c1_freeze_a_sampler.py`):** Implements frame parsing, freeze planning, and deterministic drawing under seed `20261021`, verifying exact 50/50/25/25 sample counts, frame weight calculation, and manifest hash binding.
- **Blinded Audit Pack Constructor (`c1_blind_audit_pack.py`):** Bundles randomized candidate slates ($k=5$), preserves raw strings, and seals private arm/rank/gold mappings.
- **End-to-End Pipeline Integration (`test_c1_freeze_a_pipeline_integration.py`):** Verifies the full cycle from frame freeze, sample draw, blind pack creation, synthetic two-rater judgment collection, to independent adjudication with 100% data fidelity.
- **Live Frame Regression Suite (`test_c1_real_frame_live.py`):** Validates 10,747 frame rows, zero target/gold leakage, exact strata counts, freeze plan bindings, and fail-closed human boundaries.
- **Suite Status:** All 35 focused C1 unit, frame, and pipeline tests pass cleanly (pytest -q ai/tools/test_c1_*.py: **35 passed in 4.99s**).

## 4.8 Claim boundary

The instrument's contribution is a reproducible way to carry provenance, identity, candidate membership, failures, and multiple labels through an evaluation. It does not itself establish culinary validity, benchmark bias, generalization, safety, calibrated reliability, or novelty. Those claims require their separate outcome evidence and acceptance gates. An instrument that records `UNKNOWN` correctly is a truthfulness capability, not proof that the underlying system is useful.

## Provenance map

- Canonical case schema, identity contracts, candidate/arm fields, and normalization warning: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§5–6, 9, and 16.
- Candidate-ID namespace correction and full-split structural replay: shared LEAD v379–v380 and v403–v405; source-only artifacts remain local-only.
- Frozen prospective rules and authority: [Protocol v1](../protocols/IRON-CHEF-PROTOCOL-v1.md). The pre-v1 rationale in strategy §16 is historical context, not an alternate operating procedure; appendix reference implementations or self-tests are not approved procedures.
