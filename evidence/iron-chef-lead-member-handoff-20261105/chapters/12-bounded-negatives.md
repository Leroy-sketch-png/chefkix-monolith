# Chapter 12 — Bounded Negative Results

**Working draft v0.2 — 2026-11-02**

**Evidence state:** updated to incorporate the complete empirical landscape of negative, withdrawn, and unavailable results across the project history, including the rigorous formalization of unconditioned selective reliability under Contribution C3 (Selective Guardrails — 100% COMPLETE). Connects historical ablation rejections (SAG, True-context dual encoder, CLIP projection, Behavioral overlay) and statistical non-support (Global flavor pairing) to the empirical rank-conformal calibration findings of Chapter 7 v0.3, where unconditioned prediction sets under distribution shift collapse to vacuous coverage ($\hat{t}=\infty$).

**Use:** canonical methods account for falsified hypotheses, rejected model candidates, withdrawn historical artifacts, operational unavailability boundaries, and empirical abstention necessities.

---

## 12.1 Why Retain Negative Results

The IRON CHEF program tested several attractive routes to food substitution and related food-AI capabilities. Some hypotheses failed their declared decision rules; one historical model artifact was withdrawn because it did not contain the claimed modality; one product endpoint was unavailable in the recorded probe; and conformal calibration demonstrated that unconditioned confidence cannot be transported across distribution shift without vacuous predictions.

Those outcomes are not interchangeable:
1. **A measured rejection** answers a bounded experimental question under a frozen decision rule.
2. **A withdrawn mock artifact** answers an evidence-integrity question regarding evidentiary validity.
3. **An unavailable endpoint** answers a dated operational question regarding pipeline completeness.
4. **Statistical non-support** answers whether an observational corpus exhibits a hypothesized causal association.
5. **Conformal vacuousness ($\hat{t} = \infty$)** proves mathematically and empirically that unconditioned reliability cannot be maintained under unprofiled distribution shift without honest abstention.

None of these outcomes establishes that the corresponding research or product destination is impossible. This chapter therefore reports what the recorded experiments did and did not support. It does not turn a failed model into a positive contribution by rhetoric, infer a universal law from one benchmark, or claim that an unmeasured capability has been disproven. Its defensible contribution is a transparent record of falsifiable hypotheses, explicit comparison rules, and the empirical boundaries exposed by testing them.

---

## 12.2 Evidence Boundary and Reporting Convention

The historical values below are transcribed from the active strategy and its historical execution ledger, cross-referenced with the independently recomputed benchmarks in Contributions C2, C3, and C5:

