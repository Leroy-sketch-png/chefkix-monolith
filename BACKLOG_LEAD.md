# IRON CHEF v2: LEAD BACKLOG

> **Track:** ML & Data Science (Reasoning Engine + Flywheel)  
> **Mandate:** Own the knowledge graph, the feedback loop, and the evaluation pipeline. You own the intelligence that makes ChefKix learn from every kitchen.

---

## EPIC 1: The Heterogeneous Knowledge Graph + HGAT ✅ COMPLETE

*Built the foundation of context-dependent substitution.*

- [x] Parse 2.2M RecipeNLG → normalized vocabulary (chunked processing, OOM-safe)
- [x] Stack 3-vector node features: Epicure + USDA nutritional + FooDB compound (dim: 560)
- [x] Build PyTorch Geometric `HeteroData` graph (195K ingredient nodes, 7,247 substitution edges)
- [x] Extract substitution ground-truth via regex pipeline ("or use X", "substitute with X")
- [x] Train HGAT on Kaggle T4/P100 (30 epochs, test accuracy: 50.69%)
- [x] Build lean `substitution_subgraph.pt` (418 MB) — eliminated 7.87 GB OOM permanently
- [x] Dynamic `forward()` projection for present node types
- [x] Selective Kaggle weight installer (`install_kaggle_weights.py`)
- [x] Wire HGAT into `chefkix-ai-service` (`ml_loader.py`, `ml.py`, `copilot.py`)
- [x] Promote `graph-rag/hgat-v1` to active in model registry (100% traffic)

**Evidence:** Kaggle v10 complete. Weights installed. AI service serving real embeddings.

---

## EPIC 2: The Co-Evolving Flywheel ⬜ THESIS CENTERPIECE

**Mission:** Build the bridge between the knowledge graph and ChefKix's behavioral data, so the graph learns from real cooking outcomes. This is the novel contribution that nobody in food AI has published.

**Why this matters:** Every food KG in existence (FlavorGraph, GISMo, RecipeRAG, MISKG) is static — trained once on a corpus, evaluated on expert benchmarks, published. None of them learn from real user behavior. We close that loop.

- [ ] **Feedback Signal Design:** Define how a cooking session's behavioral signals (completion × rating × substitution-used × share-flag) translate into a graph edge weight adjustment rule.
- [ ] **Edge Weight Update Worker:** Implement an async worker (Kafka `substitution-feedback` topic → graph updater) that processes cooking session outcomes and adjusts HGAT edge weights.
- [ ] **Online Evaluation Pipeline:** Build MRR, Hit@K evaluation on the evolving graph. Track substitution acceptance rate, completion delta, and rating delta over time.
- [ ] **Ablation Framework:** Static graph vs. feedback-updated graph. Epicure-only vs. triple-encoded features. Time-windowed vs. cumulative updates.
- [ ] **Emergent Edge Detection:** Identify substitution edges that appear in the feedback-updated graph but were NOT in the original RecipeNLG-extracted ground truth. These are discoveries from real kitchens.

---

## EPIC 3: Cross-Modal Memory ⬜ PRODUCT GOAL (Tier C)

*Food retrieval without Recipe1M+. Deliver if Tier A is on track.*

- [ ] CLIP ViT-B/32 frozen + custom food projection layers (512→256→512)
- [ ] Contrastive training on Recipes5k + Food-101
- [ ] FAISS index over 2.2M recipe embeddings
- [ ] Evaluate: Recall@K, MedR

---

## EPIC 4: VLM Orchestrator ⬜ PRODUCT GOAL (Tier C)

*The brain that speaks to the user. Deliver if time allows.*

- [ ] Synthetic data generation: 10-20K food Q&A pairs via API rotator
- [ ] SmolVLM-256M QLoRA fine-tune + SmolVLM-2B QLoRA fine-tune
- [ ] Benchmark vs GPT-4o zero-shot on food tasks
- [ ] GGUF Q4 export for llama.cpp

---

## EPIC 5: Flavor Pairing Analysis ⬜ BONUS (Tier B)

*Test Ahn et al. (2011) at 39× scale. Comes free from the graph already built.*

- [ ] For each cuisine cluster, compute average compound overlap between paired ingredients
- [ ] Compare against randomized null model
- [ ] Quantify HOW the hypothesis is incomplete (which cuisines, which compound classes)
- [ ] Write up as bonus thesis chapter

---

## EPIC 6: Thesis & Defense

- [ ] Draft thesis chapters 1-6 (see vision doc for chapter map)
- [ ] Demo scripting: the flywheel narrative (Acts 1-6)
- [ ] Defense presentation
- [ ] Demo backup video
