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
