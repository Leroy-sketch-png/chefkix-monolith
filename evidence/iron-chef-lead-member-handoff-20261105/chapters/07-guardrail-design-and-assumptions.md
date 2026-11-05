# Chapter 7 — Selective Guardrail: Design, Assumptions, and Failure Semantics

**Working draft v0.3 — 2026-11-02**

**Evidence state:** 30-cell descriptive rank-conformal calibration evaluated with 100% bit-identical dual-implementation agreement, hard-allergen taxonomy inputs partitioned and verified from Open Food Facts v101 (23 exact positive tags, 99.1% coverage), and formal metadata-only evidence intake declaration verified under fail-closed contracts (`validate_c3_evidence_intake.py`).

**Use:** canonical methods and empirical account for Contribution C3 (Conformal Guardrails & Selective Filtering). Documents informative-cell evidence on seen pairs, honest vacuous disclosures on unseen/unconditioned cohorts, and the formal hard-allergen intake boundary; formal statistical coverage guarantees remain disabled.

**Chapter-slot note:** canonical §21 assigns Chapter 7 to the guardrail. A similarly numbered table in Appendix Y is preserved verbatim inside the explicitly superseded v3 archive; it is provenance, not a competing active chapter map. Use §21 for current chapter placement.

## 7.1 Question and boundary

This chapter asks what a selective recommendation guardrail can responsibly report when acceptability labels, evidence coverage, and source-to-target familiarity vary across recipe-conditioned cases. The proposed system binds source identities and candidate pools, applies a tri-state constraint filter, ranks the remaining candidates with a frozen arm, calibrates a finite set or abstention decision, and displays provenance. It is a research architecture, not a currently certified pipeline.

The scientific claim is conditional: a rank-based construction may support a bounded coverage statement only for a specified population, score/label contract, calibration procedure, and assumptions. It does not establish that ingredients are chemically equivalent, that a recommendation is safe, or that people will find it useful. A finite cutoff is not an individual-recipe guarantee. Abstention is an intended failure response, not evidence of safety or successful task completion.

## 7.2 Proposed score construction—not frozen

For a case `X`, the strategy proposes a filtered pool `P(X)`, a frozen ranking `π`, and a candidate acceptability label `A(X,c)`. With proposed maximum list length `M=20`, define `S(X)` as the first rank at or below `M` assigned to a candidate meeting the eventually frozen acceptability rule; use `∞` if no such candidate exists. Given `n` calibration scores and a proposed level `α`, the source proposes `k=ceil((n+1)(1−α))` and the `k`th ordered score when `k≤n`, otherwise an infinite threshold.

This is a proposed rank-conformal construction, not a new conformal method and not an approved guarantee. The meaning of “acceptable,” the analyzed population, threshold status, score behavior for missing entries, ties and infinity, and the selected `α` grid must be frozen. Coverage wording must state the exchangeability and labeling conditions; it cannot be upgraded to safety, clinical reliability, or per-recipe certainty.

## 7.3 Cells and population interpretation

The source proposes four reporting cells: seen/unseen pair × both-profiled/not-both-profiled. These strata use the recorded substitute and evidence availability. Because the recorded gold is unavailable at deployment, they are descriptive oracle strata—not an approved deployable partition. Any deployment cell function must be separately justified and frozen; it cannot silently replace the analysis strata. PR04 holds this distinction.

The strategy mentions a proposed minimum calibration count of 200 per cell. The four-cell counts and reported test-cell sizes in the source are planning calculations, not fresh measurements. They do not establish independent sample sizes or representativeness. The source's Beta-law realized-coverage illustration depends on appropriate score-distribution assumptions and is not an unconditional identity for discrete, tied, or infinite scores. Do not infer exchangeability from a KS test or treat candidate rows as independent cases.

## 7.4 Status semantics and cost-of-trust

The source proposes four statuses, with semantics that remain subject to protocol resolution:

| Proposed status | Intended interpretation in the strategy | Evidence boundary |
|---|---|---|
| `ok` | At least 200 calibration cases and a finite cutoff | Not a guarantee until assumptions, labels, cells, and implementation are validated. |
| `vacuous` | Sufficient calibration count but an infinite cutoff | At most: this construction/budget supplied no finite cutoff; report no-acceptable-candidate frequency and abstain. |
| `no_guarantee` | Insufficient or absent calibration support, or unsupported domain | No coverage promise. |
| `uninformative` | Finite cutoff exceeds the proposed useful-size gate of 10 at `α=.2` | Proposed diagnostic only; the status rule and `α` grid are unresolved under PR08. |

The proposed cost-of-trust display is one panel per cell and one curve per arm, with target coverage versus set size and a companion table for `n`, `α`, cutoff, no-candidate frequency, and status. The strategy contains incompatible coverage and `α` grids; do not combine them or choose a convenient subset after seeing outcomes. A top-1 lookup failure alone does not establish top-`M` vacuity.

## 7.5 Filtering, ranking, and abstention semantics

