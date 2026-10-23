# ChefKix Modular Monolith

Unified application replacing the 5-microservice architecture. All services run in a single JVM on port 8080.

## Architecture

```
chefkix-monolith/
├── shared/          ← DTOs, events, exceptions, utilities (shared kernel)
├── identity-api/    ← ProfileProvider interface + DTOs (contract)
├── culinary-api/    ← RecipeProvider, SessionProvider interfaces (contract)
├── social-api/      ← PostProvider interface + DTOs (contract)
├── identity/        ← Auth, profile, social, leaderboard, settings, Keycloak
├── culinary/        ← Recipes, cooking sessions, challenges, uploads, AI client
├── social/          ← Posts, comments, likes, chat, conversations
├── notification/    ← Bell notifications, email (Brevo), Kafka consumers
└── application/     ← Entry point, SecurityConfig, WebSocketConfig, application.yml
```

## Quick Start

```powershell
# 1. Start infrastructure (MongoDB, Kafka, Keycloak, Redis)
cd chefkix-infrastructure
docker compose -f compose-infra-only.yaml up -d

# 2. Build & run monolith
cd chefkix-monolith
mvn clean package -DskipTests
java -jar application/target/application-1.0.0-SNAPSHOT.jar

# 3. Start frontend
cd chefkix-fe
npm run dev
```

The monolith runs at `http://localhost:8080` with context-path `/api/v1`.
All API endpoints are at `http://localhost:8080/api/v1/...`.

## Module Dependencies

```
shared ← identity-api ← identity
       ← culinary-api ← culinary
       ← social-api   ← social
                       ← notification
                       ← application (depends on ALL)
```

Cross-module calls use **Provider interfaces** (defined in `-api` modules, implemented in domain modules):
- `ProfileProvider` → identity module implements
- `RecipeProvider`, `SessionProvider` → culinary module implements
- `PostProvider` → social module implements

## Key Configuration

| Setting | Value | Notes |
|---------|-------|-------|
| Port | 8080 | `server.port` |
| Context Path | `/api/v1` | `server.servlet.context-path` |
| MongoDB | `localhost:27017/chefkix` | Single DB |
| Kafka | `localhost:9094` | Async events |
| Redis | `localhost:6379` | OTP rate limiting |
| Keycloak | `localhost:8180/realms/nottisn` | JWT issuer |
| AI Service | `localhost:8000` | External Python FastAPI |

## Controller Path Mapping

| FE Endpoint Prefix | Controller Path | Module |
|---------------------|-----------------|--------|
| `/api/v1/auth/**` | `/auth/**` | identity |
| `/api/v1/social/**` | `/social/**` | identity |
| `/api/v1/recipes/**` | `/recipes/**` | culinary |
| `/api/v1/cooking-sessions/**` | `/cooking-sessions/**` | culinary |
| `/api/v1/challenges/**` | `/challenges/**` | culinary |
| `/api/v1/posts/**` | `/posts/**` | social |
| `/api/v1/chat/conversations/**` | `/chat/conversations/**` | social |
| `/api/v1/chat/messages/**` | `/chat/messages/**` | social |
| `/api/v1/notification/**` | `/notification/**` | notification |
| `/api/v1/ws` | WebSocket STOMP | application |

## Graph Explorer and evidence status

The FE route `/explore/graph` is disabled unless `NEXT_PUBLIC_GRAPH_EXPLORER_ENABLED=true`. The current opt-in preview reads the bounded legacy knowledge API and is not the accepted M-C product graph. Keep it disabled as a product surface until the canonical graph artifact, applicable rights, provenance, and tamper-refusal checks are accepted.

The additive shadow endpoint is `GET /api/v1/graph/evidence`; `chefkix.knowledge.graph-evidence.enabled` defaults to `false`. It requires a hash-verified bundle and exact resolved ingredient IDs, reports train-record counts with unit `TRAIN_PAIRS`, and always abstains. It does not recommend substitutions or replace the legacy suggestion route. The FE shadow panel also has its own build-time flag, `NEXT_PUBLIC_GRAPH_EVIDENCE_ENABLED`, which defaults to off.

