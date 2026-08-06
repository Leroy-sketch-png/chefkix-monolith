# IRON CHEF v2: LEAD BACKLOG

> **Track:** ML & Data Science (Reasoning Engine + Flywheel)  
> **Mandate:** Own the knowledge graph, the feedback loop, and the evaluation pipeline. You own the intelligence that makes ChefKix learn from every kitchen.

---

## EPIC 1: The Heterogeneous Knowledge Graph + HGAT ✅ REALITY CHECK: VERIFIED COMPLETE

*Built and trained on Kaggle GPUs — real weights deployed in production.*

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

**Evidence:** Kaggle v10 complete. Weights `hgat_weights.pt` and `substitution_subgraph.pt` installed and serving active inference.

---

## EPIC 2: The Co-Evolving Flywheel 🔄 IN PROGRESS (THESIS CENTERPIECE)

*Bridge between Knowledge Graph and behavioral cooking telemetry.*

- [x] **Telemetry & Signal Architecture:** Monolith Kafka event `SubstitutionFeedbackEvent` + REST controller + AI Service `FeedbackFlywheelWorker` score calculator. (**Code Scaffold Deployed**)
- [x] **Exact Vocab Indexing:** Real `ingredient_vocab.json` string-to-index resolution built into `ml_loader.py` and `flywheel.py`. (**Code Scaffold Deployed**)
- [x] **Ablation Suite Simulator:** `ablation_flywheel.py` initial signal separation model. (**Prototype Ready**)
- [ ] **Real Telemetry Ingestion (3-Month Window):** Accumulate $N > 500$ real cooking session feedback events from live users / staging sessions.
- [ ] **Graph Retraining (Kaggle T4):** Re-train HGAT edge attributes using accumulated real-world feedback vectors.
- [ ] **Emergent Edge Mining:** Extract novel substitution edges from trained graph deltas that were absent in initial RecipeNLG extraction.

---

## EPIC 3: Cross-Modal Memory 🔄 IN PROGRESS (Tier C)

*Visual & semantic food memory.*

- [x] **Model Architecture & FAISS Service:** `FoodProjectionHead` ($512 \rightarrow 256 \rightarrow 512$) in `src/models/cross_modal.py` + `CrossModalService` L2 FAISS indexer. (**Code Scaffold Deployed**)
- [ ] **Dataset Preparation:** Unpack Recipes5k (4.8k image-recipe pairs) & Food-101 on Kaggle workspace.
- [ ] **Contrastive Fine-Tuning (Kaggle T4):** Train CLIP ViT-B/32 projection heads using InfoNCE loss (50 epochs on Kaggle).
- [ ] **Full Index Generation:** Embed 2.2M RecipeNLG text vectors + Recipes5k image vectors into production `recipe_faiss.index`.

---

## EPIC 4: VLM Orchestrator 🔄 IN PROGRESS (Tier C)

*JARVIS cooking copilot engine.*

- [x] **Prompt Engine & Evaluator:** `VLMOrchestrator` Graph-RAG prompt formatting + synthetic Q&A template generator in `vlm_orchestrator.py`. (**Code Scaffold Deployed**)
- [ ] **Instruction Dataset Construction:** Generate 10k-20k food Q&A pairs via 7-provider API rotator script.
- [ ] **SmolVLM QLoRA Fine-Tuning (Kaggle T4 / A100):** Run QLoRA fine-tuning on `SmolVLM-256M` and `SmolVLM-2B` on Kaggle T4 (estimated 4-6 GPU hours).
- [ ] **GGUF Q4 Quantization:** Convert fine-tuned PyTorch checkpoints to GGUF format for on-device llama.cpp execution.

---

## EPIC 5: Flavor Pairing Analysis 🔄 IN PROGRESS (Tier B BONUS)

*Testing Ahn et al. (2011) at 39× Scale.*

- [x] **Analysis Engine:** `flavor_pairing.py` vector similarity & null-model z-score calculator. (**Code Scaffold Deployed**)
- [ ] **Full-Scale Dataset Sweep:** Execute vector similarity sweep across all 195K ingredients and 2.2M recipes using full FooDB chemical compound database on Kaggle.

---

## EPIC 6: Thesis & Defense 🔄 IN PROGRESS

- [x] **Chapter & Strategy Structure:** 11 thesis chapters mapped in `academic_vision_and_strategy.md`.
- [ ] **Data Gathering:** Run multi-week benchmarks during training phases.
- [ ] **Drafting & Defense Prep:** Write formal thesis chapters based on empirical GPU training outputs.
