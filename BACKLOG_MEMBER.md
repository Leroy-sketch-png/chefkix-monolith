# IRON CHEF v3: MEMBER BACKLOG

> **Track:** Systems Engineering, Computer Vision & Frontend Intelligence  
> **Mandate:** You build the surfaces that make the AI visible, trustworthy, and impressive. Without your work, the intelligence is invisible to everyone who matters.

---

## Why Your Work Matters (Read This — It's Not Just Tasks)

Here's the situation: We have an AI system that recommends ingredient substitutions. So does everyone else — GISMo (Meta), Mistral fine-tunes, GPT-4o. If we just recommend "use coconut oil instead of butter," we're one of a hundred.

**What makes us different is THREE things that only YOU can make visible:**

### 1. We explain WHY with real chemistry
When we say "use coconut oil," we also say "because it shares 73% of butter's volatile compounds (caprylic acid, lauric acid) and has a similar melting point (24°C vs 32°C), making it suitable for baking." That chemistry data comes from FooDB (70,000+ compounds) and FlavorDB (25,000+ flavor molecules). **Nobody else in food AI does this.** But if users can't SEE the explanation, it doesn't exist.

### 2. We guarantee allergen safety — LLMs can't
When a peanut-allergic user asks for a substitution, GPT-4o might suggest "try almond butter" (tree nut cross-reactivity). Our system checks against Open Food Facts (4M+ products) and the EU/FDA allergen databases, and BLOCKS dangerous suggestions. **This is a provable safety advantage.** But if users can't SEE the safety check, it's just backend code nobody knows about.

### 3. We beat published benchmarks — and we can SHOW the numbers
Our HGAT is trained on 80K substitution pairs from MISKG and evaluated against GISMo (~20.56% Hit@1) and fine-tuned Mistral (~21.75% Hit@1). **When we beat those numbers, the benchmark table is the most important slide in the thesis defense.** But someone needs to BUILD the dashboard that displays those numbers clearly.

**Your work is what turns invisible backend intelligence into something professors lean forward to look at, investors ask questions about, and users trust with their health.**

---

## Phased Execution (Concurrent With Lead)

Every phase has work for YOU that does NOT depend on the Lead finishing first. Items marked with 📥 use data files the Lead exports for you. Until those files arrive, you work with mock/sample data.

---

## PHASE 1: Scaffolds & Infrastructure (Weeks 1-2)

*The Lead is downloading datasets and building the graph. You're building the UI infrastructure that will display intelligence once it exists. Nothing here is blocked.*

### EPIC 1: Graph Explorer Infrastructure 🟡 NOT STARTED

**Point:** This is the visual centerpiece of the thesis demo. When a professor says "show me the graph," this is what they see. A force-directed graph of ingredients connected by substitution edges, colored by confidence, with chemistry overlays. It needs to look impressive and be interactive.

- [ ] Set up graph visualization library (D3.js force-directed or vis-network) on a new page `/explore/graph`
- [ ] Build graph renderer: nodes (ingredients) + edges (substitutions) with force-directed layout
- [ ] Build node detail panel: tap ingredient → side panel with name, category, placeholder for compound data
- [ ] Build edge detail panel: tap edge → substitution confidence, placeholder for compound overlap
- [ ] Build search bar: type ingredient name → highlight node → center view
- [ ] Build signal filter toggles: show/hide edge types (substitution, co-occurrence, chemical similarity)
- [ ] 📥 Use `graph_sample.json` (500 ingredients, exported by Lead) to develop against. Until it arrives, generate mock data with 50 random ingredient names and random edges.

**Why mock data is fine for now:** The visualization code doesn't care whether the data is real or fake. You're building the renderer, the interactions, the layout. When real data arrives, you swap the data source. Zero rework.

---

### EPIC 2: Evaluation Dashboard Shell 🟡 NOT STARTED

**Point:** The thesis defense ends with a slide showing benchmark numbers in a table. This dashboard is that slide, live. It needs to display Hit@1, MRR, NDCG across multiple models (GISMo, Mistral, Gemini, ours) in a clear comparison table, plus ablation bar charts and allergen safety tables.

