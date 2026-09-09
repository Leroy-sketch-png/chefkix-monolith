# IRON CHEF v3: LEAD BACKLOG
> **AUTHORITY:** `academic_vision_and_strategy.md` is canonical. This backlog is derived from §4 (Nine Contributions), §6 (Execution Plan), and the LEAD evidence ledger. When they conflict, the strategy file wins.
> **CURRENT-STATE OVERRIDE — 2026-09-09 (v76):** Historical narrative below is superseded. Atomic truth is `ai/out/iron-chef-lead-v2/BACKLOG_LEDGER.md`. Do not quote fabricated metric claims (Hit@1 0.222, allergen 0.00%, or the removed 0.94/0.81 Voice-Vision grounding comparison) as current evidence.

---

## Current executable state (v22+)

| Ledger | Count | Key open items |
|:---|:---|:---|
| **VERIFIED** | 15/35 | corrected five-arm MISKG ablation (negative), GISMo reproduction, FooDB factuality, allergen concordance (E2), RecipeNLG negative, Recipes5k baseline |
| **PARTIAL** | 16/35 | Senath preferences complete/DPO running, USDA, FlavorDB, allergen reviews, LLM allergen raw pilot, cross-modal, bounded photo-to-evidence chain |
| **OPEN** | 4/35 | Context-aware IRON CHEF model, VLM training, explanation user study, defense evidence |

### What is RUNNING now
- **OFF allergen snapshot:** v75 replaces the 503-record/6,400-row systematic sample ceiling with an immutable-revision scan of all 4,726,416 current food rows. It atomically emits 351,351 eligible English ingredient-label records at SHA `807fe58e...9c68`; independent reconstruction passes 16/16 fields, 16/16 mutations reject, and seven tests pass. The raw 1,225 allergen/2,379 trace tag vocabularies contain substantial malformed/non-allergen contamination. L20 remains PARTIAL pending normalization, independent labels/negatives, clinical validity, production lifecycle, rights review, and user evidence.
- **Senath DPO:** stage 1, 7,500 preferences, and bounded DPO Segments 1-2 are transactionally complete through exact step 87/261. Segment-2 manifest is `f34d33ff...4a84f`; Segment 3 was launched once with that exact pin and is authenticated RUNNING. Pulled/local Segment-3 source is byte-identical `9887b7e5...6fbc`; generic chain mutations reject 21/21, package mutations reject 12/12, and ten transactional/refusal tests pass. Near-zero Segment-2 loss is not quality evidence: exact preference correctness remains 0/7,500 and rejected responses are systematically longer (median 5.14x), so shortcut separability remains explicit.
- **Senath downstream gate:** DPO uses the frozen 7,000 train/500 disjoint validation split from 7,500 unique bounded preference identities. Transport validates before promotion, preserves canonical evidence on failure, and now resolves Kaggle 403 absence only after complete authenticated inventory. Post-DPO SFT/inference remain gated on verified DPO completion.
- **Substitution feedback:** receipted candidates only. V70 proves exact offline recovery after actual isolated source-volume deletion. V71 adds preview-first local archival: keep at least two verified copies, hash-bind the complete inventory, preserve corrupt/unknown artifacts, reject stale/tampered/escaping/colliding plans, and move old verified copies into unique batches with zero deletion. Exact-source image `f7933732...ffb80` passes 10/10; tests are 24/24 and retention attacks 7/7. Automated quiescence, policy/scheduling, replicated storage, external/multi-instance durability, orchestrated recovery, representative events, and a scorer remain open.
- **Allergen adjudication protocol:** frozen (`3ef4eff1...`), 228 blinded FDA cases. Awaiting two independent reviews.
- **Allergen LLM benchmark:** matched 208-query protocol frozen; Mistral `mistral-small-2603` raw arm is 208/208 with zero errors and independently validated hashes/metadata. GPT-4o/Gemini and two-reviewer adjudication remain absent; no score is authorized.
- **L13 context-model falsifiers:** COMPLETE and independently validated. Corrected context-free SAG is rejected (`0.1171%` mean Hit@1). The true-context Recipe1MSubs dual encoder is also rejected: `16.1223%` overall Hit@1 and `0.016601` unseen MRR versus GISMo `20.6941% / 0.043084`. L13 remains open because no candidate passed its frozen gate.
- **AI runtime certificate:** loopback 700/700, image/restart/load, privacy-bound Mistral identity, service/proxy auth, rotation, and enforced previous-key expiry PASS. V59 image `6904519f...3f7b` rejects the previous key live after deadline while active/health/receipt remain valid; two incomplete configurations exit 3. V1 is preserved rejected; v2 passes 10/10 gates and 13/13 mutations. Managed secret delivery, clock policy, revocation audit, multi-replica ordering, mTLS/ingress, quality, safety, calibration, soak, fallback, recovery, and product proof remain open.
- **AI rate-limit boundary:** all 17 decorated routes now share the registered limiter, emit headers, and honor configured recipe budgets. Full suite passes 328/16 skipped. Image `055c8f25...3adb` proves 60 successes, budget 59 -> 0, request 61 rejected, wrong-key isolation, health exemption, clean dependencies/secrets/lifecycle, 11/11 gates, and 12/12 mutations. This remains process-local/source-IP only; distributed identity-aware quotas and ingress enforcement are open.
- **AI complete route throttling:** all 32 protected routes are now limited; the 15 prior omissions and 14 scattered literals are eliminated through validated recipe/read/compute/write/admin tiers. A rejected 335/1 full run exposed and repaired settings-mock coupling. Final suite passes 336/16 skipped. Image `ea8c8ec4...39f1` proves four independent budgets, wrong-key ordering, health exemption, clean lifecycle, 9/9 runtime, 12/12 static, and 14/14 result mutations. Distributed/user-aware/proxy-trust/ingress enforcement remains open.
- **AI distributed rate-limit state:** production now requires authenticated Redis, masked credentials, a fixed namespace, fail-closed backend errors, disabled local fallback, bounded timeouts, startup probing, and compose health ordering. Final suite passes 347/16 skipped. Image `1fba2c31...a15fd` proves one 3/minute budget across two replicas and a replacement, 429 excess, 4.188-second outage failure, dead-backend startup exit 3, and clean secrets/resources. Static/runtime/independent gates pass 14/14, 11/11, 15/15. Trusted proxy/user/tenant/ingress/multi-region/soak evidence remains open.
- **Voice-Vision Graph-RAG:** current end-to-end capability REJECTED. Frozen five-stage audit binds 783 L29 cases and 100 runtime requests: voice 0, visible-food predictions absent, answers/citations/labels absent, runtime unavailable 100/100. Bounded graph proxy is 536 complete / 179 paired-recipe-correct. Fabricated comparison code was removed; 11/11 claim inflations reject.

