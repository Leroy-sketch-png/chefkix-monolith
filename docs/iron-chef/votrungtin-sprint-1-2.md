# VoTrungTin — Sprint 1 and Sprint 2 member deliverables

**Checkpoint:** 2026-10-06
**Purpose:** Turn the two sprint reports into an actionable member-owned SRS, use-case and data-flow baseline, with acceptance criteria mapped to both team backlogs.

## Source and scope

The source reports are output/kltn_sprints/PhanPhuTho_VoTrungTin_IRON_CHEF_Sprint1.docx and output/kltn_sprints/PhanPhuTho_VoTrungTin_IRON_CHEF_Sprint2.docx. They match the copies supplied from Downloads byte-for-byte (SHA-256: Sprint 1 E715F690F95E0860204B83A37A0297CB724459AD8396FAD40C70EB7863F7E078; Sprint 2 E5EAD3FB4B11816261EA5195CB76EA3086A841914008E6B0E252E35972271E78).

These reports define planned work and role assignments; they do not prove completion. Sprint 2 runs through October 10, 2026, so its planned work is assessed as active at this October 6 checkpoint. Sprint 2's section 7 describes Sprint 3 work and is not counted as a Sprint 2 completion requirement.

The user request is to complete Võ Trung Tín's member-owned work and keep it aligned with the team backlogs. It does not reassign Phan Phú Thọ's Lead work. The reports assign Tín frontend intelligence surfaces, camera, dashboard, allergen profile and evidence UI in Sprint 1; Sprint 2 explicitly assigns Tín Use Cases, Data Flow, UI requirements for the SRS and Member scaffold acceptance. Dataset survey/matrix, canonical vocabulary, evaluation protocol and AI API requirements are Lead-owned in the Sprint 2 role table.

The alignment authority is the current [Lead backlog](../../BACKLOG_LEAD.md) (v102, updated 2026-10-06) together with the [Member backlog](../../BACKLOG_MEMBER.md). The member backlog points to the canonical strategy and atomic evidence ledger, which are not present in the checked-out repositories. This is an engineering trace against committed source and code, not an independent validation of scientific claims.

## Assignment trace

| Sprint report item | Owner in the report | Member disposition at this checkpoint |
| --- | --- | --- |
| Sprint 1 §3–4: inventory frontend intelligence, camera, dashboard, allergen and evidence surfaces | Tín member track | Scaffold and integration work is present across the FE, monolith and AI service branches. Local code presence is not model, deployment, safety or user evidence. Acceptance is traced below and to Member Epics 1–11. |
| Sprint 1 1.1: review capstone baseline and AI integration points | Shared planning; Tín's contribution is systems/frontend inventory | Existing ChefKix routes and services are reused. The Lead's model and research baseline remains owned by the Lead. |
| Sprint 1 1.2: research questions and novelty | Research/Lead direction, with member alignment | Member surfaces expose the repeated/unseen, safety and explanation evidence without claiming results. Scientific protocol and findings remain Lead-owned. |
| Sprint 1 1.3: split Lead and Member backlogs | Shared | BACKLOG_MEMBER.md now links this trace and is synchronized to Lead v102. |
| Sprint 1 1.4: Gantt and evidence plan | Team deliverable; not individually assigned to Tín | Preserve as a team planning item. This document supplies member acceptance/evidence links; it does not claim the team Gantt is complete. |
| Sprint 2 2.1: dataset matrix, provenance, schema and use scope | Phan Phú Thọ (Lead) | Out of Tín's ownership. The member UI must display Lead-provided source and claim limits when available; it must not invent the matrix. |
| Sprint 2 2.2: SRS for Creator, User and research journeys | Võ Trung Tín (Member) | Specified in the SRS and use cases below, reusing existing ChefKix creator/community flows and defining IRON CHEF-specific UI behavior. |
| Sprint 2 2.3: Use Cases and Data Flow for AI pipeline and ChefKix | Võ Trung Tín (Member) | Specified below, with production and unavailable-service branches called out. |
| Sprint 2 2.4: API contract and safety/explanation acceptance | Shared at the boundary; Tín owns Member scaffold acceptance, Lead owns AI API requirements | UI-facing contracts and Member acceptance are specified below. Existing adapters are not treated as proof that the Lead has delivered a production model or canonical data. |
| Sprint 2 §7: Figma and scaffolds scheduled for Sprint 3 | Sprint 3 plan | Future-plan text, not a missed Sprint 2 deliverable. Current code already contains graph, evaluation, allergen and camera scaffolds; remaining dependencies are listed below. |

