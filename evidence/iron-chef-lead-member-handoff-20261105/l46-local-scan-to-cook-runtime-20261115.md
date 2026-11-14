# L46 local scan-to-cook runtime receipt — 2026-11-15

## Scope and boundary

This is a bounded local browser observation of the existing authenticated frontend and local monolith. It is evidence for one manual-ingredient → stored-overlap → recipe-detail → CookingPlayer path only. It is not an accepted study-graph demo, HGAT result, exhaustive search, safety result, independent quality result, timed defense rehearsal, or C1–C5 acceptance.

## Runtime observed

- Frontend: isolated integration worktree `ai/out/midterm-integrity-20261113/fe-integration`, commit `0a5dbef0da030864a153ee6c4e7938e7bd6cc298`, local port 3001. Local monolith API was already serving on port 8080. No tracked frontend source was changed.
- Authentication: used the existing development Quick Login flow for an existing test persona; the app transitioned to its signed-in dashboard before the scan. No account was created/reset, no credential was typed/copied, and no token was read or exposed. Optional microphone access was denied; cooking continued in quiet mode.
- Ingredient matching: entered one manual ingredient. The panel rendered **7 exact-name-overlap matches** from the first bounded stored recipe page and **0 withheld** recipes. UI disclosure says this list uses exact trimmed/case-folded names, is limited to the first page (up to 100 recipes), is not exhaustive, and is not HGAT. These counts describe this one local run only.
- Recipe and session: opened one returned card's detail, then used its normal Start Cooking path. CookingPlayer rendered **step 1 of 3**, which the existing client reaches after a successful session-start response and successful recipe load. The raw response body, recipe title/ID, session ID, user details and recipe data are deliberately not retained here.
- Timer: a 10:00 step timer visibly ran to 09:58. The follow-up button is the existing toggle's skip/remove action, not a pause operation; after it, the timer was inactive and displayed its initial 10:00 state. No cooking step was advanced or marked complete. The local session remains intentionally unfinished at step 1.

## Recovery and limits

An earlier signed-in pass left the scan page too soon via its separate general-search link, so it did not establish a visible match-panel result. This receipt relies on the subsequent pass that stayed on the scan page and observed the panel. No external service, paid API, network dataset, or source material was used. The initial guest 401 remains a historical anonymous-session boundary, not a current matcher failure.

The observed UI state is consistent with the source flow: `startCooking` stores the returned session only after the backend start and recipe fetch succeed. The rendered step/timer proves this client path reached CookingPlayer; this receipt does not preserve an independent HTTP capture of the session response. Do not infer a product-value, usability, safety or model-quality result from this technical check.

## Result

The previously blocked signed-in scan-to-cook runtime boundary is now demonstrated narrowly: one entered ingredient → bounded local overlap card list → one result detail → active session/player → timer start and removal. Remaining L46 acceptance includes the accepted study-graph/manifest route, served-hash proof, actual timed presenter/listener rehearsal and its existing rubric. No score, milestone, protocol or scientific claim changed.

## Mobile-width recheck (2026-11-15; browser emulation only)

- At a Chromium responsive viewport of **390 × 844 CSS pixels**, the manual ingredient field, Add ingredient button, resulting ingredient chip, chip-removal control, search action and fixed bottom navigation all fit and were operable. Adding then removing the one test ingredient returned the form to its empty state. This is browser emulation, not a physical-device, performance or accessibility certification.
- Activating “Search recipes with these ingredients” navigated to the separate general search page. That page displayed 52 total search results (9 recipe results); this is **not** evidence that the ingredient-matching endpoint succeeded. The separate `/api/photo-intelligence/ingredient-recipes` request returned **HTTP 503** during this mobile pass. Do not substitute general search results for the matcher or count this pass as a successful ingredient-recommendation run.
- This recheck created no account, uploaded no photo, changed no saved recipe, and completed no cooking step. It did not modify frontend code. The existing unfinished cooking session was left untouched.
