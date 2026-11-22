# L36 conditional H=0 descriptive point result — 2026-11-22

## Status and scope

One HUMAN-authorized local calculation completed using the frozen L36 inputs and unchanged Protocol v1. This is a **descriptive conditional point result only**. It is not a confidence interval, H1 decision, C1-G4 acceptance, overall C1 acceptance, safety claim, or model-quality claim. The primary uncertainty-method review remains held; Protocol v1 was not changed.

## Fixed-cohort result

- Integrated cohort: 150 cases and 867 candidate judgments; 717 agreed and 150 adjudicated. The denominator-preservation flag is true.
- Assigned unseen cohort: 100 cases, 50 in each frozen stratum; zero case records missing and zero adjudications defaulted to unknown.
- H=0 membership: 49/50 `unseen_profiled` and 48/50 `unseen_unprofiled` (97 total); observed top-five hits are 1 and 2 respectively (3 total).
- Weighted H=0 denominator: 3,665.92.
- Conditional strict-A-plus-unknown descriptive partial bounds: lower 0.711494, upper 0.752160. These bounds reflect the frozen case rule's unknown handling; they are **not** an uncertainty interval.
- H=0 case values: `unseen_profiled` — 38 success, 10 failure, 1 unknown; `unseen_unprofiled` — 34 success, 12 failure, 2 unknown.
- Interpretation flags: descriptive only; interval not computed; H1 not decided; G4 not claimed.

## Integrity receipt

The retained v5 caller and source-free tests are SHA-256 `9405ece272b70d438a5ee470f7211fdc1b7dd9afe784b460b2e8a4580aaecaf1` and `562e68608da43d1c607520bab47ba83f7069cd81341b929c86605e63179b23dc`. Independent source review passed. The fresh pre-access receipt is `11c84ed8b2149206b7cfd7fa9b300d0cc61df79bc9d544521e74e3fc87daf57c`; metadata-only preflight passed and bound all 22 file hashes, five dependency-code hashes, and required receipt roles. It parsed no outcome rows and called no scorer.

Private aggregate artifact hashes: membership file `af0b31eb83e24a16f1b0441c5c8f73d1867ffe29b58540f4ea7ae62f7dbe8feb`; point file `9683bd13bde23f82480899622ceaac62ca08ee419c3af01c5d3325a1bd043a82`; final point-receipt self-digest `85f7aa1f3fd2ae4573f01595110e0920d196cd31131b9ca1ba047b61c18ba676`; run-state `8d262b66e2d355dc6b1d3469f62da786f6312526722310ca5db084d3557e1efb`. These are distinct digest domains: the point-file SHA binds the serialized file bytes; its `receipt_sha256` binds the final canonical point payload; its top-level `membership_receipt_sha256` binds the actual membership sidecar file; the nested conditional field of that name is instead the canonical source-role-map digest. The evaluator continues to mark `membership_provenance_status=CALLER_VERIFICATION_REQUIRED`; the separate source-binding review confirms the caller's exact input/receipt checks, not a change to that field.

Lead validation passed 13 integrity/shape/binding checks, including fixed counts, membership receipt binding, complete final point-receipt digest, finite ordered bounds, fixed interpretation flags, no case-identifier fields, and no missing assigned records. Independent aggregate arithmetic review PASS: stratum weights 176/50 and 3,639/50 yield weighted H=0 denominator 3,665.92; lower/upper weighted numerators 2,608.28/2,757.36 yield exact bounds 0.7114939769553073–0.7521604399441341 (reported rounded to six decimals above). The 0.040666 width is unknown-value partial-identification width, not a confidence interval. Full-cohort case values remain V=1/0/unknown 117/28/5. Reviewer read aggregate artifacts only; no raw ratings or IDs were opened. The single authorized calculation is complete; no further scoring run is authorized here.

No raw cases, recipe/candidate identifiers, source rows, private keys, ratings, adjudication rows, or case-level outcomes are included here. The underlying caller, input package, pre-access/preflight files, and aggregate receipt files remain owner-private; only this sanitized summary and opaque hashes are shared.
