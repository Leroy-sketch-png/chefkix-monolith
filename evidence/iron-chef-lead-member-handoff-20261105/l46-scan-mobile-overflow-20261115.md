# L46 scan mobile-overflow correction — 2026-11-15

## Finding and correction

At a measured 390×844 browser viewport, the scan page's implicit one-column CSS grid used a content-sized track wider than its parent: the track began at x=32 and ended at x=395.375 (363.375 px), while the parent content width was 326 px. The page's visible text still ended within x=378, so the initial issue was a narrow background/panel overflow rather than text clipping. The scan page now declares a single mobile column and constrains both sibling cards with `min-width: 0`. Manual ingredient chips and scan/matcher error text also wrap unbroken long values without widening their parents.

The broader scan-feature sweep found the outer scan grid was the only implicit grid in the feature. `PhotoRecipeMatchCard` uses a two-column grid only at `sm` and above; no change was needed there. The sibling camera card and tips card received the same min-width constraint. A maximum-length 120-character unbroken ingredient regression verifies the entry remains representable and removable.

## Verification

- FE commit: `e45744021cca14b881e31ea9fac93c242a088d0b` (`fix(scan): contain narrow-screen panel content`), pushed directly to `chefkix-fe` `origin/main`; after a fresh fetch, local commit and `origin/main` matched exactly.
- Mobile browser emulation: 390×844; document width 390 px; grid x=32..358 (326 px), one 326 px track; both cards measured 326 px with `min-width: 0`. The 120-character field remained inside the panel; its own input scroll width was 818 px, a normal horizontally scrollable text-input behavior, while document width remained 390 px.
- Desktop browser emulation: 1440 px; document width 1440 px; grid x=208..1112 (904 px), tracks 528/352 px; both cards remained within the grid.
- Automated checks: scan suite 6/6 suites and 14/14 tests; `npm run lint`, `npm run typecheck`, scoped Prettier check, and `git diff --check` passed.
- A live screenshot showed the fixed mobile navigation. It also showed the separate floating “scroll to top” control overlapping the Join button; that shared-navigation issue was not modified in this scan-panel correction and remains an observed follow-up. No screenshot file was retained.

## Evidence ceiling

This is local browser-emulation and code/test evidence only, not a physical-device, accessibility, usability, matcher-success, product-value, rehearsal, T56 acceptance, or C1–C5 contribution result. The browser only filled (and later cleared) the manual ingredient field; it did not submit it or trigger any API/network request. No camera, upload, sign-in, cooking session, provider/model, API, GPU, or research-data operation occurred. No claim or milestone status changes.
