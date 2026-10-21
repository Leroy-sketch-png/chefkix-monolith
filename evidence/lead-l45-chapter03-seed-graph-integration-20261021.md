# L45 Chapter 3 integration receipt — current seed source interpretation

**Purpose:** integrate an already-produced, bounded static-source interpretation into the Chapter 3 draft without conflating it with historical Round-0 graph claims or runtime evidence.

## Reproduction boundary

- Monolith source checkout: `D:/code/chefkix/chefkix-monolith/.git-worktrees/monolith-stage3-segment8`, branch `master`, commit `4df0f9f2e4c4384132102ea94aaed5efe84a80be`.
- Exact source: `culinary/src/main/java/com/chefkix/culinary/features/knowledge/service/KnowledgeGraphSeeder.java`, SHA-256 `cb7befabc03897f45c3d3d5b3a4f29262d93ec82622e9d5a8cf285dd28b15174`.
- Parser: `ai/tools/audit_monolith_seed_graph.py`, SHA-256 `ba61a541004fea5c767502eb8389f0c96ff0abdeb1cb6238287e25c8338f283e`.
- Fixture suite: `ai/tools/test_audit_monolith_seed_graph.py`, SHA-256 `af8544fdfad2ae176a9037e7b2a1e83a39ce161ff0524d9b8d3573377e2a5ba8`; rerun from `ai/tools` passed 7/7 tests.
- Recomputed JSON receipt: `ai/evidence/lead-ma-seed-graph-source-reconstruction-v2-20261021/seed_graph_report.json`, SHA-256 `0aead4a4a583e57057dadf733956988a33fe5c8bedca86026be4e276a3c8ab05`.
- Node table: `ai/evidence/lead-ma-seed-graph-source-reconstruction-v2-20261021/GRAPH_TRUTH.csv`, SHA-256 `11273128e8f2a9e34dfc18a7d97e783995946df455e3a386c8968a1960c85823`.
- Independent in-memory invocation of the pinned parser on the pinned source reproduced the report's source hash and counts.

## Observed interpretation

25 canonical ingredient seed records; 59 embedded directed substitution statements; 79 typed vertices; 59 directed arcs; five canonical-name target resolutions, one unique-alias resolution, and 54 ambiguous/unmatched literal target statements. Directed in-degree and out-degree sums each equal 59; total in-plus-out degree sum is 118. Maximum canonical-source out-degree and maximum total degree across the interpreted graph are both four. The parser uses trimmed, case-folded exact canonical/alias matching, resolves only a unique identity, and retains ambiguous or unmatched targets as literal vertices.

## Evidence ceiling

This confirms only that the stated parser returns these counts for the exact checked-out source bytes. It does not establish that the seeder ran, that its output is persisted, that it produced the separate HGAT artifact, that the AI service loads this graph, or that the current process serves it. The numeric overlap with the historical “25 nodes / 59 edges” claim is not reconciliation: here 25 counts canonical source records, 59 counts embedded directed statements, and the interpreted graph has 79 typed vertices. The historical R0 source, input snapshot, and metric definitions remain missing. R39/R40 remain `UNVERIFIED`; M-A and L45 remain `OPEN`.

This receipt does not independently validate the research question, claim status, usability, rights, safety, or model quality. It records an evidence-bound manuscript integration only.
