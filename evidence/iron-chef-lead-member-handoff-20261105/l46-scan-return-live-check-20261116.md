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
| `l46_scan_flow_garlic_matches_1440x900_20261116.jpg` | 1440×900 | 139130 | `f6928197851698220640cff1e884c7d18756fdf12f2ac28c15c1b38efb2ba3b3` |
| `l46_scan_flow_carnitas_detail_1440x900_20261116.jpg` | 1440×900 | 108094 | `e6226255e2937f75f39e0f631cc40ef9b6d28bc058a57f435bb9074bda51eae2` |
| `l46_scan_flow_dashboard_390x844_20261116.jpg` | 390×844 | 40266 | `1a23bbc2bb10b3d357f63a065e8ed5171787c07814d83ebf832d1c826e69a750` |
| `l46_scan_flow_more_menu_390x844_20261116.jpg` | 390×844 | 26987 | `a4b8381fa6abd565e13f91d1fd16f9fafc2427b13935dff3ca33e8093067e793` |
| `l46_scan_flow_garlic_matches_390x844_20261116.jpg` | 390×844 | 52220 | `8a8d7f18395a850d936994c04495e29f325e939647d52efb6e17f9434db3cd84` |
| `l46_scan_flow_carnitas_detail_390x844_20261116.jpg` | 390×844 | 41485 | `9818b1897209dd758af7bb9852d58bb854fff7524b7c361cd650af9f5d0703be` |

No T56/demo, contribution, or milestone acceptance follows. The screenshots do not prove human findability, usability, accessibility, rehearsal, or end-to-end workflow latency.

## Desktop results-context correction — 2026-11-16

- The 1440×900 results screenshot showed a large blank neighboring column: the fixed-height “Make the scan count” guidance had scrolled out of view while the long left match list continued. This was a concrete context-loss layout issue, not a matching/identity defect.
- FE commit `8a5d23900cbcf117677cf4988459f91c3586506c` adds a desktop-only sticky/self-start treatment to the existing guidance aside. On the same live `/scan` state, the aside computed as `position: sticky` with top 120px at the anchored results scroll, keeping the three instructions and safety disclaimer visible beside the matches. It does not change scoring, matching, state, navigation, or protocol.
- At 390×844 after the correction, the aside remained static and document width stayed 390px; the current mobile result screenshot was recaptured. Temporary garlic was cleared and default Brave viewport restored.
- Verification on the FE integration checkout: `npm run lint`, `npm run typecheck`, and `npx jest src/components/scan/__tests__/ScanIngredientWorkspace.test.tsx --runInBand` (3/3) passed; Prettier and scoped diff check pass. FE `origin/main` was fetched before direct push and fetched afterward at 0/0 divergence. This is a narrow layout/runtime correction, not usability, physical-device, accessibility, T56/demo, or milestone acceptance.

## CookingPlayer preview access gate — 2026-11-16

- In the same signed-in Brave school-profile session (Test User), opened the first visible recipe activity card from Dashboard. The detail page was under the generic route shape `/recipes/<recipe-id>`; the exact identifier is omitted.
- **Correction to the earlier task-note attribution:** the live detail page names another profile as author, not Test User. The page exposes `Start Cooking` but no owner-only `Manage` menu or `Preview cooking` item. The earlier Test User ownership wording was an inference from the dashboard activity card and was wrong.
- No control was activated. No real cook session, timer, XP, rating, API mutation, account switch, or recipe creation occurred. The dashboard showed no active player/continue-cook card in its accessible content, but this does not prove persisted session state absent. Preview entry, player keyboard/focus, ingredient/help dialogs, and preview exit/restore remain unobserved.
- The normal signed-in Profile → Recipes view showed 0 recipes created and the empty state “You haven't created any recipes yet.” No owner recipe was available to inspect; no search, pagination, creation, or account switch occurred. Therefore the owner-only preview path remains untested.
- Source-free safeguard verification at FE commit `8a5d23900cbcf117677cf4988459f91c3586506c`: the existing `cookingSessionRestoration`, `cookingUiContinuity`, and `cooking-player-keyboard` suites pass (3 suites / 41 tests). They cover preview preservation/restoration and keyboard ownership for controls/dialogs in unit tests only; they do not demonstrate a live preview or authorize replacing the missing recipe.
- Local-only screenshot `ai/out/showcase/l46_cooking_preview_ownership_gate_1440x900_20261116.jpg`: 156,680 bytes, SHA-256 `11c9ad169a27c23ae824aebc8533740cb607cbb024a58b43318b44507a5719e8`. It is ignored under `ai/out/`; do not stage or publish it.
- Next safe runtime prerequisite: a future existing recipe detail owned by Test User. Do not create a recipe, switch accounts, or use `Start Cooking` to reach preview. Other existing demo work remains eligible. This is not T56, human-usability, demo, contribution, or milestone acceptance.

