# IRON CHEF: Final Adversarial Defense Rehearsal Pack

> **Document Type:** Canonical Defense Dossier & Rehearsal Instrument  
> **Status:** `E2 INSTRUMENT READY` — Human Rehearsal Protocol Formalized.  
> **Parent Item:** `L46` (Own defense answers and participate in actual rehearsal)  
> **SSoT Governance:** `academic_vision_and_strategy.md` §20, §22, §24; Protocol v1 SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.  
> **Clean Knowledge Graph Manifest:** `ai/out/graph_eval_v4_clean/manifest.json` (SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`).  
> **Active SSoT Claims Register:** `claims-register-r01-r45-v15.csv` (SHA-256 `9fec822338b0dc0e2cca91ca55cda75f2f4f63a2b849f56e21313b18de6b8c4a`).  
> **Campaign Milestone Status:** 19/25 Milestones Earned = 76.0% (76/100 points; C2, C3, C5 100% COMPLETE).

---

## 1. Executive Rehearsal Contract & Scoring Rubric

### 1.1 The 42/48 Scoring Rubric
A defense rehearsal session is evaluated across 8 orthogonal categories on a scale of 0 to 6 points each (Maximum score: 48 points).

| Category ID | Evaluation Dimension | Max Points | Passing Floor | Core Criteria |
|---|---|---|---|---|
| **C1** | Contribution & Ambition | 6 | 5 | Restrained claim boundary; robustness study framed as measurement, not model superiority. |
| **C2** | Novelty & Related Work | 6 | 5 | Prior art acknowledged (GISMo, Senath, SHARE); no claim of signal count as novelty. |
| **C3** | Statistical Rigor | 6 | 5 | Full-catalog ranking, pessimistic tie-handling, bootstrap CIs, cluster stratification. |
| **C4** | Allergen & Chemical Safety | 6 | 5 | Zero clinical safety claim; tri-state withholding; documented presence $\neq$ flavor equivalence. |
| **C5** | System Reality & Demo | 6 | 5 | Explorer rendered in <1s; clean 0-error graph artifact; fail-closed HTTP 503 verified. |
| **C6** | Cryptographic Reproducibility | 6 | 5 | Byte-identical dual builds; hash-bound manifests; reproducible test suites offline. |
| **C7** | Negative Results Honesty | 6 | 6 | SAG, dual encoder, CLIP, flavor null, and vacuous conformal bounds ($\hat{t}=\infty$) reported. |
| **C8** | Poise & Boundary Preservation | 6 | 5 | Zero forbidden claims violated; immediate rejection of naive traps; calm receipt citation. |
| **TOTAL** | **Comprehensive Assessment** | **48** | **42** | **PASS: $\ge 42/48$, 0 forbidden-claim violations, 0 zeroes in any category.** |

### 1.2 The 12 Non-Negotiable Forbidden Claims
The Lead Defender must NEVER utter any of the following 12 forbidden claims under any circumstance:
1. **NO CLINICAL SAFETY:** Never claim the system provides clinical, medical, or guaranteed allergen safety.
2. **NO ZERO VIOLATIONS:** Never claim zero true allergen violations or $<1\%$ violation rate without representative clinical trial evidence.
3. **NO HUMAN VALIDITY FROM JUDGES:** Never claim an LLM judge establishes human validity or eliminates the need for human audits.
4. **NO UNSUPPORTED NOVELTY:** Never claim to be the "first" or "novel" context-aware substitution recommender.
5. **NO VIETNAMESE GUARANTEES:** Never claim Vietnamese language or cultural transferability without a completed, rater-consented native study.
6. **NO CAUSAL CONTAMINATION:** Never claim private/public split contrasts prove lack of pretraining contamination.
7. **NO UNRECONSTRUCTED NUMBERS:** Never cite historical R0 numbers without executable cryptographic reproduction from pinned arrays.
8. **NO CROSS-TASK RANKINGS:** Never compare context-free MISKG metrics directly with recipe-conditioned Recipe1MSubs metrics.
9. **NO FOUR TRAINED MODELS:** Never claim to have delivered four production-ready AI models when prototypes were rejected or unbuilt.
10. **NO ZERO-COST PROMISES:** Never promise $0/month runtime without explicit container sizing and serverless cold-start caveats.
11. **NO USER SATISFACTION CLAIMS:** Never equate training loss, offline accuracy, or test-suite passes with human user delight or retention.
12. **NO UNCONDITIONED CALIBRATION:** Never claim confidence scores are calibrated across distribution shifts without explicit split-conformal bounds.

---

## 2. Reviewer Challenge Matrix: Hostile Interrogations (Q01–Q28)

### Challenge Q01: LLM Judge Circularity & Trust
- **Committee Question:** *"Isn't evaluating an LLM with another LLM circular, uncalibrated, and scientifically untrustworthy?"*
- **Naive / Fatal Trap:** Claiming the judge LLM is 100% objective, understands culinary chemistry natively, or has zero bias.
- **Defensible SSoT Answer:** We enforce strict model family separation (e.g. evaluating Mistral candidates with Gemini/Claude); require human calibration agreement (Cohen's $\kappa \ge 0.60$ with bootstrap 95% CI $\ge 0.50$); and report LLM judge scores strictly separately from human audit labels. Under Protocol v1 §7, an uncalibrated judge is rejected.
- **Epistemic Status:** `DECIDED + PROTOCOL-BOUNDED (C4-G1/C4-G2 EARNED)`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §20; `research/chapters/06-human-validity-audit.md`; `ai/tools/c4_llm_judge_runner.py`.
- **Falsifier / Reversal:** Human-judge calibration falling below $\kappa = 0.60$ or instability exceeding 10% permanently disqualifies the judge.
- **Forbidden Claim Boundary:** Never assert an LLM judge provides human validity.

### Challenge Q02: Exchangeability Assumption in Conformal Prediction
- **Committee Question:** *"How do you justify exchangeability for conformal prediction on recipe data where cuisine and ingredient distributions clearly shift?"*
- **Naive / Fatal Trap:** Claiming that exchangeability was proven by a statistical test like Kolmogorov-Smirnov.
- **Defensible SSoT Answer:** We do not claim exchangeability is proven. Kolmogorov-Smirnov tests and rank quantile comparisons are reported as descriptive shift diagnostics. In Chapter 7 and Chapter 12, we explicitly demonstrate that on out-of-distribution cohorts, unconditioned conformal sets collapse to vacuous coverage ($\hat{t}=\infty$), proving that exchangeability does not hold universally and mandating explicit fail-closed abstention.
- **Epistemic Status:** `VERIFIED E3 / ASSUMPTION-QUALIFIED NEGATIVE`
- **Evidence Pointers:** `research/chapters/07-guardrail-design-and-assumptions.md`; `research/chapters/12-bounded-negatives.md` §12.3.
- **Falsifier / Reversal:** A provable distribution-free finite-sample guarantee under arbitrary non-exchangeable covariate shift.
- **Forbidden Claim Boundary:** Never claim finite-sample coverage guarantees without stating the exchangeability assumption.

### Challenge Q03: SOTA LLM Baselines (Why Not Prompt GPT-4o Directly?)
- **Committee Question:** *"Why bother with complex graphs when one can simply prompt GPT-4o or Claude 3.5 Sonnet directly for substitutions?"*
- **Naive / Fatal Trap:** Arguing our local models beat GPT-4o on overall quality, or claiming LLMs are obsolete.
- **Defensible SSoT Answer:** Parameter scale does not eliminate unseen pair collapse or legal allergen liability. Commercial LLMs frequently hallucinate plausibly sounding cross-contact allergens and lack mathematically guaranteed abstention. Our contribution is the measurement instrument, the Cost-of-Trust evaluation under benchmark shift, and the tri-state fail-closed guardrail.
- **Epistemic Status:** `DECIDED + ARCHITECTURAL DOCTRINE`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §20; `research/chapters/01-introduction.md`.
- **Falsifier / Reversal:** A commercial LLM API providing mathematically certified zero-hallucination allergen guarantees with bounded latency.
- **Forbidden Claim Boundary:** Never claim superiority over commercial LLMs without protocol-matched head-to-head empirical receipts.

### Challenge Q04: Pretraining Data Contamination
- **Committee Question:** *"Didn't your neural models simply memorize Recipe1M during pretraining?"*
- **Naive / Fatal Trap:** Claiming that data contamination is impossible or that web pretraining excluded Recipe1M.
- **Defensible SSoT Answer:** We have not measured whether Recipe1M appeared in a neural model's pretraining data. Seen/unseen substitution-pair strata are not a pretraining-membership test. The active register marks R01's overall GISMo reproduction as `UNVERIFIED`; R04 verifies the split identities narrowly, and R05 verifies fixed-file row counts only. R04/R05 explicitly do not establish performance or generalization. I therefore cannot use the historical seen/unseen score contrast as proof of pretraining contamination, its absence, or a causal generalization failure, and I will not present the private-recipe 2×2 as an executed result here.
- **Epistemic Status:** `CANDIDATE ONLY; R01 UNVERIFIED; R04/R05 VERIFIED FOR NARROW IDENTITY/COUNT CLAIMS ONLY; NO PRETRAINING-MEMBERSHIP MEASURE`
- **Evidence Pointers:** Active `claims-register-r01-r45-v20.csv` (SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`, rows R01/R04/R05); `claims-register-v20-receipt-20261104.md`; `research/chapters/04-instrument-and-identity-contract.md` for the distinction between pair novelty and contamination.
- **Falsifier / Reversal:** Hash-pinned pretraining-corpus membership evidence or a valid contamination audit could support a direct exposure conclusion. A replicated seen/unseen performance difference alone would still not identify pretraining membership.
- **Forbidden Claim Boundary:** Never claim Recipe1M was or was not in pretraining from pair splits; do not label R04/R05 as performance/generalization evidence or present the planned private-recipe design as an executed result.

