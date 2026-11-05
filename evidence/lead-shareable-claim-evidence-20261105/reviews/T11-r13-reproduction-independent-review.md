# Task T11: Independent review of the narrow R13 reproduction

## Outcome And Acceptance
- Independently inspect T10's new driver, focused tests, machine receipt and report; rehash the pinned profile artifact, provenance sidecar, service source, reproduction driver, receipt and report.
- Independently recompute pair denominator/diagonal/cross-pair accounting and the shared-name/Jaccard oracle results over the exact profile bytes, or document a concrete blocker. Check the inclusive-vs-cross denominator language, one-row R13 fit, license boundary, zero-label/external/GPU scope, and no-usefulness/safety/quality/C-outcome overclaim.
- Write only this review report with verdict `PASS narrowly` or `FAIL`, exact checks, evidence hashes, limitations, and decision-changing discrepancy. Do not edit register, SSoT, backlog or implementation.

## Workspace And Boundaries
- Read-only sources: T10 driver/test, R13 JSON/Markdown, SSoT §15.1 R13, T08 report, exact profile artifact and provenance/runtime source.
- Allowed write: only `tasks/T11-r13-reproduction-independent-review.md`.
- No use of labels, case data, network, GPU, paid services, or source artifact copying. Do not promote R13 status.
- Retry class: inspect-first local; reviewer should hash-check current files before review.

## Checkpoint
- State: REVIEWED; see independent result below. (This does not apply evidence or change R13 status.)
- Result under review: T10 reports `PASS_NARROW_ARITHMETIC_ONLY`, 971 profiles, 471,906 unordered pairs including 971 diagonals, 470,935 distinct cross-profile pairs, 2,819 zero-shared and 469,087 nonzero-shared pairs, zero runtime/oracle mismatches. T10 report/receipt hashes are recorded in MISSION; independently confirm before verdict.

---

# Independent review result — 2026-10-21

**Verdict: PASS narrowly** for the R13 arithmetic-only boundary. The bounded evidence application is justified for lead review only if the denominator is stated as unordered pairs *with replacement*, including diagonal self-pairs. I did not edit or recommend changing claim status.

## Rehashed evidence

All hashes below were checked from the current local files (not trusted from T10's assertions):

- Profile artifact: `D:/code/chefkix/chefkix-ai-service/models/foodb_compounds.json` — `8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac`.
- Provenance sidecar: `.../foodb_compounds.provenance.json` — `5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208`; its `data_sha256` matches the profile digest. Authority is FooDB 1.0 official JSON download; declared license is CC BY-NC 4.0, with commercial use requiring FooDB-author permission; semantics are presence-only and expressly exclude concentration, flavor, functional-equivalence, and safety claims.
- Runtime source `D:/code/chefkix/chefkix-ai-service/src/compound_reasoning.py` — `e2bcad3cd81e196490aaa6bcc4f014721e9cb9e69734351520fabedef7463e3a`.
- T10 driver `ai/tools/reproduce_foodb_profile_pair_arithmetic.py` — `bbf10b9f0f5aa3dfdf559fa61555973c90550c633673df56c337c5269bfa9006`.
- Focused test source `ai/tools/test_reproduce_foodb_profile_pair_arithmetic.py` — `9ab4844c7f11b2b76d94e1dd2fe5e702c9091d452a6d30053310c10c0c723186`.
- T10 v2 machine receipt — `c63db935d919d5c87c3bbffe4aafb2a1ff476ef66a3a1d7a5db34d9c8d6482fa`; it binds the four input digests above, including the driver.
- T10 v2 Markdown report — `06bbc96de07ee3ef826a953ff972838a3821c9187985eec5913ccde7f5da803d`; its receipt digest agrees with the current v2 receipt.

The previous v1 report/receipt are preserved (hashes `73d9b38b718752eba0b285072a6ac327c30e1fa8bf7c351eff56c686d7dae6fd` / `cac5dd8af8a7718a1538a433d26bfd0492b0afe1e672adfd431e8345508e596d`). T10 v2 closes v1's missing driver-hash binding; I reviewed v2 for this verdict. I did not inspect recipe cases, labels, or other project data.

## Independent arithmetic and runtime comparison

Without importing or calling the T10 reproduction driver, I loaded the pinned profile JSON, cached each profile's set of lowercase compound names, and independently iterated sorted profiles with `i <= j`. For each pair I computed `A ∩ B`, `A ∪ B`, and `|A ∩ B| / |A ∪ B|` (zero if union is empty), then compared these to the actual pinned `SharedCompoundAnalyzer.analyze()` output: identical shared-name sets and finite Jaccard values within absolute tolerance `1e-15`.

Observed result: 971 profiles; `971×972/2 = 471,906` inclusive unordered pairs; 971 diagonals; `971×970/2 = 470,935` distinct cross-profile pairs; 2,819 zero-shared and 469,087 nonzero-shared pairs; 0 service/oracle mismatches. Counts reconcile (`2,819 + 469,087 = 471,906`; `971 + 470,935 = 471,906`). The independently computed tuple matches the v2 receipt.

The service source computes compound-name sets, their intersection and union, and Jaccard with a zero-union fallback of 0.0. The independent oracle was separately implemented in the review command, rather than imported from the driver. The indexed profiles are nonempty per the driver's validator, so the zero-union branch is not exercised by the real artifact; T10's focused unit test covers it synthetically. T10's 10 focused tests were inspected by source; this review did not rerun them.

## Claim fit, evidence grade, and exclusions

SSoT §15.1 L587 states R13 as “471,906 profile-pair consistency checks” and limits it to “Factuality, not usefulness.” The reproduced 471,906 is specifically an inclusive combinations-with-replacement denominator; it contains 971 self-pairs, so it must not be described as 471,906 distinct food-to-food pairs. The evidence supports the literal count and arithmetic consistency of the service result on these pinned profiles, with that denominator caveat. Evidence ceiling: E2 local reproduction for arithmetic only.

The sidecar's CC BY-NC statement supports only the documented local, attributed, noncommercial preparation boundary; this review is not legal advice, a rights-transfer decision, or redistribution authorization. Nothing here establishes ingredient or food-form similarity, flavor, substitution usefulness, nutrition, safety, human preference, product utility, paper-protocol equivalence, model quality, or any C1–C5 outcome. No case labels, external service, network, GPU, or paid resource was used. A decision-changing discrepancy would be any pinned-byte mismatch, a denominator/count reconciliation error, or a service/oracle mismatch on this exact artifact.
