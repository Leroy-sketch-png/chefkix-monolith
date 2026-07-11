# ChefKix Monolith — AI Agent Instructions
<!-- Cross-ref: workspace root .github/copilot-instructions.md (philosophy) + AGENTS.md (audit trail) + SCOPE.md (role contract + work queue) -->

# LIVE FIXES (read before touching any Java file)
- CookingSessionService.java:243 — NPE when `profileProvider.getBasicProfile()` returns null (auto-draft silently dropped). Fixed with null guard.
- PostService.java:1752-1762 — PostCreatedEvent kafka + PostIndexEvent were NOT sent for auto-drafts (RECENT_COOK). Fixed by adding both.
- TrackingEventType.java:31 — `BATTLE_VOTE` was missing from BE enum (FE sent it, BE rejected). Added.
- `TERMS_NOT_ACCEPTED` error code exists in BE but signup form has no ToS checkbox.

# MONOLITH CONVENTIONS
- Modular monolith: `identity`, `culinary`, `social`, `notification`, `shared`
- Controller → Service → Repository pattern
- Kafka topics: `post-delivery`, `story-delivery`, `chat-delivery`, `xp-reward`, `otp-delivery`, `comment-delivery`, etc.
- MongoDB for documents, Redis for caching/rate-limiting, Keycloak for auth
- Cross-module calls via `-api` SPI interfaces (no Feign/HTTP between modules)
- Single DB: `chefkix` (all collections)
- Response: `ApiResponse.<T>builder().success(true).statusCode(200).data(x).build()`
- Errors: `throw new AppException(ErrorCode.XXX)`
- Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @FieldDefaults(level = AccessLevel.PRIVATE)`
- MapStruct: `@Mapper(componentModel = "spring")`

# TESTING
```powershell
mvnw test
mvnw spring-boot:run -pl application  # dev server
```
**Creds**: testuser/test123, Keycloak admin/admin

# KEY FILES
| Path | Why |
|------|-----|
| `identity/.../TrackingEventType.java` | Tracking event enum (must match FE) |
| `culinary/.../CookingSessionService.java` | Session + XP + auto-draft |
| `social/.../PostService.java` | Post CRUD + 7-signal feed ranking |
| `social/.../StoryFeedServiceImpl.java` | Story feed + seen/unseen |
| `identity/.../AuthenticationController.java` | Register/login/OTP |
| `application/src/main/resources/application.yml` | BE config |
| `application/.../config/SecurityConfig.java` | Public endpoints, CORS |
| `..vision_and_spec/` | All product specs (40 files) |
| `..AGENTS.md` | Full audit trail + key file map |