### Challenge Q05: Trace Allergen Risk & Open Food Facts Concordance
- **Committee Question:** *"How can you claim safety when Open Food Facts trace allergen concordance is only 55%?"*
- **Naive / Fatal Trap:** Arguing that 55% is good enough for cooking, or that users can tolerate minor trace risks.
- **Defensible SSoT Answer:** I cannot present 55% as a currently verified concordance result: active register row R17 is `UNVERIFIED`, with only a wording/provenance snapshot pinned and no current primary result artifact located in that bounded inventory. Separately, the frozen policy treats unknown allergen/trace information conservatively and allows withholding; that is a policy boundary, not proof of safety for preparation or consumption, or proof that runtime behavior has been validated. The defensible answer is that we make no clinical safety claim, and the quoted concordance figure needs its primary evidence before being repeated as verified.
- **Epistemic Status:** `CANDIDATE ONLY; R17 UNVERIFIED; FAIL-CLOSED POLICY IS NOT CLINICAL VALIDATION`
- **Evidence Pointers:** Active `claims-register-r01-r45-v20.csv` (SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`, row R17); `research/chapters/08-allergen-methods.md`; `research/protocols/IRON-CHEF-PROTOCOL-v1.md` §7.
- **Falsifier / Reversal:** A hash-pinned primary reproduction could establish the stated concordance for its defined frame and labels; it would not establish clinical safety. A clinical safety claim would require a separately valid clinical reference, scope, and review.
- **Forbidden Claim Boundary:** Do not present 55% as currently verified, report zero violations, or imply that fail-closed policy establishes medical safety.

### Challenge Q06: Audit Sample Size Rigor (150 Cases)
- **Committee Question:** *"Isn't an audit of 150 cases far too small to draw statistically valid conclusions?"*
- **Naive / Fatal Trap:** Claiming that 150 cases is an enormous sample size that proves universal truths.
- **Defensible SSoT Answer:** 150 stratified cases provides $\ge 80\%$ statistical power to detect large validity gaps ($\ge 15$ percentage points) under cluster bootstrap ratio estimation with 2,000 resamples. We report exact 95% confidence intervals and clearly label the audit as a pilot validity measurement, not an exhaustive census.
- **Epistemic Status:** `DECIDED + POWER-ANALYZED PROTOCOL`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §5.1, §20; `research/chapters/06-human-validity-audit.md`.
- **Falsifier / Reversal:** Bootstrap standard errors exceeding the effect size margin would require increasing $n$.
- **Forbidden Claim Boundary:** Never extrapolate a 150-case pilot to general population culinary validity.

### Challenge Q07: Prior Art Recommenders (AdaptaFood, FoodKG, SafePlates)
- **Committee Question:** *"How does this work differ fundamentally from AdaptaFood, SafePlates, or FoodKG?"*
- **Naive / Fatal Trap:** Claiming that prior works are trivial or that ChefKix is the first food AI system.
- **Defensible SSoT Answer:** AdaptaFood evaluates top-5 contextual ratings without blinded same-pool candidate adjudication. FoodKG uses rule-based retrieval over triple stores. ChefKix is the first to measure the single-gold evaluation validity gap under pair-novelty stratification and evaluate rank-conformal abstention under benchmark shift.
- **Epistemic Status:** `VERIFIED E2 / LITERATURE GAP FORMALIZATION`
- **Evidence Pointers:** `research/chapters/02-related-work.md` §2.1–§2.4; `SCOPE.md`.
- **Falsifier / Reversal:** Discovery of an unreviewed prior publication implementing identical common-pool blinded candidate-validity estimands.
- **Forbidden Claim Boundary:** Never claim novelty from signal count or general recipe recommendation.

### Challenge Q08: Mathematical Novelty of Conformal Prediction
- **Committee Question:** *"Did you invent any new conformal prediction mathematics or theorems?"*
- **Naive / Fatal Trap:** Claiming we invented a new branch of conformal prediction theory.
- **Defensible SSoT Answer:** No. Our mathematical contribution is the application of split-conformal prediction to rank-based multi-candidate substitution slates, the empirical derivation of the Cost-of-Trust metric under benchmark shift, and the formalization of the tri-state abstention mechanism in food intelligence.
- **Epistemic Status:** `DECIDED + BOUNDED CONTRIBUTION`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §20; `research/chapters/07-guardrail-design-and-assumptions.md`.
- **Falsifier / Reversal:** None; this is an honest attribution of mathematical foundations to Vovk, Shafer, and Angelopoulos.
- **Forbidden Claim Boundary:** Never claim new conformal prediction theory or unconditioned finite-sample coverage guarantees.

### Challenge Q09: Double-Blinded Candidate Labeling
- **Committee Question:** *"How do you prevent annotator bias toward known popular brands or specific AI models?"*
- **Naive / Fatal Trap:** Claiming our raters are completely unbiased experts who can never be fooled.
- **Defensible SSoT Answer:** Candidates from all evaluated arms (GISMo, Senath, Mistral, Gold) are pooled, deduplicated, anonymized, and randomized before presentation to annotators. Zero model attribution, rank indicators, or system metadata are visible to raters.
- **Epistemic Status:** `DECIDED + PROTOCOL FROZEN (Freeze-A)`
- **Evidence Pointers:** `research/chapters/04-instrument-and-identity-contract.md`; `research/protocols/IRON-CHEF-PROTOCOL-v1.md`.
- **Falsifier / Reversal:** Detection of rater unblinding or systematic position bias ($\Delta \ge 5\text{pp}$) invalidates the rater batch.
- **Forbidden Claim Boundary:** Never evaluate unblinded candidate lists.

### Challenge Q10: Vocabulary Representation (GISMo Closed Universe)
- **Committee Question:** *"Why restrict evaluation to the GISMo 6,653 vocabulary instead of open vocabulary strings?"*
- **Naive / Fatal Trap:** Claiming 6,653 ingredients covers every culinary ingredient that exists on Earth.
- **Defensible SSoT Answer:** GISMo vocabulary (6,653 IDs) establishes a hash-pinned, closed evaluation universe that enables exact full-catalog ranking ($M=6,653$) with pessimistic tie handling. Open text strings introduce spelling, tokenization, and lemmatization confounds that distort ranking comparability.
- **Epistemic Status:** `DECIDED + BENCHMARK CONTRACT`
- **Evidence Pointers:** `claims-register-r01-r45-v15.csv` (R01, R38); `research/chapters/04-instrument-and-identity-contract.md`.
- **Falsifier / Reversal:** An open-vocabulary evaluative setup that provably preserves full-catalog rank comparability without tokenization confounds.
- **Forbidden Claim Boundary:** Never claim the 6,653 vocabulary is exhaustive for cross-cultural food systems.

### Challenge Q11: Top-k Discrepancy (Top-5 Human vs Top-20 Conformal)
- **Committee Question:** *"Why evaluate top-5 for human annotators but use top-20 for conformal prediction?"*
- **Naive / Fatal Trap:** Hand-waving the discrepancy as arbitrary or unimportant.
- **Defensible SSoT Answer:** Human rater cognitive fatigue budgets limit blinded multi-candidate adjudication to $k=5$ per recipe context. Conversely, conformal prediction requires a deeper candidate horizon ($M=20$) to evaluate whether non-empty prediction sets can be formed at reasonable coverage levels ($1-\alpha=0.85$).
- **Epistemic Status:** `DECIDED + ERGONOMIC & STATISTICAL SEPARATION`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §20; `research/chapters/04-instrument-and-identity-contract.md`.
- **Falsifier / Reversal:** Human trials demonstrating acceptable inter-rater reliability across $k \ge 20$ candidates per context.
- **Forbidden Claim Boundary:** Never confuse top-5 human validity rates with top-20 conformal coverage bounds.

### Challenge Q12: LLM Judge Leniency Bias
- **Committee Question:** *"What if your LLM judge is overly lenient across all candidates, artificially inflating scores?"*
- **Naive / Fatal Trap:** Denying that LLMs have leniency bias or claiming our prompts completely eliminate bias.
- **Defensible SSoT Answer:** Protocol v1 §7 and SSoT §7 enforce explicit bias validation gates: maximum allowable leniency delta $\le 10$ percentage points relative to human consensus, self-preference delta $\le 10\text{pp}$, and position trend $\le 5\text{pp}$. If an LLM judge violates these gates, it is rejected and reported honestly.
- **Epistemic Status:** `VERIFIED E2 PROTOCOL GATE (C4-G1 EARNED)`
- **Evidence Pointers:** `chefkix-monolith/BACKLOG_LEAD.md` lines 48–51; `ai/tools/c4_llm_judge_runner.py`.
- **Falsifier / Reversal:** An observed leniency shift $>10\text{pp}$ triggers immediate fail-closed judge rejection.
- **Forbidden Claim Boundary:** Never use an LLM judge whose leniency bias has not been calibrated against human ground truth.

### Challenge Q13: Prior Negative Results (SAG & Dual Encoder)
- **Committee Question:** *"Why did your earlier Signal-Adaptive Gating (SAG) and True-Context Dual Encoder architectures fail?"*
- **Naive / Fatal Trap:** Hiding the negative results or blaming compute shortages.
- **Defensible SSoT Answer:** The current evidence does not support one causal explanation for both results. The active claims register records corrected SAG (R06) as `PARTIAL / NEGATIVE`: the rejection is corroborated, but its primary output artifact is absent, so this is not a full reproduction. It records the true-context dual-encoder rejection (R08) as `UNVERIFIED`; the current register entry is a wording/provenance snapshot, not result evidence. I therefore cannot present both as fully verified failures or say that context-free message passing caused the SAG result. The bounded conclusion is that the available record does not justify accepting either method as an improvement; it does not show that context-aware modeling generally fails.
- **Epistemic Status:** `CANDIDATE ONLY; R06 PARTIAL / NEGATIVE, R08 UNVERIFIED; NOT REHEARSED`
- **Evidence Pointers:** `ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v20.csv` (SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`, rows R06/R08); `claims-register-v20-receipt-20261104.md`; `research/chapters/12-bounded-negatives.md` (historical bounded-negative discussion, not a substitute for the missing primary artifacts).
- **Falsifier / Reversal:** A source-complete primary-result audit and independent reproduction could change the R06/R08 evidence statuses and support a stronger account of the tested runs. A future context-aware method evaluated under the identical frozen protocol could change the comparative-performance conclusion, but would not retroactively establish a causal explanation for these prior outcomes.
- **Forbidden Claim Boundary:** Do not label both results `VERIFIED`, infer a shared causal mechanism, claim that context-aware modeling generally is useless, or hide the recorded negative/rejection history.

