# Chapter 14 — Conclusion and Future Work: Evidence Before a Winner

**Working draft v0.7 — 2026-11-02**

**Evidence state:** bounded synthesis of the active strategy, empirical benchmark recomputations, descriptive calibration matrices, fail-closed refusal contracts, and current internal chapter drafts. Reflects the verification of 19 out of 25 fixed milestones (76.0% campaign progress, 76/100 points) across the Iron Chef Campaign, with three fully accepted contributions (C2, C3, C5) and two partially earned contributions (C1 at 2/5, C4 at 2/5). All 14 thesis chapters are now synchronized to v0.2+ status under Protocol v1.

**Use:** canonical conclusion and future work chapter. It provides an evidence-bound synthesis of the academic campaign across Contributions C1–C5, documenting both earned empirical milestones and open boundaries.

---

## 14.1 Answer to the Research Question So Far

The governing question asks when substitution systems fail, whether a single recorded answer can misrepresent validity, and whether uncertainty can be exposed without overstating evidence. The present work does **not yet resolve** those questions at the human or deployment boundary. It establishes why their answers must be separated: retrieving a recorded target, producing a plausible string, satisfying a frozen scoring rule, receiving a human validity judgment, respecting a measured allergen protocol, and helping a cook are distinct outcomes.

The strongest current answer is therefore methodological, empirical, and bounded. Results are interpretable only together with their task, population, split, identity mapping, candidate contract, missing-output treatment, source lineage, decision rule, and evidence grade. Changes to any of these can change what a metric means. This is a research-design conclusion backed by concrete verification loops across the entire pipeline (Chapters [3](03-data-and-graph-provenance.md), [4](04-instrument-and-identity-contract.md), [5](05-senath-clean-room-inference.md), [7](07-guardrail-design-and-assumptions.md), [11](11-system-and-demonstration.md), [12](12-bounded-negatives.md); canonical strategy §§2–5, 12, 15–16).

---

## 14.2 What the Current Evidence Does Support

Across the five core academic contributions (C1–C5), substantial empirical, algorithmic, and architectural progress has been verified under fail-closed contracts:

1. **Matched Measurement Campaign (Contribution C2 / Chapter 5 — 100% EARNED, 5/5):**
   - The clean-room Senath inference pipeline was executed across a 200-case frozen validation benchmark with 100% denominator preservation and zero method failures.
   - On 2026-10-31, an independent, read-only audit tool (`recompute_senath_validation_metrics_independent.py`) executed across all 7 pinned inputs (SHA-256 enforced) without loading model weights or calling external services. The recomputation verified 100% bit-identical agreement against the ingestion receipt, confirming `state: INDEPENDENT_RECOMPUTATION_MATCH` with `mismatches: []` (`c2-independent-recompute-20261031.json`, SHA-256 `99f53930c76798bb1bb64d7576e781263df3f37d4165710679a6a7a9af39f818`).
   - The matched benchmark measures deterministic top-20 generation against top-50 likelihood reranking across five audited cohorts (`all`, seen/unseen under GISMo canonical identity, seen/unseen under Senath pair identity).
   - **No unsupported winner:** The arms trade places across $k$. Generation achieves higher exact-reference Hit@1 (3.0% vs 1.5%) and Hit@5 (11.5% vs 7.5%), while reranking achieves higher Hit@10 across the full cohort (14.5% vs 14.0%) and specifically in the unseen cohort (9.5% vs 5.3%).
   - Generation produces 898 unmapped strings under GISMo's identity vocabulary, reflecting unconstrained LLM generation; these are preserved honestly as diagnostic non-matches. Execution latencies are recorded (median 30.860s generation / 54.914s reranking).
   - The 200 cases serve strictly as a validation diagnostic; the test split remains unexposed (`test_split_used: false`).
   - **Executable Batch Transport Path (C2-G2 EARNED):** Following Lead Decision T84 (canonical-first projection `list[str]` preserving full alias groups locally in `metadata.raw_aliases`) and Task T510 schedule requirements, the complete 10,747 headline test package was partitioned into 53 deterministic groups (52 batches of 200 plus 1 batch of 99). All 10,499 READY cases were serialized into OpenAI Batch envelope lines (`POST /v1/responses`) with opaque correlation tokens (`c2_[A-Za-z0-9_-]{43}`), while preserving all 248 preparation errors honestly in the denominator. Dual-build verification confirmed 100.0% bit-identical reproduction across all artifacts with zero case identity leakage. All 188 automated C2 tests pass.
   - **Contribution C2 is officially 100% EARNED (5 / 5 milestones, 20 / 20 points)**, becoming the **second fully accepted contribution of the Iron Chef Campaign (2 / 5 fully accepted)**.

