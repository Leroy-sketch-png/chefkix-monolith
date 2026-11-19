# L39 / C3 Typed Source-Preserving Allergen Observation Graph Model and Admission Validator

This package implements and verifies the typed allergen observation graph schema and admission validator governed by:
- **Decision:** "L39/C3 allergen evidence construction and source-admission decision (2026-11-08)" in `BACKLOG_LEAD.md`
- **Protocol:** Campaign Protocol v1 (SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`)
- **Strategy:** Section 12 (Graph Serving & Edge Provenance Contracts)

## Core Invariants

1. **Strict Destination Boundary:**
   - Admitted source-backed allergen observations belong strictly in `graph_product_v4`.
   - Allergen observations are **forbidden** from `graph_eval_v4` (which remains train-derived substitution evidence only).
   - Neither graph becomes a scoring input to the frozen 208-case benchmark.
2. **FATO Predicate Isolation:**
   - Separates distinct observation predicates: `ingredient_declaration`, `derivative`, `additive`, `imported_ingredient_declaration`, `cross_contact`, `precautionary_label`, and `protein_source`.
   - Never conflates advisory / precautionary labels with direct ingredient declarations.
3. **Source Admission Limits:**
   - **FoodOn:** Vocabulary and relation semantics only (`has ingredient`, `has defining ingredient`). Links require version-pinned crosswalks, not name similarity.
   - **WHO/IUIS:** Positive protein/source-species evidence only (`PROTEIN_SOURCE`, `scope="species_protein"`). Never infer source-species $\to$ ingredient $\to$ clinical safety.
   - **FooDB:** Compound/nutrient co-presence is **strictly rejected** as allergen evidence.
   - **Open Food Facts (OFF):** Deferred from ingredient-level admission. Product-level tags cannot instantiate ingredient-level edges.
4. **Tri-State / Fail-Closed Logic:**
   - `PRESENT`: Explicit positive observation at declared scope with authoritative source record.
   - `UNKNOWN` (Default): Missing, unmatched, ambiguous, or out-of-scope relations.
   - `ABSENT`: Only if source explicitly declares a complete closed-world universe proof for the exact entity set, jurisdiction, and time window. Never inferred from missing data.
5. **Edge Provenance (§12):**
   - Every edge binds `source`, `license`, `split`, `tier`, and `artifact_hash`.

## Verification

Run the test suite:
```bash
python -m pytest tools/research/c3/test_allergen_observation_graph.py -v
```