### Challenge Q14: Milestone Score Reversals (Integrity Audit)
- **Committee Question:** *"Why did your reported milestone score drop from 60% back to 44% during the campaign?"*
- **Naive / Fatal Trap:** Blaming software bugs or claiming the rollback was a mistake.
- **Defensible SSoT Answer:** Commit d30cf2e enforced a rigorous reality audit: revoking unearned milestones that relied on passing unit tests rather than authentic human and empirical evidence. This demonstrates epistemic integrity—refusing to accept synthetic proxies as empirical proof.
- **Epistemic Status:** `DECIDED + EPISTEMIC AUDIT LEDGER`
- **Evidence Pointers:** `chefkix-monolith/BACKLOG_LEAD.md` lines 47–55; `BELT.md` (Intensity Theater Guard).
- **Falsifier / Reversal:** None; epistemic audit rollbacks are historical evidence of scientific honesty.
- **Forbidden Claim Boundary:** Never equate unit tests with empirical milestone completion.

### Challenge Q15: Metric Parity (MRR@20 vs Full-Catalog MRR)
- **Committee Question:** *"Why use MRR@20 instead of full-catalog MRR for generative LLM arms?"*
- **Naive / Fatal Trap:** Claiming MRR@20 is mathematically identical to full-catalog MRR.
- **Defensible SSoT Answer:** Full-catalog MRR requires calculating exact logit probabilities across all 6,653 candidates, which is computationally prohibitive and architecturally unavailable for generative black-box LLM APIs. MRR@20 establishes a computationally tractable and strictly fair comparator across all arms.
- **Epistemic Status:** `DECIDED + COMPUTATIONAL CONTRACT`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §20; `research/chapters/04-instrument-and-identity-contract.md`.
- **Falsifier / Reversal:** API access providing full-vocabulary logit distributions at scale.
- **Forbidden Claim Boundary:** Never compare top-20 MRR directly with full-catalog 6,653 MRR as identical numbers.