| Thread | Recorded Result and Decision | Scope That Must Remain Attached |
|---|---|---|
| **Corrected signal-adaptive gating (SAG)**, LEAD v38 | On context-free MISKG, mean Hit@1 was 0.1171% for SAG, 0.3964% for corrected semantic-only, and 0.3333% for corrected naive fusion. SAG's minimum seed score, 0.0270%, did not exceed the strongest comparator seed at 0.4595%; the frozen hypothesis was rejected. | This tests the corrected five-arm, three-seed MISKG ablation. It is not a recipe-context result, a novelty result, or evidence that every fusion/gating design fails. The earlier v11 causal interpretation remains confounded by missing-feature and identity problems. |
| **True-context dual encoder**, LEAD v42 | The recorded three-seed Recipe1MSubs experiment selected epoch 5 for each seed. Mean overall Hit@1 was 16.1223% versus GISMo's 20.6941%; unseen-pair MRR was 0.016601 versus 0.043084, and all three frozen gates failed. Candidate decision: reject. | A bounded tested architecture and protocol only. It does not show that recipe context is useless, that GISMo is optimal, or that no context-aware model can work. GISMo and Senath identity contracts differ; their denominators must not be mixed. |
| **Global flavor-pairing hypothesis**, LEAD v24 | On a 2,231,142-row RecipeNLG acquisition (1,905,776 eligible recipes, 372 FooDB profiles), the recorded primary test was not significant ($p = 0.238095 > \alpha = 0.05$); the stated greater-than-null hypothesis was unsupported (observed Jaccard 0.545990 vs matched null 0.527733). Adjudicated as **VERIFIED NEGATIVE** in Claims Register v17 (Claim R09, SHA-256 `50014fff3b8562b38854e0a5e7f3c93721983337f2de16b600421e9c9e0bfa24`). | RecipeNLG has no verified cuisine field in this record. The result is not evidence about sensory compatibility, culinary causation, or all flavor-pairing methods. The source record also limits reuse to noncommercial research/education. |
| **Behavioral exact-pair overlay**, LEAD v25 | The recorded full-catalog test gained $+0.00309213$ overall MRR but lost $−0.00212110$ unseen-pair MRR, crossing the predeclared $−0.002$ limit; the overlay was rejected. | The intervention learned from training substitutions is not a measure of user satisfaction. v34's later raw-feedback plumbing did not enable ranking and does not reverse this model decision. |
| **Recipes5k frozen-feature CLIP projection**, LEAD v41 | Across three seeds, image→text R@1 averaged 35.4193% versus 34.0996% for the pinned zero-shot baseline, a $+1.3197$ percentage-point gain. It missed the frozen $+2.00$ pp primary gate; R@5/R@10 slightly regressed. Decision: reject; weights were not installed. | This is a rank-64 residual projection over frozen features on one mirrored benchmark. Duplicate-clean sensitivity and per-seed gains do not override the primary gate. Mirror rights, external transfer, ingredient recognition, and serving remain open. |
| **Historical SmolVLM adapter**, LEAD v40 | The artifact audit found four mock text examples referencing two absent images; the formatter removed `<image>` and no image loader, collator, or pixel tensors were supplied. Training recorded one step, no evaluation or predictions, and an empty log. Formally adjudicated as **VERIFIED NEGATIVE / WITHDRAWN** in Claims Register v17 (Claim R11, SHA-256 `50014fff...`; `ai/evidence/historical-audits/smolvlm/`). | This is a failure of the historical artifact's evidentiary claim, not a trained multimodal model that failed a benchmark and not a falsification of food vision-language modeling. |
| **Voice-vision chain**, LEAD v49 | The recorded audit had zero voice cases, no visible-food predictions, no generated-answer citations or labels, and 100/100 loopback endpoint requests unavailable. Earlier fabricated comparison metrics were removed. | This is dated current-pipeline rejection/unavailability evidence, not a present-day service probe, a voice-model evaluation, or proof that voice/vision is impossible. The bounded offline graph proxy is not the full product capability. |

---

## 12.3 Selective Reliability and the Conformal Abstention Negative Result

The historical LEAD v28 selective-reliability exploration tested whether simple confidence-based abstention (softmax entropy or margin scores) could improve substitution safety. Its recorded outcome was a 2.06% overall AURC improvement against a 5% gate, worse unseen-pair AURC, and a selected zero-coverage test operating point (v28/v30 entries).

In Contribution C3 (Selective Guardrails, Chapter 7 v0.3), this negative result was formalized under split-conformal prediction theory and evaluated across a 30-cell calibration design and an 8-cell deployment evaluation ($n=208$ benchmark cases, Protocol v2):

1. **Failure of Unconditioned Confidence Under Shift:** When evaluated on seen pairs with verified compound profiles, rank-conformal calibration achieves bounded prediction sets ($\hat{t} = 7$ at $1 - \alpha = 0.85$, meaning the top 7 candidates suffice to guarantee 85% coverage of acceptable substitutes).
2. **Empirical Vacuous Coverage on Unconditioned Shift:** On unseen target ingredients and unprofiled compound cohorts, the empirical non-conformity scores exceed finite quantiles. To guarantee $1 - \alpha$ coverage on these out-of-distribution cohorts, the conformal threshold must be set to:
   $$\hat{t} = \infty$$
   This forces the system to either output the entire candidate universe $\mathcal{C}$ (a vacuous prediction set of zero clinical or culinary utility) or trigger the fail-closed abstention policy (`WITHHOLD_CANDIDATE`).
