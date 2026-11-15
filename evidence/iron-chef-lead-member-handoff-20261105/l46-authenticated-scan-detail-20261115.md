# L46 authenticated scan-to-detail runtime receipt — 2026-11-15

## Scope

One local, human-paced browser walkthrough on `http://localhost:3001` after the HUMAN completed Quick Login and confirmed the dashboard. Served FE checkout: `chefkix-fe` isolated integration worktree, clean at `75dc74c7968a2fd34ab8b4832248450d3742f0eb`. This receipt records only visible UI results; no screenshot was retained.

## Observations

- Signed-in dashboard displayed the Test User profile. Explore rendered 12 recipe cards.
- A Khao Soi detail page rendered its recipe metadata and four cooking steps.
- On `/scan`, one manually entered `tofu` query returned 0 exact-name-overlap cards.
- One manually entered `garlic` query returned 7 cards and 0 withheld recipes. The UI said matching was trim/case-fold exact ingredient-name overlap against only the first page (up to 100 recipes), with no aliases or semantic model; the result is non-exhaustive and is not HGAT.
- Activating the “Search recipes with these ingredients” link moved the page to the actual match cards. Opening Garlic Fried Rice rendered its detail, five ingredients and three instruction steps.
- Content consistency issue observed on Garlic Fried Rice: displayed duration metadata is 15 minutes; description says 10 minutes; visible step timers are 3 + 5 + 2 = 10 minutes. This is a data/content discrepancy, not a user-study finding.

## Boundaries

No cook session was started; no timer, XP, rating, follow, shopping-list action, upload, camera, AI/provider call or other account-state mutation was triggered. Manual ingredient entry and the displayed recipe GET/fallback path are the only scan interactions recorded. No source rows, labels or research cases were accessed. No physical-device, 390px viewport, accessibility, human-usability, rehearsal, workflow-latency, T56 acceptance, contribution acceptance or milestone claim follows.

## Reproduction context and interpretation

The served checkout is identified by its clean Git commit above; the local browser rendered the route and UI disclosures. `tofu` and `garlic` were entered separately. No request/response body, raw account payload or screenshot was saved. The seven-card result is evidence that this bounded fallback rendered live recipe cards for this account and this query only; it is not evidence of HGAT/graph matching, exhaustive coverage, recommendation quality or user value.

## Next

Keep the visible-duration discrepancy available for a focused content-data correction. T56 still requires its accepted T51–T55 inputs, integrated workflow, measured end-to-end performance, actual presenter/listener rehearsal and rubric, and an actual-flow backup video.

## Supplemental mobile observation — 2026-11-16

The HUMAN-started local frontend was viewed in browser responsive emulation set to 390×844 on the laptop; this is not physical-device evidence. On `/scan`, manually entering `garlic` yielded 7 first-page exact-name-overlap recipe cards and 0 withheld after the matcher endpoint returned HTTP 503 and the existing bounded recipe-list fallback ran. The UI disclosed the bounded method (up to 100 account recipes; trim/case-fold exact-name overlap; no aliases or semantic model; non-exhaustive; not HGAT). No request/response bodies or account identifiers were retained.

The first rendered result card clipped its title/count badge at this width. FE correction `PhotoRecipeMatchCard.tsx` stacks title and badge on narrow screens, restores a horizontal layout at `sm+`, and wraps long text. A focused regression test plus lint, typecheck, Prettier and diff checks passed; the matching FE commit is recorded in the shared Lead entry. The card and Garlic Fried Rice detail rendered after the correction. Browser Back returned to the scan hash but cleared the manually entered garlic/results. This is a recovery caveat only; persistence was not added. The owner-only cooking preview was unavailable on this non-owned recipe and was not tested.

No screenshot was retained. No cooking session, timer, XP, rating, follow, shopping-list mutation, upload, camera, provider/model call or research-source access occurred. The Computer Use helper then refused further browser control because it could not determine the current URL with sufficient confidence; no alternate UI-control path was attempted. Evidence ceiling remains one local emulated walkthrough plus code/test evidence, not a physical-device/accessibility/usability check, rehearsal, p95, T56/C5 acceptance or milestone credit.

## Scan-return continuity implementation — 2026-11-16

FE commit `c25a9a8314417f13767aba6a0fd6f0d27589b30d` is published on `origin/main`, fetch-verified at 0/0. It retains only confirmed scan-name strings and manually entered name strings in owner-scoped in-memory state across client route unmount/remount. It does not store image bytes, detector results, or recipe payloads. It preserves source categories, rebuilds the current matching request on return rather than reusing saved results, keeps the existing cancellation behavior, validates names (maximum 120 characters; empty/control-character strings rejected), and bounds the deduplicated selection to 30. Clear/remove, logout, and account-change paths clear the selection; scanner mount no longer emits a false empty update, while explicit new-image/reset and parent removal/clear remain synchronized.

Verification: focused workspace/scanner/store/auth tests 12/12; all scan-component suites 6/6 (15/15); lint, typecheck, Prettier and `git diff --check` passed. This is implementation/test evidence only. The after-fix live detail→Back / remove / clear path is not yet observed. The prior browser-control helper refusal remains the boundary; no alternate UI automation or post-fix browser action was used. Therefore no live recovery proof or T56 acceptance is claimed.