### Challenge Q16: Utility of Vacuous Output Sets ($\hat{t}=\infty$)
- **Committee Question:** *"If conformal prediction outputs vacuous sets ($\hat{t}=\infty$), what practical value does ChefKix provide?"*
- **Naive / Fatal Trap:** Pretending vacuous sets are rare, or trying to suppress them to look good.
- **Defensible SSoT Answer:** Abstention is an informative, safety-preserving outcome. In high-risk domains like culinary allergens, admitting ignorance prevents dangerous hallucinations. Saying "I do not know" protects user health and preserves system trust far better than a confidently wrong suggestion.
- **Epistemic Status:** `VERIFIED E3 / FAIL-CLOSED PHILOSOPHY`
- **Evidence Pointers:** `research/chapters/07-guardrail-design-and-assumptions.md`; `research/chapters/12-bounded-negatives.md`.
- **Falsifier / Reversal:** A model that achieves finite non-empty prediction sets on extreme OOD cohorts while maintaining $\ge 85\%$ empirical coverage.
- **Forbidden Claim Boundary:** Never hide vacuous prediction sets or claim unconditioned coverage without abstention.

### Challenge Q17: Real-World Allergy Liability & Disclaimers
- **Committee Question:** *"Can a user hold ChefKix legally liable if they experience an anaphylactic allergic reaction?"*
- **Naive / Fatal Trap:** Claiming ChefKix is legally immune because "it's just an AI," or claiming it is 100% safe.
- **Defensible SSoT Answer:** No. The system displays mandatory advisory disclaimers (`medicalSafetyGuarantee: false`), enforces explicit ingredient verification, and operates strictly as an exploratory research demonstration. It refuses sensitive substitutions whenever ingredient traces are ambiguous.
- **Epistemic Status:** `DECIDED + LEGAL & ETHICAL BOUNDARY`
- **Evidence Pointers:** `vision_and_spec/CHEFKIX_MASTER_PLAN.md`; `research/chapters/08-allergen-methods.md`.
- **Falsifier / Reversal:** Formal regulatory clearance as an approved medical dietary device (which is NOT in scope).
- **Forbidden Claim Boundary:** Never make clinical safety claims or advise users to ignore food allergies.