## UI system requirements

### Functional requirements

| ID | Requirement | Member backlog mapping |
| --- | --- | --- |
| UI-01 | A creator can draft, edit and publish a recipe using ChefKix's existing creator workflow. Recipe ingredients and quantities must remain editable and must be the context supplied to an ingredient substitution request. | Existing ChefKix baseline; integration point for Epic 5 and Epic 7. |
| UI-02 | A user can enter ingredients manually or scan a photo. A scan shows permission, loading, error, unavailable and demo states distinctly. The user can review/edit detections before they are used. A demo result is visibly labeled and never presented as live model output. | Epic 4, Epic 8 / G8. |
| UI-03 | A user can request ingredient substitutions with recipe context, reason, dietary tags and the saved allergen profile. The card distinguishes candidate identity, quantity ratio, confidence, source and unavailable fields. A quantity ratio is not confidence. | Epic 5, Epic 6 / G3, G4. |
| UI-04 | The profile editor supports the configured standard allergen groups and custom terms, persists changes to the authenticated profile/settings service and reflects the saved values in later requests. | Epic 3 / G4. |
| UI-05 | Allergy status is three-valued: Safe only when backed by the supplied reviewed policy, Check when unknown, missing, malformed or conflicting, and Blocked when the policy says the candidate conflicts. A missing name match is never evidence of safety. A blocked primary action cannot be started. | Epic 6 / G4. |
| UI-06 | A compound explanation identifies its source and grounding status. Presence/overlap is described as chemical co-occurrence only; it does not assert sensory equivalence, cooking suitability, melting point, nutritional equivalence or safety. Missing measurements remain unavailable rather than zero. | Epic 5 / G3. |
| UI-07 | The graph explorer searches and loads bounded neighborhoods, reports partial coverage and renders only returned nodes and edges. A local mock is opt-in and labeled. | Epic 1, Epic 10 / G5. |
| UI-08 | The protected evaluation view separates completed, pending and rejected results, hides pending numeric values, preserves negative findings and shows the supplied provenance and claim limits on charts and exports. Different protocols are not shown as a matched winner comparison. | Epic 2, Epic 9 / G1, G6. |
| UI-09 | A user can submit substitution choice and post-cooking feedback through the existing session workflow. Receipt-bearing candidates are the boundary for feedback ingestion; the UI reports retry/failure without claiming that feedback improved a model. | Epic 7 / G7. |
| UI-10 | Existing community, recipe interaction and gamification features remain available in the User and Creator journeys. XP, streaks, likes or comments are product interactions, not research outcome labels and not model-quality evidence. | Existing ChefKix baseline; preserve the separation required by Epic 7 and Epic 9. |
| UI-11 | Thesis evidence views identify the chapter, artifact, source route and readiness state. Pending captures remain pending until the referenced source exists and is reviewed. | Epic 11 / G2, G6, G9. |

### Quality requirements

| ID | Requirement | Acceptance |
| --- | --- | --- |
| UI-NQ-01 Safety | No UI state or copy may infer allergen safety from absence of a detected allergen, a model confidence score, compound overlap, a plant/animal name heuristic or an unreviewed taxonomy alias. | Missing or invalid policy yields Check; conflicts yield Blocked; regression evidence covers all three states. |
| UI-NQ-02 Evidence | Every displayed research metric or scientific explanation retains its source, status and applicable limitations. A supplied hash is metadata until the corresponding artifact is independently checked. | Pending values stay hidden; exports retain captions and supplied provenance; no fabricated score, seed, hash, review or outcome. |
| UI-NQ-03 Degraded operation | Unconfigured or failed AI/model/data services produce an explicit waiting/error state and a usable manual path where possible. Production mode does not silently switch to mock data. | Missing scan or recipe-match service returns an integration-pending response; explicit demo configuration is the only mock path. |
| UI-NQ-04 Privacy and access | Profile changes, protected evaluation views and photo uploads use the existing authenticated application boundaries. Do not log or export private profile values or raw images as public evidence. | Review auth and upload lifecycle in deployed acceptance; this repository checkpoint does not certify production deployment. |
| UI-NQ-05 Accessibility | Forms and safety states have labels, keyboard operation, visible focus and text/icon cues in addition to color. | Keyboard/mobile and assistive-technology acceptance evidence is still needed where the backlog marks it open. |
| UI-NQ-06 Bounded work | Graph requests and rendered neighborhoods remain bounded; photo and AI requests expose cancellation/timeouts and actionable failures before production acceptance. | Graph API is bounded. Photo route-level caller auth, upload caps, cancellation and a 30-second upstream timeout are implemented; provider credentialing and real-device acceptance remain open. |

