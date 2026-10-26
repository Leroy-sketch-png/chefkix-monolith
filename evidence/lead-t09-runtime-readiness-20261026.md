# T-09 Graph Explorer runtime readiness — lead check

**Observed:** 2026-10-26, from the ChefKix research execution worktree. This is an environment-readiness receipt only; it does not prove Graph Explorer non-mutation or close T-09.

## Checks and limits

- The monolith endpoint at `localhost:8080` was not reachable.
- Docker CLI was present, but the Docker Engine was unavailable. Windows reported the `com.docker.service` state as `Stopped`; an ordinary `Start-Service` attempt was denied (`Cannot open 'com.docker.service' service`). No elevation or workaround was attempted.
- No `mongod` command/service was found. Names-only process-environment inspection found no `MONGODB_URI`, `IDP_CLIENT_SECRET`, or `APP_OTP_SECRET`; no secret values were read.
- The documented ignored infrastructure `.env` exists; only its presence and file metadata were checked. Its contents were not read or loaded.
- The documented `compose-infra-only.yaml` mounts the local persistent Mongo directory at `./data/mongo` and starts multiple shared services. No compose operation was attempted; no persistent database was started or modified by this check.

## Disposition

T-09 remains **OPEN**. The required differential still needs an authorized existing monolith+Mongo runtime, a known fixture, and a permitted persisted-state inspection path. Capture the same suggestion request/response and relevant state before and after the Explorer sequence; keep the research route default-off, provisional graph refused, and flags unchanged. Source inspection, local page availability, refusal tests, or this readiness receipt are not runtime acceptance and earn no C5/milestone credit.

The originating lead card is `D:/code/chefkix/.git-worktrees/root-master-segment10-20261014/ai/campaigns/iron-chef-outcome-push-20261021/tasks/T395-c5-t09-explorer-nonmutation-source-review.md` (local SHA-256 `086b47c6ac5c1ee159163e83b8349972d053fc617b41da75e136a71b26f7826d`). The research checkout has no configured remote; the relevant observation is copied here so the shared backlog has a fetchable record. No runtime, credential, account, API, source-data, or infrastructure operation was performed beyond the observations above.