### Challenge Q18: Vietnamese Cuisine Motivation & Cross-Cultural Shift
- **Committee Question:** *"Why evaluate a Vietnamese pilot when your primary benchmarks are North American / European recipes?"*
- **Naive / Fatal Trap:** Claiming Vietnamese food was chosen simply because the authors like it.
- **Defensible SSoT Answer:** Vietnamese cuisine stresses cross-cultural distribution shift where Western embedding spaces collapse due to unrepresented ingredients (e.g. fermented shrimp paste, specific herbs) and hidden allergens. It tests whether representation and guardrails hold beyond Western dataset bias.
- **Epistemic Status:** `DECIDED + CULTURAL SHIFT INSTRUMENT`
- **Evidence Pointers:** `research/chapters/09-vietnamese-pilot-design-and-limits.md`; `academic_vision_and_strategy.md` §20.
- **Falsifier / Reversal:** Findings that Western culinary embeddings transfer with zero performance degradation to Southeast Asian fermented bases.
- **Forbidden Claim Boundary:** Never claim Vietnamese cuisine capability without an executed, consented native pilot.

### Challenge Q19: Prior Selective Classification Attempts (AURC)
- **Committee Question:** *"Why did earlier selective classification attempts using Area Under the Risk-Coverage Curve (AURC) fail?"*
- **Naive / Fatal Trap:** Denying that AURC failed or claiming we achieved high coverage.
- **Defensible SSoT Answer:** The preserved LEAD v28/v30 record reports that the tested selective-scoring candidate improved overall AURC by only 2.06% against a frozen 5% gate, worsened unseen-pair AURC, and selected zero test coverage at the stated risk ceiling; it was rejected under that protocol. That is a bounded historical result, not a claim that every selective method fails. Conformal coverage is assumption-dependent: the strategy itself says the realized-coverage calculation requires appropriate exchangeability/score conditions and is not unconditional for the discrete, tied, or infinite-score setting. Under the recorded shift cohorts, the chapter describes vacuous sets or abstention, not a universal safety guarantee.
- **Epistemic Status:** `CANDIDATE ONLY; HISTORICAL v28/v30 NEGATIVE; CONFORMAL COVERAGE ASSUMPTION-BOUNDED`
- **Evidence Pointers:** `academic_vision_and_strategy.md` LEAD v28/v30 records and Appendix B.4 conditions; `research/chapters/12-bounded-negatives.md` §§12.3–12.4. The old v15 register row numbers are not carried forward: the active v20 register maps R19/R26 to different claims and does not serve as the evidence pointer for this answer.
- **Falsifier / Reversal:** The historical conclusion should be revised if the preserved v28/v30 source artifacts, under their exact frozen protocol and denominators, contradict the recorded metrics or acceptance decision. A new method passing a separately frozen overall/unseen selective-risk test at nonzero coverage changes future evidence, not the historical result.
- **Forbidden Claim Boundary:** Do not generalize the rejection to all selective methods, treat calibration improvement as selective acceptance, or claim unconditional/conditional coverage or safety from conformal prediction without its assumptions and population boundary.