---

## The Four Trained Models (Thesis M1–M4)

| Model | Epic | Status | Next action |
|:---|:---|:---|:---|
| **M1 IRON CHEF GNN** (HGAT + SAG) | EPIC 3 + EPIC 3b | Corrected SAG and true-context dual encoder VERIFIED E3 NEGATIVE; accepted model absent | Preserve both rejections; reopen only with a materially stronger preregistered graph-preserving/OOD candidate |
| **M2 ChefKix-Mistral-7B** (SFT+DPO+SFT) | EPIC 10 | Stage 1 + preferences + DPO segment 1 verified; monolithic DPO rejected; DPO segment 2 RUNNING | Advance only hash-pinned completed segments; launch frozen post-DPO SFT after segment 6 verifies |
| **M3 ChefKix-VLM** (QLoRA) | EPIC 11 | OPEN; real collator E2 + 50-candidate/20-label per-file rights snapshot E3 + two-reviewer pack; label truth absent | Complete two independent reviews, adjudicate and replace rejects to accepted quotas, freeze hash-disjoint splits, then repackage/launch |
| **M4 ChefKix-CLIP** (projection fine-tune) | EPIC 12 | VERIFIED E2 trained projection NEGATIVE; R@1 34.10→35.42%, +2pp gate missed | Preserve rejection; reopen only with rights-resolved prospective/external protocol |

---

### L29: Photo → Evidence Graph → Explanation ⚠️ PARTIAL E2

- [x] Hash-verify all 783 Recipes5k publisher test images
- [x] Retrieve against all 783 pinned base-CLIP recipe texts without target access
- [x] Map only unique normalized-exact ingredient identities into official FooDB
- [x] Reconstruct exact shared-compound edges and bounded explanations
- [x] Separate graph-production coverage from correct end-to-end attribution