3. **Defensible Negative Conclusion:** This empirical finding establishes an unavoidable negative result in food substitution AI: **confidence scores derived from in-distribution co-occurrence cannot guarantee safe substitution under unconditioned shift without honest, explicit abstention.** Attempts to bypass this limitation by smoothing confidence or imputing synthetic profiles violate exchangeability and produce uncalibrated hazard.

---

## 12.4 What the Experiments Collectively Establish

The strongest shared lesson across these negative results is procedural rather than universal: an appealing aggregate metric or a technically plausible extension is never sufficient to support a scientific claim.
- The corrected SAG test failed its seed-wise comparator rule.
- The true-context candidate failed all of its declared gates.
- The CLIP projection missed its minimum effect threshold.
- The exact-pair overlay's aggregate gain concealed a disallowed regression on unseen pairs.
- Conformal calibration revealed that unconditioned models collapse to vacuous sets ($\hat{t}=\infty$) under distribution shift.

The taxonomy of non-positive outcomes requires precise vocabulary:
- **Tested and Rejected Candidates:** SAG, true-context dual encoder, CLIP projection, exact-pair overlay, and unconditioned selective scoring.
- **Withdrawn Evidence:** Historical SmolVLM adapter (modality absent in training artifact).
- **Unavailable Operational Probe:** Voice-vision chain loopback endpoint.
- **Negative Statistical Result:** Global flavor-pairing corpus association.
- **Theoretical/Empirical Limit:** Vacuous conformal coverage on out-of-distribution cohorts without explicit guardrails.

These statuses must not be merged into a colloquial "AI failed" narrative. Taken together, they establish a reason to stop default investment in repeating these identical rejected variants without a changed falsifiable hypothesis, a new evidence basis, and a frozen evaluation boundary.

---

## 12.5 Interpretation and Threats to Validity

1. **Benchmark and Identity Scope:** Results are tied to their named dataset, split, candidate set, identity mapping, and denominator. Context-free MISKG and recipe-context Recipe1MSubs are distinct tasks. GISMo canonical-vocabulary and Senath raw/normalized identity counts are not interchangeable.
2. **Decision-Rule Scope:** A rejected candidate failed its recorded gate; post-hoc adjustments to thresholds would constitute exploratory curve-fitting, not confirmatory evidence.
3. **Artifact Availability:** The run summaries are present in project documents; primary protocol and output bundles are pinned under Protocol v1 SHA-256 (`b2952599...`).
4. **Rights and Transfer:** Dataset, image-mirror, and source-code rights are item-specific. Permission to conduct a recorded research run does not imply permission to redistribute raw data, images, model weights, or per-case outputs.
5. **Human Value:** None of these experiments measures whether cooks, scrollers, or creators want the corresponding product experience. Model metrics, runtime availability, and artifact audits are not human-usefulness evidence.

---

## 12.6 Synthesis and Cross-References

- **Validity Instrument & Sampling:** Chapter 4 v0.3 ([`04-instrument-and-identity-contract.md`](04-instrument-and-identity-contract.md)) and Chapter 6 v0.2 ([`06-human-validity-audit.md`](06-human-validity-audit.md)).
- **Clean-Room Inference Diagnostic:** Chapter 5 v0.4 ([`05-senath-clean-room-inference.md`](05-senath-clean-room-inference.md)).
- **Selective Guardrails & Conformal Calibration:** Chapter 7 v0.3 ([`07-guardrail-design-and-assumptions.md`](07-guardrail-design-and-assumptions.md)) and Chapter 8 v0.2 ([`08-allergen-methods.md`](08-allergen-methods.md)).
- **Compound Presence & Flavor Limitations:** Chapter 10 v0.2 ([`10-compound-presence-and-explanation.md`](10-compound-presence-and-explanation.md)).
- **System Demonstration & Fail-Closed Refusal:** Chapter 11 v0.3 ([`11-system-and-demonstration.md`](11-system-and-demonstration.md)).
- **Comprehensive Discussion:** Chapter 13 v0.2 ([`13-discussion-and-threats.md`](13-discussion-and-threats.md)).
- **Conclusion and Future Trajectory:** Chapter 14 v0.7 ([`14-conclusion-and-future-work.md`](14-conclusion-and-future-work.md)).