## Use cases

### UC-01 — Creator authors a recipe

1. The Creator opens the existing recipe editor, enters title, ingredients, quantities, instructions and media, and saves a draft.
2. The application validates the ordinary recipe fields and preserves the entered ingredient context.
3. The Creator previews and publishes through the existing ChefKix workflow.
4. Community interaction and XP follow the existing product rules; they are not entered as benchmark or safety evidence.

**Acceptance:** A saved/published recipe retains its ingredient list and quantities; existing authorization and validation errors remain visible; editing the recipe does not silently overwrite a user's allergen profile.

### UC-02 — User scans or enters ingredients

1. The User chooses camera upload or manual entry. The browser requests camera permission only after an explicit user action.
2. A selected image is checked for presence, image type and nonzero size, then sent to the same-origin detector adapter.
3. A live response is displayed with source/confidence and editable detections. An unconfigured detector displays an integration-pending state; an explicitly configured demo is labeled mock.
4. The User confirms or corrects ingredient names before recipe matching or substitution starts.
5. If camera use is denied or unavailable, the User can proceed through manual input.

**Acceptance:** No image failure produces invented live detections; the request does not proceed with unreviewed or empty input; the source label follows the actual adapter response.

### UC-03 — User requests a context-aware substitution

1. The User opens a recipe ingredient and selects a reason or enters a text ingredient.
2. The application combines the ingredient and recipe context with dietary tags and the persisted allergen profile.
3. The AI service returns candidate substitutions and any safety/evidence fields it can support.
4. The UI applies local profile conflicts and the returned policy conservatively. It renders Safe, Check or Blocked and suppresses unsupported compound/nutrition values.
5. The User can inspect sources and limitations, choose an allowed candidate, or decline all options.
6. The choice and eventual cooking feedback are sent through the receipted session workflow when available.

**Acceptance:** Empty/malformed policy or uncertain candidate status yields Check; a conflict blocks the primary selection; an unavailable service does not offer a fabricated substitute; receipt and retry behavior stays observable.

### UC-04 — User inspects a graph or compound explanation

1. The User searches for an ingredient or opens a returned candidate's graph context.
2. The client requests a bounded graph result and renders the returned coverage.
3. If official compound evidence exists, the client can request profile/pair details and display source, grounding and measurement semantics.
4. If the official index, nutrition source, technique evidence or full graph is absent, the UI shows unavailable/partial rather than zero or an inferred claim.

**Acceptance:** Graph depth/limit stay within the API range; unrelated whole-graph loading is not triggered; substitution ratios, overlap values and compound presence are labeled as different things.

### UC-05 — User provides feedback and uses community features

1. The User accepts, rejects or skips a candidate and may enter a post-session outcome.
2. The application attaches the session and candidate receipt where supported.
3. A failed write is visible and retryable; duplicate/replayed events are handled by the existing idempotent backend path.
4. Ordinary community reactions and gamification continue independently of research metrics.

**Acceptance:** No receipt is fabricated; a failed projection or retry is not reported as a successful learning update; representative outcome and model-improvement claims remain pending until the Lead's evidence exists.

### UC-06 — Research reviewer inspects evaluation and thesis evidence

1. An authorized reviewer opens the protected evaluation or thesis workspace.
2. The client loads the bundled or configured Lead export through schema validation.
3. Pending, rejected and complete records retain their states and caveats; malformed or inconsistent manifests fail closed.
4. The reviewer exports a figure with the provenance actually supplied by the source.