**Result:** recipe retrieval is correct for 267/783; 536/783 produce a graph/explanation, but only 179/783 (`22.8608%`) do both. The 357 remaining complete graphs describe the wrong retrieved recipe. Protocol/result validation rejects 5/5 and 6/6 corruptions.

**Boundary:** `PARTIAL E2 BOUNDED OFFLINE CHAIN / USER-CAMERA + PRODUCT OPEN`. No ingredient-detection, hidden-ingredient, safety, functional-chemistry, external-transfer, serving, or commercial claim.

---

## PHASE 1: Data Acquisition & Graph Reconstruction

### EPIC 1: Multi-Signal Knowledge Graph ✅ DELIVERED (leakage-safe)

- [x] Parse MISKG: 80,044 raw rows, 39,194 unique directed pairs, SHA-256 `362b2ef5...`
- [x] Unified vocabulary: 8,552 ingredients, reciprocal-grouped split
- [x] Node features: Epicure 300-D || USDA 5-D || FooDB 206-D = 511-D total
- [x] Official FooDB fingerprints: 971 foods, 206 compounds, SHA-256 `8490cd6a...`
- [x] Substitution subgraph: `substitution_subgraph.pt` (lean, 438 MB)
- [x] Export `ingredient_vocab_v2.json`, `graph_sample.json` to `chefkix-fe/public/data/`

**Evidence:** E3 source hashes pinned; v11 leakage-safe protocol validated.

**Open:** USDA vocabulary only 14/6,632 non-special GISMo classes mapped (E2 fail-closed). FlavorDB CC BY-NC-SA 3.0 — noncommercial only.

---

### EPIC 2: Matched Baseline Reproduction ✅ VERIFIED

- [x] GISMo reproduction: 20.694% Hit@1 / 0.31636 MRR overall; 1.206% / 0.04308 unseen
- [x] Stratification: 6,932/10,747 repeat pairs; 3,815 unseen pairs
- [x] Lookup-frequency: 18.0% Hit@1 overall → 0.0% on unseen pairs
- [x] All fine-tuning variations rejected: OOD-weighted, shallow head, full upstream — all reduce unseen MRR below gate
- [x] Senath audit: 11 paper/release contradictions found; 6 blockers; L12 = OPEN

**Evidence:** E3 round-trip for GISMo; v2/v3 reports independently reconstruct.

---

## PHASE 2: M1 HGAT Training & Novel Architecture

### EPIC 3: HGAT v11 Full-Catalog Baseline ✅ E3 VERIFIED

- [x] Leakage-safe split; full-vocabulary evaluation (8,552 candidates); 3-seed training
- [x] **LEGACY RESULT, INTERPRETATION CONFOUNDED:** the v11 semantic slice reports **0.495%** vs. legacy all-signals **0.252%**, but missing semantic rows were random identities and feature linkage was lossy
- [x] Legacy slice outputs recorded, but random-missing and identity confounds prohibit interpreting them as clean Epicure/USDA/FooDB causal contributions
- [x] Export ablation results to `chefkix-ai-service/models/ablation_results.json`

**Evidence:** `ai/out/iron-chef-lead-v11/`. 3/3 seed corruption attacks rejected.

---

### EPIC 3b: SAG — Signal-Adaptive Gating ✅ VERIFIED E3 NEGATIVE

**Research question:** Can a learned per-signal attention gate fix the verified negative transfer?

**Architecture (Signal-Adaptive Gating):**
- `SignalGate`: cross-attention between query context and each signal branch (semantic/nutritional/chemical)
- Produces per-query scalar gates: softmax-normalized weights summing to 1
- Sparse gate → model suppresses noisy signal for this substitution context

**Tasks:**
- [x] Implement corrected node-conditioned `SignalAdaptiveHGAT` falsifier in an isolated Kaggle package
- [x] Run 3 seeds (42/43/44) under the same split/loss/full-catalog protocol as EPIC 3
- [x] Ablation: semantic, nutritional, chemical, naive fusion, and SAG
- [x] Log availability-masked gate distributions and independently reconstruct them

**Corrected v37 acceptance criteria (predeclared before remote result):**
- minimum corrected SAG seed Hit@1 > maximum corrected semantic-only and naive-fusion seed Hit@1 → accepted
- If SAG fails: negative result is still thesis contribution — report trung thực


