# IRON CHEF v3: MEMBER BACKLOG

> **Aligned 2026-10-04 with LEAD v101**, monolith `origin/master` commit `e2e0dfc` (latest update dated 2026-10-02).
> **Authority:** `academic_vision_and_strategy.md`, then `ai/out/iron-chef-lead-v2/BACKLOG_LEDGER.md`, then current updates in [BACKLOG_LEAD.md](BACKLOG_LEAD.md). The strategy and atomic ledger are absent from these four checkouts. Lead scientific claims below are attributed summaries, not independently reconstructed results.
> This replaces the historical member backlog, preserved in Git history. A checked engineering task means code exists with the cited checks; it does **not** certify a model, deployment, clinical safety or thesis result.

## Current handoff

| Lead evidence | Member consequence |
| --- | --- |
| M1 corrected SAG and true-context dual encoder rejected | Preserve negative results; no accepted HGAT production score. |
| M2 DPO complete; Stage 3 exact step 70/561 verified; Segment 2 queued toward 140 as of v99 | Show training progress separately from quality. No frozen predictions/matched M2 score. Do not infer later completion from elapsed time. |
| M3 50 rights-bound candidates and two review packs; accepted labels/training absent | Camera UI may exist; production detection and user-camera capability remain open. |
| M4 trained projection rejected | Do not install rejected weights or advertise a production replacement index. |
| Official FooDB factuality verified | Display compound presence; overlap does not prove cooking suitability, melting point, flavor equivalence or usefulness. |
| OFF v101 taxonomy risk partition reconstructed | Exact taxonomy matches differ from review-only alias candidates. Adjudication, negatives, clinical validity, serving and legal review remain open. |
| Mistral small 2603: matched 208/208 raw responses | Raw capture status only. GPT-4o/Gemini, two independent reviews/adjudication remain open. No winner/rate claim. |
| L29 offline chain: 179/783 paired-correct complete chains | Graph coverage is not end-to-end correctness. No user-camera, hidden-ingredient, functional chemistry or safety claim. |
| Durable receipts and runtime certificates exist | Preserve receipt/auth boundaries. Operational policy, representative outcomes, recovery and user evidence remain separate. |

## Implementation sequence

| Priority / gap | Scope | Owner / dependency |
| --- | --- | --- |
| P0 / G1 | Validate evaluation exports; retire withdrawn public scores, winner cues and scripted provider attribution | Member; first implementation in this alignment |
| P0 / G2 | Remap thesis capture workspace to all 14 Lead chapters | Member; first implementation in this alignment |
| P1 / G3 | Missing compound overlap/nutrition stays unavailable in every substitution card; preserve factuality/source boundaries | Member + official compound API |
| P1 / G4 | Tri-state safety, custom allergens and profile-to-policy flow; distinguish taxonomy candidates from accepted labels | Member integration + Lead adjudicated policy |
| P1 / G5 | Import rights/provenance-bound graph vocabulary into bounded API; expose real coverage | Member import + Lead exact manifest |
| P1 / G6 | Per-result dataset, split, protocol, seed count, hashes and claim limits in views/exported figures | Member renderer + Lead metric manifests |
| P2 / G7 | Receipted feedback UI/retry/recovery with representative cooked-session evidence | Member + Lead/runtime evidence |
| P2 / G8 | Complete photo pipeline after accepted model/endpoint handoff; real device evidence | Joint; M3/M4 acceptance/rights gates |
| P2 / G9 | Thesis figures, deployment evidence, scored rehearsal | Joint; source artifacts and human reviews/study/rehearsal |
| Tier C / G10 | Voice-Vision integration and lifecycle | Deferred until core gates pass |

## EPIC 1: Graph explorer infrastructure — IMPLEMENTED, coverage partial

- [x] Force renderer, node/edge detail, search and signal filters at `/explore/graph`.
- [x] Explicit opt-in mock mode; ordinary requests use the monolith graph API.
- [x] Bounded search/neighborhood loading rather than whole-graph rendering.
- [ ] Capture keyboard/mobile and large-graph performance evidence using the accepted imported graph (G5).