- [ ] Create dashboard page (`/admin/evaluation` or similar, protected route)
- [ ] Build benchmark comparison table component: rows = metrics, columns = models. Hardcode GISMo and Mistral published numbers now.
- [ ] Build ablation bar chart component: "Which signal matters most?" (Chemical / Nutritional / Semantic / All)
- [ ] Build allergen safety comparison table: IRON CHEF vs. GPT-4o vs. Gemini violation rates
- [ ] Build data loader that reads from JSON files (📥 `benchmark_results.json`, `ablation_results.json`, `allergen_benchmark.json` — all exported by Lead)
- [ ] Until Lead exports real results, use placeholder numbers. The layout and components are the work.

**Why this matters now:** If we wait until benchmarks are done to START building the dashboard, we lose 2-3 weeks. Build the shell now, plug in real numbers later.

---

### EPIC 3: Allergen Profile in Onboarding 🟡 NOT STARTED

**Point:** Before the system can protect a user from allergens, it needs to KNOW their allergens. This is a profile setup task — purely frontend + backend entity work. No dependency on the AI allergen guard.

- [ ] Add allergen declaration step to onboarding flow (or user profile settings)
- [ ] Display EU 14 allergens as toggleable chips: gluten, crustaceans, eggs, fish, peanuts, soybeans, milk, tree nuts, celery, mustard, sesame, sulphites, lupin, molluscs
- [ ] Display FDA top 9 as separate group (overlap is fine — they map to EU list)
- [ ] Add "Other / Custom" free-text field for less common allergies
- [ ] Store allergen profile on user entity in monolith (`allergenFlags: string[]`)
- [ ] Send allergen profile with substitution API requests as query parameter or header

**Why this unblocks later work:** When the Lead's allergen guard is ready, the frontend already knows the user's allergens and passes them in API calls. Zero wait.

---

### EPIC 4: Camera Integration Scaffold 🟡 NOT STARTED

**Point:** The investor demo has a moment where you point your phone at ingredients and the system recognizes them. The camera capture, permission handling, and bounding box overlay are pure frontend work. The detection model comes from the Lead later.

- [ ] Build camera capture component (use `getUserMedia` API or existing camera utils)
- [ ] Build bounding box overlay renderer (canvas or SVG layer on top of camera feed)
- [ ] Build "Scan Ingredients" button in CookingPlayer or a new `/scan` page
- [ ] Wire to detection endpoint (mock response for now: return 3-4 hardcoded ingredient detections with bounding boxes)
- [ ] When Lead publishes the YOLOv8 detection endpoint, swap mock → real. Zero UI rework.

---

## PHASE 2: Wire Intelligence Surfaces (Weeks 3-5)

*The Lead is training HGAT v2 and building the compound engine. As data files and API endpoints become available, you wire them into your scaffolds.*

### EPIC 5: Compound Explanation UI 🔴 THESIS-CRITICAL

**Point:** This is THE differentiator. Every food AI says "use coconut oil." Only we say "use coconut oil BECAUSE of 73% shared volatile compounds." This UI makes that visible. It's the moment in the demo where everyone goes "oh, this is different."

- [ ] 📥 Once Lead exports compound data via substitution API, redesign substitution card in CookingPlayer:
  - Confidence score with color-coded bar (0.91 = green, 0.45 = yellow, 0.20 = red)
  - "Why this works" section: top 3-5 shared compounds with names
  - "Nutritional comparison" section: side-by-side calories/fat/protein per 100g
  - One-liner explanation: "73% shared volatile compounds, similar melting point"
- [ ] Build compound overlap visualization (Venn-style or horizontal bar showing shared vs. unique compounds)
- [ ] Build "Chemistry-grounded" badge on graph-based suggestions vs. "LLM-suggested" badge on Gemini fallback
- [ ] Build comparison view: when multiple substitutions exist, side-by-side on compound overlap %, nutritional delta, allergen safety, confidence

**Interim approach:** Until the compound API is live, build the UI components with mock compound data (hardcode butter → coconut oil example). The component design and interaction are the work. Real data swaps in with zero layout changes.