**Evidence state:** `VERIFIED E3 NEGATIVE`. Mean Hit@1: semantic-only `0.3964%`, naive fusion `0.3333%`, SAG `0.1171%`. Frozen acceptance is rejected; 8/8 result mutations fail. This is context-free MISKG evidence and establishes no novelty or product value.

### EPIC 3c: Recipe1MSubs True-Context Dual Encoder ❌ VERIFIED E3 NEGATIVE

- [x] Freeze official `49,044/10,729/10,747` rows, all 6,653 candidates, and row-aligned titles
- [x] Restrict title vocabulary to train; consume source + other canonical ingredients + title
- [x] Train seeds 42/43/44 with full-catalog loss and validation-only epoch selection
- [x] Keep test inaccessible until every seed selection exists
- [x] Independently reconstruct every test rank from saved arrays and weights

**Evidence state:** protocol `a3f0ee94...f3d90` rejects 8/8 corruptions; result reconstruction rejects 9/9. Every seed selects epoch 5. Mean overall Hit@1 `16.1223%` loses to GISMo `20.6941%`; mean unseen MRR `0.016601` loses to `0.043084`; all frozen gates fail. Candidate `REJECT`. L13 remains open because a rejected model is evidence, not capability.

**Identity boundary:** GISMo canonical-vocabulary pair identity yields test `6,932/3,815` seen/unseen; Senath raw/normalized identity yields `6,866/3,881`. Both are valid only inside their own protocols and must not be mixed.

**What professors SEE:** Scientific method in action — verified failure, diagnosis, proposed fix, honest result.



---

## PHASE 2b: M2 LLM Fine-Tuning

### EPIC 10: ChefKix-Mistral-7B — Custom LLM Training Pipeline 🔴 ACTIVE (M2)

This is NOT reproduction. We built our own 3-stage pipeline fixing 11 contradictions, generating our own preference pairs, designing 6-segment checkpointed execution.

**Stage 1 — SFT (LoRA 64/32, 15K examples, 3 epochs):**
- [x] Segment 1 verified: step 90, all 12 checkpoint files, independent manifest match (E3)
- [x] Segment 2 verified: step 187, all 12 checkpoint files, best eval loss `0.18699303269386292`
- [x] Segment 3 v2 verified: exact step 280, 12 checkpoint files, full resume state, manifest `5160b3cd...09383`
- [x] Segment 4 verified: exact step 374, 12 checkpoint files, full resume state, manifest `ab3625a1...3299e6`, eval loss `0.1930581`
- [x] Segment 5 verified: exact step 468, 12 checkpoint files, manifest `1e1ce0f6...f00d`
- [x] Segment 6 verified: exact step 561, final manifest `ae51d556...57e99`; retained-best adapter exported rather than worse final checkpoint
- [x] Hash-bound stage-1 adapter: eight files equal the retained step-187 best adapter

**Stage 2 — Preference Generation + DPO:**
- [x] Generate and transactionally validate 7,500 unique bounded DPO pairs from stage-1 errors; exact provenance bound
- [x] DPO Segment 1 verified: exact step 44/261, 12 checkpoint files, manifest `d726c48a...09df`
- [x] DPO Segment 2 verified: exact step 87/261, 12 checkpoint files, manifest `f34d33ff...4a84f`
- [ ] DPO Segment 3 RUNNING with exact Segment-2 predecessor pin; Segments 4-6 gated

**Stage 3 — Post-DPO SFT + Inference:**
- [ ] Post-DPO SFT; 10,747-case inference under frozen evaluator
- [ ] Hash all predictions; stratify repeated/unseen; compare vs. GISMo

**Evidence state:** L12 = `PARTIAL E3 STAGE-1 + 7,500 PREFERENCES + DPO SEGMENTS 1-2 COMPLETE / DPO SEGMENT 3 RUNNING`. Historical failures and incomplete transports remain preserved and rejected. Only 87/261 DPO steps are proven; no final DPO adapter, post-DPO model, predictions, or test score exists. Near-zero preference loss is treated as shortcut-confound evidence, not model quality.

---

## PHASE 3: M3 + M4 Visual Models

### EPIC 11: ChefKix-VLM — Food Vision-Language Model (M3) 🔴 OPEN

**Motivation:** v16 = base CLIP 8.02% mAP; v17 = frozen probe 55.02% but category prior 64.95% beats it. QLoRA fine-tuning may learn genuine food understanding.

