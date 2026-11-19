# L49 / H4 measurement instrument v1

**Status:** Lead-authored wording for the adopted prospective design. This is not participant-ready, an approval, a deployed form, Protocol v1, or evidence of participant results. No cases are supplied here. The twelve case slots below are structural placeholders and must be populated only from the separately validated, permission-cleared case manifest before review or use.

**Authority:** `BACKLOG_LEAD.md` L49, adopted 2026-11-07. The fixed design remains N=24 adults in 12 counterbalance pairs; each participant receives 12 distinct cases (six bounded-record-present and six record-withheld). The common task contrast is conditional accuracy on this evidence-limit item—not a generalized comprehension, usability, safety, or treatment-effect claim. Protocol v1 remains unchanged.

## Participant-facing copy

### Opening instructions

> You will review a series of short cases. Each case shows an ingredient, a recipe context, a mapped compound under review, and information from a named source record. Use only the information shown in that case to answer the question. Choose the one answer that is best supported. You may leave an item unanswered or stop, following the study information and consent process provided to you.

Do not add source explanations, examples, definitions, or condition-specific instructions. Display the same opening text in both conditions.

### Case card template

Show only fields authorized in the frozen case manifest; retain the same layout and wording in both conditions.

- Case content: `[case slot 01–12; bind from the frozen, source-validated case manifest]`
- Ingredient: `[exact validated ingredient label]`
- Mapped compound under review: `[exact validated compound label; shown identically in both conditions]`
- Recipe context: `[approved source-backed context, or the frozen neutral context field]`
- Source: `[exact source label and record reference allowed for display]`
- Bounded record: either `[exact mapped-compound record value and citation]` or `Not shown in this record`.

`Not shown in this record` is literal display text, not a participant-facing interpretation. Never replace it with “absent,” “does not contain,” “allergen-free,” or equivalent. The displayed-record condition may show only the exact, validated mapped-compound value; do not add inferred chemistry or interpretation.

### Primary question — show once per case

> Based only on the information shown for this case, which one statement is best supported?

Select one:

1. The source record reports the mapped compound for the specified ingredient.
2. The specified ingredient does not contain the mapped compound.
3. The specified ingredient is suitable as a substitute in this recipe.
4. The specified ingredient has sensory or functional properties equivalent to the original.
5. The specified ingredient provides a nutritional benefit.
6. The specified ingredient’s relevant allergen status is established.
7. The substitution is safe to use.
8. The information shown does not establish whether the source record reports the mapped compound for the specified ingredient.

Use the identical stem, option order, and option wording in both conditions. Do not display the response-code names or the owner key below to participants. Provide an unanswered/decline route without forcing a choice; record it as missing under the approved study procedure, not as an incorrect answer.

### Secondary ratings — after the primary answer for each case

Ask these as two separate items; do not combine them into a trust, safety, confidence-in-source, or overall-usability score.

**Usefulness**

> How useful was the information shown in helping you answer the question above?

1 — Not at all useful · 2 — Slightly useful · 3 — Moderately useful · 4 — Very useful · 5 — Extremely useful

**Confidence**

> How confident are you that the answer you selected is supported by the information shown?

1 — Not at all confident · 2 — Slightly confident · 3 — Moderately confident · 4 — Very confident · 5 — Extremely confident

Each rating may be left unanswered. These are secondary descriptive outcomes; the historical “useful ≥4” and 70% values are not acceptance thresholds.

### Optional comment — after both ratings

> Optional: Was anything about the information or wording unclear or important to your answer? Please do not include your name or other personal or sensitive information.

Comments are exploratory only. Do not solicit health history, dietary restrictions, allergy experience, or other sensitive personal information in this field. A blank comment is not a failure.

## Twelve case slots (not stimuli)

| Slot | Frozen case-manifest binding | Display condition binding |
|---|---|---|
| 01–12 | Each slot must resolve to one distinct, source-valid, permission-cleared identity and its hash-bound record/profile relation. | The counterbalance assignment selects the exact bounded record or `Not shown in this record`; no case wording may be improvised. |

There are deliberately no ingredient names, recipes, compounds, source excerpts, or participant responses in this file. Do not treat this template as ready to deploy. If any slot lacks a valid identity, source-use permission, or required profile/record linkage, preserve the shortfall and stop stimulus freeze; do not fuzzy-match, invent a case, or substitute a different source.

## Owner-only scoring and interpretation key — never render in the participant form

