# L42 S15 FoodGuardBench version and task-scope disposition

Date: 2026-11-06; decision owner: LEAD; risk: T1 source-relevance and claim-scope correction; no method or product-direction change.
Evidence ceiling: E1 primary-source inspection; author-reported experiment results, not independently reproduced

## Source and version reconciliation

The source is Luo et al., “Cooking Up Risks: Benchmarking and Reducing Food Safety Risks in Large Language Models,” arXiv:2604.01444. The arXiv record has v1 dated 2026-04-01 and current v3 dated 2026-10-13. The canonical strategy had carried v1's abstract claim of “3,339” FDA-grounded queries without a version pin. Do not present that count as the current benchmark size or silently erase the historical v1 figure.

The v3 full text describes 356 FDA-grounded seed policies; 2,124 malicious seed questions; eight jailbreak variants per seed (16,992); and 1,000 benign questions, for 20,116 total benchmark queries. Its unaugmented set is 3,124 (2,124 malicious seeds plus 1,000 benign). The seed-risk categories are contamination, preparation, temperature control, hygiene, and storage. The paper describes GPT-5 query generation/classification, semantic-similarity filtering, and researcher review of 100 random queries per category; this is not exhaustive human labeling of every item.

## What the evidence supports

The v3 abstract and §§3–5 report author-run tests of 12 LLMs and guardrails against this synthetic food-safety query set. It reports mean overall attack-success rate 25.51%, average per-model maximum across eight attacks 60.59%, general-purpose-guardrail average benign utility 96.25% and detection accuracy 55.80%, and FoodGuard-4B OOD accuracy 97.64%. The authors describe seed-disjoint train/validation/test partitions and five attack families held out for OOD testing; their own limitations say independently collected scenarios and unseen underlying policies remain untested, ordinary non-malicious unsafe advice is not directly evaluated, and an input guardrail's detection does not establish the factual correctness or real-world safety of downstream responses.

This is direct adjacent prior art for food-domain, FDA-grounded adversarial safety evaluation and a specialized input guardrail. It motivates testing food-specific safety failures, but does **not** supply an ingredient-substitution benchmark, an allergen-specific evaluation, a per-query allergen profile/filter, a benign substitution error rate, or evidence that ChefKix/Senath recommends allergens. The paper mentions severe allergies as an example of personalized risk and future work; that mention is not an evaluated allergen task. It cannot be used to claim that FoodGuardBench demonstrates allergen-containing substitutions or that general guardrails are universally “blind.”

## L42 / thesis disposition

- Keep S15 in the food-safety motivation and source funnel as adjacent prior art; label v1 and v3 counts by version.
- Replace the unqualified “3,339 queries demonstrate systematic guardrail failure” statement with the v3 threat-model-bound author result and limitations.
- Keep allergen substitution risk, allergy-response error evidence, adversarial food-handling guardrail evidence, and our proposed C3 route as separate claims/evidence streams.
- Frame the combined substitution failures and literature-wide explanation absence as questions to test, not established findings; keep the chapter plan's cited evidence task-distinct.
- This narrows an overbroad source attribution; it does not resolve L42's complete funnel, establish novelty, clear allergen safety, change the frozen protocol, or earn C1–C5 acceptance, score, or milestone credit.

## Challenge and review trigger

Strongest alternative: retain the v1 3,339 count as a valid historical v1 claim. Accepted only when explicitly version-labeled; it is not the v3 count. Revisit if an official later version changes the benchmark inventory or the strategy locates a distinct substitution/allergen study that directly measures the claimed behavior. Do not infer author intent or explain the v1→v3 count change beyond the visible versions and contents.

## Primary sources

- [arXiv record and version history](https://arxiv.org/abs/2604.01444) (v1 2026-04-01; v3 2026-10-13).
- [Current v3 full text](https://arxiv.org/pdf/2604.01444): §3.1–3.2/Table 1 and Figure 2 (taxonomy and construction); §5 and Table 3–4 (author-reported evaluation); §6/Limitations (scope and evidence ceiling).
- [Version 1 full text](https://arxiv.org/pdf/2604.01444v1): abstract, which reports the historical 3,339 figure and broader language.
