# IRON CHEF v3: MEMBER BACKLOG
> **Track:** Systems Engineering, Computer Vision & Frontend Intelligence
> **CURRENT-STATE OVERRIDE — 2026-08-24:** Fabricated metric claims purged. Do NOT display Hit@1 0.2222, allergen 0.00% vs 10.00%, or behavioral +1.01% as current results — all are withdrawn. The evaluation dashboard now shows REAL evidence: what is verified, what is partial, what is open.

---

## What You Are Building (And Why It Matters)

We have four trained models under construction. Without your work, they are invisible:

| Model | What it does | What you make visible |
|:---|:---|:---|
| **M1 HGAT + SAG** | Substitution reasoning; diagnoses why naive fusion fails | Graph explorer, ablation charts, SAG gate visualization |
| **M2 ChefKix-Mistral-7B** | LLM fine-tuned on substitution preferences | Benchmark comparison table vs. GISMo baseline |
| **M3 ChefKix-VLM** | Food visual understanding; points phone at fridge | Camera integration, ingredient detection overlay |
| **M4 ChefKix-CLIP** | Photo → recipe cross-modal retrieval | Cross-modal search UI, dish recognition |

**The allergen killshot:** Nobody has run a head-to-head LLM allergen safety benchmark. When Lead runs it, you build the table that shows GPT-4o recommending dangerous substitutions and IRON CHEF catching them. That table is a thesis defense slide.

---

## Phased Execution (Concurrent With Lead)

Items marked with 📥 use Lead-exported files. Until those arrive, build with mock/sample data — real data swaps in with zero UI changes.

---

## PHASE 1: Scaffolds & Infrastructure

### EPIC 1: Graph Explorer Infrastructure 🟡 UNBLOCKED

The visual centerpiece of the thesis demo. When a professor says "show me the graph," this is what they see.

- [ ] Set up graph visualization library (D3.js force-directed or vis-network) at `/explore/graph`
- [ ] Node renderer: ingredients, colored by signal coverage (semantic/nutritional/chemical)
- [ ] Edge renderer: substitution edges, width proportional to MISKG frequency
- [ ] Node detail panel: tap ingredient → name, category, compound data, USDA nutrients
- [ ] Edge detail panel: tap edge → substitution context, compound overlap %, signal gate weights (once SAG runs)
- [ ] Search bar: type ingredient → highlight node → center view
- [ ] Signal filter toggles: show/hide semantic / nutritional / chemical signals
- [x] 📥 `graph_sample.json` (1,840 ingredients, multi-relational) available at `chefkix-fe/public/data/`

**New: SAG Gate Visualization (once EPIC 3b results arrive)**
- [ ] For each substitution edge: show 3-bar mini chart of [semantic gate / nutritional gate / chemical gate] weights
- [ ] Color code: gate > 0.5 = dominant signal; < 0.15 = suppressed
- [ ] This is what makes SAG visible to professors — the model "decided" which signal to trust

---

### EPIC 2: Evaluation Dashboard — Real Evidence Only 🟡 UNBLOCKED

> ⚠️ **Do NOT populate with old fabricated numbers.** The dashboard must show real current evidence. When numbers are pending, show "PENDING — training in progress" or "OPEN — protocol not yet executed." Honesty is a feature.

**Benchmark table component:**
- [ ] Create dashboard page (`/demo/evaluation`, auth-protected)
- [ ] Model comparison table — columns: Hit@1 / Hit@5 / Hit@10 / MRR / NDCG@10 — rows:
  - GISMo baseline (overall): 20.694% / — / 54.164% / 0.31636 / — ✅ VERIFIED
  - GISMo baseline (unseen pairs): 1.206% / — / — / 0.04308 / — ✅ VERIFIED
  - Lookup-frequency (overall): 17.996% / — / — / 0.27625 / — ✅ VERIFIED
  - Lookup-frequency (unseen): 0.0% / — / — / 0.00015 / — ✅ VERIFIED
  - IRON CHEF HGAT v11 semantic-only: 0.495% Hit@1 ✅ VERIFIED (full-catalog evaluation)
  - IRON CHEF HGAT v11 all-signals: 0.252% Hit@1 ✅ VERIFIED NEGATIVE
  - IRON CHEF SAG: PENDING (running on Kaggle)
  - ChefKix-Mistral-7B: PENDING (training)
- [ ] Add protocol note under each row: dataset, split, seed count, evaluation date

**Ablation chart component:**
- [ ] 📥 Load `ablation_results.json` → ablation bar chart: semantic 0.495% vs. USDA 0.171% vs. FooDB 0.114% vs. all-signals 0.252%
- [ ] Title: "Signal Ablation — Why Naive Fusion Fails" (this is the SAG motivation)
- [ ] Color: semantic = blue, nutritional = green, chemical = orange, all-signals = red (worse)
- [ ] Note: "SAG attempts to fix this — results pending"