| Option | Frozen response code | Displayed bounded record | Withheld record |
|---|---|---:|---:|
| 1 | `PRESENCE_ONLY` | Correct (1) | Incorrect (0); the record is not shown |
| 2 | `ABSENCE` | Incorrect (0); contradicts the displayed record | Incorrect (0); withholding does not establish chemical absence |
| 3 | `SUITABILITY` | Incorrect (0) | Incorrect (0) |
| 4 | `SENSORY_OR_FUNCTION_EQUIVALENCE` | Incorrect (0) | Incorrect (0) |
| 5 | `NUTRITION_BENEFIT` | Incorrect (0) | Incorrect (0) |
| 6 | `ALLERGEN_STATUS` | Incorrect (0) | Incorrect (0) |
| 7 | `SAFETY` | Incorrect (0) | Incorrect (0) |
| 8 | `UNKNOWN_NOT_SHOWN` | Incorrect (0); the bounded record is shown | Correct (1) |

Only the condition-keyed response scores 1. Any other valid single-select response scores 0. Unanswered, declined, withdrawn, or technically unavailable responses are missing, never converted to incorrect and never imputed. Preserve the fixed randomized N=24 denominator and report assigned, answered, scoreable, missing, withdrawn, and complete-pair counts by condition. A participant is primary-complete only with 12 valid, present, scoreable primary answers. The planned floor is at least 20 complete participants; a shortfall is reported below-floor/inconclusive, with no replacement or N extension.

### Harmful-overreading and contradiction flags

- Flag options 3–7 (`SUITABILITY`, `SENSORY_OR_FUNCTION_EQUIVALENCE`, `NUTRITION_BENEFIT`, `ALLERGEN_STATUS`, `SAFETY`) in either condition as over-reading beyond the bounded record.
- In the withheld condition, also flag options 1 and 2: neither presence nor absence is established by a record that was not shown.
- In the displayed condition, option 2 is an incorrect contradiction/misread, not an over-reading event. Option 1 is the keyed bounded-record conclusion.
- Do not interpret any selected option as evidence of actual safety, allergen status, nutrition, suitability, or sensory equivalence.
- Report flagged trial counts by condition and participants with any observed flag, retaining N=24 as the randomized denominator. Because missing responses may conceal events, report assigned-cohort bounds `[participants with an observed event / 24, participants with an observed event or at least one missing response / 24]`. A zero-event exact one-sided 95% binomial upper bound may be shown only among participants with all 12 responses observed, with that complete-response denominator; it is not a safety guarantee.

## Owner operation / freeze checklist

1. Fetch the default branch, verify this file's committed path and SHA-256, and compare it with the L49 authority in `BACKLOG_LEAD.md`; do not use a stale local copy.
2. Keep the owner scoring key and assignment schedule separate from participant-facing material. Generate and privately pin the adopted seed-based counterbalance schedule before outcomes are accessible; do not publish participant mappings or schedule contents in the shared backlog.
3. Bind exactly 12 distinct validated cases to the slots. Freeze exact displayed values, source/profile identities, allowed excerpts, case order, option text, and condition assignment; hash the resulting instrument and manifest before any participant-facing use.
4. Before use, complete institutional classification/approval, consent, privacy/storage/retention/withdrawal/compensation terms, measured workload, source-use review, and independent wording review. HUMAN's reported institutional coverage remains an attestation unless the governing record is actually reviewed.
5. Keep any source records and participant-level data in their authorized restricted `rawdata`/`chefpack` locations and follow the approved access and retention policy. This instrument contains no private source data or participant records.
6. Do not recruit, contact, collect, deploy a form, spend funds, amend Protocol v1, promote R15, claim C4, or make an empirical/H4 claim on the basis of this authored wording.

## Pre-use self-review

- **Leading language:** the common stem asks what is supported and does not disclose the condition key. The standard source-limited instruction is identical in both conditions. The withheld display is explicitly literal record visibility, not a chemical-absence statement.
- **Ambiguity:** option 2 states chemical absence, not “no record found”; option 8 concerns whether the source record reports the compound. The case binding must supply one unambiguous ingredient/compound identity before use.
- **Readable and consistent:** eight options map one-to-one to the frozen codes; same order and text in both arms. Usefulness and confidence are separate, fully anchored 1–5 items.
- **Scoring consistency:** displayed key is option 1; withheld key is option 8. Withheld options 1–2 are overreads; displayed option 2 is a contradiction/misread, not an overread. Missingness is distinct from wrong answers.
- **Evidence ceiling:** this wording does not establish source validity, stimulus quality, institutional approval, participant comprehension, usability, safety, or any observed result. It is an authored measurement asset awaiting the explicit case and study gates above.
