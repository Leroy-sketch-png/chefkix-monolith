# LEAD evidence handoff — C2 payload boundary and C3 Recipe1MSubs source trace

Observed 2026-10-26. This is a sanitized cross-repository handoff of two lead-reviewed, local research findings. The raw T208 request package and source rows remain in the research worktree; this receipt does not copy them or claim they are fetchable from this repository. No protocol, cohort, method, rights, or milestone status changes.

## C2 — local payload shape only

The lead reviewed T469's streaming audit of the exact frozen T208 v3 package in `D:/code/chefkix/.git-worktrees/root-master-segment10-20261014/ai/out/T208/package-v3/`.

- Package manifest SHA-256: `82973520e41a41160eb938ab9921fff64046bfe5cc133b6e05456dae7a8030d1`.
- `request_bodies.jsonl` SHA-256: `7c54ea1b0995263a3e67ad5210454d1c4363604a69b1f238c4a4f471e1e3175`; 10,499 bodies.
- `dispositions.jsonl` SHA-256: `d533439d247e43af696c8bc3ab6963e5fcd095557282d9c1179b2b625c552d9a`; 10,747 records.
- All 10,499 serialized user payloads contain exactly `recipe_title`, `ingredient_list`, and `ingredient_to_replace`; no case identifiers, labels, profiles, or pools were found. Zero malformed/extra/missing payload shapes; 248 `TARGET_SLOT_MATCH_MULTIPLE` preparation failures remain in the denominator.
- T469 report SHA-256: `b068a158b208ce2bbc5e234abf34e0c83972b3d0cc8576c9bbb63dab9174dcb3`; packet SHA-256: `7c1dd55a579cb42937d419680a5045674966057f01e9337776372198df8128fa`.
- **Boundary:** verifies saved local serialization only. No source-transfer permission, current provider/model/capacity, budget reservation, cohort selection, upload, dispatch, comparison, or C2 acceptance is established.

Keep identity denominators distinct: T300 records 122 unresolved identities in the 10,729-row full validation split and separately 122 `TARGET_SLOT_MATCH_ZERO` failures in the frozen 200-case validation payload attempt. T469 concerns the 10,747-row test package, with 248 multiple-slot failures. These counts are not interchangeable; T188 found no demonstrated spelling-normalization repair. Preserve all failures and counts.

## C3 — Recipe1MSubs source lead, not an accepted input

Lead reviewed T470 against T457/T448/T376. The publisher paper describes positive directional substitution tuples keyed by Recipe1M recipe ID and extracted from scraped recipe-site comments. The immutable GISMo README names split pickle/vocabulary/merge assets at commit `f5d2394c7ade897987ce584d2a75d8bbbbae058e`; its repository license is CC BY-NC 4.0 and the README says most ingredient-substitution data is CC BY-NC, but does not map that notice to each hosted data artifact or establish rights in upstream comment content. The creator-associated Recipe1M access README says research/institutional access; operative accepted terms for this project and third-party processing are not in the public page.

- [GISMo paper, arXiv v1](https://arxiv.org/html/2302.07960v1)
- [GISMo README at immutable commit](https://github.com/facebookresearch/gismo/blob/f5d2394c7ade897987ce584d2a75d8bbbbae058e/README.md)
- [GISMo LICENSE at immutable commit](https://github.com/facebookresearch/gismo/blob/f5d2394c7ade897987ce584d2a75d8bbbbae058e/LICENSE.md)
- [Recipe1M+ project page](https://im2recipe.csail.mit.edu/)
- [Creator-associated Recipe1M access instructions](https://github.com/torralba-lab/im2recipe-Pytorch/blob/master/README.md)
- Research-worktree T470 report SHA-256: `db8269a92312de159163b42fc3d5320d79c7c0f977dfc4ef80fd2dee63a48065`; packet SHA-256: `8469c5d3a112dd65577398960e30fb0b315e9817e0c6438cd0f6b5c81bab11b1`.

The reviewed public sources do not establish the hosted binary schema/checksums, per-comment provenance, complete candidate acceptability states, allergen profiles, project-case joins, or artifact-specific local/transfer/retention/publication rights. No data bytes or rows were acquired/opened; no source-owner or legal contact occurred. The exact rights question remains in the research-worktree T470 card. No C3 input or milestone is accepted.

## Current disposition

Fixed measure remains **3/25 (12 points)** and fully accepted C1–C5 contributions remain **0/5**. Keep C2 transfer/provider work parked until its actual rights, compatible release, current account/capacity and reserved-cost gates are satisfied. Keep C3 input acquisition scoped to actual access/rights terms and the missing source-backed profiles, candidate references, joins, and provenance; do not infer any of these from positive tuples or repository-level licensing.