The AI-service route contract test confirms `/api/v1/suggest_substitutions` returns the same product response when the request-scoped graph-preflight flag is off or on without a research marker. The flag alone does not call the graph resolver or candidate gateway; the mocked Gemini path runs in both cases. See T-09 in the Member backlog. This is a TestClient check, not a live deployment run.

The FE graph loader requires node and edge arrays, nonblank ingredient IDs/names, unique node IDs, and a recognized relationship type. It accepts the `substitutes` legacy alias plus the supported substitution, chemical-similarity, and co-occurrence types. Missing or unknown types render an error instead of being mislabeled as a substitution. An empty successful search reports that the ingredient is not in the current graph and does not add a made-up node.

The recipe research-evidence panel displays only provenance returned in a typed graph-evidence response: its manifest digest is labeled as returned provenance, and its source split is shown only when the response contains graph provenance. Tests cover recommendation and abstention states, absent provenance, and malformed hashes. The panel requires an injected fetcher that no product route currently supplies; it does not verify served artifact bytes or establish T55 integration.

The legacy graph endpoint resolves substitution targets only by exact canonical name or a declared ingredient alias. Canonical names take precedence over aliases; aliases shared by multiple ingredients remain unresolved. In the isolated seed check, the API returned 25 nodes and 5 edges: 4 of 59 substitution statements matched canonical names, 1 matched a declared alias, and 54 stayed unresolved. The FE labels this bounded data as a preview and does not present the omitted targets as safe or calibrated recommendations. This seeded API check does not accept the Leader's hash-pinned graph artifact or its research identity map.

When graph evidence is enabled, responses are tied to the selected edge; a late response for a previously selected edge is ignored.

Current implementation evidence and open acceptance gates are tracked in [the Member backlog](BACKLOG_MEMBER.md). FE request/response details and the live browser harness are maintained in `chefkix-fe/docs/epic-10-graph-integration.md` and `chefkix-fe/tests/e2e/graph-explorer-live.spec.ts`. Headed Chrome passed the live FE-to-Monolith graph smoke on desktop and iPhone-sized viewports against an isolated database with API mocks off; the checks confirm the exact alias edge and keep unknown searches empty. The fresh run receipt is `evidence/member-mc-live-graph-20261023/live-browser-catchup-run-20261023.json`. This remains a local legacy-seed preview and does not establish rights clearance, accepted canonical graph provenance, or deployed behavior. Compound detail/pair data and the shadow evidence endpoint still require their separate AI artifacts/configuration and acceptance gates.

The separate AI-service Graph-RAG model remains unready: this checkout lacks its required graph sidecar, and the tracked checkpoint does not match the bundled model implementation. The registry now fails closed on the missing sidecar and the graph loader uses restricted deserialization; see the current M-A entry in the Member backlog for the evidence and remaining model-provenance gate.

The tracked Java ingredient seeder has a reproducible, source-only graph audit at `evidence/member-ma-seed-graph-source-20261023/`. It reports 25 source records and 59 directed statements, with the directed degree-sum invariant passing. Recreate it with `python tools/audit_seed_graph.py` and check the parser with `python tools/test_audit_seed_graph.py`. This does not establish runtime graph contents or coverage curves; the query cohort, runtime artifact, and Round-0 acceptance gates remain open.

Graph Evidence provider failures return a typed generic error, and the FE displays a generic unavailable state when the upstream diagnostic payload is untrusted. Backend sentinel regressions pass, and visible Chrome checks pass on desktop and iPhone-sized viewports. A separate local graph API response rendered successfully from isolated legacy seed data; T-11 remains partial pending a full scan of demo outputs and the live error-path/log/artifact inventory. See `evidence/member-t11-error-output-20261023.json` and `evidence/member-mc-live-graph-20261023/`.