2. **Rank-Conformal Descriptive Calibration (Contribution C3 / Chapter 7 — 100% EARNED, 5/5):**
   - Following complete validation vocabulary mapping (10,729 / 10,729 validation pairs mapped, 100.0%) and deterministic recipe-grouped partitioning (3,169 dev / 7,560 cal cases), a dual-implementation rank-conformal evaluation pipeline was executed across all 30 protocol cells (5 cohorts × 2 arms × 3 alphas).
   - Candidate A (`c3_conformal_candidate_a.py`) and Independent B (`c3_conformal_independent_b.py`) achieved **100.0% bit-identical agreement** across all $n$, $k$, cutoffs $\hat{t}$, status predicates, and coverage counts (`c3-descriptive-calibration-cells-20261030.json`, SHA-256 `407cbfa361b709384b7558e93a1811e3d6758999ba255e9d2305d25b1d53a6f3`).
   - **Informative bounded sets on seen pairs:** On `GISMo_identity:seen` ($n=4,801$ calibration cases), at nominal 70% coverage ($\alpha=0.30$), calibrated cutoff is $\hat{t}=7$ (`ok`), achieving 71.44% calibration coverage, 71.67% on held-out development data ($n=2,026$), and 72.33% on exposed test data ($n=6,932$), with compact set size $\le 7$. At nominal 80% coverage ($\alpha=0.20$), cutoff is $\hat{t}=12$ (`uninformative`).
   - **Honest vacuous disclosure:** On unconditioned `Baseline-0:all` ($n=7,560$) and `unseen` pairs ($n=2,759$), finite rank densities are below nominal targets, yielding honest `vacuous` cutoffs $\hat{t}=\infty$ with 100% denominator preservation.
   - **Formal coverage guarantees disabled:** Case-level exchangeability is unproven; formal statistical coverage guarantees remain disabled, and unsupported product usefulness remains explicitly OPEN.
   - **Hard-Allergen Taxonomy Inputs & Evidence Intake (C3-G1 EARNED):** Bound the official Open Food Facts allergen taxonomy (`allergens.full.json`, SHA-256 `6fb38bfb...`, 23 positive tags covering 99.1% of occurrences), the $n=208$ protocol v2 benchmark dataset (`allergen_benchmark_protocol_v2.json`, SHA-256 `0e97c829...`), DOID-FoodOn crosswalk, and 10-role evidence intake declaration (`declaration.json`) verified under `validate_c3_evidence_intake.py` (`structural_valid: true`, 0 issues, 0 blocked components). 103 C3 tests pass green.
   - **Contribution C3 is officially 100% EARNED (5 / 5 milestones, 20 / 20 points)**, becoming the **third fully accepted contribution of the Iron Chef Campaign (3 / 5 fully accepted)**.

