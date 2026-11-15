# L46 scan-return live check — 2026-11-16

## Scope

One local Brave browser check of the already-running ChefKix frontend at `http://localhost:3001`. The HUMAN opened `/scan`; no account was changed. This checks the published FE scan-draft continuity change from commit `c25a9a8314417f13767aba6a0fd6f0d27589b30d` only. No screenshot was retained.

## Observations

- On `/scan`, one manually entered `garlic` produced 7 exact-name-overlap recipe cards. This is the existing bounded fallback presentation, not HGAT, exhaustive search, or model-quality evidence.
- Opened the first matched recipe (Carnitas), then used browser Back. The browser returned to `/scan`; the manually entered Garlic selection remained. Matching briefly showed its loading state, then rendered 7 exact-name-overlap cards again.
- Activated “Remove garlic”; the selection disappeared.
- Added a temporary manual `tofu`, then activated “Clear ingredients”; the selection and matcher list were absent afterward. The scan was left empty.
- Expanded the visible “More” sidebar menu. It showed Kitchen Copilot, Cook Together, Pantry, Meal Plan, and Shopping, but no Scan/Kitchen Scanner item. The inspected page had been opened directly at `/scan`; this check did not audit every possible route, device, or navigation surface.

## Boundaries

No photo, camera capture, upload, cooking action, timer, XP, rating, follow, shopping-list mutation, account switch, provider/model call, or research-source access occurred. Inputs were temporary and the scanner was left empty. No screenshot, raw request/response payload, or account identifier was retained.

This is narrow live UI evidence for selection persistence, re-query after route return, individual removal, and Clear behavior. It does not establish physical-device or assistive-technology behavior, human usability, product value, rehearsal, end-to-end p95, T56 acceptance, contribution acceptance, or milestone credit. The absent scanner entry in the one inspected “More” menu is an actionable navigation-discoverability observation, not a complete navigation audit.

## Discoverability follow-up — 2026-11-16

- FE commit `773ffd4765d416b4097d99d0d6b492be8c90eac5` adds the existing public Kitchen Scanner to desktop and mobile “More” navigation, makes it the first mobile Cooking item, and exposes a guest-only More menu containing only this public tool. Follow-up FE commit `eab048b6fbed93513889890dc91fd60b3d32f75e` lets the desktop sidebar label wrap to two lines rather than truncate to “Kitchen Sc…”. Both are published directly to FE `origin/main`; the latest post-push fetch verified 0/0 divergence.
- Live desktop check in Brave: from Dashboard, opened More → Kitchen Scanner; the app navigated to `/scan` and marked Kitchen Scanner active in the expanded sidebar.
- After the label-wrap correction, a fresh desktop capture showed the full “Kitchen Scanner” label in the narrow sidebar.
- Live mobile-emulation check at 390×844: opened More; the Cooking section appeared first and Kitchen Scanner was fully visible in the first drawer view. Selecting it closed the drawer and navigated to `/scan`. This is laptop browser emulation, not physical-device evidence.
- Focused navigation tests: 3 suites / 10 tests pass. `npm run lint`, `npm run typecheck`, focused ESLint, Prettier checks, and `git diff --check` pass. Screenshots were inspected live but not retained; no account change, camera, upload, ingredient entry, provider/model call, or external request occurred in this navigation check.

## Next

The route is now reachable through the existing navigation without URL typing. Keep L46/T56 acceptance open: this narrow route check is not evidence of human findability, physical-device/accessibility behavior, product value, rehearsal, end-to-end p95, or milestone credit. Continue from the remaining canonical T56 prerequisites and acceptance gaps.