**Cross-modal baseline component:**
- [ ] Recipes5k zero-shot CLIP: R@1 34.10% / R@5 77.01% / R@10 91.19% ✅ VERIFIED
- [ ] Ingredient identification: base CLIP 8.02% mAP / category prior 64.95% ✅ VERIFIED
- [ ] ChefKix-CLIP trained: PENDING
- [ ] ChefKix-VLM ingredient mAP: PENDING

---

### EPIC 3: Allergen Profile in Onboarding 🟡 NOT STARTED

- [ ] Add allergen declaration step to onboarding or profile settings
- [ ] EU 14 allergens as toggleable chips: gluten, crustaceans, eggs, fish, peanuts, soybeans, milk, tree nuts, celery, mustard, sesame, sulphites, lupin, molluscs
- [ ] FDA Big 9 as separate group (overlap is expected)
- [ ] "Other / Custom" free-text field
- [ ] Store `allergenFlags: string[]` on user entity in monolith
- [ ] Send allergen profile with substitution API requests

---

### EPIC 4: Camera Integration Scaffold 🟡 NOT STARTED

- [ ] Camera capture component (`getUserMedia` API) at `/scan`
- [ ] Bounding box overlay renderer (canvas/SVG layer on camera feed)
- [ ] "Scan Ingredients" button in CookingPlayer or scan page
- [ ] Wire to detection endpoint (mock: 3-4 hardcoded ingredient detections with bounding boxes)
- [ ] When Lead publishes YOLOv8 detection endpoint, swap mock → real

---

## PHASE 2: Wire Intelligence Surfaces

### EPIC 5: Compound Explanation UI 🔴 THESIS-CRITICAL

The differentiator. Every food AI says "use coconut oil." Only we say "use coconut oil BECAUSE of shared bounded compounds."

> ⚠️ Do NOT claim "73% shared volatile compounds" or chemistry compatibility. Only display: shared compound list (binary presence), Jaccard overlap %, missing coverage flags. Usefulness user study is open — we cannot yet claim this helps.

- [ ] Redesign substitution card in CookingPlayer:
  - Confidence: show source (graph-based vs. LLM-fallback) with badge — do NOT show fabricated confidence scores
  - "Shared compounds" section: list shared compound names (labeled as presence records, not flavor evidence)
  - "Nutritional comparison" section: real USDA side-by-side (only if USDA coverage exists)
  - "Missing coverage" notice: if ingredient not in FooDB, show gap honestly
- [ ] Compound overlap bar: shared compound count / total candidate compounds
- [ ] "Graph-based" vs. "LLM-suggested" badge — be honest about source
- [ ] Comparison view: multiple substitutions side-by-side with compound overlap and allergen status

---

### EPIC 6: Allergen Safety UI & Killshot Display 🔴 THESIS-CRITICAL

- [ ] Allergen indicator on every substitution suggestion:
  - ✅ SAFE (no known conflict with user profile for declared allergens)
  - ⚠️ UNKNOWN (ingredient not in vocabulary — cannot verify)
  - 🚫 BLOCKED (allergen conflict detected — show which allergen)
- [ ] Show WHICH allergen triggered the block ("Contains: tree nuts")
- [ ] Show UNKNOWN prominently — do not silently pass unrecognized ingredients

**Allergen Killshot Dashboard (`/demo/allergen-safety`):**
- [ ] Build head-to-head comparison page (populate after Lead runs EPIC 5b real API calls)
- [ ] Layout: query column | IRON CHEF response | GPT-4o response | Gemini response
- [ ] Highlight violations: red background on LLM columns where allergen was missed
- [ ] Table: violation rate per system, per allergen group, with Wilson CIs
- [ ] Note: "IRON CHEF catches all declared-allergen violations by construction; cross-contact and unknown gaps shown explicitly"
- [ ] This is empty/PENDING until Lead completes EPIC 5b real API calls

---

### EPIC 7: Feedback Instrument 🟡 PARTIALLY BUILT

- [x] Backend `SubstitutionFeedbackEvent` + Kafka topic + `FeedbackFlywheelWorker`
- [ ] In CookingPlayer, when ingredient is missing: show graph suggestions with source (HGAT/LLM) and compound records
- [ ] Capture: accept (which substitute?), reject (used what?), skip
- [ ] Send to `POST /api/v1/cooking-session/{id}/substitution-feedback`
- [ ] Post-session: "How did the substitution work?" (thumbs up / neutral / down) + dish rating

> Note: the behavioral learning framework is not yet accepted as a thesis contribution. Raw feedback collection is valid; avoid claiming it improves the model until a preregistered evaluation accepts it.

---

## PHASE 3: Polish & Demo

### EPIC 8: Photo Intelligence Pipeline 🟡 PRODUCT DEMO GOAL

