# R12 pinned FooDB service-inventory reproduction

State: `PASS_NARROW_INVENTORY_ONLY`.

Receipt SHA-256: `14a3bf7f66a74e303f09d61d3329b0d308b077aa1916de33e5017bf74b6dbd2d`.

## Observed inventory

| Measure | Direct JSON | Sidecar | Loaded runtime |
|---|---:|---:|---:|
| `indexed_food_name_count` | 971 | 971 | 971 |
| `resolved_named_compound_count` | 206 | 206 | not separately exposed |
| `indexed_food_compound_links` | 125992 | 125992 | not separately exposed |

## Provenance and limits

Authority: FooDB 1.0 official JSON download. License declared by the sidecar: CC BY-NC 4.0; commercial use requires permission from FooDB authors.

Semantics: Presence-only profiles over named Compound-authority rows within the 255 globally most documented Content source IDs; no concentration, flavor, functional-equivalence, or safety claim.

This local reproduction verifies only pinned inventory arithmetic and runtime profile count. It is not rights clearance and does not establish usefulness, safety, quality, human outcomes, or thesis-contribution acceptance. The ignored artifact was not copied or redistributed.