## Food Feed → linked recipe → Back continuity — 2026-11-16

- In the same signed-in local Brave session, opened the existing Feed and followed the first visible post's actual embedded recipe link to its detail, then used browser Back. The recipe detail rendered; Back returned to Feed and restored the nested main-content position near that linked post. At 390×844 emulation the post link was at y≈445 before navigation and y≈436 after return; the raw scroll offset varied with Feed content/layout. The same route opened and returned at 1440×900. These are laptop-browser observations, not physical-device or human-usability results.
- The concrete defect was AppShell's nested scroll container resetting when the Feed page was replaced. FE commit [`d023ad6`](https://github.com/Leroy-sketch-png/chefkix-fe/commit/d023ad657724fc3bc05b02bc2ca95d5388086ec8) records scroll at Feed recipe-link activation and restores it after Feed content is ready; unrelated routes clear a stale pending position. FE `origin/main` was fetched before direct push and fetched afterward at 0/0 divergence.
- Verification on that FE commit: `src/lib/__tests__/app-scroll-authority.test.ts` 5/5, `npm run lint`, `npm run typecheck`, and scoped Prettier all pass. No other FE paths changed in this slice.
- Local-only ignored screenshots under `D:/code/chefkix/ai/out/showcase/`:

| Capture | Viewport | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `l46-food-feed-desktop-20261116.jpg` | 1440×900 | 84357 | `bed53c978d20e8b80ce62a091eb54cac1c14a113f6a2f49cc937a8eff18629e6` |
| `l46-food-feed-desktop-post-20261116.jpg` | 1920×941 | 181845 | `663eb457a057d195d588925818b63d6f5c6df290cada80806ba3cf45ce62480b` |
| `l46-linked-recipe-detail-desktop-20261116.jpg` | 1920×941 | 215785 | `4d15cdf40a718c6752909fdc1eefadbdd16205875a7fe172bd64316bf8d69920` |
| `l46-food-feed-mobile-20261116.jpg` | 390×844 | 54410 | `86c171271d1d3ef71ed0d4e48150b6069eba7b8859ec11f8b2502cc4596e63d7` |
| `l46-linked-recipe-detail-mobile-20261116.jpg` | 390×844 | 55502 | `417b0a10844d50e82d8cd83a554034da0f809b4a118e55583857246e009fe56f` |

No like, comment, share, save, follow, cooking, timer, XP, profile, account, provider/model, or source-data action occurred. The Feed→recipe route is demonstrably available and Back context is repaired in the current local build; this does not establish that a first-time human will discover the recipe link. No T56/demo, accessibility, human-usability, contribution, or milestone acceptance follows.

## Delayed-load reliability correction — 2026-11-16

- Review found the first restoration implementation expired after 120 animation frames, even if Feed data was still loading. FE follow-up removes that arbitrary deadline and frame polling: the saved position remains pending until the Feed explicitly reports ready; wheel/touch/pointer/keyboard intent cancels the restore, and unrelated navigation clears it.
- Regression coverage now checks 200 readiness checks while Feed is unready, then readiness-based restore, user-scroll cancellation, and unrelated-route cleanup. Focused scroll-authority suite: 7/7; `npm run lint`, `npm run typecheck`, Prettier and scoped `git diff --check` pass.
- FE correction commit [`061ebd4`](https://github.com/Leroy-sketch-png/chefkix-fe/commit/061ebd43a8dcd4b11f9543455e65e5805a742a2a) was pushed directly to `origin/main` after fetching the unchanged baseline; a post-push fetch verified `HEAD == origin/main` and 0/0 divergence.
- After the correction, local Brave browser checks returned from a linked Feed recipe to the same lower Feed card at desktop and 400×910 responsive emulation. This remains laptop-browser evidence, not physical-device or human-usability evidence. The narrow preview also showed the desktop navigation/sidebar consuming substantial width beside the Feed; record that as a separate responsive-shell concern, not part of this return-continuity change.
- No product-state mutation, engagement, cooking, timer, XP, account switch, provider/model call, or research-data access occurred. This correction adds no T56/demo, contribution, or milestone acceptance.