Code: `chefkix-fe/src/features/graph-explorer/`. Checks: `GraphExplorer.test.tsx`, `GraphCanvas.test.tsx`, `GraphDetailPanel.test.tsx`, `graphExplorerService.test.ts`. Handoff: `chefkix-fe/docs/epic-10-graph-integration.md`.

## EPIC 2: Evaluation dashboard shell — IMPLEMENTED, evidence contract partial

- [x] Protected `/admin/evaluation`, benchmark table, ablation/allergen/behavioral charts and PNG export controls.
- [x] Bundled JSON or configured remote manifest loader.
- [x] G1: runtime validation for both sources; malformed sections, invalid metrics, duplicate IDs and inconsistent MRR deltas reject.
- [x] G1: pending/placeholder metrics suppressed; completed safety rates require adjudication metadata. Schema validation does not authenticate external evidence.
- [x] G1: public JSON matches dashboard records; historical synthetic exports retired from serving, preserved in Git history.
- [x] G1: unverified paper Mistral value removed from matched scoreboard; M2 stays pending; cross-protocol winner badge removed.
- [ ] Full G6 provenance, comparison groups, confidence intervals and export captions.

Code: `chefkix-fe/src/features/evaluation-dashboard/`. Checks: `evaluationDashboardService.test.ts`, `publicEvaluationExports.test.ts`. Contract: `chefkix-fe/docs/epic-9-evaluation-integration.md`.

## EPIC 3: Allergen profile — IMPLEMENTED in settings, live acceptance pending

- [x] EU 14/FDA 9 selector and custom flags in settings meet the original onboarding-or-settings requirement.
- [x] Monolith `UserProfile.allergenFlags`, normalization and profile/settings persistence.
- [x] Substitution requests carry allergen flags.
- [ ] G4: verify saved profile → authenticated request → AI policy → displayed result across all callers, custom/unknown terms and empty profiles.

Code: frontend `src/components/settings/AllergenProfileSelector.tsx`, `src/lib/allergen-profile.ts`, `src/services/ai.ts`; monolith `identity/src/main/java/com/chefkix/identity/` profile/settings services. Checks: `allergen-profile.test.ts`, `allergen-safety.test.ts`.

## EPIC 4: Camera scaffold — IMPLEMENTED, model capability open

- [x] Camera capture, ingredient overlay, confidence/source display and `/scan` workspace.
- [x] Mock detection requires explicit configuration; missing real endpoints return integration-pending states.
- [ ] G8: record permission refusal, camera switching, stream cleanup and mobile-device evidence.
- [ ] Enable production detection only after an accepted Lead model/endpoint handoff. Configuration alone is not quality evidence.

Code: frontend `src/components/scan/`, `src/app/api/ingredient-detection/`. Checks: `IngredientScanner.test.tsx`, `IngredientDetectionOverlay.test.tsx`.

## EPIC 5: Compound explanation — IMPLEMENTED surface, contract gaps open

- [x] Confidence, named compound list, overlap graphic, comparison and nutrition components exist.
- [x] Ordinary requests do not silently fall back to mock compound data.
- [x] Graph details unwrap official AI evidence and keep unavailable comparisons explicit.
- [x] G3: missing overlap/nutrition remain null/Unavailable across cards and comparisons; explicit zero and producer units have regression checks.
- [x] G3: preserve grounding, official source identity/fingerprint, and nested/degraded states; AI pantry attaches official evidence and discards generated chemistry claims.
- [x] G3: retire fabricated compound examples and independent boolean safety badges; label overlap as presence, not cooking suitability.
- [ ] Capture real payload/UI evidence for Chapter 8. Usefulness requires the Lead's ethics-reviewed N≥20 study.

Code: frontend `src/components/recipe/CompoundExplanation.tsx`, `src/lib/compound-explanation.ts`; check: `compound-explanation.test.ts`. Dependency: official FooDB API/manifest, not a hardcoded butter example.

## EPIC 6: Allergen safety UI — IMPLEMENTED surface, comparison open

