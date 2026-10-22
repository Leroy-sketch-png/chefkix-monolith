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

When graph evidence is enabled, responses are tied to the selected edge; a late response for a previously selected edge is ignored.

Current implementation evidence and open acceptance gates are tracked in [the Member backlog](BACKLOG_MEMBER.md). FE request/response details are maintained in `chefkix-fe/docs/epic-10-graph-integration.md`. A live local FE-to-Monolith browser smoke test now appears in `evidence/member-mc-live-graph-20261023/`; it uses isolated legacy seeder data and does not establish rights clearance, acceptance of the canonical graph, or deployed FE-to-backend behavior. The graph UI loaded, while compound profile/pair calls returned 503 because local AI proxy secrets were missing and the default AI service port was not running; the UI showed its generic missing-profile states.

The separate AI-service Graph-RAG model remains unready: this checkout lacks its required graph sidecar, and the tracked checkpoint does not match the bundled model implementation. The registry now fails closed on the missing sidecar and the graph loader uses restricted deserialization; see the current M-A entry in the Member backlog for the evidence and remaining model-provenance gate.

The tracked Java ingredient seeder has a reproducible, source-only graph audit at `evidence/member-ma-seed-graph-source-20261023/`. It reports 25 source records and 59 directed statements, with the directed degree-sum invariant passing. Recreate it with `python tools/audit_seed_graph.py` and check the parser with `python tools/test_audit_seed_graph.py`. This does not establish runtime graph contents or coverage curves; the query cohort, runtime artifact, and Round-0 acceptance gates remain open.

Graph Evidence provider failures return a typed generic error, and the FE displays a generic unavailable state when the upstream diagnostic payload is untrusted. Backend sentinel regressions pass, and visible Chrome checks pass on desktop and iPhone-sized viewports. A separate local graph API response rendered successfully from isolated legacy seed data; T-11 remains partial pending a full scan of demo outputs and the live error-path/log/artifact inventory. See `evidence/member-t11-error-output-20261023.json` and `evidence/member-mc-live-graph-20261023/`.

The headed Graph Explorer browser run also gates requests to same-origin FE assets and the three local graph API arms. Desktop Chrome and iPhone-sized Chromium both show the generic evidence fallback when a fixture returns `PROVIDER_ERROR`, with no non-allowlisted request attempted. The screenshot and hashed run receipt are in `evidence/member-t10-network-isolated-graph-demo-20261023/`. A separate local live FE-to-Monolith smoke test is recorded in `evidence/member-mc-live-graph-20261023/`; T-10 remains partial because the recorded Act 3 and deployed/accepted-artifact run are unavailable.

Graph Explorer now identifies edges with a collision-safe JSON tuple across graph merging, rendering, selection, and evidence state. This prevents colon-bearing ingredient IDs from merging distinct relationships or displaying details for the wrong selected edge. The failure-driven unit and headed browser checks are recorded in `evidence/member-mc-edge-identity-20261023/`; they use fixture APIs and do not close the accepted-artifact or live-backend gates.

The current branch-sync regression checkpoint passes the full FE suite (213 suites / 845 tests), the Monolith Maven reactor (352 tests in 83 reports), and the AI-service suite (518 passed / 11 skipped / 28 warnings after syncing main). Graph Explorer plus route tests pass 31/31. Exact commands, merge resolution, and remaining acceptance limits are recorded in [the Member backlog](BACKLOG_MEMBER.md).

Social cook-post regression: headed installed Chrome passed on desktop and iPhone-sized Chromium (2/2). The browser verifies blocked, flagged, and unknown moderation actions stop publication, then verifies the approved post/session-link/XP flow and generated multipart request. All 845 FE Jest tests and all 352 Monolith tests pass. Browser API calls are fixture-backed and backend tests are unit/service tests; real persistence, Kafka delivery, media upload, and feed visibility remain unverified. The build used temporary Google Font fixtures after an external font response broke Next's parser. Receipt: `evidence/member-social-cook-post-browser-20261023.json`.

Social moderation now fails closed across posts, comments, replies, chat messages, and story replies: only an explicit `approve` can be published. FE composers for posts, comments, replies, cook posts, quick posts, and recipes also stop on `flag`, missing results, failures, or unknown actions and explain that flagged content was not published. The pre-fix backend post regression failed, and all five backend flagged-content regressions now pass; see `evidence/member-social-moderation-fail-closed-20261023.json`. A manual review queue is not implemented, so flagged content is rejected for retry rather than queued.

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