**Acceptance:** No old fallback values appear after remote failure; distinct protocols are not framed as a matched comparison; schema-valid metadata is not represented as independent scientific verification.

## Data flows

### Photo to recipe candidates

Browser permission or image selection → scan UI → same-origin ingredient-detection route → configured Lead detector → normalized detections with source → user review/correction → same-origin ingredient-to-recipe route → configured recipe matcher → normalized recipe candidates → existing recipe/substitution UI.

Failure branches: invalid image returns a client error; missing detector or matcher returns integration-pending; upstream failure/invalid response returns an error. Explicit demo output is allowed only under the mock setting and is labeled. User review remains between detection and downstream action.

### Profile to substitution and safety display

Authenticated profile/settings → normalized allergen flags → substitution request with ingredient, reason, recipe context, dietary tags and session identity → AI candidate response → conservative policy/profile resolver → Safe, Check or Blocked card state → user selection/decline → receipt-bearing feedback event.

Compound data follows a separate evidence path: candidate ingredient → official compound profile/pair endpoint → source/grounding/measurement validation → explanation display. Compound presence never upgrades an allergen state. Missing values remain unavailable.

### Lead evidence to evaluation and thesis UI

Lead-produced dataset/split/protocol/prediction artifacts → Lead manifest and review decision → frontend schema validation → evaluation dashboard and exported figure → thesis artifact reference and capture state.

The frontend can reject malformed structure and render supplied hashes, but only the Lead/publisher can establish artifact identity, rights, independent review, score validity and model acceptance. A missing manifest stays pending.

### Creator and community workflow

Creator recipe draft → existing recipe validation/storage → publish → community interactions and product XP. User cooking choices/feedback flow through session APIs and durable candidate receipts. Product engagement events are not substituted for held-out evaluation labels.

## UI-facing API and adapter contract

| Flow | Current interface in code | Contract and current boundary |
| --- | --- | --- |
| Profile preferences | FE cooking settings use PUT /auth/settings/cooking; profile update also accepts allergen flags at PUT /auth/update. | The monolith persists normalized flags in the user profile/settings service. The authenticated deployed end-to-end flow is still a G4 acceptance gate. |
| Substitutions | POST /api/v1/suggest_substitutions with ingredient, reason, optional recipe_context, dietary_tags, allergen_flags and session_id; allergen flags are also forwarded in X-ChefKix-Allergen-Flags when nonempty. | AI response carries candidates and may carry receipt, allergen and compound data. Missing/uncertain policy remains Check; UI response validation must not infer safety from absent fields. |
| Compound profile/pair | GET /api/v1/compound/profile/{ingredient} and POST /api/v1/compound/analyze-pair. | Official-index grounding and source metadata are required for factual display. Pair overlap is a presence measure, not cooking suitability or safety. |
| Knowledge graph | GET /knowledge/graph with optional root or q, depth 0–2 and limit 1–500. | Response is bounded and includes coverage metadata. The checked-in seed is not the full canonical Lead graph. |
| Ingredient detection | POST /api/ingredient-detection with multipart image field. | Invalid/missing image returns 400; images over 10 MiB return 413; missing configured service returns 503 INTEGRATION_PENDING; explicit demo mode is labeled. The browser caller is verified against the app auth service; cancellation is propagated and upstream work times out after 30 seconds. Provider credentialing and real-device evidence remain open. |
| Ingredient-to-recipe matches | POST /api/photo-intelligence/ingredient-recipes with an ingredients array. | Missing matcher returns 503 INTEGRATION_PENDING; malformed or oversized requests return 400/413; upstream shape is normalized. Browser caller auth, bounds, cancellation and timeout are implemented; provider credentialing and accepted Lead endpoint remain open. |
| Evaluation | Protected /admin/evaluation and thesis evidence workspace. | Bundled/configured exports are schema-checked. Schema acceptance is not provenance authentication, independent review or score verification. |

The interfaces above record current code boundaries for FE and service alignment. They are not a replacement for the Lead-owned AI API requirements, data matrix, canonical vocabulary, OpenAPI source or deployment runbook.

