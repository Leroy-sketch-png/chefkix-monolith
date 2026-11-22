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
   - Each stable study case ID must be unique across the whole eligible frame.
   - At most one case per source-recipe cluster can be selected across both cohorts. Only clusters selected by a completed culinary allocation are excluded from the constraint cohort.
4. **Dish Family Quotas and Floors:**
   - 7 primary named families: `noodle_soup`, `rice`, `grilled_braised`, `roll`, `dipping_sauce`, `sweet_soup`, `sandwich`.
   - Named family floor: $\ge 5$ cases per named family in culinary, $\ge 3$ in constraint (where eligible records exist).
   - Fallback families (`other`, `unknown`) are retained for analysis but **never** fill named-family floors.
5. **Least-Filled Cell Round-Robin Allocation:**
   - Allocates cases across region $\times$ family cells by selecting the least-filled cell with available cases whose region has not yet reached its quota.
   - Tied cells use the seed-derived rank and stable case ID of their next eligible case.
   - Within each cell, candidates are ordered by seed-derived case rank ($\text{SHA256}(\text{seed} + \text{case\_id})$) with stable `case_id` tie-breaking.
6. **Linguistic and UTF-8 Integrity:**
   - Diacritics are strictly preserved in UTF-8 NFC format. Lossy transliteration, stripping, or fuzzy matching is rejected.
   - Each case requires a paired checked English gloss as a secondary audit field.
7. **Fail-Closed Shortfall Reporting:**
   - If a cohort cannot meet its total or regional target, it returns aggregate shortfall counts and no selected case rows.
   - Unmet named family floors are recorded as shortfalls without inventing synthetic cases.

## Reviewer payloads and evaluation

Use the existing sampler to freeze eligible cases first. Once the authorized, exact-identity candidate slates are available, pass the frozen sampling result, one completed cohort ID, and a private secret to the review serializer. The serializer reads the frame pin from the sampling result. It returns independent R1/R2 payloads and a separate restricted join key. An explicitly empty slate retains its selected case, and a missing case or failed cohort prevents payload construction. Keep the model rank, gold marker, source evidence, role assignments, and identity mapping in the owner-private records. The serializer does not write to disk or issue a reviewer pack. The L36 HTML renderer has a different fixed cohort and view schema.

The evaluator scores only the model top five, retains incomplete no-A slates as unknown, and verifies that paired filtered rows keep the same frozen case and can only contain labels already present in the raw slate. Candidate-level identity still comes from the frozen slate/filter manifest. Agreement is reliable only after the nominal gate passes and a valid interval supplies a lower bound of at least 0.60.

## Usage and Verification

Run the test suite:
```bash
python -m unittest discover -s tools/research/l40 -p "test_*.py" -v
```
