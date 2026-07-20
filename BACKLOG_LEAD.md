# IRON CHEF v2: LEAD BACKLOG (THE CUNNING EXPLOITS)

> **Track:** ML & Data Science (Reasoning Engine)
> **Mandate:** Focus on Graph Neural Networks, Large Language Model orchestration, and Computational Food Science. You own the deep intelligence layer.

## EPIC 1: Layer 2 — The Heterogeneous Knowledge Graph (The Brain)
*Building the foundation of context-dependent substitution and grounded reasoning.*

- [ ] **Data Exploit Integration:** Write chunked-processing scripts to parse the 2.2M RecipeNLG dataset into a normalized vocabulary (avoids Out-Of-Memory crashes when building the PyG graph).  
- [ ] **Feature Stacking (The 3-Vector Node):** Download Epicure embeddings (`epicure-core`), FooDB compound fingerprints, and USDA FDC nutritional data. Stack these into a single 3-part feature vector for each Ingredient node.
- [ ] **PyTorch Geometric Construction:** Build the `HeteroData` graph. 4 node types (Ingredient, Recipe, Technique, Cuisine). 4 edge types.
- [ ] **Substitution Ground-Truth Extraction:** Write the regex pipeline to extract "or use X" / "substitute with X" from recipe texts. These are the positive labels for the GNN.
- [ ] **Train the HGAT (Context-Dependent Substitution):** Train the Heterogeneous Graph Attention Network on Kaggle T4 to predict substitution edges *conditioned* on Technique and Cuisine context.
- [ ] **Ablation Studies:** Run comparisons using `epicure-cooc` vs `epicure-chem` vs `epicure-core`.

## EPIC 2: The Science Contribution (Thesis Chapter 7)
*Validating the Ahn et al. Flavor Pairing Hypothesis at 39x Scale.*

- [ ] **Network Analysis:** For each cuisine cluster in the graph, compute average compound overlap between paired ingredients.
- [ ] **Statistical Validation:** Compare against a randomized null model. Do cuisines cluster by compound-sharing (Western) vs compound-avoiding (Eastern)?
- [ ] **Documentation:** Write up the findings. This is the core "computational food science" contribution that proves this isn't just an engineering project.

## EPIC 3: Layer 3 — Cross-Modal Memory
*Food retrieval without relying on the unavailable Recipe1M+ images.*

- [ ] **CLIP Adaptation:** Load frozen CLIP ViT-B/32. Build and attach custom food-specific projection layers (512 -> 256 -> 512).
- [ ] **Contrastive Training:** Use Recipes5k (image <-> ingredient list) and Food-101 (image <-> dish name) to fine-tune the projections on T4.
- [ ] **FAISS Indexing:** Embed all 2.2M recipes and construct the FAISS vector index.

## EPIC 4: Layer 4 — The VLM Orchestrator (JARVIS)
*The brain that talks to the user and queries the graph.*

- [ ] **Synthetic Data Generation (The Exploit):** Use ChefKix's existing 7-provider API rotator to generate 10k-20k food Q&A pairs. *CRITICAL: Send ONLY the textual metadata (ingredient lists, step descriptions) to the free text-only APIs to generate Q&A, then bind those Q&As to the corresponding images from Recipes5k/RecipeGen. This bypasses the need for expensive Vision APIs.*
- [ ] **SmolVLM Fine-Tuning:** Run QLoRA fine-tuning on `SmolVLM-256M` (for on-device speed) and `SmolVLM-2B` (for server-side reasoning). 
- [ ] **GGUF Export:** Export both models to GGUF Q4 for llama.cpp execution.

## EPIC 5: Orchestration & Personalization
*Wiring the intelligence into the product loop.*

- [ ] **Food Graph-RAG Pipeline:** Write the orchestration endpoint. VLM receives user query + image -> triggers KG retrieval -> VLM synthesizes graph evidence into a grounded response.
- [ ] **Taste DNA Implementation:** Write the logic to compute a user's Taste DNA vector (averaging embeddings of their cooked/liked recipes in the graph). Implement cosine similarity recipe ranking.
- [ ] **ML Registry Takeover:** Flip the `bridge: True` flags in `src/ml_registry.py` (in `chefkix-ai-service`). Register the actual ONNX, FAISS, and GGUF models.

## EPIC 6: Thesis & Defense
*Selling the Tony Stark vision.*

- [ ] **Thesis Writing:** Draft Chapters 1-3 (Problem Formalization), 5-7 (KG, Substitution, Flavor Pairing), and 9 (Graph-RAG).
- [ ] **Demo Scripting:** Prepare the exact narrative flow. (Act 1: The fake AI. Act 2: The Graph. Act 3: Voice-Vision Copilot).