## Backlog crosswalk and acceptance

| Member backlog item | Sprint 1–2 requirement | Current code/evidence state | Remaining acceptance gate |
| --- | --- | --- | --- |
| Epic 1 / G5 — graph explorer | UI-07, UC-04 | Bounded graph explorer and monolith API are implemented; opt-in mock path is explicit. | Exact rights/provenance-bound Lead graph export/import, real coverage and keyboard/mobile/performance evidence. |
| Epic 2 / G1, G6 — evaluation | UI-08, UC-06 | Schema checks, pending-value suppression and provenance-aware figure captions are implemented on the FE branch. | Actual Lead manifests, independent artifact verification, scored intervals/strata/abstention and publisher acceptance. |
| Epic 3 / G4 — allergen profile | UI-04, UC-03 | Profile/settings persistence and request forwarding are implemented. | Authenticated deployed profile-to-policy-to-UI evidence and reviewed serving policy. |
| Epic 4, Epic 8 / G8 — camera/photo | UI-02, UC-02 | Scanner UI and same-origin detector/recipe adapters are implemented; mock is explicit. Proxy caller auth, bounded inputs and abort/timeout handling are implemented. | Accepted detector/retrieval handoff, provider-specific authentication, rights-cleared inputs and real-device lifecycle evidence. |
| Epic 5 / G3 — compound explanation | UI-03, UI-06, UC-03–04 | Missing measurements remain unavailable; source/grounding distinction is implemented. | Official source/index availability and source-backed UI capture; usefulness study remains a human/Lead gate. |
| Epic 6 / G4 — safety UI | UI-05, UC-03 | Tri-state handling and conservative request/card behavior are implemented. | Reviewed policy, deployed end-to-end acceptance and independently adjudicated benchmark evidence. |
| Epic 7 / G7 — feedback | UI-09–10, UC-05 | UI, durable receipt and backend ingestion/replay path exist. | Cross-service accept/reject/skip/taste run, retry/idempotency/recovery exercise and representative outcome evidence. |
| Epic 9 / G6 — research evaluation | UI-08, UC-06 | Captions and provenance schema preserve supplied metadata and missing-manifest state. | Lead artifact hashes and scores checked against actual artifacts; no inferred training-quality result. |
| Epic 11 / G2, G9 — thesis workspace | UI-11, UC-06 | Workspace maps the 14 Lead chapters and marks source-dependent captures pending. | Canonical strategy/ledger, reviewed source artifacts, deployed cost/recovery evidence, study and defense evidence. |
| Epic 10 / G5 — real graph data | UI-07, UC-04 | Bounded API and client integration exist. | Full manifest, rights/schema/hash validation, staged import, coverage checks and rollback evidence. |
| Epic 12 / G10 — voice copilot | No Sprint 1–2 acceptance | Optional scaffold is tracked as deferred in the Member backlog. | Remains deferred until core evidence gates pass. |

## Current team gates

The current [Lead v102 update](../../BACKLOG_LEAD.md) verifies secondary Stage 3 Segment 2 at exact step 140/561 and reports Segment 3 running toward the first evaluation boundary at step 187. Step 187, retained-best adapter integrity, remaining segments, inference, scoring and model quality are still unchecked. Training progress therefore does not satisfy UI acceptance for a production model or evaluation result.

The Lead remains owner of the dataset matrix, canonical vocabulary, evaluation protocol, model artifacts, reviewed safety labels and scientific acceptance. As the Member backlog records, the full graph import, complete evaluation manifests, accepted detection/retrieval models, adjudicated allergen policy and user/device/deployment evidence are not supplied as complete inputs. Tín's UI must keep these unavailable/pending states truthful.

## Completion record

- Member-authored Sprint 2 SRS UI requirements, Use Cases, Data Flows and scaffold acceptance crosswalk: documented here for review.
- Sprint 1 member surfaces and Sprint 2 scaffold work: code exists across the tracked FE/monolith/AI branches, with completion statuses limited to the evidence in BACKLOG_MEMBER.md.
- Lead-owned data/research items: not claimed as Tín's completed work.
- Deployed, scientific and human-review gates: remain open until the named source artifacts and acceptance evidence are available.
