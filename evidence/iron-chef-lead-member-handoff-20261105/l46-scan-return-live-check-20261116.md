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

## Next

Keep this route-discoverability issue separate from the continuity correction. If addressed, inspect the canonical navigation/feature ownership and route entry criteria first; do not treat direct URL access as adequate user discovery.
