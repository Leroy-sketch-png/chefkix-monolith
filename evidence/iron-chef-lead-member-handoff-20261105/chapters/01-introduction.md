# Chapter 1 — Introduction: Measuring Ingredient-Substitution Systems Without Overclaiming

**Working draft v0.2 — 2026-11-02**

**Evidence state:** updated to reflect the accepted empirical baselines of Contributions C2 (Matched Measurement — 100%), C3 (Conformal Guardrails — 100%), C5 (Product Proof — 100%), C1 (Real Sampling Frame & Instrument — 2/5 earned), and C4 (LLM Judge Contract & Runner — 2/5 earned; 19/25 milestones verified, 76.0% campaign progress).

**Use:** canonical introduction setting the research proposition, falsifiable hypotheses, fail-closed boundaries, and contribution structure across Contributions C1–C5.

## 1.1 Problem and motivation

Ingredient substitution is a consequential recommendation task: a system proposes a replacement for an ingredient, often in a recipe or dietary context. Yet the word *substitution* covers several different outputs—ranking a candidate, generating one answer, changing a full recipe, or rewriting its instructions. Their inputs, targets, and evaluation units differ. A result from one task cannot automatically stand in for another. The related-work review therefore treats task definition and comparison protocol as part of the research problem, not as implementation details (Chapter 2; canonical strategy §4).

This thesis begins from a narrower measurement concern. A benchmark commonly records one target answer and rewards recovery of that target. Such a score answers whether the recorded target was retrieved under a specified identity and candidate contract. It does not, by itself, establish that every other suggestion is invalid, that a fluent alternative is acceptable, or that a recommendation is safe or useful to a cook. These are separate empirical questions. The project treats the single-reference concern as a falsifiable hypothesis, not as a demonstrated defect in every benchmark or label (canonical strategy §§2–3; Chapters 4–7).

Ingredient identity is another part of the measurement contract. Different normalization maps can change whether a prediction is counted as a match, which cases are classified as seen or unseen, and the denominator represented by a reported cell. A credible result must state its identity system, source lineage, split, candidate pool, missing-output treatment, and population. Sensitivity analyses can illuminate those choices but must not replace the predeclared primary identity after outcomes are observed (Chapters 3–5; canonical strategy §§5, 11, 15–16).

## 1.2 Research question and thesis proposition

The governing question is:

> When do ingredient-substitution systems fail, when can a single recorded answer misrepresent validity, and can a system expose uncertainty without overstating its evidence?

The thesis proposition is that aggregate single-answer scores may under-credit valid alternatives and conceal performance differences across pair novelty, evidence coverage, and cuisine. IRON CHEF therefore develops a measurement instrument and tests an allergen-aware, set-valued abstention guardrail over frozen scorers. The proposition is deliberately conditional: validity, shifts, informative coverage, human understanding, and safety each require their own protocol and evidence. No result for one construct is used as a proxy for another (canonical strategy §§2–3).

The associated hypotheses remain at distinct maturity levels:

| Hypothesis | Question | Current verified boundary |
|---|---|---|
| H1 | Are some single-gold misses culinary-valid, especially for unseen pairs? | Frozen Freeze-A 10,747-case sampling frame, verified weights ($W_h$), and deterministic 150-case sample verified (C1-G1, C1-G2 earned); real human ratings remain strictly gated on institutional ethics clearance. |
| H2 | Do aggregate scores hide drops across pair novelty or evidence coverage? | Matched 200-case clean-room comparison across 5 cohorts independently verified (Chapter 5 v0.4; C2 100% earned); generation and reranking trade places across $k$ without an unsupported winner. |
| H3 | Can rank-conformal prediction provide informative sets on substitution pairs? | Dual-implementation 30-cell descriptive calibration verified (Chapter 7 v0.3; C3 100% earned); informative bounded sets on seen pairs ($\hat{t}=7$ at $\alpha=0.30$) and honest vacuous disclosures on unconditioned/unseen cohorts ($\hat{t}=\infty$). |
| H4 | Does bounded compound evidence provide interpretable context without safety overclaiming? | Verified FooDB presence runtime (971 profiles, 206 dimensions, 125,992 links) and full-split endpoint coverage reproduced (1,162/10,747 test cases both-profiled); product-proof knowledge graph served with 0 identity errors (Chapter 11 v0.3; C5 100% earned). |
| H5 | Can complementary ranker and language-model arms improve set size at equal coverage? | Exploratory only after matched arms exist; no test-selected hybrid is confirmatory; human ratings remain gated on institutional ethics clearance. |

These hypotheses are not interchangeable success criteria. A negative, inconclusive, or uninformative result is reportable and may constrain the thesis; it is not permission to change the protocol after observing outcomes. The active resolution and freeze requirements are collected in canonical §16.

## 1.3 Research design and evidence boundary