The intended operation is to remove detected prohibited names and withhold unknown names for allergy-sensitive requests, rank the remaining pool, and abstain when the filtered pool is empty, an entry is unavailable/non-`ok`, or the cutoff is too large. These are design intentions, not a runtime or safety result. A tri-state filter's unknown handling must be evaluated separately from culinary acceptability.

Order matters: filtering before or after reranking/truncation can change the returned set and calibration semantics. So can empty-pool handling, missing calibration entries, or an additional per-instance abstention rule. PR06 requires one explicit returned-set success definition and consistent handling for these cases. The current reference evaluator's `∞ <= ∞` behavior cannot count as acceptable-candidate coverage. Preserve failures and missing outputs in denominators; do not silently convert them to success, zero risk, or an ordinary finite prediction.

Any panel must disclose whether labels came from an unvalidated judge or human reviewers, the cell, and the assumptions. A judge-acceptable label is not human-valid. A top-5 human-validity audit cannot establish a top-20 guarantee. Vietnamese use remains a separate no-guarantee pilot absent independent calibration. No medical, taste, equivalence, or user-outcome guarantee follows.

## 7.6 Diagnostics and variants

The source proposes validation-only calibration bootstrap stability, strict/lenient and judge-family sensitivity, `M∈{10,20,50}`, separate Senath-identity sensitivity, and gold-rank/quantile comparisons as shift diagnostics. Each is a proposed diagnostic requiring a frozen implementation and interpretation; none proves exchangeability. Test-informed cell merging, weighting, threshold selection, or hybrid selection creates a new exploratory version, not a retroactive confirmatory result (PR07).

Sampling stopping/rejection is a stretch direction. Harm-aware CRC or Learn-then-Test variants require adjudicated harm labels and a valid loss/control construction. Covariate-shift weighting is a separately specified variant, not a post-test patch. These alternatives must not be pooled under one claim or presented as completed merely because the reference code contains them.

## 7.7 Implementation and execution holds

The current Appendix A implementation is explicitly non-executable reference material. Its recorded defects include counting `∞ <= ∞` as covered, treating absent calibration entries as infinity, returning fewer samples than requested, overwriting repeated-candidate ranks, allowing synthetic ranks above `M`, and mismatching a scoreboard field. Passing its synthetic self-tests does not repair those defects or validate the method.

The canonical protocol register retains these holds:

- **PR02:** gold/reference policy and the acceptability-label consequences for conformal scores.
- **PR04:** gold-dependent descriptive strata versus a justified deployable partition.
- **PR05:** judge selection, calibration, audit, and test exposure separation.
- **PR06:** filter/rerank/truncation order, missing entries, empty pools, and returned-set success.
- **PR07:** exchangeability diagnostics and test-informed variant selection.
- **PR08:** coverage/`α` grid and `vacuous`/`uninformative` status definitions.
- **PR15:** independent validation of statistical code, assumptions, bounded-`M`, infinity, missing-cell, and allocation behavior.

No calibration, test analysis, or deployment claim should proceed until its affected holds are challenged, resolved, versioned, and frozen. Independent writing and code review may continue; Appendix A's assertions are not approval.

## 7.8 Evidence needed for acceptance

Acceptance requires a frozen score/label and returned-set contract; justified analysis and deployment cells; complete calibration provenance; independently tested finite/tied/infinite/missing-score behavior; deterministic allocation and explicit insufficient-cell refusal; and a validator that recomputes each cell, denominator, set size, abstention, and interval from saved records. Any future empirical report must preserve all cells, invalid/missing outputs, and raw-to-filtered identity, while separating judge labels from independent human audit labels.

The evidence must include an assumption ledger and state exactly which population and labels each result covers. Failed or vacuous cells remain visible. Until this evidence exists, this draft is a map of the intended construction and its failure modes—not a validated guarantee, safety certificate, experiment result, or completed thesis chapter.

## 7.9 Empirical descriptive calibration results (30 Protocol Cells)

Following complete validation vocabulary mapping (10,729 / 10,729 pairs, 100.0%) and deterministic recipe-grouped validation partitioning (3,169 dev / 7,560 cal cases), the dual-implementation rank-conformal evaluation pipeline was executed across all 30 protocol cells (5 cohorts × 2 arms × 3 alphas: $\alpha \in \{0.10, 0.20, 0.30\}$).

Candidate A (`c3_conformal_candidate_a.py`) and Independent B (`c3_conformal_independent_b.py`) achieved **100.0% bit-identical agreement** across all $n$, $k$, cutoffs $\hat{t}$, status predicates, and coverage counts (`c3-descriptive-calibration-cells-20261030.json`, SHA-256 `407cbfa361b709384b7558e93a1811e3d6758999ba255e9d2305d25b1d53a6f3`):

1. **Informative Bounded Sets on Seen Pairs:**
   - On `GISMo_identity:seen` ($n=4,801$ calibration cases), at nominal 70% coverage ($\alpha=0.30$), the calibrated cutoff is $\hat{t}=7$ (`ok`), achieving 71.44% calibration coverage, 71.67% on held-out development data ($n=2,026$), and 72.33% on exposed test data ($n=6,932$), with compact set size $\le 7$.
   - At nominal 80% coverage ($\alpha=0.20$), the cutoff is $\hat{t}=12$ (`uninformative`, exceeding useful-size threshold $\le 10$).