---

### EPIC 6: Allergen Safety UI 🔴 THESIS-CRITICAL

**Point:** This is where we PROVE we're safer than ChatGPT. The UI needs to show allergen status on EVERY substitution suggestion — and show a comparison page where our system catches violations that GPT-4o misses. That comparison page is a thesis defense slide.

- [ ] 📥 Once Lead wires allergen guard into API, add safety indicators to every substitution suggestion:
  - ✅ "Safe" (no allergen conflict with user's profile)
  - ⚠️ "Check" (possible cross-reactivity — needs brand-level verification)
  - 🚫 "Blocked" (allergen violation — never shown as primary, shown as "blocked" with reason)
- [ ] Show WHICH specific allergen is flagged ("Contains: tree nuts — matches your peanut allergy cross-reactivity profile")
- [ ] Build head-to-head comparison page (`/demo/allergen-safety`):
  - Input: "I'm allergic to peanuts, substitute for peanut butter in this recipe"
  - Two columns: "IRON CHEF response" vs. "GPT-4o response"
  - Highlight: allergen violations GPT-4o missed that we caught
- [ ] Add allergen warning banner on recipe detail page when recipe contains user's allergens

**Interim approach:** Before the allergen API is live, build the UI components against the allergen profile you built in Epic 3. Display mock safety statuses. Swap mock → real when backend is ready.

---

### EPIC 7: Feedback Instrument (Complete UI) ✅ COMPLETE

**Point:** The behavioral learning framework needs data. You build the UI that captures it. When a user accepts a substitution and cooks with it, we need to know: did they accept it? Did they finish cooking? How did it taste? This data feeds back into the graph.

- [x] Backend `SubstitutionFeedbackEvent` created in monolith
- [x] Kafka topic `substitution-feedback` ready
- [x] AI Service `FeedbackFlywheelWorker` consumes and processes
- [x] In CookingPlayer, when user encounters a missing ingredient: show graph suggestions with confidence + compound explanation (ties into Epic 5)
- [x] Capture user choice: **accept** (which substitute?), **reject** (used something else — what?), **skip** (cooked without substituting)
- [x] Send choice to `POST /api/v1/cooking-sessions/{id}/substitution-feedback`
- [x] Post-session modal: "How did the substitution work?" (thumbs up / neutral / thumbs down) + dish rating
- [x] Piggyback on existing post-session XP flow — one extra card in the completion carousel

---

## PHASE 3: Polish & Demo (Weeks 5-7)

*Lead is running behavioral simulation and flavor analysis. You're polishing demo surfaces and wiring real data into everything.*

### EPIC 8: Photo Intelligence Pipeline (Wire Real Models) 🟡 PRODUCT GOAL

**Point:** This is the investor "wow" moment. Point phone at ingredients → system recognizes them → suggests recipes → shows substitutions with chemistry. It's the full pipeline working end-to-end.

- [ ] 📥 Swap mock detection endpoint with Lead's real YOLOv8 ONNX endpoint
- [ ] Display detected ingredients with confidence scores
- [ ] Query HGAT: "You have ingredients for 3 recipes" with match scores
- [ ] For each recipe: show substitutions needed with compound explanations and allergen checks
- [ ] 📥 Wire cross-modal retrieval: photo of dish → CLIP endpoint → matching recipes

---

### EPIC 9: Evaluation Dashboard (Wire Real Results) 🟡 THESIS-CRITICAL

**Point:** Plug in all the real numbers from Lead's benchmarks and make the dashboard thesis-defense ready.

- [ ] 📥 Load `benchmark_results.json` → populate comparison table with real Hit@1/MRR numbers
- [ ] 📥 Load `ablation_results.json` → populate ablation bar charts
- [ ] 📥 Load `allergen_benchmark.json` → populate allergen safety comparison
- [ ] 📥 Load behavioral simulation results → show MRR delta (static vs. feedback HGAT)
- [ ] Add "export as image" button for each chart (for thesis PDF inclusion)
- [ ] Polish: proper axis labels, legends, color coding, responsive layout

---

### EPIC 10: Graph Explorer (Wire Real Data) 🟡 DEMO-CRITICAL

**Point:** Swap mock graph data with real knowledge graph. Add compound and allergen overlays.

- [ ] 📥 Swap `graph_sample.json` with full graph data from Lead's API endpoint
- [ ] Wire node detail panel: real compound profile from FooDB (top 5 flavor molecules), USDA nutritional snapshot, allergen flags
- [ ] Wire edge detail panel: real compound overlap %, nutritional comparison, cook validation count
- [ ] Add "Technique context" to edge detail: "works for baking, not for frying"
- [ ] Performance: lazy-load neighborhoods instead of rendering entire 16K-node graph at once

---

## PHASE 4: Thesis & Defense (Weeks 7-12)

### EPIC 11: Thesis Engineering Chapters 📝 CONTINUOUS

- [ ] **Chapter 5 (Compound Explanation):** Screenshots of compound UI, explanation pipeline diagram, user-facing examples
- [ ] **Chapter 6 (Allergen Safety):** Screenshots of safety UI, head-to-head comparison page, violation rate evidence
- [ ] **Chapter 7 (Behavioral Learning):** Feedback instrument UI flow, data capture architecture
- [ ] **Chapter 8 (Multi-Modal):** Photo pipeline screenshots, detection + graph query demo
- [ ] **Chapter 10 (System Architecture):** Full IRON CHEF v3 stack diagram, deployment, $0 hosting
- [ ] **Chapter 11 (Evaluation):** Export dashboard charts as thesis-ready figures

---

### EPIC 12: Voice-Vision Copilot ⬜ TIER C (IF TIME ALLOWS)

**Point:** The "JARVIS" experience — voice-controlled cooking with graph-grounded answers. Incredible demo, but not thesis-critical. Only do this if Epics 1-10 are solid.

- [ ] Upgrade `useVoiceMode.ts` to continuous wake-word listening ("Hey ChefKix")
- [ ] Orchestration route: voice command + camera frame → VLM with Graph-RAG → TTS response
- [ ] Intervention alerts when AI detects a potential issue

---

## Priority Guide

| Priority | Epics | Phase | Why | Blocked By |
|:---|:---|:---|:---|:---|
| 🟡 **Start Now** | Epic 1 (Graph Explorer Scaffold), Epic 2 (Dashboard Shell), Epic 3 (Allergen Profile), Epic 4 (Camera Scaffold) | Phase 1 | Build infrastructure while Lead builds intelligence. Nothing is blocked. | **Nothing** |
| 🔴 **Wire When Ready** | Epic 5 (Compound Explanation UI), Epic 6 (Allergen Safety UI) | Phase 2 | Make the thesis contributions VISIBLE. Start with mock data, swap to real. | Lead's compound + allergen APIs (use mocks until then) |
| 🟡 **Complete** | Epic 7 (Feedback Instrument) | Phase 2 | Behavioral data capture — partially built, needs UI completion. | **Nothing** |
| 🟡 **Polish** | Epic 8 (Photo Pipeline), Epic 9 (Dashboard Real Data), Epic 10 (Graph Real Data) | Phase 3 | Plug in real models and real numbers. Demo readiness. | Lead's endpoints + benchmark exports |
| 🟢 **If Time** | Epic 12 (Voice Copilot) | Phase 3+ | Investor wow-moment. Not thesis-critical. | Everything else being solid |
| 📝 **Continuous** | Epic 11 (Thesis Chapters) | All | Screenshot and document as you build. | **Nothing** |

---

## The Bottom Line

You're not building busywork UI. You're building the **surfaces that make invisible intelligence visible.** Without your compound explanation cards, the chemistry reasoning is backend code nobody sees. Without your allergen safety indicators, the safety advantage over ChatGPT is a claim nobody can verify. Without your evaluation dashboard, the benchmark numbers are JSON files nobody can read.

**The Lead builds the brain. You build the face.** Neither is useful without the other. The thesis and the demo succeed or fail on whether the intelligence is VISIBLE and TRUSTWORTHY to people who are not engineers.