- [x] Audit historical SmolVLM adapter: four mock text examples, two absent images, `<image>` stripped, one step, no evaluation; rejected as VLM evidence and all producer/deployment paths retired fail-closed
- [x] Write governed real multimodal SmolVLM-256M QLoRA trainer; exact model revision and real pixel/answer-only collator validate
- [x] Rehash 4,826 Recipes5k rows; resolve 197 mirror path drifts; remove 54 duplicate-image clusters and all effective cross-split overlap
- [ ] Replace unknown-license Recipes5k mirror with rights/attribution-bound ingredient-image corpus; launch guard currently rejects before model load
- [x] Acquire and independently reconstruct a 33-image/14-label per-file public-domain Commons transport pilot; 33/33 remote identities/licenses and local bytes pass
- [x] Resumably expand without mutating v47 to 50 unique candidates covering 20/20 labels at >=2; 50/50 live rights/hash/byte reconstruction and 17/17 corruptions pass
- [x] Build two independently shuffled 50-item visual review packs with hidden source context, byte-exact opaque assets, sealed identity key, and 7/7 attack rejection
- [ ] Complete both reviews, adjudicate visible/primary-food/real-photo labels, replace rejects to accepted minimum quotas, then create hash-disjoint splits
- [ ] Training data: Recipes5k + Food-101 + IRON CHEF instruction pairs
- [ ] Method: QLoRA rank-16 to 64, 3-5 epochs, Kaggle T4 or A100
- [ ] Evaluate: ingredient mAP vs. base CLIP (8.02%) and vs. category prior (64.95%)
- [ ] Target: beat category prior — genuine learning, not memorization
- [ ] Serve: GGUF Q4 via llama.cpp; update `ml_registry.py`

**Evidence state:** `OPEN / E2 REAL COLLATOR + E3 RIGHTS-BOUND 50-CANDIDATE MINIMUM-QUOTA SNAPSHOT + TWO-REVIEWER PACK / REVIEWED SCALE + TRAINING ABSENT`. Runtime collation and all per-file rights/bytes reconstruct; candidate quotas are not accepted-label quotas and review answers remain zero. No split, training, or visual-learning result exists.

---

### EPIC 12: ChefKix-CLIP — Food Cross-Modal Retrieval (M4) ⚠️ TRAINED PROJECTION REJECTED / SERVING OPEN

**Motivation:** v15 zero-shot baseline: R@1 34.10%, R@5 77.01%, R@10 91.19% (Recipes5k). Installed checkpoint REJECTED (no provenance). We train our own.

- [x] Freeze/validate a three-seed rank-64 residual projection protocol over pinned CLIP vectors; 8/8 protocol corruptions rejected
- [x] Train on 3,409 Recipes5k pairs and select epochs 10/13/2 only on 634 validation pairs
- [x] Evaluate all 783 test identities plus 771-row exact-duplicate-clean sensitivity; independent weight/metric reconstruction and 9/9 result mutations pass
- [ ] Training expansion: rights-resolved Recipes5k + Food-101 or prospective external pairs; do not tune against the already exposed test set
- [ ] Identity-backed FAISS index mapping to IRON CHEF ingredient vocab
- [x] Evaluate R@1/5/10/MRR vs zero-shot: R@1 `34.10→35.42%`, but +1.32pp misses frozen +2pp gate and R@5/R@10 regress; decision `REJECT`
- [ ] Replace `recipe_faiss.index` or update `ml_registry.py` only after a future accepted external/rights-cleared result

**Evidence state:** `VERIFIED E2 TRAINED PROJECTION NEGATIVE / RIGHTS-LIMITED / PRODUCT TRANSFER OPEN`. Historical installed checkpoint remains rejected; new weights are also rejected and not installed.

---

## PHASE 4: Safety & Explanation

### EPIC 5: Allergen Policy ⚠️ PARTIAL E2 — REAL BENCHMARK PENDING

- [x] FDA Big 9 + EU 14 families; tri-state policy (SAFE/UNKNOWN/BLOCKED)
- [x] v7 concordance: declared recall 400/408 (98.04%), trace recall 22/40 (55%)
- [x] v13 FDA export: 29,278 records, 7,047 policy candidates, 126 cross-contact
- [x] v14 blinded adjudication protocol frozen (`3ef4eff1...`), 228 cases — INSUFFICIENT_REVIEW
- [ ] Two independent qualified reviews + adjudication under frozen protocol
- [ ] Verified negatives + representative runtime inputs

---