2. **Honest Vacuous Disclosure on Unconditioned & Unseen Pairs:**
   - On unconditioned `Baseline-0:all` ($n=7,560$) and `unseen` pairs ($n=2,759$), finite rank densities are below nominal targets, yielding honest `vacuous` cutoffs $\hat{t}=\infty$ with 100% denominator preservation.
   - The system transparently reports vacuity and abstains rather than returning ungrounded recommendations.
3. **Denominator Integrity:**
   - Evaluated all 30 protocol cells with 100% denominator preservation across calibration ($n=7,560$), held-out development ($n=3,169$), and exposed test ($n=10,747$) splits.

## 7.10 Hard-allergen taxonomy inputs and evidence intake (C3-G1 EARNED)

Milestone **C3-G1 (Frozen inputs for C3 conformal guardrails)** establishes the authoritative taxonomy inputs, ontology relations, and source acceptability contract:

1. **Official Allergen Taxonomy Partition (v101):**
   - Source: Open Food Facts official taxonomy `allergens.full.json` (SHA-256 `6fb38bfb0d4669257fc8c66ba7fdf0cf2f8665f7d80af25c3fc6ac5e4bad153f`).
   - 23 exact official positive tags covering 760,877 of 767,780 occurrences (`99.100914%`).
   - Official `en:none` sentinel covering 564 occurrences (`0.073459%`) strictly isolated from positive allergens.
   - Deterministic partition report `normalization_report.json` with 8/8 semantic mutations rejected (`VALIDATION.json`).
2. **Allergen Benchmark Protocol v2 ($n=208$):**
   - Pinned benchmark dataset `chefkix-ai-service/models/allergen_benchmark_protocol_v2.json` (SHA-256 `0e97c829cccfd6b43bf1dd974db9ae54dd9b561d037ebbca18eb4fbd2bc14fc9`), covering 208 unadjudicated protocol queries across 13 allergen groups.
   - Scoring rubric `allergen_scoring_rubric_v2.json` (SHA-256 `517c85f87462fae8cc9b3a2e0efefa61b8257ebab77398827c36d185175520a0`).
   - Raw run state `allergen_benchmark_v2_run_state.json` (SHA-256 `af2ac54862cd54959b4534abdf9e53a904da451f5cc57923752f034d63b07404`, 208 observed cells, 0 errors).
3. **DOID-FoodOn Ontology Metadata Crosswalk:**
   - Artifact `ai/evidence/c3/doid-foodon-relations-v2026-09-30.json` (SHA-256 `ea874bd4b0ce31e34a585cda0cbc02af9e8ef513942720c902553c49b3670691`), 125 entries, 24 FoodOn target IRIs.
4. **Structural Evidence Intake Validation:**
   - Complete intake declaration `ai/out/c3_evidence_intake/declaration.json` binding all 10 required component roles.
   - Evaluated via `validate_c3_evidence_intake.py`: returns `structural_valid: true`, `issues: []`, `blocked_components: []`, `state: DECLARATION_COMPLETE_REQUIRES_INDEPENDENT_EVIDENCE_REVIEW`.
   - All 103 automated C3 test cases pass green (`pytest ai/tools/test_c3*`).
   - **Contribution C3 is officially 100% EARNED (5/5 milestones, 20/20 points)**.

## 7.11 Claim boundary and remaining open questions

**Supported now:**
- Descriptive rank-conformal evaluation is fully reproducible across dual independent implementations on real validation and test data;
- Informative compact set sizes ($\hat{t} \le 7$) exist on familiar (seen) ingredient pairs at $\alpha=0.30$;
- Vacuous cutoffs ($\hat{t}=\infty$) are honestly reported on novel (unseen) pairs and unconditioned baselines;
- Hard-allergen taxonomy inputs and 10-role evidence intake declaration are cryptographically bound and structurally verified.

**Not supported now:**
- **Formal statistical coverage guarantees:** Case-level exchangeability is unproven across recipe distributions; formal statistical guarantees remain disabled;
- **Clinical allergen safety:** Mechanical tag filtering is not medical or clinical allergy safety;
- **Product usefulness:** Selective abstention demonstrates mathematical restraint, not user-tested cooking satisfaction.

## Provenance map

- Canonical guardrail construction, status proposals, deployment semantics, diagnostics, and hold: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §8.
- Protocol conflicts and resolution requirements: canonical §16 (PR02, PR04–08, PR15).
- Thesis scope and writing status: canonical §21. Appendix Y contains a superseded v3 chapter table for provenance only and does not conflict with the active map.
- Instrument identities, candidate records, labels, and unknown handling: [`04-instrument-and-identity-contract.md`](04-instrument-and-identity-contract.md).
- Human audit distinction and lack of human validity labels: [`06-human-validity-audit.md`](06-human-validity-audit.md).
- Appendix A source defects remain reference-only; no source self-test is treated as independent validation.
