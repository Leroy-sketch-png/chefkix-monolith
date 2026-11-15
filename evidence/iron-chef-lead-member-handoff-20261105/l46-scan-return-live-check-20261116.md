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

## Same-session route, match, detail, and screenshot follow-up — 2026-11-16

- Using the same signed-in Brave `school` profile/tab and localhost FE process, verified the process's checkout as clean `ai/out/midterm-integrity-20261113/fe-integration` at `eab048b6fbed93513889890dc91fd60b3d32f75e`. No account or server was changed.
- At 1440×900, started on Dashboard and navigated through More → Kitchen Scanner; the active route opened `/scan`. The scanner’s “Search recipes with these ingredients” link successfully changed the fragment and scrolled the nested main content to the returned matches.
- Entered one temporary manual `garlic`. Seven recipes were shown under “Recipes with exact ingredient-name overlap.” The UI explicitly stated that it compares trimmed/case-folded names against only the first page (up to 100 account recipes), uses no aliases or semantic model, is not exhaustive, and is not HGAT. This is bounded fallback behavior only—not model ranking, search completeness, or quality evidence.
- Opened the first returned recipe (Carnitas), then browser Back. The browser returned to `/scan#ingredient-match-results`, retained garlic, and reran matching to the same seven cards.
- At 390×844 browser emulation, Dashboard → More showed Kitchen Scanner first under Cooking; selecting it closed the drawer and opened `/scan`. This is laptop emulation, not a physical-device test.
- Inspected one currently visible dashboard Recent Activity recipe attributed to the signed-in Test User (Arepas). Its detail had “Start Cooking,” not a distinct “Preview cooking” action. It was not activated; this one route does not establish whether a preview exists elsewhere.
- Returned to `/scan`, used Clear ingredients, confirmed both garlic and match results were absent, then restored Brave's default viewport. No camera/photo, upload, cooking session, timer, XP, rating, follow, shopping mutation, account switch, provider/model call, or research-source access occurred.
- Nine review screenshots are local-only, ignored artifacts in `D:/code/chefkix/ai/out/showcase/`. Their names, viewport, size and SHA-256 are listed below. They show signed-in preview UI and are intentionally not committed or uploaded; hashes and paths are retained here only for provenance.

| Local screenshot | Viewport | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `l46_scan_flow_dashboard_1440x900_20261116.jpg` | 1440×900 | 116031 | `407a1d735d8ccdafa6616655a0f30dea80c7ef7b7366268a1a92b49dfeb440f8` |
| `l46_scan_flow_more_menu_1440x900_20261116.jpg` | 1440×900 | 117657 | `2275c90742fa3ed7dc35db3a9badf95e4db481f9a91ac3a510e531628a34f3d5` |
| `l46_scan_flow_scanner_empty_1440x900_20261116.jpg` | 1440×900 | 129816 | `8dca64b900f433540e72ebd199b10aad767fa6f024cb18911e16060a00bd37f7` |
| `l46_scan_flow_garlic_matches_1440x900_20261116.jpg` | 1440×900 | 119904 | `03729773e1fff823ee0c3974286efeee7d9863ece673ebe3435608fb5f8ac9fe` |
| `l46_scan_flow_carnitas_detail_1440x900_20261116.jpg` | 1440×900 | 108094 | `e6226255e2937f75f39e0f631cc40ef9b6d28bc058a57f435bb9074bda51eae2` |
| `l46_scan_flow_dashboard_390x844_20261116.jpg` | 390×844 | 40266 | `1a23bbc2bb10b3d357f63a065e8ed5171787c07814d83ebf832d1c826e69a750` |
| `l46_scan_flow_more_menu_390x844_20261116.jpg` | 390×844 | 26987 | `a4b8381fa6abd565e13f91d1fd16f9fafc2427b13935dff3ca33e8093067e793` |
| `l46_scan_flow_garlic_matches_390x844_20261116.jpg` | 390×844 | 55542 | `36faa676d084ee6f97f10abf60d90c5ea07761ca122937936528c7d3bd24a6fe` |
| `l46_scan_flow_carnitas_detail_390x844_20261116.jpg` | 390×844 | 41485 | `9818b1897209dd758af7bb9852d58bb854fff7524b7c361cd650af9f5d0703be` |

No T56/demo, contribution, or milestone acceptance follows. The screenshots do not prove human findability, usability, accessibility, rehearsal, or end-to-end workflow latency.