### EPIC 5b: Real LLM Allergen Safety Benchmark — 🟡 MATCHED MISTRAL RAW ARM COMPLETE / RESULT UNAUTHORIZED

No novelty or first-of-kind claim is accepted. One matched raw arm is complete; a head-to-head benchmark and safety result are not.

- [x] 115 queries built: 13 allergen groups (FDA Big 9 + EU 14), ≥10 per major group
- [x] Query types: direct, cross-reactivity, multi-allergen, trace, ambiguous
- [x] Synonym traps included: groundnuts, arachide, marzipan, surimi, malt extract, ghee, satay
- [x] 115-prompt source file exists; the declared protocol self-hash does **not** reconstruct and must be repaired before a benchmark run
- [x] Scoring rubric file exists; its self-hash reconstructs, but its undeclared-cross-reactivity rules conflict and must be repaired before a benchmark run
- [x] Files: `models/allergen_benchmark_protocol.json`, `models/allergen_scoring_rubric.json`
- [x] Raw pilot executed: deterministic IRON CHEF classifier + Mistral `mistral-small-latest` + Cohere `command-r-plus-08-2024`; 345 complete cells, zero recorded provider errors, hashes reconstruct
- [x] Repair to a matched 208-query/208-family protocol where every system performs the same three-candidate task; structurally encode all 13 declared allergen groups and cross-contact policy
- [x] Pin and persist requested/provider-returned identity, response ID, timeout, per-cell atomic checkpoint, attempt, latency, finish, usage, response hash, and sealed run hash
- [x] Run Mistral `mistral-small-2603`: 208/208 schema-valid cells, zero errors, 208 unique provider response IDs; raw validation rejects 8/8 corruptions
- [ ] Run GPT-4o `gpt-4o-2024-08-06` and Gemini `gemini-2.5-flash` when credentials are configured
- [x] Generate two independently shuffled 208-item v2 review instruments and sealed identity key; validation rejects 6/6 corruptions and response-style/single-system blinding limits are explicit
- [ ] Complete two independent reviews and adjudication under a consistent frozen rubric
- [ ] Compute: violation rate + Wilson 95% CI + abstention rate per system
- [ ] Hash-bind all responses + scores
- [ ] Report: the table — who recommended dangerous substitutes, who caught them

**Evidence state:** `PARTIAL E2 MATCHED 208-CASE MISTRAL RAW ARM / GPT-4O + GEMINI + REVIEW OPEN`. The old heuristic scorer is retired fail-closed. Raw calls do not authorize safety or comparative rates.

**Allowed defense claim now:** under a frozen matched protocol, Mistral `mistral-small-2603` produced 208/208 schema-valid raw responses with zero errors and independently reconstructing evidence. Do not show a winner table, safety rate, zero-violation claim, GPT-4o/Gemini result, or novelty claim until remaining arms and independent adjudication pass.

---

### EPIC 4: Compound Explanation Engine ✅ FACTUALITY VERIFIED / USEFULNESS OPEN

- [x] Official FooDB: 971 foods, 206 compounds, 125,992 links, SHA-256 `8490cd6a...`
- [x] Exhaustive factuality: 471,906 pairs, 0 errors; 512 response contracts pass
- [ ] **User study (N≥20):** Likert-scale usefulness rating with vs. without compound records
- [ ] Ethics review before recruit; preregister analysis plan
- [ ] Target: ≥70% rate compound records "helpful" or "very helpful"

---

## PHASE 5: Thesis & Defense (14 Chapters)

### EPIC 9: Thesis ⚠️ PARTIALLY BLOCKED

| # | Chapter | Status |
|:---|:---|:---|
| 1 | Introduction: Three Failures | Evidence collected; draft needed |
| 2 | Related Work | Drafted |
| 3 | Multi-Signal Food KG | Verified; USDA partial |
| 4 | IRON CHEF GNN: Fusion Paradox & SAG (M1) | corrected SAG and true-context dual encoder VERIFIED E3 NEGATIVE; accepted model absent |
| 5 | ChefKix-Mistral-7B (M2) | segments 1-5 verified; segment 6 running; no predictions |
| 6 | ChefKix-VLM (M3) | historical mock text-only LoRA rejected; real multimodal training pending |
| 7 | ChefKix-CLIP (M4) | real three-seed frozen-feature projection trained and rejected; external/serving path open |
| 8 | Compound Explanation | Factuality E3; user study open |
| 9 | Real LLM Allergen Benchmark | matched Mistral raw arm complete; GPT-4o/Gemini + adjudication open |
| 10 | Selective Abstention | Protocol frozen; no candidate |
| 11 | Photo → Intelligence Pipeline | Components exist; integration open |
| 12 | System Architecture | Drafted |
| 13 | Evaluation & Ablation | v11 done; Recipe1MSubs open |
| 14 | Conclusion & Future Work | Drafted |