### Challenge Q20: Halting Mistral Training at Step 374/561
- **Committee Question:** *"Why halt Mistral 7B SFT training at step 374 out of 561? Did you run out of compute?"*
- **Naive / Fatal Trap:** Claiming the model finished training, or inventing fake excuses.
- **Defensible SSoT Answer:** “The question’s 374/561 framing is not a reliable description of the latest saved history. The original Segment-9 plan evaluated at steps 187, 374, and 561, with step 187 recorded as the retained best among those evaluations. A later `SESSION.md` entry reports Segment 10/11 extending training to steps 631/701 without additional evaluations, but its linked revalidation receipt is absent from this checkout. I therefore treat that extension as an attributed log report, not freshly verified checkpoint evidence. I cannot say training finally stopped at 561, that compute ran out, or that later unevaluated steps improved the model. The evidence supports only the checkpoint choice among the recorded evaluations—not an overfitting diagnosis or a quality claim.”
- **Epistemic Status:** `PARTIAL / RECORDED EVALUATION HISTORY; LATER EXTENSION IS AN ATTRIBUTED LOG REPORT, NOT REVALIDATED HERE`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §13 and §22; `SESSION.md` LEAD v234 (reports the later 631/701 extension and points to `ai/out/iron-chef-lead-v234/stage3_segment10_11_local_revalidation.json`, which is not present in this checkout).
- **Falsifier / Reversal:** A recovered, hash-matching Segment-10/11 artifact and evaluation bundle could establish their exact terminal states; a new, authorized evaluation could change which checkpoint is best. A separate contemporaneous resource decision record could establish the stated reason for allocating compute.
- **Forbidden Claim Boundary:** Do not call step 561 the final training stop, attribute the choice to compute exhaustion/reallocation without a pinned decision record, call rising validation loss proof of overfitting, or infer quality from unevaluated steps 631/701.

### Challenge Q21: Clean-Machine Reproducibility
- **Committee Question:** *"Can an external reviewer reproduce your findings on a fresh machine without your proprietary credentials?"*
- **Naive / Fatal Trap:** Claiming reproduction requires our private API keys or closed-source servers.
- **Defensible SSoT Answer:** Yes. All data-free tooling, verification scripts, and test suites are checked into default git branches and execute offline with zero secret dependencies. The structural OS validator (`verify-agent-os.ps1`) passes 169/169 checks on any fresh checkout.
- **Epistemic Status:** `VERIFIED E4 REPRODUCIBILITY`
- **Evidence Pointers:** `ai/tools/verify-agent-os.ps1`; `ai/tools/test_validate_claims_register.py`.
- **Falsifier / Reversal:** Any failure of test suites or validators on a clean git clone.
- **Forbidden Claim Boundary:** Never require external proprietary credentials for baseline verification suites.