- [x] Safety indicators, recipe warning banner and source-aware resolver exist.
- [x] G1: `/demo/allergen-safety` is a fixed illustration. Scripted examples are not attributed to GPT-4o; no fake prompt execution; unverified examples stay Check.
- [x] Dashboard shows matched Mistral raw capture without a safety rate.
- [x] G4: regression checks cover SAFE/UNKNOWN/BLOCKED, malformed/empty policy, local/custom conflicts, profile forwarding and blocked primary actions on recipe/cooking surfaces. Neither frontend nor AI name screening infers Safe from absence of a match.
- [ ] G4: certify the authenticated deployed profile → policy → UI flow and reviewed policy handoff. Local mocks/API tests do not close this acceptance gate.
- [ ] G4: keep v101 taxonomy alias candidates review-only when a reviewed endpoint is supplied.
- [ ] Real head-to-head view requires matched arms, two independent reviews, adjudication, rates/abstention/Wilson intervals and hash-bound scores.

Code: frontend `src/lib/allergen-safety.ts`, `src/components/recipe/AllergenSafetyIndicator.tsx`, `RecipeAllergenBanner.tsx`. Checks: `allergen-safety.test.ts`, `allergenIllustration.test.tsx`.

## EPIC 7: Feedback instrument — IMPLEMENTED, product evidence partial

- [x] Choice/outcome and post-session feedback controls in CookingPlayer.
- [x] Monolith endpoint, Kafka projection, durable candidate receipt forwarding and service/listener checks.
- [x] AI ingestion/replay path exists; receipt-bearing candidates remain the acceptance boundary.
- [ ] G7: execute accept/reject/skip/taste across frontend/monolith/AI; verify missing receipt, retry/idempotency, failed projection and replay.
- [ ] G7: representative outcomes, scorer, automated quiescence, replicated storage and operational recovery remain open. UI completion is not learning improvement.

Code: frontend `src/components/cooking/CookingPlayer.tsx`; monolith `culinary/.../features/session/`. Checks: `substitution-button-feedback.test.tsx`, `SubstitutionFeedbackControllerTest`, `SubstitutionFeedbackServiceTest`, `SubstitutionFeedbackListenerTest`.

## EPIC 8: Photo intelligence — ADAPTERS IMPLEMENTED, accepted models blocked

- [x] Same-origin detection, ingredient-to-recipe and dish retrieval adapters; unconfigured services stay unavailable.
- [x] Match cards, missing ingredients and substitution handoff exist.
- [ ] G8: accepted detector, retrieval model/index and manifest identities before real integration.
- [ ] G8: rights-cleared external/user-camera evidence; separate recipe correctness from graph coverage.
- [ ] G8: auth, cancellation/timeouts, upload failures and camera lifecycle on real devices.

Contract: `chefkix-fe/docs/epic-8-photo-intelligence-integration.md`. Check: `src/app/api/photo-intelligence/__tests__/routes.test.ts`. L29's offline result does not complete this epic.

## EPIC 9: Evaluation with real results — PARTIAL

- [x] Bundled records preserve GISMo reproduction and rejected ablation/feedback evidence with caveats.
- [x] Missing M1/M2/allergen results stay pending; G1 prevents placeholder rates entering charts.
- [ ] G6: detailed Lead exports with dataset, split, protocol, seeds, source/prediction hashes and decision.
- [ ] G6: repeated/unseen strata, rejected M1/M4 results and selective-abstention gates; benchmark completion is not deployment acceptance.
- [ ] G6: safety review status, Wilson intervals and abstention only when scored evidence exists.
- [x] G6: SVG/PNG captions carry status, notes, supplied dataset/split/protocol/seeds/full hashes/decision/claim limits; missing provenance is explicit. Runtime validation rejects malformed supplied provenance.
- [ ] G6: bind these fields to independently checked Lead artifacts and validate scored intervals/abstention/strata; rendering metadata is not scientific verification.

Detailed atomic ledger/artifacts are absent locally. Never reconstruct a missing score from training loss, checkpoint count or a paper summary.

## EPIC 10: Graph with real data — BOUNDED API IMPLEMENTED, import pending

