# Chapter 12 — Bounded Negative Results

**Working draft v0.2 — 2026-11-02**

**Evidence state:** bounded synthesis of selected negative, withdrawn, unavailable, and descriptive calibration results. It is not a complete landscape of project evidence. Chapter 7's recorded C3 calibration cells are descriptive; formal statistical coverage guarantees remain disabled, and campaign contribution accounting is separate from scientific validity. This chapter preserves those limits while connecting the tested SAG, context, CLIP, overlay, flavor-pairing, and conformal results.

**Use:** canonical methods account for falsified hypotheses, rejected model candidates, withdrawn historical artifacts, dated operational observations, and bounded abstention diagnostics.

---

## 12.1 Why Retain Negative Results

The IRON CHEF program tested several routes to food substitution and related food-AI capabilities. Some hypotheses failed their declared decision rules; one historical model artifact was withdrawn because it did not contain the claimed modality; and one historical product-endpoint probe recorded unavailability. In the specified conformal cells, the recorded implementation produced finite cutoffs for one seen-pair cohort and vacuous cutoffs for the tested unconditioned/unseen cohorts. These results do not establish a universal limit on transport across distribution shift.

Those outcomes are not interchangeable:
1. **A measured rejection** answers a bounded experimental question under a frozen decision rule.
2. **A withdrawn mock artifact** answers an evidence-integrity question regarding evidentiary validity.
3. **An unavailable endpoint** answers a dated operational question regarding pipeline completeness.
4. **Statistical non-support** answers whether an observational corpus exhibits a hypothesized causal association.
5. **Conformal vacuousness ($\hat{t} = \infty$)** describes the cutoffs observed in the specified unconditioned/unseen cells; it does not prove a universal reliability limit under distribution shift.

None of these outcomes establishes that the corresponding research or product destination is impossible. This chapter therefore reports what the recorded experiments did and did not support. It does not turn a failed model into a positive contribution by rhetoric, infer a universal law from one benchmark, or claim that an unmeasured capability has been disproven. Its defensible contribution is a transparent record of falsifiable hypotheses, explicit comparison rules, and the empirical boundaries exposed by testing them.

---

## 12.2 Evidence Boundary and Reporting Convention

The historical values below are transcribed from the active strategy and its historical execution ledger, cross-referenced with the independently recomputed benchmarks in Contributions C2, C3, and C5:

| Thread | Recorded Result and Decision | Scope That Must Remain Attached |
|---|---|---|
| **Corrected signal-adaptive gating (SAG)**, LEAD v38 | On context-free MISKG, mean Hit@1 was 0.1171% for SAG, 0.3964% for corrected semantic-only, and 0.3333% for corrected naive fusion. SAG's minimum seed score, 0.0270%, did not exceed the strongest comparator seed at 0.4595%; the frozen hypothesis was rejected. | This tests the corrected five-arm, three-seed MISKG ablation. It is not a recipe-context result, a novelty result, or evidence that every fusion/gating design fails. The earlier v11 causal interpretation remains confounded by missing-feature and identity problems. |
| **True-context dual encoder**, LEAD v42 | The recorded three-seed Recipe1MSubs experiment selected epoch 5 for each seed. Mean overall Hit@1 was 16.1223% versus GISMo's 20.6941%; unseen-pair MRR was 0.016601 versus 0.043084, and all three frozen gates failed. Candidate decision: reject. | A bounded tested architecture and protocol only. It does not show that recipe context is useless, that GISMo is optimal, or that no context-aware model can work. GISMo and Senath identity contracts differ; their denominators must not be mixed. |
| **Global flavor-pairing hypothesis**, LEAD v24 | On a 2,231,142-row RecipeNLG acquisition (1,905,776 eligible recipes, 372 FooDB profiles), the recorded primary test was not significant ($p = 0.238095 > \alpha = 0.05$); the stated greater-than-null hypothesis was unsupported (observed Jaccard 0.545990 vs matched null 0.527733). The current v20 row remains **VERIFIED / NEGATIVE** for this exact test; its reproduction receipt SHA-256 is `b2c80426fb51713f4ccdcda9bec9fbac98c4df8649d89f97d938b6019fe45005`. | RecipeNLG has no verified cuisine field in this record. The result is not evidence about sensory compatibility, culinary causation, or all flavor-pairing methods. The source record also limits reuse to noncommercial research/education. |
| **Behavioral exact-pair overlay**, LEAD v25 | The recorded full-catalog test gained $+0.00309213$ overall MRR but lost $−0.00212110$ unseen-pair MRR, crossing the predeclared $−0.002$ limit; the overlay was rejected. | The intervention learned from training substitutions is not a measure of user satisfaction. v34's later raw-feedback plumbing did not enable ranking and does not reverse this model decision. |
| **Recipes5k frozen-feature CLIP projection**, LEAD v41 | Across three seeds, image→text R@1 averaged 35.4193% versus 34.0996% for the pinned zero-shot baseline, a $+1.3197$ percentage-point gain. It missed the frozen $+2.00$ pp primary gate; R@5/R@10 slightly regressed. Decision: reject; weights were not installed. | This is a rank-64 residual projection over frozen features on one mirrored benchmark. Duplicate-clean sensitivity and per-seed gains do not override the primary gate. Mirror rights, external transfer, ingredient recognition, and serving remain open. |
| **Historical SmolVLM adapter**, LEAD v40 | The artifact audit found four mock text examples referencing two absent images; the formatter removed `<image>` and no image loader, collator, or pixel tensors were supplied. Training recorded one step, no evaluation or predictions, and an empty log. The current v20 row remains **VERIFIED / NEGATIVE** for withdrawal of this specific historical artifact; its reproduction receipt SHA-256 is `7e8c164e0fda2d4c7c1f24a17ea94a89fc7027dfbfbfee4d152192644fe7dea1`. | This is a failure of the historical artifact's evidentiary claim, not a trained multimodal model that failed a benchmark and not a falsification of food vision-language modeling. |
| **Voice-vision chain**, LEAD v49 | The historical audit recorded zero voice cases, no visible-food predictions, no generated-answer citations or labels, and 100/100 loopback endpoint requests unavailable. Earlier fabricated comparison metrics were removed. The current v20 R31 row is `UNVERIFIED / NA`; this historical observation is not a voice-specific or current-service result. | This is a dated artifact/probe boundary, not a present-day service probe, a voice-model evaluation, or proof that voice/vision is impossible. The bounded offline graph proxy is not the full product capability. |

