# Recipe1MSubs test-partition exposure reconciliation

Observed during lead reconciliation on 2026-10-26. This is a sanitized provenance note, not a data receipt, metric, or C3 acceptance claim.

- Source receipt: `evidence/member-mb-mf-recipe1msubs-source-license-audit-20261024.json`, SHA-256 `e0a882ce32a37dc78aa9491b147a91edccb40cfcd67bee7867ae34a1bd79c3ad`.
- Receipt reports that all three official GISMo split pickle files were downloaded outside Git and restricted-unpickled, including the test payload. The test payload hash reported by the receipt is `0758382cae59697958dd2a5e509c15f8d0417ea1a9e079fd75792779f1ce6174`; its reported structural aggregates include 10,747 records and an `id` / `ingredients` / `subs` schema. No test rows, labels, examples, model outputs, scores, or metrics are reproduced here.
- The owning research checkout records this operation retrospectively in `ai/evidence/EXPOSURE_LOG.md`; no matching pre-access declaration was found there. The member receipt does not identify the operator or parser digest. Do not infer either from commit authorship.
- Disposition: the test partition is **EXPOSED**. Do not reopen, re-download, re-hash, or use it for selection, calibration, tuning, primary evaluation, or new result claims. Preserve the fixed cohort and keep test-derived material out of future analysis.
- This status does not establish source rights, transfer permission, identity/split validity, a usable C3 input, or acceptance credit. Protocol v1 remains unchanged.