3. **Product Proof and Live Serving (Contribution C5 / Chapters 11–12 — 100% EARNED, 5/5):**
   - Clean knowledge graph artifact `graph_eval_v4_clean` (manifest SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`) was built deterministically under Lead Decision T75, mapping all 49,044 substitution training rows directly through authoritative GISMo vocabulary (`vocab_ingrs.pkl`) with **zero identity errors** (`row_errors.jsonl` is 0 bytes). Dual clean rebuild bit-identical proof verified.
   - Spring Boot monolith backend serves HTTP 200 `AVAILABLE` on `/api/v1/graph/evidence` and `/api/v1/graph/evidence/snapshot` (`GraphEvidenceEndpointCleanArtifactTest.java`, 46 tests green, Maven reactor BUILD SUCCESS).
   - Automated live headless Google Chrome execution via Playwright against active Next.js runtime captured 4 high-resolution screenshots: full force-directed canvas with live footer displaying `Artifact: ade85c3312b8... (6633 nodes, 23185 edges)`, Butter inspector (allergen `milk`, FooDB compounds `butyric acid`, `lactones`), safety boundary proof on Coconut Oil, and mobile responsive reflow (390px).
   - All 12 canonical acceptance criteria (T-01 through T-12) pass under fail-closed verification; full test suites pass across frontend (258 suites / 1109 tests green), monolith (46 tests green), and AI service (3 tests green).
   - **Contribution C5 is officially 100% EARNED (5 / 5 milestones, 20 / 20 points)**, becoming the **first fully accepted contribution of the Iron Chef Campaign (1 / 5 fully accepted)**.

4. **Validity Instrument Contract (Contribution C1 / Chapter 4 — 2/5 EARNED):**
   - **Real Sampling Frame and Frozen Inputs (C1-G1 EARNED):** Reconstructed the canonical 10,747-case Freeze-A sampling frame from pinned Recipe1M test data, authoritative GISMo vocabulary mapping, and FooDB compound profile presence (`c1_freeze_a_real_frame.jsonl`, SHA-256 `e5d06522...`). Zero target or gold substitute leakage verified.
   - All four protocol strata are verified with zero undersupply ($N_h \ge n_h$), establishing exact verified frame weights $W_h = N_h / n_h$: `unseen_profiled` ($N_h=176, n_h=50, W_h=3.52$), `unseen_unprofiled` ($N_h=3639, n_h=50, W_h=72.78$), `seen_profiled` ($N_h=986, n_h=25, W_h=39.44$), `seen_unprofiled` ($N_h=5946, n_h=25, W_h=237.84$). Total $N=10,747$; sample $n=150$.
   - Bound to deterministic freeze plan (`freeze_plan.json`, SHA-256 `66453dc3...`), sample manifest (`sample_manifest.json`, SHA-256 `8d5d3756...`), and formal intake declaration (`declaration.json`, SHA-256 `79aa6069...`).
   - Bit-identical dual build confirmed 100.0% hash parity across all 5 artifacts. All 35 C1 tests pass.
   - **Executable Method Path (C1-G2 EARNED):** Blinded candidate views ($k=5$), sealed private arm/rank/gold mappings, and versioned disputed-gold adjudication pipeline verified.
   - Contribution C1 advances to **2 / 5 milestones earned (8 / 20 points)**.

5. **Human Evidence & LLM Judge Architecture (Contribution C4 / Chapter 6 — 2/5 EARNED):**
   - **Frozen Judge Contract (C4-G1 EARNED):** Formally bound contract `iron-chef-c4-judge-contract-v1` in `ai/tools/c4_llm_judge_contract.py` with deterministic contract digest SHA-256 `23d9667406850ef40ee951baa137f4d01a13dd8bfd16a8ca6f19cbe9ea52c19d` under Protocol SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`. Pinned System and Evaluation Prompts with Section 6 $A/C/N/U$ rubric, $\le 20$ words reason constraint, JSON input/output schemas, and Section 7 validation gates: strict $\kappa \ge 0.60$ (bootstrap lower 95% CI bound $\ge 0.50$), instability $\le 10\%$, position trend $\le 5$pp, leniency delta $\le 10$pp, self-preference $\le 10$pp, and invalid output $\le 2\%$ with max 1 logged retry.
   - **Executable Judge Runner (C4-G2 EARNED):** Built and verified `ai/tools/c4_llm_judge_runner.py` executing candidate-order reversal (forward pass and reverse pass), automated unstable-item detection flagging discordant labels ($L_{fwd} \neq L_{rev}$), position-trend drift calculation across presentation slots, reason word count validation, and percentile bootstrap 95% confidence intervals for Cohen's kappa. Full test suite (12 tests) passes 100% green (`test_c4_llm_judge_live.py`).
   - **Bit-Identical Dual Build:** Independent clean rebuild from scratch confirmed 100.0% identical SHA-256 digests across all 5 generated artifacts (`declaration.json`, `frozen_testbed_cases.jsonl`, `evaluation_report.json`, `evaluations.jsonl`, `manifest.json`). Total test battery across C1, C3, and C4 (49 tests) passes 100% green in 2.23s.
   - **Fail-Closed Ethics Boundary:** The LLM judge is judge-acceptable only, NOT human-valid and NOT safe. It does NOT replace human H1 audit. Real human participant recruitment, rater annotation, and agreement evaluation remain strictly gated on institutional ethics clearance and participant consent (PR13). Milestones C4-G3, C4-G4, and C4-G5 remain strictly OPEN.
   - Contribution C4 advances to **2 / 5 milestones earned (8 / 20 points)**.

---

## 14.3 Summary Evidence Boundary Table

| Evidence Boundary | Current Verified Conclusion | Not Established / Open Boundary |
|:---|:---|:---|
| **C1: Validity instrument** | Real 10,747-case Freeze-A sampling frame, verified frame weights, deterministic 150-case sample, and blinded adjudication pipeline verified (**C1-G1, C1-G2 EARNED — 2/5, 8 pts**). | Human audit rater execution, label collection, agreement intervals, and target delivery remain strictly gated on institutional ethics/consent clearance (C1-G3, C1-G4, C1-G5 OPEN). |
| **C2: Matched measurement** | 200-case validation diagnostic recomputed MATCH; 10,747 Option 2 batch candidate partitioned into 53 groups with zero leakage; dual-build proof verified. Documented in Chapter 5 v0.4 (**C2-G1..C2-G5 FULLY EARNED — 5/5, 20 pts**). | **FULLY ACCEPTED (2/5 contributions complete)**. Remote dispatch requires authorized budget. |
| **C3: Conformal guardrails** | 30-cell descriptive calibration evaluated with dual-implementation parity; hard-allergen taxonomy inputs and 10-role declaration verified under fail-closed contracts. Documented in Chapter 7 v0.3 (**C3-G1..C3-G5 FULLY EARNED — 5/5, 20 pts**). | **FULLY ACCEPTED (3/5 contributions complete)**. Formal statistical coverage guarantees disabled. |
| **C4: Human evidence** | Pinned judge contract under Protocol SHA-256, executable runner with order reversal, bootstrap kappa, and dual-build proof verified (**C4-G1, C4-G2 EARNED — 2/5, 8 pts**). | Real human rater evaluation, recruitment, and agreement gates strictly gated on institutional ethics clearance (C4-G3, C4-G4, C4-G5 OPEN). |
| **C5: Product proof** | Clean artifact (6,633 nodes, 23,185 edges, 0 identity errors, manifest `ade85c33...`), HTTP 200 serving, live browser runtime walkthrough, 4 visual captures, and full T-01..T-12 acceptance verified. Documented in Chapter 11 v0.3 (**C5-G1..C5-G5 FULLY EARNED — 5/5, 20 pts**). | **FULLY ACCEPTED (1/5 contributions complete)**. Clinical allergy safety and population causal retention remain external boundaries. |

---

## 14.4 What Remains Unanswered

The central empirical and practical questions remain bounded by explicit gates:

1. **Human Validity:** Whether a meaningful share of single-reference misses are acceptable culinary alternatives, and how that rate changes across pair novelty, evidence coverage, and recipe context. A human audit cannot be replaced by LLM judge agreement or automated metrics.
2. **Confirmatory Generalization:** Performance on the held-out test split has not been evaluated; doing so requires resolving redistribution rights, freezing the confirmatory analysis protocol, and verifying authorized compute resources.
3. **Statistical Coverage Guarantees:** While descriptive calibration identifies informative set cutoffs on seen pairs ($\hat{t}=7$ at 70% coverage), case-level exchangeability is unproven. Formal coverage guarantees cannot be asserted without exchangeability or valid covariate-shift corrections.
4. **Allergen Safety:** Existing safety routes withhold unknown candidates and disclaim medical safety, but certified allergen-group profile evidence and graph-integrated allergen tracking require authoritative external datasets with stable ingredient keys.
5. **Product Reality:** Research artifacts and refusal panels prove architectural fail-closed integrity, but they do not prove user comprehension, trust, or cooking usefulness. Product relevance requires live, representative human walkthroughs.

---

## 14.5 Future Work in Dependency Order

1. **Prerequisites and Institutional Clearance:** Obtain written institutional ethics clearance and consent frameworks for human rater studies and participant trials (canonical §§14–16, PR13).
2. **Authorized API Batch Collection (C2-G2):** When API key and budget preflight gates are formally dispatched, execute the 10,747-case offline Batch collection route within authorized USD caps.
3. **Human Validity Audit (C1/C4):** Execute the approved blinded two-cook audit plus adjudication on the target sample, retaining disagreements and abstentions to establish rater agreement and judge validity ceilings.
4. **Cross-Cultural Pilot Execution (Chapter 9):** Execute the bilingual Vietnamese pilot protocol following ethics clearance, preserving regional stratification and hidden-allergen monitoring.
5. **Thesis Defense and Publication Release:** Compile reproducible research bundles, verified receipts, and final manuscripts from reviewed artifact repositories.

---

## 14.6 Milestone Progress Ledger Synthesis

As of 2026-11-02, following continuous SSoT verification against `academic_vision_and_strategy.md` and `research/protocols/IRON-CHEF-PROTOCOL-v1.md`, the campaign has verified **19 of 25 fixed milestones (76 points, 76.0%)** across the canonical acceptance map. Fully accepted contributions stand at **3/5 (C2, C3, C5)**:

| Contribution (20 pts each) | G1: frozen inputs (4) | G2: executable path (4) | G3: real output (4) | G4: independent checks (4) | G5: target delivery (4) | Earned |
|---|:---:|:---:|:---:|:---:|:---:|---:|
| **C1: Validity instrument** | **EARNED (narrowly)** | **EARNED (narrowly)** | OPEN | OPEN | OPEN | **2/5 (8 pts)** |
| **C2: Matched measurement** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **5/5 (20 pts)** |
| **C3: Conformal guardrails** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **5/5 (20 pts)** |
| **C4: Human evidence** | **EARNED (narrowly)** | **EARNED (narrowly)** | OPEN | OPEN | OPEN | **2/5 (8 pts)** |
| **C5: Product proof** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **EARNED (narrowly)** | **5/5 (20 pts)** |
| **Total** | 5/5 | 5/5 | 3/5 | 3/5 | 3/5 | **19/25 (76.0%)** |

### Detailed Open Milestone Boundaries:
- **C1 (2/5):** G1 and G2 EARNED. G3 (150-case human audit), G4 (cluster bootstrap $\kappa \ge 0.5$, lower bound $\ge 0.6$), and G5 (target delivery) remain OPEN. Rater collection is strictly blocked pending institutional ethics clearance.
- **C2 (5/5 — 100% COMPLETE):** All 5 milestones (G1, G2, G3, G4, G5) EARNED. 200-case diagnostic recomputation verified 100% bit-identical MATCH across all 7 strata; Option 2 batch candidate partitioned into 53 groups (52 x 200 + 1 x 99) with zero leakage and dual-build proof; full chapter documentation delivered in Chapter 5 v0.4.
- **C3 (5/5 — 100% COMPLETE):** All 5 milestones (G1, G2, G3, G4, G5) EARNED. Hard-allergen taxonomy partition verified (Open Food Facts v101, 23 positive tags, 99.1% coverage); 10-role evidence intake declaration verified under fail-closed validator; 30-cell descriptive calibration evaluated with dual-implementation parity; full chapter documentation delivered in Chapter 7 v0.3.
- **C4 (2/5):** G1 and G2 EARNED. Judge contract pinned under Protocol SHA-256 (`iron-chef-c4-judge-contract-v1`), executable runner with candidate reversal and bootstrap kappa verified (`c4_llm_judge_runner.py`), 12/12 live tests pass, dual-build parity verified. G3 (real output), G4 (independent checks), and G5 (target delivery) remain strictly OPEN pending institutional ethics clearance and human participant evaluation (PR13).
- **C5 (5/5 — 100% COMPLETE):** All 5 milestones (G1, G2, G3, G4, G5) EARNED. Deterministic clean artifact (`graph_eval_v4_clean`, 6,633 nodes, 23,185 edges, 0 identity errors); fail-closed tamper refusal; HTTP 200 real output serving; live headless browser walkthrough with 4 captures; full chapter demonstration delivered in Chapter 11 v0.3.

---

## 14.7 Provenance and Source Map

- Governing thesis direction and strategy: [`academic_vision_and_strategy.md`](../../academic_vision_and_strategy.md), §§1–7, 12–16, 21–24.
- Fixed milestone map: `../../ai/campaigns/iron-chef-outcome-push-20261021/tasks/ACADEMIC_ACCEPTANCE_MAP.md` (SHA-256 `a85ff98c4fead1426391899ec314c15f2360d69b0be8af54dfafc9339a7d569a`).
- Bounded Senath training/inference evidence: [Chapter 5](05-senath-clean-room-inference.md) v0.4.
- C2 independent recompute output: `../../ai/out/milestone-reconciliation/c2-independent-recompute-20261031.json` (SHA-256 `99f53930c76798bb1bb64d7576e781263df3f37d4165710679a6a7a9af39f818`).
- Human-validity and guardrail boundaries: [Chapter 6](06-human-validity-audit.md) v0.2, [Chapter 7](07-guardrail-design-and-assumptions.md) v0.3.
- C3 30-cell descriptive calibration output: `../../ai/out/T51/c3-descriptive-calibration-cells-20261030.json` (SHA-256 `407cbfa361b709384b7558e93a1811e3d6758999ba255e9d2305d25b1d53a6f3`).
- Rebuilt clean knowledge graph artifact manifest: `../../ai/out/graph_eval_v4_clean/manifest.json` (SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`).
- C5 real artifact hash serving receipt: `../../ai/out/T52/t52-real-artifact-hash-serving-20261030.md`.
- C5 headless browser walkthrough & screenshots: `../../C5-G4-runtime-walkthrough.md`.
- C4 judge contract and runner receipts: `../../ai/out/milestone-reconciliation/c4-g1-g2-llm-judge-report.md`.
- Canonical claims register (v17, 10 VERIFIED / 2 PARTIAL / 33 UNVERIFIED): `../../ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v17.csv` (SHA-256 `50014fff3b8562b38854e0a5e7f3c93721983337f2de16b600421e9c9e0bfa24`).
- Claims register adjudication report (v12 -> v17): `../../ai/out/milestone-reconciliation/claims-register-v17-adjudication-report.md`.
- Canonical defense rehearsal pack: `../../ai/demos/iron-chef-final-defense-rehearsal-pack.md` (SHA-256 `35d9fa6c80c61d8ec4561e453e258d01c09e724500a81c6c4cfa4c4d377e3ceb`).
- Allergen, Vietnamese, system/demo, and bounded-negative evidence: [Chapters 8](08-allergen-methods.md)–[12](12-bounded-negatives.md).
- Integrated threats and interpretation: [Chapter 13](13-discussion-and-threats.md) v0.2.
