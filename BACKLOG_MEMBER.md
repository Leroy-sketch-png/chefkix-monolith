# IRON CHEF v2: MEMBER BACKLOG

> **Track:** Systems Engineering, Computer Vision & Frontend Intelligence  
> **Mandate:** You build the instruments that capture cooking behavior and make the AI visible to users. Without your work, the knowledge graph is deaf and blind — it can't hear what users think of its suggestions, and users can't see why it makes them.

---

## The Mission (Read This First)

ChefKix has a knowledge graph of 195,000 ingredients and their substitution relationships, learned from 2.2 million recipes. When a user is missing an ingredient, the graph suggests a context-aware replacement (e.g., "Use cold coconut oil instead of butter for lamination — 0.83 confidence").

**The problem:** Every food AI system in existence stops here. They suggest a substitution and hope it works. Nobody measures whether the user actually tried it, whether the dish turned out well, or whether the suggestion was garbage.

**Our thesis contribution:** We close the loop. When a user accepts a substitution and cooks the recipe, we capture the outcome (did they finish? how did they rate it? did they share it?). That signal flows back into the graph, making it smarter. The graph learns from every kitchen in the network.

**Your role:** You build the feedback instrument (the UI that captures substitution choices and cooking outcomes), the graph explorer (the UI that makes the knowledge graph visible), and the evaluation dashboard (the metrics that prove the flywheel works). You also own the perception layer (ingredient detection via YOLO/RT-DETR) — the "eyes" of the system.

---

## EPIC 1: The Feedback Instrument ⬜ THESIS-CRITICAL

**Mission:** Build the UI and backend pipeline that captures *which* substitution a user accepted, tracks the cooking session that follows, and records the outcome. Without this, the graph is static and the thesis has no novel contribution.

### 1A: Substitution Acceptance UI
- [ ] During recipe cooking (in the CookingPlayer), when the user encounters a missing ingredient, display graph-suggested substitutions with confidence scores.
- [ ] Capture the user's choice: **accept** (which one?), **reject** (used something else — what?), or **skip** (cooked without substituting).
- [ ] Send the substitution decision event to the backend (`POST /api/v1/cooking-session/{id}/substitution-feedback`).

### 1B: Cooking Outcome Capture
- [ ] After a cooking session that involved a substitution, show a brief post-session modal: "How did the substitution work?" (thumbs up / thumbs down / neutral) + optional dish rating.
- [ ] This is NOT a new rating system — it piggybacks on the existing post-session XP flow. One extra card in the completion carousel.
- [ ] Send the outcome event to the backend.

### 1C: Backend Feedback Pipeline
- [ ] Create the `SubstitutionFeedbackEvent` in the monolith: records (sessionId, userId, originalIngredient, substituteIngredient, technique, cuisine, accepted, sessionCompleted, rating, shared).
- [ ] Publish to Kafka topic `substitution-feedback`.
- [ ] The Lead's graph update worker consumes this topic — you don't need to touch the ML side.

---

## EPIC 2: The Graph Explorer ⬜ DEMO-CRITICAL

**Mission:** Build a visualization UI where users (and thesis examiners during the demo) can explore ingredient relationships, see confidence scores, and see "Validated by X cooks" badges on substitution edges. This is the demo centerpiece.

- [ ] **Graph Visualization Page:** A new page (`/explore/graph` or similar) showing an interactive ingredient relationship map. Use a force-directed graph layout (D3.js or vis-network).
- [ ] **Substitution Detail Panel:** Tap an ingredient → see its top substitutions with confidence scores, technique context, and "Tried by X cooks, Y% success rate" badges.
- [ ] **Search Integration:** Type an ingredient name → highlight it in the graph → show its neighborhood.
- [ ] **Backend Endpoint:** `GET /api/v1/graph/ingredient/{name}/substitutions` → returns ranked substitutions with confidence, cook count, and success rate.

---

## EPIC 3: The Evaluation Dashboard ⬜ THESIS-CRITICAL

**Mission:** Build an internal dashboard that shows the flywheel metrics. This produces the evidence for the thesis evaluation chapter.

- [ ] **Substitution Acceptance Rate Over Time:** Line chart showing what % of suggested substitutions users accept, week over week.
- [ ] **Graph Edge Weight Drift:** Visualize how confidence scores on specific substitution edges change as feedback accumulates.
- [ ] **Emergent Edges:** Highlight substitution relationships that appeared in the feedback data but were NOT in the original 2.2M recipe corpus. These are discoveries from real kitchens.
- [ ] **A/B Comparison:** If we run static-graph vs. feedback-graph experiments, show the metric deltas.

---

## EPIC 4: Perception Layer — The Eyes ⬜ PRODUCT GOAL

**Mission:** Give ChefKix the ability to see ingredients through the camera. This feeds the product's investor narrative and supports future copilot features.

### 4A: Model Training & Benchmark
- [ ] Set up training pipeline for YOLOv8n, YOLOv11n, and RT-DETR-L on the Roboflow FOOD-INGREDIENTS dataset.
- [ ] Run comparative benchmark: mAP@50, mAP@50:95, inference latency, model size.
- [ ] Export the winning model to ONNX INT8. Target: <100ms inference on CPU.

### 4B: Service Integration
- [ ] Wire the ONNX inference endpoint into `chefkix-ai-service`. Return standardized bounding boxes matching the ingredient taxonomy.
- [ ] Frontend: In CookingPanel, capture camera frames and send to the detection endpoint. Display bounding boxes over the camera feed.

---

## EPIC 5: Voice-Vision Copilot Wiring ⬜ PRODUCT GOAL (IF TIME ALLOWS)

**Mission:** Connect the frontend voice and camera systems to the intelligence backend. This is the "JARVIS" experience — the investor wow-moment. Deliver if Epics 1-3 are on track.

- [ ] Upgrade `useVoiceMode.ts` to continuous wake-word listening ("Hey ChefKix").
- [ ] Build the orchestration route: voice command + camera frame → backend VLM → TTS response.
- [ ] Implement intervention UI alerts (warning toast when AI detects an issue).

---

## EPIC 6: Thesis Engineering Chapters

- [ ] **Chapter 5 (The Flywheel):** Document the feedback pipeline architecture, the substitution acceptance UI, and the data flow from cooking session → graph update.
- [ ] **Chapter 6 (Flywheel Evaluation):** Compile the evaluation dashboard metrics into thesis-ready tables and charts.
- [ ] **Chapter 8 (Perception):** Write the YOLO vs RT-DETR comparative analysis with metric tables and latency graphs.
- [ ] **Chapter 10 (System Architecture):** Document the full IRON CHEF stack, ONNX optimization, and deployment.

---

## Priority Guide

| Priority | Epics | Why |
|:---|:---|:---|
| 🔴 **Do First** | Epic 1 (Feedback Instrument), Epic 3 (Eval Dashboard) | Without these, the thesis has no novel contribution and no evidence. |
| 🟡 **Do Next** | Epic 2 (Graph Explorer), Epic 4 (Perception) | The demo needs the graph explorer. Perception is the product differentiator. |
| 🟢 **If Time** | Epic 5 (Voice Copilot) | Investor wow-moment. Not thesis-critical. |
| 📝 **Continuous** | Epic 6 (Thesis Chapters) | Write as you build. Don't leave it all for the end. |
