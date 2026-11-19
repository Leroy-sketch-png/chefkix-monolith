# C5 Section 12 FooDB-Native Chemical Product Graph Evaluator

This tool provides deterministic, fail-closed enforcement of the §12 `graph_product_v4`
contract under `BACKLOG_LEAD.md`.

## Key Invariants

1. **Strict Destination Isolation:** Target is strictly `graph_product_v4`. Product chemistry,
   held-out substitutions, or source-derived mappings must NEVER enter the frozen evaluation
   graph (`graph_eval_v4`).
2. **Exact Native Pair Inventory:** Validates 125,992 unique Food->Compound pairs joined strictly
   through typed Content rows (`Content.source_type == 'Compound'`, `Content.food_id == Food.id`,
   `Content.source_id == Compound.id`).
3. **No Normalized Name Joining:** Edges must not be derived by expanding/grouping profile names.
4. **Export Eligibility Partition:** Preserves the 107,496 export-eligible pairs and 18,496
   non-export pairs as present-but-not-export-eligible. Neither is marked absent.
5. **Top-255 Scope:** 206 artifact compounds, 49 dangling IDs excluded, 4 Food records without
   compounds excluded.
6. **Support Count:** Per-edge support computed solely from matching Content rows, not global
   compound row counts.
7. **Tri-State Semantics:** Default missing profile/dimension is `UNKNOWN`. Never infer `ABSENCE`
   without closed-world universe proof.
8. **Nutrient/Allergen Isolation:** Compound co-presence cannot imply allergens or nutrients.

## Test Execution

```powershell
python -m unittest discover -s tools/research/c5 -p "test_*.py" -v
```