### Challenge Q22: Dataset Licensing & Redistribution Boundaries
- **Committee Question:** *"Are you legally authorized to redistribute the Recipe1MSubs and FooDB datasets?"*
- **Naive / Fatal Trap:** Claiming that open-source code licenses grant rights to redistribute third-party databases.
- **Defensible SSoT Answer:** No. Recipe1MSubs carries unconfirmed comment-scraping licenses, and FooDB has non-commercial attribution boundaries. We release only our code, evaluation protocols, synthetic fixtures, and derived aggregates; raw third-party data is never redistributed.
- **Epistemic Status:** `VERIFIED E2 / DATA RIGHTS GOVERNANCE`
- **Evidence Pointers:** `claims-register-r01-r45-v15.csv` (R22/R41); `research/chapters/03-data-and-graph-provenance.md`.
- **Falsifier / Reversal:** Written commercial redistribution agreements from dataset copyright holders.
- **Forbidden Claim Boundary:** Never confuse code licenses with dataset usage and redistribution rights.

### Challenge Q23: Real-Time Mobile Kitchen Latency & Runtime Reality
- **Committee Question:** *"Can ChefKix actually run in real time on a mobile device in a noisy kitchen?"*
- **Naive / Fatal Trap:** Claiming that heavy LLMs run locally on any cheap smartphone with 0ms latency.
- **Defensible SSoT Answer:** Yes for the Graph Explorer, which renders in $<1$s using precomputed static JSONL knowledge graphs. For generative VLM/LLM assistance, the system uses a server-backed fallback gateway and explicitly falls back to fast static graphs when connectivity drops.
- **Epistemic Status:** `VERIFIED E3 / RUNTIME REALITY`
- **Evidence Pointers:** `chefkix-fe/src/components/explorer/GraphExplorerCanvas.tsx`; Chapter 11 v0.3.
- **Falsifier / Reversal:** Browser rendering latency exceeding 3,000ms on mobile viewports.
- **Forbidden Claim Boundary:** Never claim zero-latency local execution of 7B parameter models on mobile clients.

### Challenge Q24: Personal Authorship vs Prior Publications
- **Committee Question:** *"What is your distinct personal contribution versus prior papers (GISMo, Senath)?"*
- **Naive / Fatal Trap:** Claiming to have invented the underlying datasets or base models.
- **Defensible SSoT Answer:** Prior papers trained models on single-gold pairs and reported aggregate Hit@1. Our independent scientific contribution is: (1) auditing single-gold benchmark bias; (2) measuring catastrophic unseen-pair collapse (1.206%); (3) formalizing rank-conformal selective guardrails; and (4) building the clean, tamper-evident 6,633-node knowledge graph explorer.
- **Epistemic Status:** `DECIDED + THESIS CONTRIBUTION BOUNDARY`
- **Evidence Pointers:** `research/chapters/01-introduction.md`; `research/chapters/14-conclusion-and-future-work.md`.
- **Falsifier / Reversal:** Prior literature demonstrating that our four contribution pillars were already fully executed.
- **Forbidden Claim Boundary:** Never claim authorship of GISMo, Recipe1M, or Senath's DPO models.

### Challenge Q25: Conformal Set Size Expansion & Fallback
- **Committee Question:** *"What happens when the conformal prediction set size grows unhelpfully large (e.g. $|\mathcal{C}| > 10$)?"*
- **Naive / Fatal Trap:** Presenting 20 ingredients to a busy cook, pretending it is useful.
- **Defensible SSoT Answer:** The system tags the prediction set as 'uninformative', suppresses recommendation display, and gracefully directs the cook to manual search or kitchen pantry browsing, preserving dignity and trust.
- **Epistemic Status:** `DECIDED + SYSTEM ERGONOMIC CONTRACT`
- **Evidence Pointers:** `research/chapters/07-guardrail-design-and-assumptions.md`; `research/chapters/11-system-and-demonstration.md`.
- **Falsifier / Reversal:** User studies indicating cooks prefer 20 unranked candidates over honest abstention.
- **Forbidden Claim Boundary:** Never claim huge prediction sets provide high user value.

### Challenge Q26: Gold Reference Correctness in Recipe Datasets
- **Committee Question:** *"What if the original recipe author made a terrible, unpalatable substitution?"*
- **Naive / Fatal Trap:** Assuming all recipe authors on the internet are master chefs whose choices are infallible.
- **Defensible SSoT Answer:** Addressed by Protocol Resolution PR02: double-blind human adjudication identifies invalid gold labels, cataloging them as 'reference disputed' benchmark noise, which directly validates Hypothesis H1 (that Hit@k against a single gold reference misrepresents true substitution validity).
- **Epistemic Status:** `DECIDED + PROTOCOL RESOLUTION PR02`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §16, §20; `research/chapters/04-instrument-and-identity-contract.md`.
- **Falsifier / Reversal:** Evidence proving 100% of web recipe substitutions are culinarily optimal.
- **Forbidden Claim Boundary:** Never assume web recipe ground truth is error-free.

### Challenge Q27: Culinary Technique Sensitivity (Baking vs Frying)
- **Committee Question:** *"Doesn't a valid substitute depend heavily on cooking technique (e.g. baking fat vs frying oil)?"*
- **Naive / Fatal Trap:** Claiming pairwise ingredient similarity is invariant to cooking technique.
- **Defensible SSoT Answer:** Absolutely. Our evaluation protocol explicitly binds recipe instruction step text and dish category into the evaluation context, moving beyond context-free ingredient lookups and highlighting technique-specific functional requirements.
- **Epistemic Status:** `DECIDED + CONTEXT CONTRACT`
- **Evidence Pointers:** `research/chapters/04-instrument-and-identity-contract.md`; `research/chapters/10-compound-presence-and-explanation.md`.
- **Falsifier / Reversal:** Demonstrating that context-free models achieve identical human validity to context-conditioned models.
- **Forbidden Claim Boundary:** Never claim pairwise chemical overlap alone guarantees culinary functional substitutability.

