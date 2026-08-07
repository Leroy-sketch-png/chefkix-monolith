# IRON CHEF v3: LEAD BACKLOG

> **Track:** ML & Data Science (Multi-Signal Food Intelligence Engine)  
> **Mandate:** Own the knowledge graph, the benchmark pipeline, the chemical reasoning engine, and the allergen safety layer. You own the intelligence that makes ChefKix reason about food — and PROVE it beats the competition.

---

## Phased Execution (Concurrent With Member)

The Lead and Member backlogs are designed to run **in parallel**. Each phase has work for both roles with no blocking dependencies. Items marked with 🤝 produce outputs the Member consumes.

---

## PHASE 1: Data Acquisition & Graph Reconstruction (Weeks 1-2)

*While you download and build the graph, Member is building UI scaffolds and the graph visualization infrastructure.*

### EPIC 1: HGAT v1 ✅ DELIVERED (Foundation)

- [x] Parse 2.2M RecipeNLG → normalized vocabulary (chunked processing, OOM-safe)
- [x] Stack 3-vector node features: Epicure + USDA nutritional + FooDB compound (dim: 560)
- [x] Build PyTorch Geometric `HeteroData` graph (195K ingredient nodes, 7,247 substitution edges)
- [x] Train HGAT on Kaggle T4/P100 (30 epochs, test accuracy: 50.69%)
- [x] Build lean `substitution_subgraph.pt` (418 MB)
- [x] Wire HGAT into `chefkix-ai-service` (`ml_loader.py`, `ml.py`, `copilot.py`)
- [x] Promote `graph-rag/hgat-v1` to active in model registry (100% traffic)

**Evidence:** Kaggle v10 complete. Weights serving. **Superseded by Epic 2.**

### EPIC 2: Multi-Signal Knowledge Graph v2 🔴 NOT STARTED

- [ ] Download MISKG dataset (80,110 substitution pairs, 16,077 ingredients) from Kaggle
- [ ] Download FooDB bulk CSV (70K+ chemical compounds, 900+ foods)
- [ ] Download FlavorDB compound profiles (25K+ flavor molecules)
- [ ] Download Epicure-Core pretrained embeddings from HuggingFace (1,790 × 300-D)
- [ ] Download Open Food Facts allergen dataset (HuggingFace Parquet, 4M+ products)
- [ ] Build unified ingredient vocabulary: map RecipeNLG → MISKG → FooDB → Epicure canonical names
- [ ] Construct new `HeteroData` graph: MISKG substitution edges + FooDB compound edges + recipe co-occurrence edges
- [ ] Build triple-encoded node features: Epicure 300-D + USDA nutritional vector + FooDB compound fingerprint
- [ ] 🤝 Export `ingredient_compound_index.json` for Member's compound explanation UI (ingredient → top compounds with names + concentrations)
- [ ] 🤝 Export `allergen_index.json` for Member's allergen UI (ingredient → allergen flags)
- [ ] 🤝 Export `graph_sample.json` for Member's graph explorer development (sample 500 ingredients with edges for mock visualization)

---

## PHASE 2: Train, Benchmark & Chemical Engine (Weeks 2-4)

*While you train HGAT v2 and build the compound engine, Member is building the evaluation dashboard and wiring compound/allergen UI to your exported data files.*

### EPIC 3: HGAT v2 Training & Benchmarking 🔴 THESIS CENTERPIECE

- [ ] Split MISKG 80/20 train/test (stratified by ingredient frequency)
- [ ] Train HGAT v2 on Kaggle T4 with multi-signal node features and multi-relational edges
- [ ] Evaluate: **Hit@1, Hit@5, Hit@10, MRR, NDCG**
- [ ] Run baselines: PMI co-occurrence, Epicure cosine similarity, GISMo (published ~20.56% Hit@1), Gemini zero-shot, Mistral (published ~21.75% Hit@1)
- [ ] Architecture iteration if Hit@1 < GISMo: deeper attention, edge-type weighting, residual connections
- [ ] 🤝 Export benchmark results as `benchmark_results.json` for Member's evaluation dashboard
- [ ] Deploy `chefkix:hgat-v2` weights to `chefkix-ai-service` model registry
- [ ] Wire HGAT v2 into substitution endpoints (`ml.py`, `copilot.py`, `flywheel.py`)
- [ ] Run ablation: chemical-only features vs. nutritional-only vs. semantic-only vs. all combined
- [ ] 🤝 Export ablation results as `ablation_results.json` for Member's dashboard

### EPIC 4: Chemical Compound Reasoning Engine 🔴 THESIS CONTRIBUTION