Adjacent evidence is not itself a negative result: current v20 R18 is limited to the deterministic taxonomy partition of one hash-pinned OFF snapshot against a later taxonomy, and R19 to verification of the frozen 208-query/13-group protocol specification. Neither establishes clinical truth, allergen safety, model execution, or human adjudication; see their bounded receipts in `ai/evidence/claims-register/instantiated-r01-r45-v1/`.

---

## 12.3 Selective Reliability and Conformal Abstention Diagnostics

The historical LEAD v28 selective-reliability exploration tested score-based ranking and abstention behavior; it did not measure substitution safety. Its recorded outcome was a 2.06% overall AURC improvement against a 5% gate, worse unseen-pair AURC, and a selected zero-coverage test operating point (v28/v30 entries).

Chapter 7 v0.3 reports a 30-cell descriptive calibration design. The separate 208-query/13-group allergen protocol is not part of that calibration experiment, and neither record enables a formal statistical coverage guarantee:

1. **Recorded finite-cutoff cell:** On `GISMo_identity:seen` ($n=4,801$ calibration cases), the recorded nominal 70% cell ($\alpha=0.30$) has cutoff $\hat{t}=7$ and observed coverage of 71.44% on calibration, 71.67% on development, and 72.33% on the exposed test partition, with set size at most seven. This is descriptive output, not an approved formal guarantee, individual-case guarantee, or human/clinical acceptability result. At nominal 80% ($\alpha=0.20$), the cutoff is 12 and is labeled uninformative under the stated useful-size threshold.
2. **Recorded vacuous cells:** In the evaluated `Baseline-0:all` and unseen-pair cells, the two implementations reported $\hat{t}=\infty$ at the tested levels, with denominator preservation. This supports only the narrow statement that those recorded cells supplied no finite cutoff under this design and implementation. It does not prove that all models or all distribution shifts require abstention, nor does it establish a valid coverage guarantee:
   $$\hat{t} = \infty$$
   Under the recorded useful-set-size rule, a vacuous set is not an informative bounded recommendation; the system reports vacuity and abstains rather than implying a supported candidate. No clinical or culinary utility outcome was measured here.
3. **Bounded Conclusion:** For the recorded populations and tested implementation, the unconditioned cells were vacuous rather than informative. Honest abstention is the reported system response, not evidence of safety or usefulness. Smoothing scores or imputing profiles would be a separately specified method and cannot be described as retaining calibrated coverage without new justification and evaluation.

---

## 12.4 What the Experiments Collectively Establish

The strongest shared lesson across these negative results is procedural rather than universal: an appealing aggregate metric or a technically plausible extension is never sufficient to support a scientific claim.
- The corrected SAG test failed its seed-wise comparator rule.
- The true-context candidate failed all of its declared gates.
- The CLIP projection missed its minimum effect threshold.
- The exact-pair overlay's aggregate gain concealed a disallowed regression on unseen pairs.
- The recorded conformal diagnostics yielded vacuous cutoffs in the specified unconditioned cells; Chapter 7 leaves formal coverage guarantees disabled, so this is not a universal statement about models or distribution shift.

The taxonomy of non-positive outcomes requires precise vocabulary:
- **Tested and Rejected Candidates:** SAG, true-context dual encoder, CLIP projection, and exact-pair overlay.
- **Bounded Descriptive Calibration:** finite and vacuous cutoffs in the specified cohorts; this is not a general rejection of selective scoring.
- **Withdrawn Evidence:** Historical SmolVLM adapter (modality absent in training artifact).
- **Historical Unavailable Operational Probe:** Voice-vision chain loopback endpoint (current R31 remains `UNVERIFIED / NA`).
- **Negative Statistical Result:** Global flavor-pairing corpus association.
- **Bounded Descriptive Result:** Vacuous cutoffs in the specified unconditioned/unseen evaluation cells; formal coverage guarantees remain disabled.

These statuses must not be merged into a colloquial "AI failed" narrative. Taken together, they establish a reason to stop default investment in repeating these identical rejected variants without a changed falsifiable hypothesis, a new evidence basis, and a frozen evaluation boundary.

---

## 12.5 Interpretation and Threats to Validity

1. **Benchmark and Identity Scope:** Results are tied to their named dataset, split, candidate set, identity mapping, and denominator. Context-free MISKG and recipe-context Recipe1MSubs are distinct tasks. GISMo canonical-vocabulary and Senath raw/normalized identity counts are not interchangeable.
2. **Decision-Rule Scope:** A rejected candidate failed its recorded gate; post-hoc adjustments to thresholds would constitute exploratory curve-fitting, not confirmatory evidence.
3. **Artifact Availability:** Claim status is versioned separately in the R01–R45 v20 register. A receipt that reproduces a protocol specification (such as R19) is not a model-run output, and a chapter summary is not a substitute for the pinned primary artifact. Availability and verification must be stated per artifact; this chapter does not imply that every underlying bundle is present in a shared checkout.
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