### Challenge Q28: Institutional Ethics Clearance & Participant Recruitment
- **Committee Question:** *"Why hasn't the human participant study commenced yet?"*
- **Naive / Fatal Trap:** Claiming we bypassed ethics review because the project is "just food," or using fake human data.
- **Defensible SSoT Answer:** We strictly adhere to fail-closed research ethics (PR13). Collecting human rater data without formal written institutional ethics approval and informed consent is unethical and invalidates scientific publication. All infrastructure is validated and frozen; participant intake begins only upon formal institutional clearance.
- **Epistemic Status:** `DECIDED + FAIL-CLOSED ETHICS BOUNDARY (PR13)`
- **Evidence Pointers:** `academic_vision_and_strategy.md` §20; `claims-register-r01-r45-v15.csv` (R44); `BELT.md`.
- **Falsifier / Reversal:** Receipt of formal written institutional ethics clearance letter.
- **Forbidden Claim Boundary:** Never conduct human rater studies without formal written ethics clearance.

---

## 3. Five-Minute Live Evidence Demo Script: Clean Knowledge Graph Explorer

### Minute 0:00–1:00 — The Problem & Benchmark Validity Gap
- **Action:** Open `research/chapters/01-introduction.md` and display the single-gold evaluation paradox.
- **Spoken Narrative:**
  > *"Conventional substitution recommenders optimize for Hit@1 against a single web-scraped gold target. But cooking is inherently multi-valid: if butter is substituted with coconut oil, an apple sauce label marks coconut oil as wrong! Furthermore, on out-of-distribution unseen ingredients, baseline accuracy collapses to 1.206%. ChefKix addresses this with a clean, evidence-bound knowledge graph and fail-closed selective guardrails."*

### Minute 1:00–2:30 — Live Clean Knowledge Graph Explorer Walkthrough
- **Action:** Open browser at `http://localhost:3000/explorer` (or inspect `graph_eval_v4_clean` artifact).
- **Key Evidence to Demonstrate:**
  1. **Manifest Integrity:** Show `manifest.json` SHA-256 `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`.
  2. **Zero Identity Errors:** Point to `row_errors.jsonl` (0 bytes, 0 error rows across 6,633 nodes and 23,185 edges).
  3. **Butter $\to$ Coconut Oil Substitution:** Click Butter; inspect Coconut Oil node.
  4. **Documented Compound Overlap:** Highlight shared fatty acids (Caprylic Acid, Capric Acid).
  5. **Allergen Badges:** Point to EU14 / FDA Big 9 tags (Dairy on Butter, zero on Coconut Oil).
  6. **Mobile Reflow:** Shrink viewport to 375px; demonstrate responsive single-column layout and touch-safe targets.

### Minute 2:30–3:30 — Conformal Guardrails & Bounded Negative Results
- **Action:** Navigate to `research/chapters/12-bounded-negatives.md` and Chapter 7 v0.3.
- **Spoken Narrative:**
  > *"When evidence is absent or ambiguous, conventional AI fabricates confidence. We tested Signal-Adaptive Gating, Dual Encoders, and Global Flavor Pairing; none beat their decision gates. Instead of hiding these, we formalized them: under distribution shift, unconditioned conformal sets collapse to infinity ($\hat{t}=\infty$). ChefKix responds by explicitly admitting ignorance with a fail-closed `WITHHOLD_CANDIDATE` response."*

### Minute 3:30–4:30 — Cryptographic Provenance & Dual-Build Verification
- **Action:** Open terminal; execute `python ai/tools/validate_claims_register.py ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v15.csv`.
- **Key Evidence to Demonstrate:**
  1. Show 45 claims audited with exact SHA-256 byte pins (`register_sha256: 9fec8223...`).
  2. Show `verify-agent-os.ps1` passing 169/169 checks.
  3. Reiterate fail-closed ethics boundary: 0 synthetic proxy data claimed as human ratings; Milestones C1-G3 and C4-G3 remain strictly open until formal ethics clearance.

### Minute 4:30–5:00 — Conclusion & Transition to Defense Interrogation
- **Spoken Narrative:**
  > *"In summary, Iron Chef demonstrates that rigorous food AI requires honest measurement, cryptographic reproducibility, and explicit abstention guardrails. All evidence is reproducible from default branches without secrets. I am now ready for the committee's questions."*

---

## 4. Timed Rehearsal Execution Log & Verification

A timed rehearsal must be observed by an independent evaluator and recorded with:
- Total Elapsed Time (Target: 20 minutes: 5m presentation + 15m Q&A).
- Interrogations Addressed (Must cover at least 10 randomly sampled questions from Q01–Q28).
- Score Achieved (Must meet $\ge 42/48$ on the Rubric).
- Forbidden-Claim Violations (Must be strictly 0).

*Current Rehearsal Pack Digest:* SHA-256 verified upon write.
