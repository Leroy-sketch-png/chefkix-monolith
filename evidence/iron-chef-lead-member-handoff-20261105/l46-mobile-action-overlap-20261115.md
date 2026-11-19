# L46 mobile action overlap correction — 2026-11-15

## Trigger and correction

The scan route's fixed scroll-to-top control overlapped the mobile bottom-navigation Join action. The earlier panel-overflow correction had already recorded this as a separate issue; the Join link still received the browser hit test, but its visible CTA was partly covered. In the isolated FE worktree, the floating control now clears the mobile nav by using the nav-height + spacing + safe-area offset. The existing desktop offset remains `bottom-6`. A focused component test locks both responsive classes.

## AoE rinse

- **Pass 1 — epicenter:** corrected the shared `ScrollToTop` offset used by the global UI and added a regression check.
- **Pass 2 — local blast radius:** reviewed `MobileBottomNav` dimensions/safe-area padding and adjacent fixed actions (`QuickPostFAB`, `MiniCookingBar`, cookie notice). Their current bottom offsets already clear the nav; no sibling changes were warranted.
- **Pass 3 — systemic sweep:** searched the frontend for the shared scroll-to-top consumer and relevant fixed bottom offsets. The global enhancement is the sole component consumer; no second copy of this same overlap pattern was found. Existing local overlay stacking remains unchanged.

## Evidence

- Browser emulation at **390×844**: viewport/document width 390px; FAB bounds x=326..366, y=732..772; nav top y=778; 6px gap, zero rectangle overlap; Join center hit-test resolves to its anchor.
- Browser emulation at **390×667**: viewport/document width 390px; FAB y=555..595; nav top y=601; 6px gap, zero overlap; Join center hit-test resolves to its anchor.
- Browser emulation at **1440×900**: mobile nav hidden; FAB keeps 24px desktop bottom offset; document width 1440px.
- Focused Jest: scroll-to-top + mobile-navigation suites **2/2**, **7 tests**. `npm run lint`, `npm run typecheck`, scoped Prettier check and `git diff --check` pass.
- FE commit [`7d10b4c`](https://github.com/Leroy-sketch-png/chefkix-fe/commit/7d10b4cc07de35e0379d171bbd7165b4a8c6c112) was pushed to `origin/main`; a fresh post-push fetch verified `HEAD == origin/main` and 0/0 divergence. Only `scroll-to-top.tsx` and its focused test were included.

## Boundary and remaining work

This is localhost browser-emulation plus code/test evidence. A screenshot was inspected live but not retained; no physical-device or screen-reader check, user-comprehension/usability observation, matcher result, integrated showcase, rehearsal, T56 completion, contribution acceptance or milestone credit is claimed. The Join link was not activated. No sign-up, API/network call, account/session mutation, camera, upload, cooking session, model/provider call, private-data access or API/GPU budget use occurred. Related implementation details and the no-credit boundary are recorded in the root T56 task card and mission continuation 414.
