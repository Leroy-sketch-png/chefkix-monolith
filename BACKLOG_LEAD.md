# IRON CHEF v4: LEAD BACKLOG
> **AUTHORITY:** `academic_vision_and_strategy.md` (canonical SSoT) is the single source of truth. This backlog is derived from §3 (C1–C5), §17 (Work Packages), §18 (Schedule), §19 (Risks), and §16 (Protocol Resolutions). When they conflict, the strategy file wins.
> **MIGRATION:** Updated from v3 on 2026-10-12 (LEAD v113). v3 "four trained models" and "nine contributions" structure retired. See Appendix Y of SSoT for superseded v3 text.

## LEAD v236 — L46 Q12 cross-source vocabulary currentness (2026-10-20)

- Active SSoT §22 refreshes Q12 as a bounded candidate: the historical v23 matrix reports 467/6,632 non-special classes and 57,641/141,040 endpoint occurrences with at least two source families after collapsing FlavorDB/FlavorGraph; exact names are not semantic or food-form identity, and 54 ambiguous classes remain unpromoted.
- Fresh isolated validator run failed on source drift: offline-retrainer SHA in the frozen summary is `5762096e…9941`, while the current source is `1111dd71…0591`. Do not repeat the historical PASS as a fresh current validation. L46 is 9/24 candidates refreshed; 15 unrefreshed, zero rehearsals; R43 UNVERIFIED.
- Receipt: `../ai/out/iron-chef-lead-v236/q12_defense_answer_currentness.json` (SHA-256 `5db088b3e2fba5c4aaf814520faa4fda44ee00fcb616a0e2a44fc432f0fa40de`). No claim promotion, model-quality assertion, experiment, Kaggle action, or thesis percentage.

## LEAD v235 — L46 Q05 reciprocal-edge answer refresh (2026-10-20)

- SSoT §22 now gives an unrehearsed, evidence-bounded Q05 candidate: reciprocal grouping prevents direct reverse-direction exposure by design; preserved v11 report/log record the rule but do not independently reconstruct implementation or quantify metric inflation. No claim of eliminating all leakage.
- L46: 8/24 answer candidates refreshed, 16 unrefreshed, zero human rehearsals; R43 remains UNVERIFIED. Historical v27 pack is unchanged; no experiment, model-quality claim, Kaggle action, or thesis percentage.
- Receipt: `../ai/out/iron-chef-lead-v235/q05_defense_answer_refresh.json` (SHA-256 `2229b454bf2a77cad7e17d2ebee58c4f3a9068dfb8db69d1d45cf256d530dc9e`).

## LEAD v234 — L44 Segment-10/11 local artifacts rechecked; source gate still blocked (2026-10-20)

- Fresh offline verification passes the preserved Segment-10/11 manifests, 12-file checkpoint and eight-file retained-best hash inventories, pinned ancestry from Segment 9 through 11, frozen scientific/data-selection hashes, and the exact step-187/374/561 evaluation history. Trainer states are at 631 and 701; neither boundary adds an evaluation. Each manifest reports ~8.1h training runtime, but training/runtime loss is not a thesis outcome; the best recorded eval remains step 187.
- This does not resolve the one nonzero Segment-10 ancestry fetch or certify immutable Segment-11 version-1 source bytes. The source pull was not version-addressed and differs from the launch hashes. Keep outputs unpromoted; no retry or later segment.
- Receipt: `../ai/out/iron-chef-lead-v234/stage3_segment10_11_local_revalidation.json` (SHA-256 `B1B4EDAA069CD63748955A7EBEB990147EDBD5F0FD2505B228047276B26E8F57`). Source authority: SSoT §1.1 and SESSION v234.

## LEAD v233 — L46 Q03 GISMo answer-currentness correction (2026-10-20)

- Refreshed Q03 as candidate wording: GISMo is a historical comparator, not an accepted best/deployment-ready model. Current R01 remains PARTIAL because the stored run has a diagnostic-only provenance warning and no accepted independent reproduction; do not present its metrics as accepted reproduction or quality evidence.
- The historical v27 defense pack remains unchanged. L46 now has 7/24 answer drafts refreshed as candidates, 17 unrefreshed, and zero human rehearsals recorded. R43 remains UNVERIFIED; no inference, rerun, thesis percentage, or queue change.
- Receipt: `ai/out/iron-chef-lead-v233/q03_defense_answer_refresh.json` (SHA-256 `B507054A62EBE177C4037E4685455E9F010EBA51FA0A3D976C969EDA94E6BEBE`); source boundary: canonical SSoT §§15.1, 17, 22.

## LEAD v232 — L48 R26 bounded artifact-claim reconstruction (2026-10-20)

- Canonical SSoT §15.2 now points to claims derivative v185: 45 rows, 18 VERIFIED / 8 PARTIAL / 19 UNVERIFIED. R26 is VERIFIED only for the fresh recursive Segment-9 step-561 artifact boundary and the pre-existing Segment-7 positive version-1 launch receipt. These are register counts, not thesis progress.
- Segment 11 remains unpromoted with exact source provenance blocked; this update does not certify inference, scoring, model quality, or overall Stage-3 completion. Q124 stays frozen; L48 remains PARTIAL/open.
- Evidence: `ai/out/iron-chef-lead-v232/R26-reconstruction.json` (SHA-256 `db0159b4150ccb223df64156c564094aa1864054a778345cc4990f1140410c7f`); v185 register SHA-256 `8eac99ef54e0752b6cf2ed6e7938553a47e4d3aa18e4c7c8cd94cd0373a53a62`; source authority SSoT §§1.1, 15.2, 17 and SESSION v232.

## LEAD v231 — Segment-11 artifact chain/source provenance split (2026-10-20)

- Fresh official Kaggle status remains `COMPLETE`. Local Segment-9 recursive validation and Segment-10/11 manifest ancestry, step/evaluation contract, 12-file checkpoint hashes, and eight-file retained-best hashes all pass. This upgrades the local artifact-chain evidence boundary, not the source launch-provenance boundary.
- One official current-source pull is not version-addressed and its script/metadata hashes differ from the recorded version-1 launch pins. It cannot establish whether the immutable launched package matched; keep Segment-10/11 unpromoted and the exact source-package gate BLOCKED. The prior nonzero Segment-10 output request is not retried; no successor is prepared/launched.
- No inference, scoring, model quality, overall Stage-3 completion, or thesis percentage is established. Source authority: SSoT §1.1 / SESSION v231; local blocker receipt `ai/out/iron-chef-lead-v99/secondary_stage3_segment11_blocked.json`.

## LEAD v227 — R43 Q04 denominator qualification (2026-10-20)

- The reproduced GISMo `1.206%` Hit@1 / `0.04308` MRR applies to GISMo-canonical pair identity (6,932 repeated / 3,815 unseen), not the distinct raw-normalized/Senath-sensitivity split (6,866 / 3,881; 66 assignments differ). The historical pack remains unchanged; active SSoT wording now names the identity rule.
- Q04 candidate answer only; no inference/evaluation rerun, accepted-model claim, or L13 closure. R43 remains UNVERIFIED pending full answer review and human rehearsal. Receipt: `ai/out/iron-chef-lead-v224/q04_defense_answer_refresh.json`, SHA-256 `CA4ED8BD44CB1169251E12F121F104D99C5CACCC177CFAD2A66B2CA78257BD87`.

## LEAD v228 — R43 Q09 feedback-path currentness (2026-10-20)

- Q09’s old “no consumer / separate non-idempotent pair overlay” description is stale. Current source has a signed-candidate, owned-session Kafka producer and a validating/idempotent consumer projecting to authenticated raw evidence.
- Do not call this production behavioral learning: representative consented events, user outcomes, production certification, and accepted scorer integration remain unestablished; ranking remains disconnected. Candidate answer only; no claim promotion or product change. Receipt: `ai/out/iron-chef-lead-v224/q09_defense_answer_refresh.json`, SHA-256 `D111884B816CA58B23111850BEA802A017D074BF1F4D44D5C08E98F0F79855F3`.

## LEAD v229 — R43 Q01 contribution acceptance boundary (2026-10-20)

- The evidence-bound measurement/robustness study is the decided intended direction, not a completed or professor/committee-accepted contribution. Canonical C1–C5 gates remain active; a losing model neither proves nor invalidates the study itself.
- Q01 candidate wording only. No thesis result or contribution is promoted; R43 remains UNVERIFIED. Receipt: `ai/out/iron-chef-lead-v224/q01_defense_answer_refresh.json`, SHA-256 `58CFFA59E34A57E5F0919B57995D2719C509B48DA162083E55AA5601D490FA48`.

## LEAD v230 — R43 Q02 novelty boundary (2026-10-20)

- The existing source record rejects broad recipe-context substitution/constraint-repair novelty, while exact overlap with the selective Recipe1MSubs estimand remains UNKNOWN pending exhaustive screening and independent review; do not claim “first.”
- Candidate wording only: no fresh literature search, novelty/contribution promotion, or rehearsal. Historical v27 is unchanged and R43 remains UNVERIFIED. Receipt: `ai/out/iron-chef-lead-v224/q02_defense_answer_refresh.json`, SHA-256 `AB4EA569A6C2F177B63396A114C156C99C4904B7F348F6BECFACA72211E6D03E`.

## LEAD v224 — R43 defense-pack currentness gate (2026-10-20)

- Focused temporal-currentness audit of the preserved v27 defense pack found Q07 still says the v26 selective-reliability candidate is active; the current SSoT records that frozen candidate as rejected. Q16's “no configured credentials” is time-sensitive and was not reverified.
- The old pack's PASS checks structural completeness/mutations, not current-answer truth or oral performance. Preserve v27 unchanged; do not present it as current. Refresh all 24 answers and evidence pointers against SSoT §1/§15.2/§17 and record a separate human rehearsal before counting R43 evidence. R43 remains UNVERIFIED.
- Receipt `ai/out/iron-chef-lead-v224/defense_pack_currentness_audit.json`, SHA-256 `16DFE00273621C0AB7FA05EED1808F53484D8EA227BFE088FFC60AF97BBBB326`. No claim, protocol, experiment, training state, or thesis percentage changed.

## LEAD v225 — R43 Q07 evidence-bound answer refresh (2026-10-20)

- Updated only Q07's candidate wording from the hash-bound v26 result: calibration improved ECE/Brier, but the selective-reliability candidate missed the 5% AURC gate (2.06%), worsened unseen AURC, and retained zero cases at the frozen Wilson-risk ceiling; it was rejected. This rejects the tested candidate, not calibration in general.
- The answer remains unrehearsed; 23 other questions are not refreshed. R43 remains UNVERIFIED pending a full pack/evidence-pointer update and human rehearsal. Receipt `ai/out/iron-chef-lead-v224/q07_defense_answer_refresh.json`, SHA-256 `37F0614ED178FA8117096C27FC374E5A99E89825D484FC94A7FA0204130AF34F`.

## LEAD v226 — R43 Q16 matched-baseline answer boundary (2026-10-20)

- Replaced the inherited global “no credentials/no executions” wording with a bounded statement: preserved v39 is Mistral-only, with 208/208 raw cells and reviewer packs ready but unreviewed. It supports absence of a matched GPT/Gemini arm from that frozen run only.
- Current credential state and separate later runs were not checked. No secret access, new calls, comparison, adjudication, or safety claim. Candidate answer only; 22 other answers are unrefreshed, no oral rehearsal is recorded, and R43 remains UNVERIFIED.
- Receipt `ai/out/iron-chef-lead-v224/q16_defense_answer_refresh.json`, SHA-256 `B22F34663FFCC7E84369F42F146F0A2BB780CB031E3D7CEC9AEC9C3E219DE39F`.

## LEAD v223 — L42 Oulu food-recommender safety/judge abstract lead (2026-10-20)

