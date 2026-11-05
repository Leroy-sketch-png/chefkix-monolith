# T54 method-v2 calibration producer contract — Lead-owned specification

**Date:** 2026-11-06
**Owner:** LEAD owns the scientific meaning and acceptance contract. M-I owns implementation and runtime operation after this interface is accepted.
**State:** Contract specification; no producer implementation, real-input verification, calibration, guarantee, route activation, or contribution acceptance is claimed.

## Authority and pinned inputs

- Frozen Protocol v1, `research/protocols/IRON-CHEF-PROTOCOL-v1.md`, SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.
- Current local strategy §§3, 6–8, and 17, SHA-256 `b5ad66201eeed44e201b471eb895aaaff0f6f3c2c774a1d1cdf978d6deacddce`. This is a local modified SSoT snapshot; it is not copied here or represented as the protocol.
- T54 task card as reviewed, SHA-256 `84bc12bf8dafb23013bb58d4a42f77b355e621e80e9456970f269d10942a3e32`.
- AI-service default branch at `ca0577115fed853c4c9179390e56000ff2bc3484` (fetched at 0/0 before this contract): method-v2 producer `src/services/substitution_calibration_receipt_v2.py` SHA-256 `2496d130286f9547003adfe7f5a94267ba35f3f9101697f235848d19c567dd93`; existing bundle writer/loader `src/services/substitution_calibration_bundle_v2.py` SHA-256 `0000957607073f520b66a294a91e62d34f27af14f0cc03ad714da45b5a6484de`; typed response `src/models/substitution_evidence.py` SHA-256 `aab5727c6456da1646d8f33bb70176e7d8d40c23ee7dd3739cc96c4f4057ea03`; focused tests SHA-256 `1724714645406a844776c5a10aef8c2df9cf0a15a28342ff5ea1bde2fe5fcf9b` and `177c7b27f775323f1c2a7ef58c871743bc75aaeb5a8b4a3ccf490ad157e341a9`.

The recorded HUMAN adoption of `c3-rank-conformal-receipt-v2` is prospective. Protocol v1 remains byte-frozen. The historical T243 log reports 3,169 development / 7,560 calibration cases, but its assignment tool, assignment manifest, and report are absent from the active checked checkout; that aggregate is not evidence for the algorithm below and historical T51 outputs must not be relabeled or recomputed under it.

## Owner-declared method-v2 contract

### 1. Validation frame and split

Use the complete, frozen validation frame only. Validate the exact source identity join and require each case exactly once with nonempty `case_id` and source-defined `recipe_id`. Do not normalize, alias, or infer recipe identity. The split is made once over the entire validation frame, before labels, outcomes, deployment cells, candidate scores, or model outputs are inspected. Test rows never enter assignment.

The deterministic group assignment is `c3-recipe-group-sha256-split-v1`:

1. Let `G` be the number of distinct exact recipe IDs. Require `G >= 4`; otherwise refuse rather than adjust the ratio.
2. For each recipe ID, compute SHA-256 over UTF-8 bytes of `iron-chef:c3-rank-conformal-receipt-v2:recipe-split:20261021\0` followed by the exact recipe-ID UTF-8 bytes.
3. Sort distinct recipe groups by raw digest bytes ascending, then exact recipe-ID UTF-8 bytes ascending as the collision tie-break.
4. Set development group count to `floor(3*G/10)`. Assign the first such groups to development and all remaining groups to calibration. Every case for a recipe inherits that single split.

This is a 30/70 allocation over unique recipe groups, not an exact 30/70 case-row promise; case counts are an observed consequence of group sizes. The algorithm is input-order invariant, outcome-blind, and seed-pinned. Record `G`, group counts by split, case counts by split, the global assignment digest, and the validation-frame/identity source pins. This new project-owned realization is prospective and does not claim to reconstruct T243's absent assignment.

### 2. Cells, support, and denominators

- Baseline-0 is the all-input cell. Partition-1 is the four request-time cells in Protocol v1: train-source presence × FooDB-profile presence. Cell membership may use only the frozen request-time identity/features; never gold-dependent seen/unseen or both-profiled strata.
- Derive each cell's membership from the full frozen validation frame before outcomes. If a Partition-1 cell has fewer than 200 validation cases, apply the Protocol v1 Baseline-0 fallback and do not describe it as a conditional guarantee.
- Split the full frame globally first, then form each cell's development/calibration projection. A recipe appearing in multiple cells must retain the same global split in all projections.
- One receipt candidate is computed for one `(cohort, cell, alpha, M)`. Its `n` is the number of distinct calibration **case IDs in that cell**, not recipes, candidate rows, or all validation cases. Require one score/failure record for each such case. `n < 200` means `no_guarantee`; no resampling, cell merging, replacement, or denominator reduction.
- Preserve every calibration case. A missing/duplicate case or a disagreement between global assignment, cell membership, identity join, and score rows invalidates the receipt. Valid operational scoring/method failures remain in `n` as `+infinity` and are counted separately.

### 3. Rank and score semantics

For each case, execute the frozen order **candidate pool → hard allergen filter → rank → truncate to M**. Primary `M=20`; `M=50` is the named GISMo sensitivity only. Rank scores are ordered by the frozen scorer; exact ties use ascending UTF-8 bytes of canonical candidate ID, and duplicate IDs refuse. Define the nonconformity score as the one-based rank of the first candidate satisfying the bound label contract within the filtered top-M list; if none satisfies it, the score is `+infinity`. Do not rank first and filter later, or change M after seeing results.

The current typed rows need to distinguish three outcomes without semantic conflation:

| Typed row | Meaning | Conformal score | Denominator treatment |
|---|---|---:|---|
| `score = 1..M`, `failure=false`, no code | A candidate satisfies the exact bound label contract | That rank | Retained |
| `score = "infinity"`, `failure=false`, no code | Valid computation; no candidate in filtered top-M satisfies the label contract | `+infinity` | Retained; not called an execution failure |
| `score = null`, `failure=true`, code `scoring_failure` or `method_failure` | Explicit operational/method failure under the adopted v2 taxonomy | `+infinity` | Retained and separately counted |

Any other encoding, unknown failure code, missing record, duplicate, or malformed rank invalidates the receipt. The current `CalibrationScore` and bundle validator do not yet support the normal no-match `"infinity"` row distinctly from failure; M-I must extend the existing typed contract/serializer in place (a versioned schema evolution is acceptable), not create a parallel serializer. A missing or unresolved label is not silently mapped to `N`, failure, or a negative.

### 4. Label/reference contract

Every run must bind an explicit, versioned `label_contract_id`; “acceptable” is not caller-defined. Protocol v1 §7 and Lead L38 already select **recorded-substitute coverage when labels are recorded gold**, while reserving human-valid coverage for an accepted human-valid reference. For the descriptive recorded-gold path, this contract makes that existing disposition executable with owner-declared ID `recorded-substitute-exact-identity-v1`: the predicate is exact identity between the recorded substitute and the reference identity under the pinned identity map. Report only recorded-reference coverage; this is not human-validity or culinary-quality evidence. This names the implementation contract; it does not assert the source bytes or identity map have been acquired or verified.

A separate human/judge-validity path must bind its accepted label artifact, rubric/prompt version, identity mapping, adjudication version, and reliability/validity state. Under the strategy rubric, strict `A` is the primary human-validity predicate and `A+C` is a separately named sensitivity; `N` is not acceptable and `U` remains unknown, never negative. Do not merge these label modes or transfer guarantees between them. The unresolved L36 `V`/`U` choice is the separate human-audit/H1 question; it does not block implementation of descriptive recorded-reference coverage.

There is no new scientific-method selection pending for the descriptive producer. Its actual blockers are source-owned bytes and role verification: until the identity map and all required source roles are available and verified against their bytes, the loader must return missing-binding/no-guarantee and the route must remain default-off. Do not present missing inputs as a permission or method-selection hold.

### 5. Provenance, files, and typed response

Reuse the existing `c3-calibration-bundle` writer/loader and typed rows. The current v2 manifest is insufficient for this contract: it verifies local JSONL bytes and typed-row digests, but does not bind the full-frame parent assignment and the per-cell projection to one another, and its current type cannot distinguish ordinary no-match infinity from operational failure. Extend that same path with a versioned manifest/schema revision carrying:

- the method version and the exact split algorithm ID, seed, ratio numerator/denominator, group-count rule, full validation-frame/identity manifest digest, global assignment digest, and cell-projection digest;
- the single cohort/cell, alpha, M, and explicit score/label contract IDs;
- an exact file inventory with repository-relative logical names, byte lengths, and raw SHA-256 for manifest-bound assignment, identity, and score records;
- separate digest roles for `graph_manifest`, `hard_filter`, `ranker`, `scorer`, `score_contract`, `label_contract`, `split_assignment`, `identity_index`, and `calibration_scores`.

The loader must derive each observed digest from the exact bytes it loads and independently verify declared parent manifests and role contents. Caller-supplied hashes are expectations only, never observations. Hash raw manifest bytes separately from raw data-file bytes and from canonical typed-row digests; name which digest each field contains. Verify that cell projections exactly match the globally recomputed assignment and request-time cell index before emitting typed rows. A role declaration, a filename, or a digest string alone is not proof that the role was loaded. Any absent, mismatched, unreadable, unrecognized, or incompatible role yields `binding_status=missing|mismatched`, `no_guarantee`, null `tHat`, and abstention.

The wire response must retain `graph_manifest_sha256` as graph identity and add a distinct verified calibration/pipeline receipt digest; never overload the graph manifest hash as proof of all nine roles. Keep existing auth, privacy, and default-off route behavior. No raw IDs, case rows, labels, candidate pools, or source data enter shared receipts or logs.

### 6. M-I implementation acceptance (no empirical credit)

1. Implement the split and cell projection from verified source bytes; reject duplicate/missing identities and prove output is invariant to source-row ordering while recipe groups never cross partitions.
2. Extend the existing bundle path and typed producer to bind global and per-cell assignment provenance and the three distinct score encodings above. Preserve exact raw-file hashes and canonical-row digests as separate fields.
3. Recompute status, `n`, `k=ceil((n+1)(1-alpha))`, `tHat`, and finite-only coverage from loader-verified rows; failures stay in the denominator, infinity is never covered even when `tHat` is infinite, and invalid binding returns `no_guarantee` with null `tHat`.
4. Independently verify role-byte bindings and response provenance. Keep route default-off and fail closed until real accepted graph/filter/ranker/scorer/label inputs and runtime gates pass.
5. Use synthetic independent fixtures for code acceptance; no fixture, test count, historical T51 number, or source-free receipt is a real calibration result or C3 acceptance.

## Evidence boundary

This is a Lead-owned implementable specification based on the pinned protocol, strategy, current T54 card, and fetched AI-service source/type bytes listed above. The inspection was source/code-only. No private rows, labels, score artifacts, source corpus, runtime, tests, API/network/GPU/Kaggle job, upload, spend, MEMBER implementation, or route activation occurred. Protocol v1 hash and mission measures/claim statuses are unchanged. Current T243 producer artifacts were not found in the active checkout; this bounded absence is not a global-absence claim.