- [ ] 📥 Swap mock detection endpoint with Lead's YOLOv8 ONNX endpoint
- [ ] Display detected ingredients with confidence scores and bounding boxes
- [ ] Query HGAT: "You have these ingredients — here's what you can cook" with substitution suggestions
- [ ] 📥 Wire ChefKix-CLIP endpoint: photo of dish → cross-modal retrieval → matching recipes
- [ ] End-to-end pipeline: camera → detect → identify → substitute → allergen check → compound record

---

### EPIC 9: Evaluation Dashboard (Wire Real Results) 🟡 THESIS-CRITICAL

- [ ] 📥 Load `benchmark_results.json` → populate comparison table with REAL numbers as they arrive
- [ ] 📥 Load `ablation_results.json` → ablation charts (v11 results already available)
- [ ] Allergen benchmark table: populate after Lead runs real API calls (EPIC 5b)
- [ ] Cross-modal table: populate after ChefKix-CLIP training (EPIC 12)
- [ ] VLM ingredient mAP: populate after ChefKix-VLM training (EPIC 11)
- [ ] "Export as image" button for each chart (for thesis PDF)
- [ ] All PENDING items show honest placeholder: "Training in progress" / "Protocol not yet executed"

---

### EPIC 10: Graph Explorer (Wire Real Data) 🟡 DEMO-CRITICAL

- [ ] 📥 Swap `graph_sample.json` with real graph API or updated export
- [ ] Wire node detail: compound profile from FooDB (presence records, not flavor claims), USDA nutrients, allergen flags
- [ ] Wire edge detail: MISKG frequency, compound overlap %, SAG gate weights (after EPIC 3b)
- [ ] Performance: lazy-load neighborhoods; do not render full 8,552-node graph at once

---

## PHASE 4: Thesis Support (14 Chapters)

### EPIC 11: Thesis Engineering Screenshots (14 chapters, updated)

| Chapter | Your contribution |
|:---|:---|
| 3 (Knowledge Graph) | Graph explorer screenshots, signal coverage map |
| 4 (M1 GNN + SAG) | Ablation bar chart, SAG gate visualization, graph explorer |
| 5 (M2 Mistral) | Benchmark table showing Mistral vs. GISMo (after training) |
| 6 (M3 VLM) | Ingredient identification UI, camera overlay |
| 7 (M4 CLIP) | Cross-modal search results, dish recognition screenshots |
| 8 (Compound Explanation) | Substitution card with compound records |
| 9 (Allergen Benchmark) | Killshot comparison table |
| 10 (Selective Abstention) | Abstention UI showing UNKNOWN status |
| 11 (Photo Pipeline) | End-to-end pipeline demo screenshots/video |
| 12 (System Architecture) | Full IRON CHEF v3 stack diagram |
| 13 (Evaluation) | Dashboard export as thesis figures |

---

### EPIC 12: Voice-Vision Copilot ⬜ TIER C (IF TIME ALLOWS)

- [ ] Continuous wake-word listening ("Hey ChefKix")
- [ ] Orchestration: voice + camera frame → VLM + Graph-RAG → TTS response
- [ ] Only if Epics 1-11 are solid

---

## Priority Guide

| Priority | Epic | Status | Why | Blocker |
|:---|:---|:---|:---|:---|
| 🔴 P0 BUILD NOW | EPIC 1 (Graph Explorer) | 🟡 Ready for `/explore/graph` | Visual centerpiece; `graph_sample.json` mounted | Nothing |
| 🔴 P0 BUILD NOW | EPIC 2 (Eval Dashboard) | 🟡 Ready with REAL verified numbers | Defense slide live; v11 ablation data available | Nothing |
| 🔴 P0 BUILD NOW | EPIC 5 (Compound UI) | 🟡 Ready with honesty constraints | Differentiator; compound API live | Nothing |
| 🔴 P0 BUILD NOW | EPIC 6 (Allergen Safety UI) | 🟡 Shell + PENDING state | Killshot display shell; real data after EPIC 5b | EPIC 5b (Lead) |
| 🟡 P1 WIRE | EPIC 3 (Allergen Profile Onboarding) | 🟡 Not started | Unblocks allergen guard | Nothing |
| 🟡 P1 WIRE | EPIC 7 (Feedback Instrument) | 🟡 Partially built | Raw feedback collection | Nothing |
| 🟢 IF TIME | EPIC 12 (Voice Copilot) | ⬜ Tier C | After everything else solid | M1-M4 complete |

---

## The Bottom Line

You build the surfaces that make invisible intelligence visible. Without your work:
- SAG gate weights are a JSON file nobody reads
- The allergen killshot is a terminal command nobody sees
- The benchmark comparison is numbers nobody can compare
- Four trained models exist in `.pt` files that impress nobody

**Lead builds four trained models. You make four trained models impressive.**