The headed Graph Explorer browser run also gates requests to same-origin FE assets and the three local graph API arms. Desktop Chrome and iPhone-sized Chromium both show the generic evidence fallback when a fixture returns `PROVIDER_ERROR`, with no non-allowlisted request attempted. The screenshot and hashed run receipt are in `evidence/member-t10-network-isolated-graph-demo-20261023/`. A separate local live FE-to-Monolith smoke test is recorded in `evidence/member-mc-live-graph-20261023/`; T-10 remains partial because the recorded Act 3 and deployed/accepted-artifact run are unavailable.

Graph Explorer now identifies edges with a collision-safe JSON tuple across graph merging, rendering, selection, and evidence state. This prevents colon-bearing ingredient IDs from merging distinct relationships or displaying details for the wrong selected edge. The failure-driven unit and headed browser checks are recorded in `evidence/member-mc-edge-identity-20261023/`; they use fixture APIs and do not close the accepted-artifact or live-backend gates.

The current branch-sync regression checkpoint passes the full FE suite (213 suites / 851 tests), the Monolith Maven reactor (360 tests in 84 reports), and the AI-service suite (531 passed / 11 skipped / 28 warnings; 76% coverage). The full headed Chrome suite passed 16 E2E tests with 4 expected skips across desktop and iPhone-sized Chromium; the opt-in live FE-to-Monolith graph spec passed separately on both viewports with API mocks off. A failure-driven compound-detail regression also verifies an empty response can be retried. Exact commands and remaining acceptance limits are recorded in [the Member backlog](BACKLOG_MEMBER.md).

Social cook-post regression: headed installed Chrome passed on desktop and iPhone-sized Chromium (2/2). The browser verifies blocked, flagged, and unknown moderation actions stop publication, then verifies the approved post/session-link/XP flow and generated multipart request. All 851 FE Jest tests and all 356 Monolith tests pass. Browser API calls are fixture-backed and backend tests are unit/service tests; real persistence, Kafka delivery, media upload, and feed visibility remain unverified. The earlier cook-post run used temporary Google Font fixtures; the latest graph-enabled production build fetched fonts successfully. Receipt: `evidence/member-social-cook-post-browser-20261023.json`.

Social moderation now fails closed across posts, comments, replies, chat messages, and story replies: only an explicit `approve` can be published. FE composers for posts, comments, replies, cook posts, quick posts, and recipes also stop on `flag`, missing results, failures, or unknown actions and explain that flagged content was not published. The pre-fix backend post regression failed, and all five backend flagged-content regressions now pass; see `evidence/member-social-moderation-fail-closed-20261023.json`. A manual review queue is not implemented, so flagged content is rejected for retry rather than queued.

## First-party recipe inventory status

M-F remains open. The latest bounded scan found no recipe/license corpus in the Monolith or FE repositories, and the Monolith `Recipe` model has no explicit source or license fields. The local MongoDB read-only probe failed authentication before reading documents. Recipe count is unknown and licenses have not been assessed; see [the M-F evidence receipt](evidence/member-mf-repository-and-schema-probe-20261023.json) and [the Member backlog](BACKLOG_MEMBER.md).

Recipe publishing now requires an explicit backend moderation approval. A failure-first regression caught flagged, unknown, and missing moderation outcomes proceeding or raising the wrong error; the focused fix passes 4/4 and the full Monolith suite passes 360/360. The backend test uses a mocked AI client, and a headed recipe-publish browser test is not present. See [the regression receipt](evidence/member-recipe-moderation-fail-closed-20261023.json).

## What Changed from Microservices

| Before | After |
|--------|-------|
| 5 JARs, 5 ports | 1 JAR, port 8080 |
| API Gateway (8888) | `context-path: /api/v1` |
| Eureka discovery | Not needed |
| Feign clients | Direct method calls via Provider interfaces |
| 5 databases | 1 database (`chefkix`) |
| 5 SecurityConfigs | 1 unified SecurityConfig |
| 2 WebSocketConfigs | 1 unified WebSocketConfig |

## Build

```powershell
mvn compile           # Compile all 10 modules
mvn clean package     # Build JAR (in application/target/)
mvn test              # Run tests
```
