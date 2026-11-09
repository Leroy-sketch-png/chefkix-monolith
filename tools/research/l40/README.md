# L40 Vietnamese Pilot Deterministic Sampling Frame Validator and Allocator

This package implements and verifies the deterministic sampling frame validator and allocator for the **L40 Vietnamese Pilot**, conforming strictly to the frozen decision `iron-chef-l40-vietnamese-pilot-v1` (2026-11-06) under Strategy §10 and Campaign Protocol v1.

## Authority and Boundaries

- **Decision ID:** `iron-chef-l40-vietnamese-pilot-v1`
- **Protocol:** Campaign Protocol v1, SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`
- **Scope:** Source-free frame validation, seed derivation, regional and named-family quotas, cross-cohort recipe cluster deduplication, and fail-closed shortfall reporting.
- **Ethics Gate (PR13):** This tooling prepares the deterministic frame and allocation logic only. No human participants are recruited, no human labels collected, and no empirical claims made without documentary institutional ethics approval and explicit consent.

## Mathematical and Protocol Invariants

1. **Target $N=100$ in Two Disjoint Cohorts:**
   - **Culinary validity cohort ($N=60$):** Regional targets are strictly 20/20/20 (`north`/`central`/`south`).
   - **Declared-allergen constraint cohort ($N=40$):** Regional targets are strictly 14/13/13 (`north`/`central`/`south`).
   - The two cohorts have separate denominators and reporting; there is no pooled 100-case headline.
2. **Deterministic Seed Derivation:**
   - The sampling seed is computed as the big-endian uint64 from the first 8 bytes of:
     $$\text{SHA256}(b"\text{iron-chef-l40-sampling-v1}\backslash 0" + \text{bytes.fromhex}(\text{frame\_manifest\_sha256}))$$
3. **Cross-Cohort Recipe Cluster Deduplication:**
   - At most one case per source-recipe cluster across both cohorts. When a cluster is allocated in the culinary cohort, all cases from that cluster are excluded from the constraint cohort.
4. **Dish Family Quotas and Floors:**
   - 7 primary named families: `noodle_soup`, `rice`, `grilled_braised`, `roll`, `dipping_sauce`, `sweet_soup`, `sandwich`.
   - Named family floor: $\ge 5$ cases per named family in culinary, $\ge 3$ in constraint (where eligible records exist).
   - Fallback families (`other`, `unknown`) are retained for analysis but **never** fill named-family floors.
5. **Least-Filled Cell Round-Robin Allocation:**
   - Allocates cases across region $\times$ family cells by selecting the least-filled cell with available cases whose region has not yet reached its quota.
   - Tied cells prioritize named families below their floor, followed by canonical region order (`north` $\to$ `central` $\to$ `south`) and canonical family order.
   - Within each cell, candidates are ordered by seed-derived case rank ($\text{SHA256}(\text{seed} + \text{case\_id})$) with stable `case_id` tie-breaking.
6. **Linguistic and UTF-8 Integrity:**
   - Diacritics are strictly preserved in UTF-8 NFC format. Lossy transliteration, stripping, or fuzzy matching is rejected.
   - Each case requires a paired checked English gloss as a secondary audit field.
7. **Fail-Closed Shortfall Reporting:**
   - If a cohort cannot meet its total or regional target, it stops and emits an explicit shortfall receipt.
   - Unmet named family floors are recorded as shortfalls without inventing synthetic cases.

## Usage and Verification

Run the test suite:
```bash
python -m pytest tools/research/l40/test_vietnamese_pilot_sampling.py -v
```