- Added C:S59 from the official OuluREPO record/abstract: a 2026 MSc thesis describes a three-judge downstream safety auditor for RAG food recommendations, with and without recommender explanations. Its abstract reports 100 recommendations and recall/HIER/F1 results; author-reported, not reproduced. The PDF endpoint returned a Cloudflare managed challenge; no bypass, local PDF/hash or full-text review.
- This is adjacent judge/safety prior art, not fixed-recipe substitution or the frozen selective-prediction estimand. Do not treat the deterministic rule-engine reference as clinical or universal truth. C:S59 is outside v219's immutable 0/4/1 five-candidate overlay. No claim/protocol/training/experiment/percentage change; L42 remains open.
- Receipt `ai/out/iron-chef-lead-v223/oulu_verified_personalized_nutrition_abstract.json`, SHA-256 `1DD92F11D6E69B28EF7FC315B0D1B15B57FF494BDD762092D737E4B7B4B43037`. See SSoT C:S59/§4/§7 and SCOPE/SESSION/implementation-plan v223.

## LEAD v222 — L42 SHARS thesis repository-route correction (2026-10-20)

- Corrected v219's use of a Theseus 403 for the University of Oulu SHARS thesis: Theseus is the wrong repository route; use OuluREPO/Jultika or OULA. OuluREPO says it contains openly published University theses and notes that some materials are workstation-restricted.
- Exact-title/author indexing returned no match; direct OuluREPO and OULA catalog API requests returned 403. This is not evidence of thesis absence or contents. SHARS remains unresolved pending primary-source content and independent review; unrelated same-name records are excluded.
- Receipt `ai/out/iron-chef-lead-v222/shars_repository_route_recheck.json`, SHA-256 `FF20CE1FC16D3071DCA52F6479197C6F3AE3195E8432D28C3072089F6EAECEB3`. No change to v219's 0/4/1 overlay, claims, novelty, experiments, training, or thesis-progress percentage. See SSoT C:S55/§4 and SCOPE/SESSION/implementation-plan v222.

## LEAD v221 — L42 AI2Cuisine direct-substitution boundary (2026-10-20)

- Added C:S58 after screening the official ScienceDirect full-text section 5.2.3: AI2Cuisine retrieves replacement food items for a specified recipe ingredient that violates dietary restrictions, filters candidates, and lets the user choose while adapting instructions. Direct task prior art; not evidence of selective prediction or safety.
- Author overlap with existing AdaptaFood source is one related line, not independent replication. UGR PDF returned HTTP 502; no local hash or reproduction. Updated L42's adjudication gate and SSoT §4; v219 overlay counts unchanged. Exact selective overlap remains UNKNOWN.
- Receipt `ai/out/iron-chef-lead-v221/ai2cuisine_direct_substitution.json` SHA-256 `AC1AC3C2ABAD129ABE2C4012579E1DDAA62780EED86D1D1A6A30E3643FA650D1`. No claim/experiment/training/percentage change.

## LEAD v220 — L42 HeaSE meal-alternative retrieval boundary (2026-10-20)

- Added C:S56–C:S57: HeaSE retrieves and ranks alternative meals/recipes from an input recipe, with GPT-3.5 Turbo selection in the full-text workshop paper; this is adjacent meal-level recommendation, not replacement of a specified ingredient in a fixed recipe. The RecSys abstract and STAI workshop paper are one author/team framework lineage, not independent replications. The University of Bari repository provides no associated file and warns its displayed record is not institutionally validated; the full text is available from CEUR.
- Receipt `ai/out/iron-chef-lead-v220/hease_adjacent_prior_art.json` SHA-256 `B4EB7E41D5174016ABC7D10D55863FD22DD6009A0C29E142B3823F8A28BA9C98`. v219 five-candidate counts remain unchanged; no novelty, claim, experiment, training, or thesis-progress change. L42 remains open.

## LEAD v219 — L42 candidate source-level recoding (2026-10-20)

- Added a version-pinned overlay for the five heuristic v214 candidates without mutating that run. Under an explicit fixed-recipe substitution rubric: zero direct, four adjacent (recipe formulation; publisher-deposited abstract for a dietary-review paper; pantry-to-whole-recipe retrieval; multimodal recipe recommendation), and one unresolved SHARS lead. Oulu's official profile confirms thesis identity but not task details; the Theseus record returned 403. The MDPI page also returned 403, but its Crossref publisher-deposited abstract supports adjacent review coding only.
- Evidence overlay `ai/out/iron-chef-lead-v219/source_level_coding.json` SHA-256 `5DB0247C39B5EB259905084472E4D8C6A4AAB4ABD2C4ED1E8976F79ED0B91FA2`; source-level limits are in SSoT §4/C:S48–S55 and SESSION/implementation-plan/SCOPE v219. This does not close L42: second-review adjudication, broader screening, and exact novelty remain open/unknown. No claim promotion, experiment, training-state change, or thesis percentage.

## LEAD v218 — L42 direct-substitution lineage/full-text boundary (2026-10-20)