- [x] Defense adversarial pack: 24 questions × 17 domains; 4/4 mutation rejection — L35 PARTIAL E2
- [ ] Recorded/scored human rehearsal — required for L35 VERIFIED

---

## Current Priority Matrix

| Priority | Task | Epic | Status | Next action |
|:---|:---|:---|:---|:---|
| 🔥 ACTIVE | Senath downstream reproduction | EPIC 10 | Stage-1 + 7,500 preferences + DPO segment 1 VERIFIED; monolithic DPO rejected; DPO segment 2 RUNNING | Ingest/validate each bounded segment after COMPLETE; release its pinned successor, then post-DPO SFT |
| ✅ COMPLETE | Corrected SAG falsifier | EPIC 3b | VERIFIED E3 NEGATIVE | Preserve result; do not tune or promote rejected architecture |
| ✅ COMPLETE | True-context dual encoder falsifier | EPIC 3c | VERIFIED E3 NEGATIVE | Preserve rejection; require a materially stronger frozen candidate before reopening L13 execution |
| ⚠️ PARTIAL | Photo → evidence graph → explanation | L29 | 179/783 correct end-to-end chains; exhaustive reconstruction PASS | Require rights-cleared external/user-camera evidence and product lifecycle before promotion |
| ⚠️ PARTIAL | AI-service runtime certification | L31 | E3 image/restart/load + provider identity + authenticated proxy + service/subject rotation and live expiry + stable distributed pseudonymous per-user quotas PASS | Add tenant/plan policy, managed automation/revocation audit, clock policy, mTLS/ingress/WAF, multi-region, soak, fallback-quality, recovery, fairness, and product evidence |
| 🔥 NEXT | Allergen matched arms + independent review | EPIC 5b | MISTRAL 208/208 RAW / RESULT UNAUTHORIZED | Configure GPT-4o/Gemini credentials, execute same frozen protocol, then complete two reviews/adjudication |
| 🟡 QUEUED | ChefKix-VLM real multimodal QLoRA | EPIC 11 | REAL COLLATOR + 50 RIGHTS-BOUND CANDIDATES + TWO-REVIEWER PACK; LABEL TRUTH BLOCKED | Complete both reviews, adjudicate/replace to accepted quotas, freeze splits, repackage exact protocol, launch |
| ✅ COMPLETE NEGATIVE | ChefKix-CLIP frozen-feature projection | EPIC 12 | VERIFIED E2 REJECTED | Preserve weights as evidence; do not deploy; reopen only under external/rights-cleared protocol |
| 🔥 ACTIVE | DPO + Post-DPO SFT | EPIC 10 | Monolithic DPO timed out at 91/261; bounded segment 1/6 COMPLETE at 44/261; segment 2 RUNNING; post-DPO SFT gated | Transactionally advance the six-segment state-preserving chain; auto-advance SFT only after final DPO verification |
| 🟡 QUEUED | User study (N≥20) | EPIC 4 | NOT STARTED | Ethics review → recruit |
| 🟡 QUEUED | USDA vocabulary expansion | EPIC 1 | PARTIAL E2 | High-frequency adjudication |
| 🟡 QUEUED | Allergen adjudication reviews | EPIC 5 | BLOCKED | Two qualified reviewers |
| ⬜ IF TIME | Voice-Vision Copilot | — | Tier C | After M1-M4 complete |

---

## Evidence integrity rules

- Never mark COMPLETE because code was written — require executable evidence (checkpoint, hash-bound predictions, scored evaluation).
- Never compare a cancelled run or paper-reported number to a locally executed result.
- Never report a metric without naming dataset, split, protocol version, seed count.
- Negative results (SAG failing, VLM losing to prior) are thesis contributions — report trung thực.
- Do NOT cite: 22.04% Senath (unverified); 0.2222 HGAT (synthetic dataset); 0.00% allergen (self-labeled, withdrawn).