- [ ] Build FooDB compound fingerprint index: ingredient → list of chemical compounds with concentrations
- [ ] Build FlavorDB flavor molecule index: ingredient → volatile compound profile
- [ ] Build shared-compound percentage calculator for substitution pairs
- [ ] Build compound-level explanation generator: returns "Butter → coconut oil: 73% shared volatiles (caprylic acid, lauric acid), similar melting point"
- [ ] Register `chefkix:compound-explainer-v1` in model registry
- [ ] 🤝 Wire compound explanations into substitution response API payloads (Member consumes this in UI)
- [ ] Build compound similarity as additional HGAT edge signal (feedback into Epic 3)

### EPIC 5: Allergen Safety Intelligence 🔴 THESIS CONTRIBUTION

- [ ] Build allergen constraint checker from Open Food Facts: ingredient → allergen flags (EU 14, FDA top 9)
- [ ] Build nutritional validation pipeline: substitution → nutritional delta check
- [ ] Build allergen-safe substitution filter: hard-constraint that NEVER suggests allergen-violating swaps
- [ ] Register `chefkix:allergen-guard-v1` in model registry
- [ ] 🤝 Wire allergen guard into substitution API flow as mandatory filter (Member consumes in UI)
- [ ] Build head-to-head allergen safety benchmark: IRON CHEF vs GPT-4o vs Gemini on known allergen test pairs
- [ ] 🤝 Export allergen benchmark results as `allergen_benchmark.json`

---

## PHASE 3: Multi-Modal & Behavioral Framework (Weeks 4-7)

*While you integrate detection and run behavioral simulation, Member is polishing demo surfaces and wiring the photo intelligence pipeline.*

### EPIC 6: Pretrained Detection Integration 🟡 PRODUCT GOAL

- [ ] Download pretrained YOLOv8-food model (Roboflow/Ultralytics)
- [ ] Wire ONNX inference endpoint into `chefkix-ai-service`
- [ ] 🤝 Publish detection endpoint for Member's camera pipeline
- [ ] Build CLIP + food projection cross-modal index on Recipes5k + Food-101
- [ ] 🤝 Publish cross-modal retrieval endpoint for Member's photo → recipe flow

### EPIC 7: Behavioral Learning Framework 🟡 TIER B

- [x] Telemetry & Signal Architecture (Kafka event, REST controller, FeedbackFlywheelWorker)
- [x] Exact Vocab Indexing (ingredient_vocab.json resolution)
- [x] Ablation Suite Simulator (3-config, +26% margin)
- [x] Telemetry Synthetic Simulator (verified e2e)
- [ ] Implement real edge weight injection into PyTorch Geometric `edge_attr` tensor (not just JSON delta)
- [ ] Design corpus-grounded simulation: use MISKG held-out pairs as acceptance proxy
- [ ] Measure MRR delta: static HGAT vs. feedback-updated HGAT
- [ ] 🤝 Export behavioral simulation results for Member's evaluation dashboard

### EPIC 8: Flavor Pairing Analysis 🟡 TIER B

- [x] Analysis engine (`flavor_pairing.py`)
- [x] Statistical PoC sweep (4 cuisine clusters, z = +15.28 SE Asian)
- [ ] Rerun with real FooDB compound fingerprints (not just Epicure proxy vectors)
- [ ] Compound-class breakdown: which classes drive pairing? Volatiles? Polyphenols? Terpenes?

---

## PHASE 4: Thesis & Defense (Weeks 7-12)

### EPIC 9: Thesis & Defense

- [x] Chapter & strategy structure: 12 chapters mapped in `academic_vision_and_strategy.md` (v3)
- [ ] Write benchmark results chapters with Hit@1/MRR tables
- [ ] Write compound reasoning chapter with FooDB explanation examples
- [ ] Write allergen safety chapter with head-to-head violation tables
- [ ] Write ablation chapter: which signal matters most?
- [ ] Build defense presentation with live demo
- [ ] Record backup demo video

---

## Priority Order

| Priority | Epic | Why | Phase |
|:---|:---|:---|:---|
| **P0** | Epic 2: Knowledge Graph v2 | Foundation for everything else | Phase 1 |
| **P0** | Epic 3: HGAT v2 Benchmarking | Thesis centerpiece — the numbers | Phase 2 |
| **P0** | Epic 4: Compound Engine | Differentiator — chemistry-grounded explanations | Phase 2 |
| **P1** | Epic 5: Allergen Safety | Demonstrable advantage over LLMs | Phase 2 |
| **P1** | Epic 7: Behavioral Framework | Close JSON→GNN gap, corpus simulation | Phase 3 |
| **P2** | Epic 6: Detection Integration | Pretrained YOLOv8, instant capability | Phase 3 |
| **P2** | Epic 8: Flavor Pairing | Complements compound reasoning | Phase 3 |
| **P0** | Epic 9: Thesis | Runs in parallel — write as evidence arrives | Phase 4 |