- Added official full-text evidence for Pacifico, Britto & Ludermir's KDMiLe 2022 context-aware single-ingredient replacement method (AMG destination-context clustering; OPM source-recipe co-occurrence; PDF SHA-256 `0EA8052431B49B4B2FCE4F99B544C6C1D4834ADB423EA6DADCB538A305A71D4D`) and the official WebMedia 2021 record for allergy-directed recipe-context replacement. Together with their BRACIS 2022 chapter these are one author research line, not independent replications.
- The KDMiLe paper's screened evaluation is qualitative/small and uses trial-and-error settings; none of these reports is reproduced or evidence of clinically safe substitution. C:S50 remains adjacent pantry-to-whole-recipe retrieval.
- SSoT §4/C:S51–S52 now strengthens the direct-prior-art boundary; broad task novelty is rejected while exact selective unseen-pair/evidence-missingness/provenance-abstention overlap remains UNKNOWN. Canonical §17 makes the remaining correction gate explicit: new version-pinned screen, reproducible DIRECT/ADJACENT rubric, lineage/evidence-ceiling coding, and independent second review. L42 remains open; no claim, experiment, protocol, queue/training state, or thesis percentage changed. v214 outputs unchanged; 15h estimate unchanged.
- Sources: official [KDMiLe PDF](https://sol.sbc.org.br/index.php/kdmile/article/download/24972/24793), [WebMedia record](https://sol.sbc.org.br/index.php/webmedia/article/view/17483), SCOPE/SESSION/implementation-plan v218.

## LEAD v217 — L42 source-level substitution/retrieval boundary (2026-10-20)

- A primary-source screen separates Pacifico et al. (2022), direct prior art for automatic single-ingredient substitution (Springer abstract; full chapter gated), from Ashraf & Maduranga (2025), ingredient-only pantry-to-whole-recipe retrieval. The 2025 source cites the 2022 chapter; citation relationship is not independent replication.
- The 2025 publisher-view text was screened, but its direct local PDF route hit a JavaScript/Cloudflare challenge; no local hash or bypass. Author-reported metrics are not verified/reproduced. v214 candidate artifacts remain immutable; label correction must use a new versioned screen.
- SSoT §4/C:S49–S50 now narrows broad task novelty while leaving exact selective unseen-pair/evidence-missingness/provenance-abstention novelty UNKNOWN. L42, full funnel, formal coding, and independent adjudication remain open. No claims, experiments, training/queue state, or thesis percentage changed.
- Sources: official [Springer chapter record](https://link.springer.com/chapter/10.1007/978-3-031-21689-3_27), [journal article record](https://faith.futuretechsci.org/index.php/FAITH/article/view/98?articlesBySimilarityPage=4), SCOPE/SESSION/implementation-plan v217.

## LEAD v216 — L48 current-register label synchronization (2026-10-20)

- Corrected the SSoT's v183 subsection label from “Current register” to “Historical register (superseded by v184)”; the v184 pointer/counts were already canonical. L48 remains open for claim-level provenance/reproduction and mapping.
- Fresh structural validation of the unchanged v184 CSV passes: 45 unique rows, local paths/hashes matched, 17 VERIFIED / 8 PARTIAL / 20 UNVERIFIED; eight focused tests pass. These are register counts, not thesis progress, and validation does not reproduce claims.
- No claim, experiment, protocol, queue/training state, checkpoint, or thesis percentage changed. Source: SSoT §15.2, SESSION/SCOPE/implementation-plan v216.

## LEAD v215 — L42 Ananno thesis/article lineage bounded (2026-10-20)

- Official Linköping DiVA records identify the v214 heuristic's Ananno `DIRECT` hit as a doctoral thesis with articles and list the already-recorded Ananno Applied Food Research recipe-design paper (C:S40). Count one research line, not independent replications. Its abstract supports many-objective recipe configuration/dynamic substitution, not the frozen selective-substitution estimand.
- The thesis defense is scheduled for Nov 13. Direct PDF retrieval timed out; DOI/publisher access encountered the repository JS gate. No full-text screen, local PDF hash, or novelty conclusion. L42 remains open/UNKNOWN; no claim, experiment, queue/training state, or thesis percentage changed. See SSoT §4/C:S48, SCOPE/SESSION v215, and implementation-plan v215.

## LEAD v214 — L42 bounded search snapshot refreshed (2026-10-20)

- Refreshed the 20-query metadata/abstract expansion through Oct 20. The isolated snapshot has 4,121 raw records and 3,120 deduplicated records; 42/60 responses were pinned, with 18 Semantic Scholar HTTP 429 failures. No retries were made; every query met the two-index floor through OpenAlex/Crossref.
- Six focused tests and raw-response/corpus/screen replay pass. Five heuristic candidates remain, with candidate-file SHA-256 identical to v199; no additional candidate surfaced. This is not proof of search completeness or novelty: metadata-only screening, provider limits, 100-result query caps, and 429s remain.
- L42 remains open and exact novelty UNKNOWN. No claim, experiment, protocol, queue/training state, or thesis percentage changed. Evidence digests and details: SCOPE v214, SSoT §4, SESSION v214, and implementation-plan v214.

## LEAD v213 — L49 H4 questionnaire desk preparation (2026-10-20)

- SSoT §14.2 now contains a candidate question/analysis skeleton for advisor and institutional review; L49 remains **PROPOSED / NOT READY**. The draft is explicitly not frozen and must not be sent to participants or treated as ethics/consent approval.
- No stimuli were invented or selected. No participant contact, recruitment, consent, data collection, protocol freeze, or R15 status change occurred. PR13, workload, privacy/data handling, recruitment wording, assignment, estimand, uncertainty, missingness, and harmful-over-reading gates remain open.
- Source: SSoT §§14, 16–17, SESSION v213, and SCOPE v213. No queue/training or thesis-percentage change.

## LEAD v212 — Claims-register structural revalidation (2026-10-20)

- Re-ran the fail-closed validator on the unchanged v184 claims CSV: 45 unique rows, all local paths/hashes matched, and the existing 17 VERIFIED / 8 PARTIAL / 20 UNVERIFIED distribution reproduced. Eight focused validator tests pass.
- This resolves only the structural/local-reference validation subtask. It does not reproduce claims, authorize status promotion, or close L48: row-level source/provenance review, independent reproduction, citation separation, and full mapping remain open. L48 is not a thesis-progress percentage.
- AoE also corrected an extra-character typo in the previously documented CSV digest; active references now use the measured hash below. The pre-check backup preserves the earlier document snapshot.
- Evidence: CSV SHA-256 `8853e4ab046c1351d5bab0c252bcb1c709aee6a5c2119ffa4461b34b5b329e6a`; validator SHA-256 `e0cc93f27755a230df2bce01cfece4b47735e429d274c404b621d84417a42009`; test file SHA-256 `8d720ba3fdeca81b1f5a61599d3ef7dcfaf40be65dd5892d25f7fb14140ef131`. See SSoT §15, SESSION v212, and SCOPE v212.

## LEAD v211 — H4 study tracking row and authority synchronization (2026-10-20)

- Added derived L49 from SSoT §17.1 for the open R15/H4 human usefulness and comprehension study. Status is **PROPOSED / NOT READY**: this is a work-tracking item, not approval to contact/recruit participants, collect data, or claim human-value evidence.
- Kept L41 and PR13 as prerequisites, not as study outcomes. Before participant activity, require the written institutional classification/clearance and consent process, approved privacy/data/retention/withdrawal/compensation terms, measured workload and reviewed recruitment wording, and a prospectively frozen 12-item counterbalanced protocol and analysis. Acceptance requires coded participant-level outcomes and uncertainty for usefulness, comprehension, confidence, harmful over-reading, missingness, comments, and inconclusive results; preserve the canonical ≥20-adult floor without an unsupported power claim. R15 remains UNVERIFIED until §15's evidence process accepts results.
- Reconciled the preserved W5/Nov 10 "recruited (or dropped)" schedule wording as a historical proposal only: it cannot trigger recruitment or an automatic scope cut. K11 remains subject to human scope decision; H4 preparation may continue independently while external gates are unresolved. No study, participant activity, protocol freeze, claim promotion, queue change, or thesis percentage is asserted.
- Sources: SSoT §§14–18 and R15; SCOPE v205/v211; SESSION v211. This is a derived-backlog synchronization, not a new research finding.

## LEAD v203 — Segment-11 and claims-register row synchronization (2026-10-20)

- Corrected derived L44 to distinguish the validated Segment-9 boundary at step 561 from Segment-11's COMPLETE status and locally hash-matching, unpromoted staging tree. Full Segment-10/11 ancestry/source validation remains uncertified after the preserved nonzero output request; no retry or successor is authorized.
- Corrected L48's current claims-register pointer from v181 (16 VERIFIED / 6 PARTIAL / 23 UNVERIFIED) to the canonical v184 snapshot (17 VERIFIED / 8 PARTIAL / 20 UNVERIFIED). The counts describe the register, not thesis completion.
- Source authority remains SSoT §§1.1, 15.1–15.2, and 17; blocker receipt: `ai/out/iron-chef-lead-v99/secondary_stage3_segment11_blocked.json`. This is a derived-document synchronization only: no checkpoint promotion, queue/experiment change, inference/quality claim, or thesis percentage.

## LEAD v204 — Schedule and risk-response authority resynchronization (2026-10-20)

- Preserved the original v4.2 schedule as a planning snapshot and added a dated evidence/status overlay. Elapsed target dates are not converted into `MISSED` or `COMPLETE` without primary evidence; Freeze A is not ready while PR01–PR03 remain unresolved.
- Reframed K1–K22 mitigations as candidate responses with authority conditions. No sample, protocol, model, scope, ethics, cost, or recruitment fallback is auto-triggered by a risk signal.
- Source authority: SSoT §§14, 16, 18–19 and Appendix D; SCOPE v207–v208 and SESSION v204. No new experiment or participant activity was authorized, no study outcome inferred, and no queue or thesis-progress status changed.

## LEAD v202 — L42 conformal-group recommender journal-lineage update (2026-10-16)

- The official Elsevier listing now records a same-titled journal article (DOI `10.1016/j.engappai.2026.115914`, published 2026-10-15) with an additional listed author and an abstract similar to arXiv v1. Same-work lineage is inferred, not explicitly linked by a version relation.
- The publisher page returned 403; no journal full text or proof was reviewed. Preserve the v201 proof-gap assessment as arXiv-v1-specific; journal proof remains UNKNOWN. This strengthens generic conformal group recommendation as published prior art, not ingredient-substitution evidence. Count one lineage, not two replications; no claims, experiments, queue/training state, or thesis percentage changed. See SCOPE v202, SESSION v202, implementation-plan v202, SSoT §4/C:S47. No files staged, committed, or pushed.

## LEAD v201 — L42 conformal group-recommender proof boundary (2026-10-16)

- Integrated the version-pinned full-text screen of *Conformal Group Recommender System* (arXiv:2307.12034v1) into canonical SSoT §4/C:S46. It is generic group-recommender prior art with candidate-wise conformal values averaged into a thresholded set; experiments use eight movie-rating datasets.
- Its Lemma 3 proof does not establish super-uniformity of the average from marginal validity alone. The independent-uniform counterexample identifies a general inference gap, not failure of this specific dependent construction. The guarantee is unresolved and the procedure is not adopted. This narrows generic conformal group recommendation claims only; no food-substitution novelty, experiment, queue/training state, or thesis percentage changed. See SCOPE v201, SESSION v201, and implementation-plan v201. Evidence/hash and caveats are in the canonical SSoT; no files staged, committed, or pushed.

## LEAD v200 — L42 FlavourBench prior-art boundary (2026-10-16)

- Integrated the full-text screen of arXiv v3 into canonical SSoT §4/C:S45: executable Epicure reward-map scoring (27 endpoints/534 tasks), a separate anchor-disjoint reward-map SFT transfer result, and a separate Recipe1MSubs pair-level validation. The author-reported results are not reproduced; the source explicitly limits its reward map from universal human taste and notes unrecovered original runtime training provenance plus Recipe1M ancestry overlap.
- This narrows broad absence claims for culinary scoring, reward-map transfer, and pair-level Recipe1MSubs validation. It does not settle the exact selective/missing-evidence/provenance estimand or establish human outcomes/safety. No novelty conclusion, claim promotion, experiment, queue/training state, or thesis percentage changed. See SCOPE v200, SESSION v200, implementation-plan v200, and SSoT §4/C:S45.

## LEAD v199 — Segment-11 state reconciliation (2026-10-16)

- Reconciled this derived backlog's current-evidence table to canonical SSoT §1.1 / SESSION v188. Segment 11 is recorded `COMPLETE` at step 701, and its staged manifest plus 12 checkpoint/eight retained-best file hashes match locally; the tree remains unpromoted because the Segment-10 ancestry output request exited 1 and full source/provenance validation is uncertified. No retry or successor is authorized by the recorded blocker.
- This is a documentation consistency repair only. It does not certify the checkpoint, inference, scoring, quality, or Stage-3 completion; it does not change the frozen execution queue or thesis status. See the durable blocker receipt `../ai/out/iron-chef-lead-v99/secondary_stage3_segment11_blocked.json`.

## L42 Bikakis et al. full-text prior-art screen — LEAD v197 (2026-10-16)

- Retrieved and screened the 39-page open-access article from UCL Discovery. It formalizes role/embedding/operational distance measures, thresholded primary candidates, primary/secondary substitutions, integrity-constraint repair/minimal inconsistencies, and four substitution drivers. The article states no data was used; this is conceptual/formal work, not empirical benchmark or calibrated selective-prediction evidence. Allergy-related constraints and human mediation are discussed, not validated as a safety protocol.
- Broad novelty based on computational cooking substitution, thresholded candidates, secondary recipe repair, or those goal framings is rejected. The paper does not itself settle frozen unseen-pair/evidence-missingness, calibration/risk-coverage, or provenance-abstention overlap. No reproduction, novelty acceptance, experiment, queue/training state, or thesis-percentage change. See SSoT §4/C:S44, root SCOPE v197, SESSION v197; PDF SHA-256 `AD0D52215F464B21E723F10590E708B279067FB197826FFFFA5D0D5D86444BA1` in ignored `ai/out/l42-bikakis-literature-audit-20261016/`. [UCL record](https://discovery.ucl.ac.uk/id/eprint/10220617/).

## L42 UCL record-level verification of existing Bikakis prior-art entry — LEAD v196 (2026-10-16)

- UCL Discovery's official record/abstract confirms bibliographic identity and summary for Bikakis et al., “A commonsense reasoning framework for substitution in cooking,” *Data & Knowledge Engineering* 163 (2026), Article 102558, DOI `10.1016/j.datak.2026.102558`. This work was already recorded in the v9 execution update and existing broad-novelty boundary; this is a record-level source reconciliation, not a new discovery. Abstract-level summary: ingredient/step similarity distances and logical recipe-integrity inconsistency detection/mitigation.
- Not a full-text screen; no PDF bytes/hash or methods/results review. Selective-prediction, frozen unseen-pair, evidence-missingness, risk/coverage and provenance-bearing abstention coverage remain `UNKNOWN`, not absent. No novelty-disposition, experiment, queue/training-state, or thesis-percentage change. See SSoT §4/C:S43, root SCOPE v196, SESSION v196; [UCL record](https://discovery.ucl.ac.uk/id/eprint/10220617/).

## L42 Crossref journal-filtered author trace — LEAD v195 (2026-10-16)

- A read-only Crossref query constrained to *Knowledge-Based Systems* plus fuzzy author `Jonghun Gim` returned one 2011 workflow article, not NutriHeal; the separate `NutriHeal` title-filter request returned HTTP 429 and was not retried.
- The result exposes fuzzy author-match noise; the rate-limited title query yields no result. Neither establishes no submission/rejection or manuscript identity. Keep exact venue/version link `UNKNOWN`; no novelty, experiment, claims status, queue/training state, or thesis percentage changed. See SSoT §4/C:S37, SCOPE v195, SESSION v195, and [Crossref filtered author query](https://api.crossref.org/works?query.author=Jonghun%20Gim&filter=container-title%3AKnowledge-Based%20Systems&rows=20&select=DOI%2Ctitle%2Ctype%2Cpublished%2Cauthor%2Ccontainer-title); ignored query note.

## L42 publisher-search access boundary — LEAD v194 (2026-10-16)

- Direct opens of two ScienceDirect search routes (`NutriHeal-Graph`, `NutriHeal`) returned HTTP 403 in the configured research browser. No publisher-side result set/article content was obtained; these were search URLs, not a confirmed article page.
- This is an access limitation, not evidence of submission absence, rejection, or distinctness from SSRN 6928681. Keep exact manuscript/venue/version linkage `UNKNOWN`; no novelty, claim status, experiment, queue/training state, or thesis percentage changed. See SSoT §4/C:S37, SCOPE v194, SESSION v194, and the [ScienceDirect search route](https://www.sciencedirect.com/search?qs=NutriHeal-Graph).

## L42 KBS-submission claim provenance — LEAD v193 (2026-10-16)

- Zenodo record 20091591's public description says the dataset supports a paper “submitted to Knowledge-Based Systems.” Its only related-work link is to the prior dataset version `10.5281/zenodo.20050999`; it lists no manuscript DOI/publisher/submission evidence. README author/year are placeholders.
- This confirms the uploader claim is in the deposit metadata, not that submission/publication occurred or that it identifies SSRN 6928681. Keep exact manuscript and venue/version linkage `UNKNOWN`; no independent paper/corroboration, novelty score, experiment, queue/training state, or thesis percentage changed. See SSoT §4/C:S37, SCOPE v193, SESSION v193, and [Zenodo record 20091591](https://zenodo.org/records/20091591).

## L42 manuscript-title trace — LEAD v192 (2026-10-16)

- A bounded exact-title Crossref query returned SSRN 6928681 among eight results but not the Zenodo README's exact *Knowledge-Based Systems* title. A second fuzzy author query returned 100 records; a local filter on exact displayed author name or `NutriHeal` surfaced the related KCI article, SSRN item, and one unrelated matching displayed author name; none is the README title/KBS paper in this bounded result set. A bounded OpenAlex title/abstract query returned zero works.
- Catalog non-retrieval is not proof of submission absence or distinct manuscripts; a name match does not establish author identity. Keep title/venue/version linkage `UNKNOWN`; no independent paper or corroboration is counted. No novelty, score, experiment, queue/training, or thesis-percentage claim changed. See SSoT §4/C:S37, SCOPE v192, SESSION v192, and the query note in ignored `ai/out/l42-zenodo-small-artifact-audit-20261016/CROSSREF.md`.

## L42 source-access follow-up — LEAD v191 (2026-10-16)

- Downloaded only the README and two leakage/exclusion manifests from Zenodo record 20091591; official MD5 checks match and local SHA-256 receipts/content notes are in ignored `ai/out/l42-zenodo-small-artifact-audit-20261016/AUDIT.md`.
- README says ~7.2% positives; separate record 20149489 metadata says ~8.7%. The records' relationship/comparability is unresolved, so do not call this a contradiction or error. README threshold/test-use statements remain uploader claims. Manifest overlap is a surface self-consistency check, not leakage verification; no split data/code was downloaded.
- L42/exact novelty remain `UNKNOWN`; no claim, experiment, queue/training status, or thesis percentage changed. See SSoT §4/C:S37, SCOPE v191, SESSION v191.

## L42 source-access follow-up — LEAD v190 (2026-10-16)

- Official Zenodo REST metadata for records 20149489, 20091591, and 20050999 declares `CC-BY-4.0` for each deposited artifact. Record 20091591 explicitly identifies 20050999 as its prior version; record 20149489 has no related identifier in the returned metadata, so its content/lineage relationship remains unresolved.
- Metadata inventories/checksums differ across the three records; no files/code were downloaded or audited. The artifact license declarations do not establish underlying source-platform recipe/nutrition content rights or provenance, which remain `UNKNOWN`.
- The publisher homepage's Digital Library Archive route returned a human-verification challenge; it was not bypassed and no PDF was obtained. Full-text methods/results screening remains open.
- This refines the source inventory only. L42/exact novelty remain `UNKNOWN`; no claim, experiment, queue/training state, or thesis percentage changed. See SSoT §4/Appendix C:S37, SCOPE v190, and SESSION v190.

## L42 source-access follow-up — LEAD v189 (2026-10-16)

- KCI’s official page/abstract is now accessible; full text remains unscreened. SSRN direct page still returns 403, but its indexed abstract exposes author-reported selective nutrition-goal verification and validation-locked test-precision claims; no methods or metrics were independently verified/reproduced.
- Two distinct Gim-authored Zenodo dataset records are listed in SSoT S37. No relationship between them is assumed; no data/code was downloaded. Rendered HTML rights slots are blank, but official Zenodo REST metadata declares `CC-BY-4.0` for both deposited artifacts; this does not establish rights/provenance for underlying source-platform recipe/nutrition content, which remain `UNKNOWN`.
- Prior-art boundary: selective nutrition-goal suitability under incomplete evidence, adjacent to—but not ingredient-substitution generation/selection. L42 and exact novelty remain `UNKNOWN`; no experiment, claim, queue state, or thesis percentage changed. See SSoT §4/Appendix C:S37, root SCOPE v189, and SESSION v189.

## Verified execution update — 2026-10-15

- **L12 Segment-11 terminal status, validation blocked — LEAD v188 (2026-10-16).** Primary-wrapper Kaggle status returned `COMPLETE`; one output request produced a preserved, unpromoted staging tree. Segment-11 manifest SHA-256 `beb463c2a1c00ca1945fa29126e1f2d9532021ffc3782f1a65786c8ff67ab636` records step 701, the expected terminal state, unchanged evaluations 187/374/561, and no step-701 evaluation; all 12 checkpoint and eight retained-best file hashes match it. Its Segment-10 pin is `24b297105fda76720d9d080e4f0cb102dc08fb151acaf39aa7356609acc6a581`. One Segment-10 ancestry download returned CLI exit 1; checked local checkpoint/best hashes match its manifest, but the nonzero transport result is a hard stop under the no-retry rule. The established validator and source receipts for segments 10/11 are absent, so full ancestry/source validation is not certified. Both staging trees remain for diagnosis; no checkpoint promotion, retry, successor, or quality/inference claim. Watcher retired. See root SSoT §1.1, SESSION v188, and blocker receipt `ai/out/iron-chef-lead-v99/secondary_stage3_segment11_blocked.json`.

- **L42 advanced, not closed — LEAD v187 Senath full-text prior-art screen.** Screened the nine-page peer-reviewed article from the official Massey repository (PDF SHA-256 `cc0506debfaee0832d17c7e28b760ecc91a2887d4915b2bce0c19062af410976`). It reports Recipe1MSubs split sizes 49,044/10,729/10,747, best SFT+DPO Hit@1 22.04%, and lower Hit@3/Hit@10 than GISMo. The methods disclose prompt selection from 500 test examples and context selection from a 1,000-example test sample before full-test reporting; overlap/re-evaluation handling is unspecified. Treat these as test-informed author reports, not reproduced or clean confirmatory held-out evidence, without concluding that every result is invalid. Human expert review and confidence scoring are described as future work; cultural alias variation and potentially inedible hallucinations are noted. R42 advances only to `PARTIAL` for source-attributed full-text provenance in v184 (17 VERIFIED / 8 PARTIAL / 20 UNVERIFIED). No safety validation or novelty conclusion; L42 remains open/UNKNOWN. Q124 still freezes the queue; no thesis percentage, experiment, training action, commit, or push. See SCOPE v187, SSoT §4/§15.2/C:S02, and SESSION v187.

- **L48 advanced, not closed — LEAD v186 R31 Copilot/voice claim-scope correction.** The independently reconstructed frozen v44 runtime artifact contains 100/100 unavailable responses for `POST /copilot/query` within 700/700 valid loopback calls; it is not a voice-specific workload. The L30 stage audit records zero voice cases, and current `copilot.py` differs from the frozen protocol input. R31 therefore moves only to `PARTIAL` in v183 (17 VERIFIED / 7 PARTIAL / 21 UNVERIFIED), strictly for historical Copilot endpoint behavior. No current-runtime, voice-capability, product, safety, quality, queue, or thesis-percentage claim follows. L48 remains open; Q124 remains frozen. See SCOPE v186, SSoT §1/§15.1–§15.2/§17, and SESSION v186.

- **L48 advanced, not closed — LEAD v185 R30 bounded photo-proxy reconstruction.** Independently rebuilt and rehashed the v43 frozen offline photo→recipe→FooDB proxy: 783/783 image files pass SHA-256 verification; paired retrieval is 267/783, complete graph/explanation paths 536/783, and end-to-end paired-recipe-correct paths 179/783. Protocol and result validators reject 5/5 and 6/6 mutations. The v182 register now records R30 `VERIFIED` only for this exact offline proxy arithmetic/protocol (17 VERIFIED / 6 PARTIAL / 22 UNVERIFIED). This does not establish visual ingredient detection, camera transfer, substitution quality, chemistry, safety, usefulness, rights, latency, or readiness; L29 remains PARTIAL and L48 remains open. No thesis percentage or queue status changed; Q124 remains frozen. See SCOPE v185, SSoT §15.1–§15.2/§17, and SESSION v185.

- **L42 advanced, not closed — LEAD v184 direct-task RAG abstract update.** Official SSRN indexing exposes the abstract for Salih Alj et al.'s 2026 preprint: role inference, 1,758-item role-enriched semantic retrieval, recipe-context reranking, and dietary/allergen filters; abstract claims 76.45% GPT-4o / 82.15% Gemini 2.5 Flash Hit@1 on its own setup. This is direct-task prior art for role-aware RAG recipe substitution and narrows broad novelty wording, but SSRN page/PDF opens return 403; no full-text protocol or metric reproduction. Dataset/split and cross-protocol comparability are UNKNOWN. Three authors overlap the 2025 role-aware paper; not independent replication. No selective unseen-pair/missingness/provenance abstention is established. L42/exact novelty remain UNKNOWN; queue and scoreboards unchanged. See SCOPE v184, SSoT C:S05, and SESSION v184.

- **L42 advanced, not closed — LEAD v183 SpheriQ metadata reconciliation.** The official OpenReview author-index search now names Anoushka Harit, Zhongtian Sun, Sunčica Hadžidedić, and Tiansi Dong and labels S36 “Submitted to ICLR 2026,” last modified 2026-02-11; the indexed PDF retains its submission-era “Under review” header. This corrects author metadata only and verifies no current decision/acceptance. Forum/PDF remain browser-verification blocked; indexed method/results are not a full-text protocol review or reproduction. SpheriQ is adjacent generic-recommender evidence, not the ingredient-substitution estimand. L42 remains open/UNKNOWN; queue, scoreboards, and claims unchanged. See SCOPE v183, SSoT S36, and SESSION v183.

- **L42 advanced, not closed — LEAD v182 risk-proportionate evaluation-framework screen.** Screened the full Frontiers publisher text for Xu et al.'s VALID-Diet Perspective (published 2026-10-13; DOI `10.3389/fnut.2026.1957211`). It proposes a staged evaluation path from intended use/risk through analytical validity, contextual safety/personalization, then human workflow/outcomes and lifecycle revalidation. The authors explicitly state it is a proposal—not a standard or consensus—and its thresholds need calibration/prospective validation. This tightens evidence-stage/claims discipline but is not ingredient-substitution algorithm prior art, does not establish safety/effectiveness, and does not close exact L42 novelty. No threshold, protocol, backlog criterion, queue status, thesis percentage, training state, or product behavior changed. See SCOPE v182 and SSoT §4/Appendix C S42.

- **L48 advanced — LEAD v181 R23 preserved allergen review-pack state.** Revalidated the two 208-item reviewer packets: all candidate label/rationale fields remain null, the orders are independently shuffled, and the identity key remains sealed until both reviews are complete; the validator rejects 6/6 adversarial mutations. R23 is `VERIFIED NEGATIVE` only for no adjudication in this preserved package—not separate later review artifacts or reviewer competence. L22 remains PARTIAL; no agreement or safety result. v181 register: 16 VERIFIED / 6 PARTIAL / 23 UNVERIFIED. See SSoT §15.2, SCOPE R23, and SESSION v181.

- **L48 advanced — LEAD v180 R21/R22 frozen-run arm absence.** The validated v39 raw run selects only `mistral_small`, and all 208 cells contain exactly that arm; the independent raw validator rejects 8/8 adversarial mutations. R21/R22 are `VERIFIED NEGATIVE` only for GPT-4o/Gemini being absent from this preserved artifact—not current credentials or separate later runs. L22 remains PARTIAL: no matched comparison, reviewer adjudication, or safety result. v180 register: 15 VERIFIED / 6 PARTIAL / 24 UNVERIFIED. See SSoT §15.2, SCOPE R21/R22, and SESSION v180.

- **L48 advanced — LEAD v179 R20 Mistral raw allergen arm integrity.** Independently validated the frozen raw artifact against protocol/rubric/runner: 208/208 schema-valid cells, one attempt each, exact requested/returned `mistral-small-2603`, unique response IDs, zero recorded provider errors, and 8/8 adversarial raw-evidence mutations rejected. R20 is `VERIFIED` only for hash-pinned raw collection integrity; reviewer packets remain unreviewed, GPT-4o/Gemini are absent, and no adjudication, comparative, clinical, or safety result is established. L22 remains PARTIAL. v179 register: 13 VERIFIED / 6 PARTIAL / 26 UNVERIFIED. See SSoT §15.2, SCOPE R20, and SESSION v179.

- **L48 advanced — LEAD v178 R19 frozen allergen protocol/frame integrity.** Revalidated the hash-pinned 208-query / 13-group protocol and rubric frame (145 direct, 41 trace, 22 multi); structured constraints, shared three-candidate schema, and unadjudicated truth state are intact. The fail-closed validator rejects 12/12 adversarial protocol/contract mutations and reports `PROTOCOL_READY_REVIEW_PENDING`. v178 marks R19 `VERIFIED` only for this frame-integrity claim; no raw model execution, human adjudication, clinical truth, comparative evidence, or safety is established, and L21/L23 remain open. Register: 12 VERIFIED / 6 PARTIAL / 27 UNVERIFIED. See SSoT §15.2, SCOPE R19, and SESSION v178.

- **L48 advanced — LEAD v177 R18 bounded OFF taxonomy partition.** Rebuilt the partition over 3,377 frozen OFF tag identities / 767,780 occurrences with the official taxonomy bytes; the independent validator reproduces report/queue and rejects 8/8 semantic mutations, and focused tests pass 6/6. The 2026-09-30 taxonomy postdates the 2026-09-08 snapshot. v177 marks R18 `VERIFIED` only for this dated current-authority partition: 23 exact official positives cover 760,877 occurrences; `en:none` stays separate and aliases remain review-only. No point-in-time, clinical, or safety claim; L20 remains PARTIAL and L21/L23 unchanged. Current derivative: 11 VERIFIED / 6 PARTIAL / 28 UNVERIFIED. See SSoT §15.2 and SESSION v177.

- **L48 advanced — LEAD v176 R16/R17 bounded OFF concordance recomputation.** Recomputed the v7 frozen sample with the current evaluator; per-case and summary outputs match exactly, including held-out 400/408 declared-group and 22/40 trace-group counts. The policy-source hash differs from the frozen report, so R16/R17 are `PARTIAL` only, not historical-source reproductions. Independent validator rebuilds the 64-page/6,400-row/503-record sample and 765/783 all-split declared-label arithmetic; focused tests pass 13/13. v176 register is 10 VERIFIED / 6 PARTIAL / 29 UNVERIFIED. No clinical/safety inference; L20 remains PARTIAL and L21/L23 are unchanged. See SSoT §15.2 and SESSION v176.

- **L48 current-register pointer synchronized (LEAD v175, 2026-10-15).** The canonical SSoT's §15.1 still named v170 as current while §15.2 and this backlog already identified the v171 R14 derivative. Revalidated v171 with the fail-closed validator: 45 unique rows, 10 VERIFIED / 4 PARTIAL / 31 UNVERIFIED; focused validator/inventory suites pass 12/12. R14 is bounded local profile-presence evidence only; this check changes no row status and proves no overall thesis progress. Register SHA-256 `c4a7350f1ccfb425da801e8a34b4a5112e2545ffd6c494e4eb4f8043f41e00bb`. See SSoT §15.1–15.2 / §17 and SESSION v175.

- **L42 advanced, not closed (LEAD v174, 2026-10-15).** Screened the full Springer publisher text for Mozaffari et al., ONFOODS (DEXA 2025): recipe-conditioned alternative meat-cut ranking via ontology/KG, nutrition/sensory similarity, user-edited quantities/order, and inventory/expiry. The reported prototype is beef cuts/200 recipes and uses nutrition-only equal weights because sensory fields were unavailable. This directly narrows broad recipe-context meat-cut substitution claims but is not calibrated abstention, Recipe1MSubs unseen-pair evaluation, or evidence-missingness analysis. Publisher-view full text was read; the only local retrieval is a 3,038-byte client-challenge shell (SHA-256 `32ed63159c77e21ee19ca1b9aa3213ccf0218eb59539560b132a8e68ef0e18ea`), not article bytes. No reproduction is claimed. L42 remains open/UNKNOWN. See root SCOPE, SSoT §4/Appendix C S41, and SESSION v174.

- **L42 advanced, not closed (LEAD v173, 2026-10-15).** Refreshed the hashed novelty acquisition through 2026-10-15, preserving the historical cutoff default and explicitly recording day-level OpenAlex/Crossref versus year-level Semantic Scholar filtering. The fresh snapshot has 41 verified response hashes, 4,100 raw / 3,087 deduplicated records, and 19 recorded Semantic Scholar HTTP 429 responses; all queries met the two-source floor and deterministic replay passes. AoE corrected an adaptation-prefix false positive for “adaptive” robotics language. A publisher-indexed 2026 many-objective recipe-design paper with dynamic substitution is added as adjacent abstract-level prior art, not a full-text review or novelty conclusion. Exact novelty remains UNKNOWN and L42 stays open. See root SCOPE L42, SSoT §4/Appendix C S40, and SESSION v173.

- **L42 advanced, not closed (LEAD v172, 2026-10-15).** Full-text screened the 2021 IEEE Access predecessor to S38 and inspected the author's 2025 adaptation source at a pinned Git commit. This adds verified prior-art scope for embedding-based recipe adaptation, diet-constrained remapping, thresholded/no-match candidate handling, and survey-rated recipe adaptations. The paper's 95% interval is for survey-average ratings, not calibrated candidate-level selective prediction. The 2021 and 2025 papers are one linked author lineage, not independent replication; code/data were not executed, no novelty conclusion is accepted, and L42 remains open. See root SCOPE L42 and SSoT §1.2 / source S39.

- **L12 Segment-11 status-only recheck (by 2026-10-15T09:57:31Z).** Official Kaggle CLI returned `KernelWorkerStatus.RUNNING` for `tnvtrung/chefkix-senath-stage-3-sft-segment-11`. The response did not expose a version number; no package/artifact validation, transport, step-boundary proof, or successor action occurred. This is not a checkpoint-validity, inference, scoring, quality, or Stage-3 completion claim. See SSoT §1.1 and SESSION v172.

- **L48 advanced, not closed (LEAD v171).** Reproduced R14's 1,162/10,747 (10.81232%) both-profiled test substitutions from the hash-pinned split and the provenance-gated FooDB artifact using the current lookup; direct presence agrees with runtime across 21,494 endpoint occurrences. Two deterministic full builds match byte-for-byte; 3 focused tests pass. The separate v4.2 occurrence/unique-name diagnostics were not reproduced under this lookup and are not adopted. Current v171 register: 10 VERIFIED / 4 PARTIAL / 31 UNVERIFIED. This verifies local presence only—not mapping validity, usefulness, safety, or model quality. L48 remains open; no thesis percentage. See root SSoT §15 and SESSION v171.

- **L48 advanced, not closed (LEAD v170).** Independently reproduced R13 twice with byte-identical outputs by checking every unordered combination-with-replacement from the 971-profile hash-bound FooDB artifact against the current analyzer using direct set/Jaccard arithmetic: 471,906 total, zero mismatches. Explicitly, that count includes 971 self-pairs; the distinct cross-profile denominator is 470,935 (468,116 nonzero, 2,819 zero). The v170 claims derivative validates at 9 VERIFIED / 4 PARTIAL / 32 UNVERIFIED. This verifies R13 only for current installed-artifact/runtime arithmetic; the old 512-response sample, broad suite totals, deployed state, usefulness, safety, and recommendation quality remain unverified. L48 remains open; no thesis percentage. See root SSoT §15, SESSION v170, and SCOPE.

- **L12 / Senath Segment 9 artifact boundary verified — LEAD v169, 2026-10-14.** One official secondary-account transactional download validated exact step 561 and `STAGE3_SEGMENT_9_COMPLETE`, the exact 12-file checkpoint inventory/hashes, recursive pinned Segment-8→1 and Stage-2 ancestry, frozen source/dataset/selection identities, and manifest integrity. The manifest and trainer state preserve exactly evaluations at 187 (`0.19342221319675446`), 374 (`0.20272061228752136`), and 561 (`0.224910706281662`). The eight-file retained-best inventory remains hash-valid from step 187 (adapter SHA-256 `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`). Segment-9 manifest SHA-256 `46f5b0f403f9840e07854ca4c1ce179e49d136b65cc5f319c56f3b2e626eb969`. This is artifact/provenance evidence only—not inference, scoring, model quality, or overall Stage-3 completion. No successor is authorized. See root SSoT §1.1 and `SESSION.md` v169.

- **L48 advanced, not closed (LEAD v168, 2026-10-14).** Rebuilt the official-data FooDB binary-presence index and matched the preserved artifact SHA-256 exactly: 971 profiles, 206 named compound identities, 125,992 links. A fresh seeded scan of 5,691,011 raw Content rows confirmed 200/200 positive and 100/100 absence samples. R12 is `VERIFIED` only for this offline inventory; FooDB is CC BY-NC 4.0, and deployment, usefulness, safety, and commercial rights are not established. Current derivative: 8 VERIFIED / 4 PARTIAL / 33 UNVERIFIED; L48 remains open, no thesis percentage. See root SSoT §15 and SCOPE L48.

- **L48 advanced, not closed (LEAD v167).** Reran the local historical SmolVLM artifact audit: four mock text-only examples, both image references absent, image token stripped, no image collator, one step, zero evaluations/benchmarks, duplicate adapter; producer/deployment paths fail closed and 8/8 invented-evidence mutations reject. R11 is `VERIFIED NEGATIVE` only as withdrawal of this artifact as VLM evidence—not evidence against real visual models. Current derivative: 7 VERIFIED / 4 PARTIAL / 34 UNVERIFIED; L48 open, no thesis percentage. See root SSoT §15 and SCOPE L48.

- **L48 advanced, not closed (LEAD v166).** Independently reconstructed the frozen Recipes5k CLIP residual-projection result: 783 publisher test / 771 duplicate-clean queries; 9/9 result and 8/8 protocol mutations rejected. Mean image→text R@1 gain is +1.3197 pp, below the preregistered +2.00 pp primary gate; frozen decision is `REJECT`. R10 is `VERIFIED NEGATIVE` only for that exact experiment. No weights installed, rights unknown, no user-camera/serving transfer. Current derivative: 6 VERIFIED / 4 PARTIAL / 35 UNVERIFIED; L48 remains open, no thesis percentage. See root SSoT §15 and SCOPE L48.

- **L48 advanced (LEAD v165).** Rebuilt the frozen full-corpus RecipeNLG/FooDB analysis in a fresh temp directory; its result bytes match v24 exactly. Independent validator passes and rejects 5/5 mutations. Global greater-than-null hypothesis is not supported (observed 0.545990 vs null 0.527733; p=0.238095). R09 is `VERIFIED NEGATIVE` only for this bounded global test; no cuisine/sensory/causal/safety/commercial claim. Current derivative: R04/R05 bounded counts and R06/R08/R09 bounded negatives `VERIFIED`, R01–R03/R07 `PARTIAL`, R10–R45 `UNVERIFIED`. L48 remains open, no thesis percentage. See root SSoT §15 and SCOPE L48.
- **L48 advanced (LEAD v164).** Reran the frozen Recipe1MSubs true-context dual-encoder validator: independent weight/metric reconstruction confirms `REJECT`, all three predeclared gates fail, and 9/9 result plus 8/8 protocol mutations are rejected. R08 is `VERIFIED NEGATIVE` only for this candidate/protocol. Current derivative: R04/R05 bounded counts and R06/R08 bounded negatives `VERIFIED`, R01–R03/R07 `PARTIAL`, R09–R45 `UNVERIFIED`; L48 remains open, no thesis percentage. See root SSoT §15 and SCOPE L48.
- **L48 advanced (LEAD v163).** Reviewed R07 against the hash-bound v37 audit report: v11's historical random-vector/missingness and identity confounds are documented, but the original v11 source bytes are absent from the preserved package, so R07 is `PARTIAL`, not source-level verified. Current derivative: R01–R03/R07 `PARTIAL`, R04/R05 bounded counts and R06 bounded negative `VERIFIED`, R08–R45 `UNVERIFIED`. 26 focused tests and full-register validation pass. L48 remains open; no thesis percentage. See root SSoT §15 and SCOPE L48.
- **L48 advanced (LEAD v162).** Revalidated the corrected five-arm/three-seed SAG result with the local ingester reconstruction: frozen context-free MISKG rule returns `REJECTED`; 8 focused tests pass. R06 is `VERIFIED NEGATIVE` only for that bounded falsifier. Current derivative: R04/R05 bounded counts and R06 negative are `VERIFIED`, R01–R03 `PARTIAL`, R07–R45 `UNVERIFIED`. No recipe-context transfer, product-quality, safety, or thesis-percentage claim. See root SSoT §15 and L48 SCOPE.
- **L48 advanced (LEAD v160).** Reran the hash-pinned GISMo-canonical identity reconstruction; output is byte-identical to L37. It verifies the fixed test denominators only: GISMo-canonical 6,932/3,815; raw-normalized/Senath-sensitivity 6,866/3,881; 66/10,747 assignments differ. Current derivative is R04 `VERIFIED` for those bounded counts, R01–R03 `PARTIAL`, R05–R45 `UNVERIFIED`; original source inventory remains unchanged. Not a progress percentage or model-performance claim. See root SSoT §15 and `SCOPE.md` L48.
- **L48 advanced (LEAD v161).** Added a fail-closed hash-bound split-count reproducer: frozen train/validation/test files yield 49,044 / 10,729 / 10,747 rows; 3 tests pass. Current derivative: R04/R05 `VERIFIED` for bounded local counts, R01–R03 `PARTIAL`, R06–R45 `UNVERIFIED`. No claim of paper equivalence, model performance, or overall thesis percentage. See root SSoT §15 and L48 SCOPE.
- **Segment 9 status — 2026-10-13T10:15:57Z:** Fresh official secondary-wrapper poll remains `RUNNING` toward step 561; no artifact download or further version launch.
- **L48 advanced, not closed (LEAD v159).** Preserved the frozen 45-row source inventory and added a separate reviewed evidence overlay: R01–R03 `PARTIAL`, 42 `UNVERIFIED`. GISMo output is diagnostic-only under its own v26 report; R02 has a 3,815-vs-3,881 unseen-denominator conflict; R03's LT+Freq result is close but not exact to the paper and its executable source is absent from the result package. Fifteen focused tests and the overlay validator pass. This is not a thesis-progress percentage. R04–R45, broader L-item mapping, and full evidence-register review remain open; see root `SCOPE.md` L48 v159.
- **Segment 9 status recheck — 2026-10-13T10:00Z:** Authenticated official Kaggle CLI returned `RUNNING` for `tnvtrung/chefkix-senath-stage-3-sft-checkpoint`; no step-561 artifacts are downloaded or validated. Existing transactional ingestion and segmented validation pass 19/19 focused tests; no transport occurs while RUNNING, and this monitor authorizes no successor.

- **L13′ COMPLETE — scope edit only.** The former new-GNN backlog wording is replaced by a shift-aware selector over frozen scorers, with its method and guarantee boundary in SSoT [§8](../academic_vision_and_strategy.md#s8). This does **not** close scientific work package L13: no accepted model is established.
- **L37 COMPLETE — fixed-benchmark descriptive intervals under both identity contracts.** The frozen v26 evaluator was independently reconstructed (`result_validation_report.json` PASS); 2,000-replicate recipe-cluster intervals are reproducible for overall/evidence strata and the frozen raw-normalized pair split. A separately labeled, hash-pinned GISMo-canonical re-stratification of the same unchanged predictions reproduces 6,932/3,815; 66/10,747 row assignments differ from the raw-normalized split. Do not retrofit the new GISMo intervals into v26's frozen `REJECT` decision, which remains under its original raw-normalized protocol. All intervals are conditional descriptions of this fixed benchmark, not population/exchangeability, causal, safety, deployment, or model-quality evidence. Optional endpoint-frequency split was not run. Raw-normalized report: [`l37_cluster_intervals.json`](../ai/out/iron-chef-lead-v102/l37_cluster_intervals.json), SHA-256 `351230465cbf51a41378f42fd1c12e21ea936ebfa2bd616c31d76572460c9283`; GISMo-canonical report: [`l37_gismo_canonical_pair_intervals.json`](../ai/out/iron-chef-lead-v103/l37_gismo_canonical_pair_intervals.json), SHA-256 `b67e5ddae8946d9a41f4e77d637f544ef3ce9f978632f5ed1f3835c9b333ac82`, byte-identical across two full builds; generator SHA-256 `494ea08b5082a759403f77e20db5f089b661ee3b2334175e2470dda804d4b2f4`; frozen evaluation SHA-256 `293f0c9952d98d389bb3b7d23ee4f1f04b6ad675914e361de9102806f5539ea8`. The v26 overall candidate Hit@1 is 20.4243%, not the separately reproduced GISMo baseline 20.694%. See [SCOPE L37 identity analysis](../SCOPE.md#l37-identity-provenance-follow-up--gismo-canonical-intervals-reproduced-2026-10-13).
- **L42 advanced, not closed.** Official CiST program evidence, the hash-recorded full arXiv:2409.08792 text, a citation-chased ingredient-substitution survey plus Shirai et al. DIISH full text, full-text screens of KDD 2025 LPShift and the NeurIPS 2023 link-prediction evaluation paper, and an author-repository screen of conformal LLM recipe-rating work now bound direct versus adjacent prior art more precisely. The HUMMUS rating-interval lead challenges broad “first conformal prediction for LLM recipe recommendation” language, but is not ingredient-substitution selection, unseen-pair Hit@1, or selective abstention; paper acceptance/full text remain unverified. The graph papers support benchmark-rigor concerns, not ingredient or calibrated-selective-prediction evidence; the exact calibrated-selective candidate remains `UNKNOWN`. The survey’s generic query and 28-work snowball are not a complete search, its omission of separately known Senath work demonstrates a coverage limit, and independent adjudication remains absent. IEEE full text for the role-aware paper, complete hashed query/citation funnel, formal coding, and independent rubric adjudication remain missing. See [SCOPE follow-up](../SCOPE.md#l42-role-aware-followup); no novelty claim is accepted.
- A 2026 *Electronics* paper reports allergen-aware multi-objective **recipe ranking** with a substitution bonus (Wang & Wang, DOI `10.3390/electronics15122628`). Its official 25-page publisher PDF is now screened (SHA-256 `98D9550151509D61B28E8A07807361653BD4C449A2CAC15A647BE33B67D83413`): the displayed algorithm ranks recipes and does not specify ingredient-substitute generation/selection or a validated substitute-evidence procedure. Authors report N=20 home cooks from one recipe source, retroactive replay for external baselines, and no clinical validation; reported metrics were not reproduced. This is direct-adjacent ranking/scoring prior art, not the exact selective ingredient-substitution claim. See root SCOPE L42; formal coding/adjudication and L42 remain open.
- The publisher HTML returned HTTP 429, but the official static PDF was retrieved and screened; acquisition hash and sampled-page visual review are recorded in root SCOPE. The author-affiliated Yamaguchi University publication list identifies an earlier IMCOM 2026 version (Wang, Ding & Wang, pp. 1–6); Crossref now identifies DOI `10.1109/imcom69009.2026.11360864` with matching bibliographic metadata. OpenAlex indexes an abstract describing an allergen-safety score involving ingredient substitution, nutrition/NIMA scores, and weighted recipe ranking; it is abstract/catalog evidence only, not a full-text screen. Its record reports closed access/no repository full text, not proof no copy exists. IEEE Xplore returned HTTP 202 with an empty body, so conference full text remains unreviewed. Treat journal/conference versions as one linked prior-work line, not independent reviews.
- **Historical v158 state (superseded by v159 below):** the fail-closed validator and source-linked inventory existed, but all 45 rows were then `UNVERIFIED` / `NA`. The v159 R01 triage and current register overlay are recorded in the first bullet and root `SCOPE.md`; L48 remains open.

## Historical Segment-3 artifact progression — snapshots through 2026-10-13

- **L12/Mistral Stage 3 — Segment 9 RUNNING toward step 561.** After explicit HUMAN direction, the verified final checkpoint successor was built from Segment-8 manifest `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`, validated for exact resume 514/stop 561, frozen evaluations 187/374/561, same private bridge, and Segment 8 as sole predecessor. One secondary-wrapper Kaggle version-1 push was positively acknowledged; fresh status returned `RUNNING`. Existing final-segment ingester plus segmented validator pass 19/19 focused tests (including no download while running and failure preservation/no retry). No artifact is downloaded pending terminal completion. Package script SHA-256 `b4278b442663fa6257384cce298489ed7177e40f1d59adce7b35a5780703734f`, metadata SHA-256 `a5e523e9ad8291d21e01d02519a8e2d193b3105e5781f31d7f00bf738bbbcd5f`. Evidence: [`secondary_stage3_segment9_launch.json`](../ai/out/iron-chef-lead-v99/secondary_stage3_segment9_launch.json). Remote activity is not step-561 validation, inference, scoring, quality, or Stage-3 completion.

- At `2026-10-13T06:34:10.055538Z`, the separately vaulted secondary-account wrapper reported Segment 8 version 1 `COMPLETE`. The already-downloaded output was revalidated locally (no new transport attempt): exact step 514, terminal state `STAGE3_SEGMENT_8_COMPLETE`, 12 checkpoint files, eight retained-best files, pinned ancestry/source/data-selection checks, and manifest SHA-256 `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`. Receipt: [`segment8_status_revalidated.json`](../ai/out/iron-chef-lead-v99/action-receipts/20261013T063410.055538Z-segment8_status_revalidated.json); full artifact evidence: [`secondary_stage3_segment8_advancement.json`](../ai/out/iron-chef-lead-v99/secondary_stage3_segment8_advancement.json). Segment 9 was not yet authorized at this historical observation; v157 below supersedes that state.
- The recorded evaluation history remains only step 187 (`0.19342221319675446`) and step 374 (`0.20272061228752136`); no step-514 evaluation is recorded. This establishes artifact/provenance continuity only—not inference, scoring, model quality, or overall Stage-3 completion. At this historical observation Segment 9 was not yet prepared or authorized; v157 below supersedes that state.

The older Segment-7 `RUNNING` observation below in the historical record is superseded for status purposes, not erased. Do not treat this dated check as a live status observation.

The supplemental interval analysis does not alter v26's frozen acceptance rules or REJECT verdict. L13 remains open; L36/L38/L39/L40/L41/L42/L43/L44/L45/L46/L47/L48 retain their listed dependencies and acceptance gates.

---

## Current Evidence Boundary (from SSoT §1)

| Area | Verified Fact | Tier |
|------|---------------|------|
| GISMo reproduction | 10,747 test: Hit@1 20.694%, Hit@3 36.010%, Hit@10 54.164%, MRR 0.31636 | E3 |
| Unseen-pair collapse | GISMo unseen Hit@1 1.206%, MRR 0.04308; Lookup overall 17.996%, unseen 0.000% | E3 |
| Corrected SAG | Mean Hit@1 0.1171% vs semantic 0.3964%, naive fusion 0.3333% | E3 NEGATIVE |
| True-context dual encoder | 16.1223% overall Hit@1 vs GISMo 20.6941%; unseen MRR 0.016601 vs 0.043084 | E3 NEGATIVE |
| CLIP projection | R@1 34.10% → 35.42%, missed +2pp gate | E2 NEGATIVE |
| Flavor pairing | 2,231,142 RecipeNLG rows, p=0.238095, not supported | VERIFIED NEGATIVE |
| FooDB service | 971 foods, 206 dimensions, 125,992 links; 471,906 profile pairs, 0 mismatches | VERIFIED |
| FooDB reach | Both endpoints: 1,162/10,747 (10.81%); unique endpoints 260/2,650 (9.81%) | MEASURED |
| OFF concordance | 400/408 declared groups (98.04%, Wilson 96.18–99.00%); trace 22/40 (55%) | PARTIAL E2 |
| OFF partition | 23 identities cover 760,877/767,780 tag occurrences (99.10%) | E3 |
| Allergen protocol | 208 cases, 13 groups, direct/trace/multi/cross-contact; Mistral raw 208/208 | PARTIAL E2 |
| Mistral Stage 3 | Segment 9 artifact boundary validated at step 561. Segment 11 is recorded COMPLETE at step 701, and local Segment-1–11 manifest/checkpoint/retained-best/evaluation continuity now passes. Exact launch-source provenance remains BLOCKED: a current unversioned source pull differs from the recorded version-1 pins, so it cannot establish the immutable launched bytes; the preserved nonzero Segment-10 output request was not retried. Segment-10/11 stay unpromoted. No inference, scoring, quality, or overall Stage-3 completion claim. | PARTIAL E3 |
| Novelty reviews | v8/v9: broad claims rejected; exhaustive re-attack open | PARTIAL |

**No accepted IRON CHEF model exists.** No allergen comparison result (GPT-4o/Gemini unrun, no adjudication). No human usefulness evidence. Segment-9 artifact completion and the unpromoted Segment-11 staging tree do not establish model acceptance or overall Stage-3 completion.

---

## The Five Contributions (C1–C5 from SSoT §3)

| ID | Contribution | Floor | Target | Stretch |
|----|--------------|-------|--------|---------|
| **C1** | **Instrument**: strata × allergen cases × valid@k audit | 208 allergen cases + Recipe1MSubs strata + 150-case 2-rater audit | + Vietnamese pilot (~100 cases, 2 native raters) | Public release with datasheet |
| **C2** | **Measurement campaign** across arms | GISMo + lookup + one API LLM | + Mistral Stage 3 + second API LLM; intervals everywhere | Contamination-aware 2×2 (public/private × Western/Vietnamese) |
| **C3** | **Guardrail**: hard allergen filter + per-cell conformal sets + abstention | Frozen protocol, honest run, cells reported | Informative sets in ≥1 cell; vacuous cells shown | Fewer allergen violations than raw arms with intervals |
| **C4** | **Human evidence** | 2-reviewer pass on audit pilot, κ (kappa) reported | LLM judge calibrated to humans (κ/kappa ≥ 0.6) | Usefulness study N ≥ 20 (N >= 20; ethics permitting) |
| **C5** | **Product proof** | Real-data Explorer + evidence endpoint + visible abstention | Live side-by-side demo vs raw LLM on same inputs | Reusable component |

**Bounded negatives (no further spend):** SAG, dual encoder, flavor pairing, CLIP projection, VLM artifact, behavioral overlay, voice copilot. One thesis chapter: *What did not work, and what each failure taught about the evaluation.*

**L13′ re-scope delivered:** the proposed selector operates over frozen scorers, with method/guarantee boundaries in SSoT [§8](../academic_vision_and_strategy.md#s8). This is a scope-text completion only; L13's scientific model gate remains OPEN.

---

## Acceptance Gates per Contribution (SSoT §3.2)

| ID | Gate (all binary, checkable) |
|----|------------------------------|
| C1 | Instrument schema validated; strata files regenerate bit-for-bit from official splits; audit pilot labels for ≥150 cases exist with two independent raters and adjudication log; κ (kappa) reported with a case-cluster interval (cluster-level interval) |
| C2 | Every arm evaluated on identical case list, identical normalization, identical scoring script; each number in a table has an interval; arms with unmatched-output rate >5% flagged |
| C3 | Protocol frozen and hashed before test access; calibration only on validation; per-cell status table (ok / vacuous / no_guarantee); cost-of-trust figure from frozen run |
| C4 | Rater agreement computed per §6.8; judge calibrated against human labels per §7.5; judge-vs-human agreement reported; if κ (kappa) < 0.6 twice, fallback applied and stated |
| C5 | Explorer loads from hash-pinned graph artifact; demo runs from clean checkout without network except API arms; abstention state reachable and tested |

---

## Work Packages (from SSoT §17.1 — Lead Items)

| ID | Deliverable / Acceptance | Dependencies | Effort | Fallback |
|----|--------------------------|--------------|--------|----------|
| **L13′** | **COMPLETE (scope-only):** re-scope to shift-aware selector over frozen scorers; edited item text links to §8 | None | 1h | None |
| **L36** | 150-case audit: all independent labels/adjudication/cluster intervals/H1 table | Raters (L41), pools (M-B), PR01–03 resolved | 30–40h | Shrink to 100 cases |
| **L37** | **COMPLETE (fixed-benchmark scope):** overall/evidence intervals plus separate raw-normalized sensitivity and GISMo-canonical repeated/unseen intervals; endpoint-frequency split omitted | Identity/counts (M-B) | 10h | None — optional frequency split not run |
| **L38** | Frozen calibration, all-cell status, diagnostics, cost-of-trust figure | Audit/judge/arms, PR04–08 resolved | 25h | Report only ok cells; label rest |
| **L39** | Matched adjudicated allergen outputs; expansion to n≥299 or honestly bounded interval | API keys, reviewers (L41), rights | 30h | Report interval at n=208 |
| **L40** | Frozen ~100-case native-reviewed Vietnamese pilot | Authors/raters/rights (L41), U8, U9 | 30h | Qualitative appendix only |
| **L41** | Written ethics outcome; named raters; handbook + T0/T1 results | Human dependencies | 12h | Fallbacks per §14.8 |
| **L42** | **PARTIAL:** Evidence-bounded prior-art screening advanced; formally adjudicate the v219 adjacent classifications, HeASe meal-level lineage C:S56–S57, AI2Cuisine direct-substitution C:S58, and unresolved SHARS; complete hashed query/citation funnel and independent second review | Sources, reviewer | 15h (source estimate unchanged) | None — current SSoT §17.1 defines no fallback |
| **L43** | Frozen judge prompt + evaluated gates, or explicit unusable outcome | Leak-free human comparison (L36) | 12h | Humans only on smaller set |
| **L44** | **PARTIAL:** Segment-9 chain validated through step 561; fresh offline checks confirm local Segment-10/11 manifests, 12-file checkpoint/eight-file retained-best hashes, pinned ancestry, and frozen evaluation history through Segment-11 step 701. Exact launched source-package provenance remains uncertified: the current unversioned Segment-11 source pull differs from v1 launch pins, and the original Segment-10 output request exited nonzero and was not retried. No promotion. 200-case timing and matched val/test ranked outputs/results remain open (see current evidence boundary above) | Quota, adapter, arm construction | 12h + waiting | Stage-2 adapter arm |
| **L45** | 14 evidence-bound thesis chapters (§21) | All results/claims | 80+h | Cut by cut line (§3.6) |
| **L46** | **PARTIAL:** 7/24 answers refreshed as source-bound, unrehearsed candidates; 17 remain; no human rehearsal; R43 remains UNVERIFIED | Thesis (L45) | 15h | None |
| **L47** | ARR paper draft per §23 | Evidence + venue check | 40h | Thesis only |
| **L48** | **PARTIAL:** Integrated strategy and source inventory complete; current v185 derivative has 18 VERIFIED / 8 PARTIAL / 19 UNVERIFIED. R26 is VERIFIED only for the bounded Segment-9 step-561 recursive artifact boundary and recorded Segment-7 v1 launch acknowledgement; it does not certify Segment 11, inference, scores, quality, or Stage-3 completion. R42 is PARTIAL for source-attributed full-text provenance; R16/R17 and R31 remain bounded PARTIAL claims; other rows retain their precise limits in SSoT §15. Complete row-level provenance/reproduction review, citation separation, reconciled L-item mapping, and full-register validation remain open. These counts are register status, not thesis completion | R01–R45 evidence audit; SSoT §15; research artifacts and citations | 10h+ | None |
| **L49** | **PROPOSED / NOT READY:** H4 human usefulness/comprehension study of bounded compound-presence records. Tracking only; no participant contact, recruitment, collection, or R15 upgrade until institutional/consent, privacy/data handling, workload/recruitment, and frozen-protocol/analysis gates are documented. After gates, report eligible sample (canonical floor ≥20 adults plus a defensible rationale), version/deviations, coded participant-level outcomes and uncertainty for usefulness, comprehension, confidence, harmful over-reading, missingness/attrition, comments, failures, and null/inconclusive findings; preserve a reproducible receipt. Do not infer chemical causality, substitution safety, clinical protection, trust/retention, or general-population value. | L41 + PR13; SSoT §§14–17; all readiness gates before participant activity | Not estimated; first measure workload excluding participant time | None automatic; defer/remove only by HUMAN decision; R15 remains UNVERIFIED until validated evidence |

> **L48 status precision (LEAD v212):** “Full-register validation remains open” refers to evidence/provenance review and claim reproduction, not the validator's structural/local-path check; that narrower check was re-run successfully as described above. The row remains PARTIAL.

> **L49 preparation status (LEAD v213):** A questionnaire/analysis skeleton now exists at SSoT §14.2 for review. It is not frozen, does not authorize participant contact or collection, and does not change L49 readiness or R15 evidence status.

**Critical dependency path (SSoT §17.4):**
```
L41 (raters/ethics) → L36 (audit) → L43 (judge) → L38 (calibration) → L45 (writing) → L46 (defense)
         ↑                              ↑
    M-B (pools)                   M-D (arm outputs) ← L44 (inference)
L41 + PR13 → L49 (H4 study tracking; participant work gated)
```
**L41 → L36 → L43 → L38 is the primary critical path.** L49 is a parallel gated human-evidence lane, not an automatic critical-path commitment; unresolved external clearance blocks participant activity, not independent preparation.

---

## Schedule & Capacity (from SSoT §18)

**Schedule status overlay — as of 2026-10-20 (source plan retained below):** This calendar is the proposed v4.2 plan, not a live calendar. W0 (Oct 8–11) and W1 (Oct 12–18) have elapsed as proposal windows only. The source record has no institutional outcome by the proposed Oct 12 ethics target and no confirmed named raters by Oct 15; SSoT §14.1 says recruitment templates have not been sent. Record these gates as **UNKNOWN / owner confirmation required**, not missed or complete. Freeze A's Oct 22 target is **NOT READY** while PR01–PR03 remain unresolved; the date does not override §16. Oct 23 Vietnamese-review/rights checks, Oct 29 allergen-review/timing checks, and later gates likewise remain unverified until owner/source evidence is recorded. Do not invent replacement dates or reduce the ideal scope from silence. Obtain current advisor/institution/participant/collaborator dates, then issue a reviewed schedule resync. Until then, the dated rows below remain historical proposal inputs, not authority to recruit, spend, freeze, or run a study.

The source W5 row's Nov 10 "usefulness participants recruited (or dropped)" is preserved as historical proposal wording only. L49's current gate controls: no recruitment before clearance/consent and protocol readiness, and no automatic drop from an elapsed date or K11 signal.

**Source schedule snapshot (authored 2026-10-12):** Defense assumed on/after Dec 8 (U3). All dates are proposals `[P]`, not live status.

| Week | Dates | Lead Focus | Member Focus | Gates / Freezes |
|------|-------|------------|--------------|-----------------|
| **W0** | Oct 8–11 | Answer U1–U3, U7, U11 (hours, advisor, defense, quota, ethics). Back up SSoT. Start asking raters. | Start M-A, M-F | **Decision: compression level** |
| **W1** | Oct 12–18 | L48 first pass. L42 query runs. Read Rita §3 + Fatemi data (U4). Check val scores + unseen share (U5). Draft rater handbook. | M-A recompute; M-B strata/identity; M-D GISMo/lookup outputs; M-F inventory | **Oct 15: two raters named. Ethics answer by Oct 12** |
| **W2** | Oct 19–25 | Obtain API keys; run API LLM #1 on 150 audit cases. Rater T0/T1. Draft allergen expansion. Write Ch2/Ch3 drafts. | Pool builder fixture test → real run. M-I runner fixture test. | **Oct 22: FREEZE A** (audit protocol, Δmin, arms, normalization). **Oct 23: Vietnamese raters/authors named, licenses checked** |
| **W3** | Oct 26–Nov 1 | Audit labeling. Judge prompt dry run. 200-case timing (by Oct 29). Allergen cases frozen/hashed. | M-C Explorer build; M-G register paths/hashes | **Oct 29: allergen reviewers named; timing done. Nov 1: profiles complete** |
| **W4** | Nov 2–8 | Adjudicate audit; κ with cluster CI; first H1 table. Judge calibration (L43). Allergen arms (GPT-4o, Gemini). Stage 3 check. | M-C Explorer T-03–T-09; M-E demo skeleton | **Nov 6: FREEZE B** (conformal protocol, §8.9) before any test coverage. Advisor interim. |
| **W5** | Nov 9–15 | Judge-label val pools; per-cell calibration (rank arms); diagnostics §8.12; test-subsample eval. Vietnamese freeze. H4 prep if ethics. | M-E recorded Act 3 from frozen files | **Nov 10: usefulness participants recruited (or dropped)** |
| **W6** | Nov 16–22 | Mistral inference + calibration (or Stage-2 adapter). Allergen adjudication. Vietnamese labeling. Cost-of-trust. | Explorer polish T-10–T-12 | **Nov 20: EXPERIMENT FREEZE.** No new runs. Analysis/writing only. |
| **W7** | Nov 23–29 | Red-team claims (§24.2). Rebuild defense answers (§22). Chapters Ch5–Ch9. | M-H reproducibility kit; clean-machine reproduction | **Nov 27: register script exits 0; zero forbidden claims** |
| **W8** | Nov 30–Dec 6 | Write Ch1, Ch4, Ch11, Ch13, Ch14. Defense dry run 1. | Demo hardening | **Dec 1: ARR service-contributor decision** |
| **W9** | Dec 7–13 | **Dec 8: thesis freeze.** Start ARR draft from thesis artifacts. | Support | Thesis frozen |
| **W10** | Dec 14–20 | ARR v1; internal review. | Figures/tables regenerate. | Service-contributor registration ready |
| **W11** | Dec 21–27 | ARR v2; limitations/datasheet. | Figures/tables regenerate. | Registration and review follow-up |
| **W12** | Dec 28–Jan 3 | Final read and hashes. | Figures/tables regenerate. | Submission package ready |
| **W13** | **Jan 4, 2027** | **ARR submission (AoE)** | | Registration form open until 48h post-deadline |

**Capacity arithmetic (SSoT §18.1):** 225 MUST + 32 SHOULD + 70 COULD = 327 lead hours; 91 member hours. 8 weeks at 30h/week = 240h (short 87h). At 40h/week = 320h (fits). This remains planning arithmetic, not actual availability; obtain current hours and revise with the human architect before selecting any compression level.

**Compression protocol (SSoT §18.4):** Level 0 (full), Level 1 (drop COULD except 20-case Vietnamese appendix), Level 2 (GISMo/lookup only calibration, no judge, n=208 allergen, minimal Explorer), Level 3 (collapse map + 100-case audit + allergen interval + bounded negatives + claims register). Gates never weakened.

---

## Risks & Pre-mortem (from SSoT §19)

The following are candidate mitigations from the source plan, **not automatic fallbacks or delegated approvals**. Tags identify the required gate: `PREP/REPORT` may continue within existing authorization; `PROTOCOL FREEZE` requires prospective resolution/versioning under §16; `HUMAN SCOPE DECISION` is required for reductions or redirects; `EXTERNAL GATE` covers ethics, rights, provider, or collaborator authorization. Preserve the original ideal scope unless the human architect explicitly changes it. No test-informed sample/model/protocol substitution may be retrofitted as confirmatory.

| ID | Risk | Likelihood | Impact | Early Warning | Candidate mitigation / authority condition |
|----|------|------------|--------|---------------|------------|
| K1 | Raters not found | M | H | <2 named by Oct 15 | `[EXTERNAL GATE]` Ask early and verify availability; shrinking to 100 is a `[HUMAN SCOPE DECISION]` under §18.4, not an automatic response. |
| K2 | Rater agreement too low (κ/kappa < 0.5 twice) | M | H | T1 failure | `[PROTOCOL FREEZE]` Revise handbook/negative anchors once only under a versioned rule; shrinking to audited subset is a `[HUMAN SCOPE DECISION]` and cannot erase prior disagreement. |
| K3 | H1 inconclusive (CI straddles Δmin) | M | M | CI straddles 15pp | `[PROTOCOL FREEZE]` Extend to 300 only if prospectively authorized before outcome-driven selection; otherwise report inconclusive. |
| K4 | Judge fails gates (§7.5) | M | M | Gates fail | `[HUMAN SCOPE DECISION + PROTOCOL FREEZE]` A humans-only smaller calibration changes the study and must state power loss; it is not an automatic substitute. |
| K5 | All cells vacuous | M | M | Early GISMo diagnostics | `[PREP/REPORT]` Report quantified vacuity. Any “abstain-by-default” operating policy is a separate `[PROTOCOL FREEZE]` decision under §8, not implied by reporting. |
| K6 | Calibration/test not exchangeable | M | H | KS diagnostic on gold rank | `[PROTOCOL FREEZE — PR07]` Diagnostics do not establish exchangeability; no post-test cell merge. Weighted conformal or merging is a separately frozen/exploratory analysis; state the limit. |
| K7 | Validation scores missing | M | H | M-B check fails | `[PREP/REPORT]` Inventory/regenerate authorized GISMo validation scores. An LLM subsample is a matched-arm and `[PROTOCOL FREEZE]` decision, not an automatic replacement. |
| K8 | Stage 3 late/fails | M | M | No 327 by Nov 1 | `[PREP/REPORT]` **OBSOLETE — 327 recorded verified.** Stage-2 adapter hedge exists; no fresh training state inferred. |
| K9 | Inference too slow | M | M | 200-case timing | `[PROTOCOL FREEZE]` A stratified subsample must be prospectively defined under §13.3; do not select it after seeing performance. |
| K10 | API keys/budget unavailable | M | H | No key by Oct 22 | `[EXTERNAL GATE + PROTOCOL FREEZE]` Obtain authorized provider/key and budget; any free/open-weight model substitution must be pinned and frozen before confirmatory use. |
| K11 | Ethics blocks usefulness study | M | L | No answer by Oct 12 | `[EXTERNAL GATE + HUMAN SCOPE DECISION]` Obtain written institutional classification before participant activity. Silence is UNKNOWN; dropping H4 requires an explicit human scope decision, not an expired date. |
| K12 | Allergen n too small to certify | H | M | Arithmetic fixed | `[PROTOCOL FREEZE + EXTERNAL GATE]` Expand only with a prospective case/sampling plan, rights and qualified review; otherwise report the bounded interval at n=208. |
| K13 | Claim breaks under review | M | H | Red-team finds forbidden | `[PREP/REPORT]` Audit under §24.2 and retain a traceable negative/changed claim; do not delete inconvenient evidence. |
| K14 | Novelty duplicate found | L–M | H | L42 search | `[HUMAN SCOPE DECISION]` Review exact overlap and decide whether/how to re-scope; one lead does not auto-change C1. |
| K15 | Advisor says scope mismatch | L–M | H | U2 reply | `[HUMAN SCOPE DECISION]` Resolve with the advisor and human architect before redirecting scope/spend. |
| K16 | No qualified ARR contributor | M | M | None by Dec 1 | `[HUMAN SCOPE DECISION]` Thesis-only or later-cycle submission is a portfolio decision, not automatic. |
| K17 | Licenses block release | M | L–M | M-F, §10.2 | `[EXTERNAL GATE]` Release only artifacts whose rights and provenance permit it; code/aggregates are not automatically cleared. |
| K18 | Scope creep | H | H | >MUST tier in progress | `[HUMAN SCOPE DECISION]` A cut-line review may propose options; no automatic “cut from bottom” without recording proof removed, destination retained, and revisit trigger (§18.4). |
| K19 | Overnight job overspend | L | M | Receipt > cap | `[SAFE OPERATIONAL GUARD]` Enforce per-job caps and refuse-to-start; no automatic paid retry or cap increase. |
| K20 | Teammate unavailable | M | M | Tasks slip 2 weeks | `[EXTERNAL GATE + HUMAN SCOPE DECISION]` Confirm capacity before reassignment; compression/redistribution is not assumed. |
| K21 | R0 numbers leak into thesis | M | H | Figure without claim ID | `[PREP/REPORT]` Enforce §15.2 claim-ID/provenance rule and reconstruct before use. |
| K22 | Non-thesis workload | H | H | Weekly hours below plan | `[HUMAN SCOPE DECISION]` Obtain actual hours and revise schedule/resources; select §18.4 compression only by explicit human decision. |

**Pre-mortem triggers (SSoT §19.3 — reopen interpretation on these):** These signals initiate review, not an automatic strategy/protocol change. Any contribution, sample, model-arm, or thesis-scope change still requires its cited protocol gates and HUMAN decision.
- H1 lower Wilson bound ≥ Δmin → lead with benchmark validity
- H1 upper bound < Δmin → lead with collapse map + abstention
- H1 straddles Δmin → report inconclusive; any extension requires prospective protocol authorization (K3)
- κ (kappa) < 0.6 twice → review one versioned handbook revision; any subset reduction requires human scope decision (K2)
- All cells vacuous → reframe C3 as quantified limit
- Novelty duplicate → re-scope C1
- Advisor mismatch → re-scope before spend
- Exchangeability diagnostic fails → state limit; any weighted conformal/merge variant must meet PR07 freeze (K6)
- No ARR contributor by Dec 1 → thesis only

---

## Protocol Resolutions (from SSoT §16 — must resolve before affected work)

| Flag | Conflict | Resolution Required |
|------|----------|---------------------|
| PR01 | §6.1 conditional rescue vs §6.9 joint rescue; > vs ≥ Δmin | Choose estimand, denominator, operator prospectively |
| PR02 | Gold forced A yet both-rater N = label noise | Specify raw/adjudicated/gold policy; implications for V≥H |
| PR03 | Equal allocation vs population inference; top-4 vs top-5 | Resolve allocation, weighting, stratum minima, consistent k |
| PR04 | Cell uses gold substitute (unavailable at deployment) | Separate descriptive oracle strata from deployable partition |
| PR05 | Judge chosen on test audit; claim freeze before test | Establish dev/audit/cal/eval separation; disclose exposure |
| PR06 | Filter/rerank/truncation/empty-pool mismatch; infinity handling | Define returned-set success, filter ordering, guarantee population |
| PR07 | KS called exchangeability check; test-driven merging | Diagnostics cannot prove assumption; freeze variants or label exploratory |
| PR08 | Coverage grid .50–.95 vs α={.05,.1,.2,.3,.5} | Freeze single grid and status semantics |
| PR09 | Lossy normalization vs v23 collision findings | Validate identity/food-form behavior before freeze |
| PR10 | Different κ point/lower-bound gates; Wilson vs exact | State each gate's population/statistic/interval method |
| PR11 | Train-only graph vs "no val/test pair" despite seen overlap | Resolve row provenance leakage test without changing splits |
| PR12 | Endpoint example seen=true/count=0/unseen simultaneously | Replace only via validated contract design |
| PR13 | Annotation pending ethics vs clearance-before-study | Obtain institutional authorization; prep continues |
| PR14 | Seed placeholders marked VERIFIED; checker accepts dummy | Implement real artifact validation separately |
| PR15 | Synthetic tests omit bounded-M/infinite/missing-cell | Independent validation before reuse |

---

## Operating Rhythm (from SSoT §24)

- **Daily (10 min):** Morning review of overnight jobs (§13.8)
- **Monday (30 min):** Claims register diff; overnight receipts; plan week against cut line
- **Wednesday (15 min):** Check gates due; chase people
- **Friday (30 min):** Update status block (generated); write one thesis paragraph from week's evidence; decide cuts
- **Biweekly advisor update (5 lines):** Evidence IDs, gates moved, plan changes/trigger, precise request, next action

**Freeze A (Oct 22):** Audit protocol, Δmin, strict/lenient, arms, blinding, normalization, primary arm/stratum, κ bar, timestamp/hash.

**Freeze B (Nov 6):** Deployable cells, n floor, M, α grid, labeling function/selection, filter/rank/set semantics, infinity/missing, informativeness, diagnostics/sensitivities, evaluation access. **Neither freeze completed here — resolve §16 first.**

**Forbidden claims (§24.2 — red-team every sentence):** No "zero/<1% violations" without n≥299; no "safe/safer/clinical"; no "human-valid coverage" from judge; no "first/novel" before 4.6+second reviewer; no Vietnamese guarantee; no "measured contamination"; no R0 figures without reconstruction; no cross-task rankings; no "four trained models/production-ready AI"; no "zero cost/local-only" without runtime proof; no user outcome claims; no untraceable numbers; no "calibrated without cell/label/assumption"; no "hypothesis confirmed" when interval includes null.

---

## Evidence Integrity Rules

- Never mark COMPLETE because code was written — require executable evidence (checkpoint, hash-bound predictions, scored evaluation).
- Never compare cancelled run or paper number to locally executed result.
- Never report metric without dataset, split, protocol version, seed count.
- Negative results (SAG, dual encoder, CLIP, flavor, VLM, behavioral, voice) are thesis contributions — report honestly.
- Do NOT cite: 22.04% Senath (unverified journal abstract); 0.2222 HGAT (synthetic); 0.00% allergen (self-labeled, withdrawn).
- Every number in thesis carries a claim ID from register (§15). Unverified = say so.

---

## Appendix: Mapping from v3 Backlog (for reference only)

| v3 Epic | v4 Treatment |
|---------|--------------|
| EPIC 3/3b/3c (M1 GNN/SAG/dual encoder) | Bounded negatives chapter; L13′ = shift-aware selector |
| EPIC 10 (M2 Mistral Stage 1–3) | L44 (Stage 3 completion + inference); Stage-2 adapter hedge |
| EPIC 11 (M3 VLM) | Bounded negative; no further spend |
| EPIC 12 (M4 CLIP) | Bounded negative; no further spend |
| EPIC 5/5b (Allergen) | L39 (matched arms + expansion + adjudication) |
| EPIC 4 (Compound explanation) | C4/H4 (usefulness study if ethics) |
| EPIC 9 (Thesis 14 chapters) | L45 (14 v4 chapters per §21) |
| L29 (Photo chain) | Bounded negative; §12 product proof replaces |
| L31 (AI runtime cert) | Separate infra work; not thesis contribution |
| L35 (Defense pack) | L46 (rebuilt answers + rehearsal) |
