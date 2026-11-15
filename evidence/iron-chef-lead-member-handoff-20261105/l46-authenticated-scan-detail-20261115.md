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