The work separates source reconstruction, benchmark measurement, human validity, selective prediction, allergen evaluation, and product proof. The instrument specifies identity, strata, candidate and failure records, scoring, and reporting (Chapter 4). The Senath clean-room study documents one bounded validation comparison and its limitations (Chapter 5). Chapters 6 and 7 describe proposed human-audit and guardrail work while preserving protocol holds. Chapters 8–10 cover allergen, Vietnamese, and compound-presence questions without conflating raw outputs, factuality, safety, or usefulness. The evidence ledger remains the authority for the state and provenance of each claim (canonical §§1, 15–16).

The present Senath comparison is a frozen 200-case **validation diagnostic** over a validation-selected adapter, not independent test evidence. The records and metrics are documented in Chapter 5. Its fixed denominator and identity-separated reporting make the execution inspectable; they do not establish generalization, human validity, model superiority, safety, or product value. Longer training, a completed remote run, or an evaluation loss does not change that boundary without the corresponding validated artifact and outcome evidence.

The thesis also preserves bounded negative and unavailable work as such. Results from the selective-reliability, graph-fusion, image, behavior, or flavor tracks do not combine into a universal claim about food intelligence. A withdrawn capability is not a negative model result; a negative result on one frozen task does not establish that all alternatives fail. Chapter 12 maintains the task-specific boundaries, and Chapter 13 synthesizes only what the evidence supports.

## 1.4 Contributions and intended value

The intended contribution set is organized around five linked but separately gated elements:

1. A validity, shift, and allergen measurement instrument with explicit identity, splits, missingness, strata, rights, and uncertainty.
2. A matched measurement campaign across graph, lookup, and language-model systems, with incompatible tasks kept out of shared rankings.
3. An allergen filter and calibrated-set/abstention study that reports every cell and makes no safety guarantee absent resolved assumptions and validation.
4. Human evidence for audit labels, judge calibration, usefulness, and comprehension only after institutional, consent, and reviewer gates are met.
5. Product proof through a provenance-preserving Explorer, additive evidence endpoint, refusal paths, and a recorded demonstration evaluated at the human-experience boundary.

These are the active strategy's C1–C5 ambitions, not a list of completed results. Their acceptance conditions and floor-to-target-to-stretch framing are specified in canonical §3. In particular, the project pursues the ideal scope and adjusts resources and schedule proactively; unknown logistics remain dependencies rather than reasons to silently reduce the destination. Resource commitment does not substitute for rights, credentials, participants, ethics clearance, or scientific validation.

## 1.5 Scope, non-claims, and decision discipline

This thesis does not claim that IRON CHEF is safer, more useful, more accurate, more novel, or commercially superior merely because a pipeline exists or an experiment ran. It does not treat published values as locally reproduced unless the artifact trail supports that status. It does not call a validation result an independent test, treat missing outputs as successes, infer safety from community labels, or infer human usefulness from chemistry arithmetic. Every comparative statement must identify the task, population, denominator, identity contract, evidence grade, and strongest limitation.

Protocol contradictions are not hidden by choosing whichever definition yields the preferred result. Affected work remains blocked until the resolution is explicit, versioned, and frozen. Human authority can set direction and authorize scope; it cannot convert an unmeasured hypothesis into an observed result. The claims register is a migration specification until it is actually instantiated and linked to primary artifacts (canonical §§0, 15–16).

## 1.6 Thesis organization

The active chapter map is canonical §21. In brief, Chapters 2–5 establish prior work, source and graph provenance, the instrument, and the bounded Senath study; Chapters 6–10 address human validity, guardrail design, allergen and Vietnamese protocols, and compound-presence evidence; Chapters 11–12 distinguish system demonstration from bounded negatives; Chapters 13–14 synthesize threats and conclusions. This outline follows §21, not the historical v3 table retained inside superseded Appendix Y.

## Provenance and drafting map

- Current thesis proposition, hypotheses, contribution ambitions, and resource decision: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§2–3.
- Active writing order and scope: canonical §21; archive status: canonical §0 and Appendix Y.
- Task distinctions and attributed prior work: [`02-related-work.md`](02-related-work.md) and canonical §4. External claims are not independently verified by this draft.
- Source and identity contract: [`03-data-and-graph-provenance.md`](03-data-and-graph-provenance.md) and [`04-instrument-and-identity-contract.md`](04-instrument-and-identity-contract.md).
- Training and validation-only inference boundary: [`05-senath-clean-room-inference.md`](05-senath-clean-room-inference.md).
- Human validity, guardrail, and discussion boundaries: [`06-human-validity-audit.md`](06-human-validity-audit.md), [`07-guardrail-design-and-assumptions.md`](07-guardrail-design-and-assumptions.md), and [`13-discussion-and-threats.md`](13-discussion-and-threats.md).

This introduction is a working synthesis. It does not independently reconstruct cited source claims, resolve protocol holds, instantiate the claims register, or establish chapter acceptance.