- [x] Monolith name/alias search, root/depth/limit neighborhoods, bounded edges and pagination metadata.
- [x] Frontend lazy neighborhood loading and maximum 500-node working view.
- [x] Grounded compound profile/pair endpoints wired; quantity ratios are not confidence.
- [ ] G5: ingest exact Lead vocabulary/graph with hash/schema/rights/coverage validation, provenance and safe rollback.
- [ ] G5: actual USDA snapshots, validated cook counts and technique-context only when supported. Current seed data lacks these fields.
- [ ] Capture API-backed evidence against the imported collection; the seeded graph is not the full research graph.

Code: monolith `culinary/src/main/java/com/chefkix/culinary/features/knowledge/`; checks: `KnowledgeGraphServiceTest`, `KnowledgeGraphControllerTest`. Frontend handoff: `docs/epic-10-graph-integration.md`.

## EPIC 11: Thesis engineering — WORKSPACE ALIGNED, captures open

- [x] G2: `/thesis` maps all 14 Lead chapters, replacing retired member numbering.
- [x] Compound → 8; safety → 9; photo → 11; architecture → 12; behavioral/evaluation → 13. Remaining chapters have explicit Lead handoff entries.
- [x] Unique artifact IDs and pending source-dependent captures; ready-to-capture is not completed evidence.
- [ ] G9: obtain canonical strategy/atomic ledger, audit captions and capture real-source figures/screenshots.
- [ ] G9: deployed regions/URLs, source fingerprints, auth/quotas/recovery and measured costs; no $0 claim from a plan.
- [ ] G9: ethics/recruitment/user study, independent allergen/image reviews and recorded/scored defense rehearsal are human/Lead dependencies.
- [ ] Reconcile preserved sprint drafts before submission. They are schedule drafts, not current completion evidence.

Code/check: frontend `src/features/thesis-engineering/`, `thesisEvidenceManifest.test.ts`.

## EPIC 12: Voice-Vision copilot — OPTIONAL SCAFFOLD ONLY

- [x] Wake-word, optional camera, TTS, intervention display and endpoint adapter code exist.
- [ ] G10: accepted model, grounded answers/citations, permission/input lifecycle, full-stage correctness and user evidence.
- [ ] Preserve Lead's rejected end-to-end finding. The scaffold does not supersede it; never show the withdrawn grounding comparison.

Contract/check: frontend `docs/epic-12-voice-vision-copilot.md`, `src/features/voice-copilot/__tests__/voiceCopilotService.test.ts`.

## Keeping the tracks aligned

1. Fetch Lead; record commit/newest update above. Do not infer completion from queued/running state.
2. Read newest updates before older narrative. Obtain the atomic artifact if they conflict; do not rewrite Lead research results.
3. Map changes to G1–G10 and member epic. Distinguish code present, locally checked, deployed, scientifically verified and human-reviewed evidence.
4. Update dashboard/thesis records and tests together. Preserve rejected history outside active score-serving paths.
5. Commit/push code and evidence pointers; exclude credentials, private raw responses, weights and generated caches.

Validation record and remaining boundaries: `chefkix-fe/docs/member-lead-alignment-2026-10-04.md`.


## Integration follow-through — 2026-10-04

Frontend G3/G4/G6 changes pair with AI branch `codex/member-evidence-contract`. AI contract/policy tests: 35 passed. Frontend regression verification and production-build results are recorded in the PR. No deployed workflow certification or database import is claimed.

G5 remains dependent on the canonical export/manifest. The available AI `models/graph_sample.json` contains 500 nodes and 37 edges (SHA-256 `0cd3c9c9b33f9935db393f41fef190a16498cdd7222badfec7333532d882dedc`), with no full graph rights/schema/import manifest. Compound FooDB provenance describes a separate presence artifact; it is not the graph import manifest. Obtain the full artifact, validate schema/hash/identities/edge endpoints/counts/rights, stage a collection, check bounded API coverage, and retain the prior collection for rollback before switching. Do not interpret sample edge confidence as a cooking ratio.

Accepted models, independent reviews, rights-cleared user/device evidence and the atomic ledger remain outstanding. These dependencies prevent reporting full leader integration, even after all currently testable member fixes merge.
