# R18 bounded reproduction: OFF snapshot allergen-tag partition

Date: 2026-10-21. Research checkout: `master` at `b45eeba2a6bdbc1b3aa7c36c6ee6206ff0873079`.

## Claim and disposition

SSoT §15.1 R18 asks for the Open Food Facts (OFF) taxonomy partition counts and explicitly warns that the snapshot and taxonomy dates differ. The current register’s historical-source wording is not primary evidence. A fresh local reconstruction now supports `VERIFIED` only for the exact deterministic partition of the pinned snapshot tag counts against the preserved taxonomy bytes. This is a narrow R18 result; it does not close L20, L39, allergen-safety, or C3 acceptance.

## Source and byte identity

The source materials were found read-only in the dirty root checkout, not copied or modified there:

| Input | Path | SHA-256 / boundary |
|---|---|---|
| OFF snapshot manifest | `D:/code/chefkix/ai/out/iron-chef-lead-v75/off_allergen_full_snapshot_manifest.json` | file `5caa20cffc3a8325bfceb3bee600199c84639d59ba2fc8b765e60bbef3da1dde`; canonical self-hash `144fb31659953231d0baa3302f59ddc1f815300727b21ba2fa496d8b30b4473a` |
| Derived 351,351-row snapshot | `D:/code/chefkix/ai/out/iron-chef-lead-v75/off_allergen_full_snapshot.parquet` | `807fe58edabb3bc11b5aca650b47983f033c8d5411c1f2ab05309eac2c709c68`; 46,087,455 bytes |
| Official taxonomy snapshot | `D:/code/chefkix/ai/out/iron-chef-lead-v101/allergens.full.json` | `6fb38bfb0d4669257fc8c66ba7fdf0cf2f8665f7d80af25c3fc6ac5e4bad153f`; 88,933 bytes; recorded last-modified 2026-09-30 |
| Prior partition report | `D:/code/chefkix/ai/out/iron-chef-lead-v101/normalization_report.json` | `a5745b992f3ac9c9aa21708fb4d0322dc2ecf0fe585919d2f1c8e4f34bfe5c1a` |
| Prior deterministic review queue | `D:/code/chefkix/ai/out/iron-chef-lead-v101/review_queue.json` | `6eac9619b1879af59a67ff88f2541976c4c8a80c4b060ad7a06d0ee8b440e790` |

The snapshot manifest is dated 2026-09-08; the taxonomy was modified 2026-09-30. The tag counts were independently recomputed from the Parquet rows and matched the manifest’s per-field maps exactly. The fresh output is [r18-snapshot-tag-counts-reproduction-20261021.json](r18-snapshot-tag-counts-reproduction-20261021.json), SHA-256 `df4845a7109603ba2e07b56646aa086353948a951b6f5aba67d00281db6afed7`.

## Reproduction and validation

The reproducible source is `ai/tools/reproduce_off_allergen_snapshot_tag_counts.py` (SHA-256 `3126dc90834403db76f4f24f3e7d3429af6dc26d5729944658bb65115eb4a350`). It verified the canonical manifest hash, Parquet file hash/size/row count, and exact allergen/trace tag maps by recounting rows. A fresh partition validation then rebuilt the count partition against the preserved taxonomy using `ai/tools/validate_off_allergen_taxonomy_bridge.py` (SHA-256 `101683c0603a0ff8c3ece73848d05ad40ac7e6035cb3fadebb6d7a03f7102551`) and its pinned builder `ai/tools/build_off_allergen_taxonomy_bridge.py` (SHA-256 `759dedae6d0879f8f57046596222ee78153ed0cc548a34d73b24cb61105297ce`). The validator returned `PASS`, reconstructed 3,377 distinct tags / 767,780 occurrences, and rejected 8/8 semantic mutations. Its saved output is [r18-taxonomy-partition-validation-20261021.json](r18-taxonomy-partition-validation-20261021.json), SHA-256 `a5f97d1caacec550178ab2dd94c03aa7fd3847e7626893398e1f19798185cdb7`.

Partition result:

- 23 exact official positive identities: 760,877 occurrences.
- `en:none`: 564 occurrences, kept separate from positive allergens.
- 17 unique same-language alias candidates: 32 occurrences; review-only, not accepted mappings.
- 3,336 unrecognized values: 6,307 occurrences; not negatives.

The six focused taxonomy tests pass; the register/evidence-tool, register-builder, register-validator and snapshot-reproduction suites pass 32/32 (38 tests total). These checks establish the bounded byte and deterministic-reconstruction result only.

## Crosswalk and history

The v171 register (`D:/code/chefkix/ai/out/iron-chef-lead-v171/claims_register_R01_R02_R03_R04_R05_R06_R07_R08_R09_R10_R11_R12_R13_R14_reviewed_v171.csv`, SHA-256 `c4a7350f1ccfb425da801e8a34b4a5112e2545ffd6c494e4eb4f8043f41e00bb`) and v197 derivative (`D:/code/chefkix/ai/out/iron-chef-lead-v197/claims-register-R01-R45-v197.csv`, SHA-256 `44689a1898dcbc09a7c40d3fa9c53ee2007540d5601eda8448c138213ea0cd9b`) were recovered from that separate dirty checkout. Their historical status totals are not imported. The v197 R18 row was historically `VERIFIED`; this pass re-ran the bounded reconstruction rather than inheriting that status.

Current crosswalk: L20 is the R16–R18 package containing the taxonomy-partition subdeliverable; L48 owns the register reconciliation. Neither package is complete from this row alone. This reconciles the earlier “no exact current owner” inventory note for R18 without assigning ownership for the other unresolved rows.

## Limits

OFF product-label tags are community data, not clinical ground truth. The taxonomy is later than the snapshot, so the result is not the taxonomy state at ingestion time. Alias candidates remain review-only; unknown/missing tags do not mean safe. No qualified adjudication, clinical sensitivity/specificity, production serving, or user outcome is established. The 7.8GB original upstream parquet was not re-fetched or revalidated in this pass; the bounded reproduction starts from the hash-pinned derived snapshot and manifest.
