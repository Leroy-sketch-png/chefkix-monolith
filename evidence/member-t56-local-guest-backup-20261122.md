# T56 local guest backup

Captured 2026-11-22 in a single isolated Chromium tab using the existing Playwright video recorder. The frontend was `56bba82f7d691e7952381e2cf56f8d296f3a95f9`. The local monolith jar was built at `0757ebcccc78f94e9a6e0256a2d86f2ec6171066`. Application sources matched the current baseline `767722a4929ee01d048cb015e196588405a73780` for the modules used by that build.

| Verification | Observed result |
| --- | --- |
| Browse | HTTP 200, 12 returned recipes, detail HTTP 200, and Back restored the first card. |
| Search | HTTP 200, 10 returned recipe hits for the demo query, detail HTTP 200, and Back restored the query and first result. |
| Narrow optional widget | No Tonight’s Pick request at 360 CSS px. |
| Playback | WebM, VP8, 360 × 640, 19.36 seconds, one video stream. Chromium decoded and played the media. Representative frames were inspected. |
| Local artifact | `member-t56-local-guest-backup-20261122.webm`, 2,019,549 bytes, SHA-256 `8bee8aea96df85dbf0eef2004f780f03c987411edb66080d6e6f9addf6adf68b`. The repository carries this receipt. The clip stays in the local workspace. |

## Privacy inventory for the local guest capture

| Surface | Data visible or handled and controls exercised |
| --- | --- |
| Browser view | ChefKix guest navigation, Explore filters, the demo query, recipe titles, food images, descriptions, and visible recipe metadata. The recording applies capture-only CSS before page rendering to conceal the recipe author name, handle, avatar, and profile link. Both detail views verified that the author element existed and was hidden. |
| Browser responses | Browse, search, and two recipe detail responses were read in memory. Browse returned an author object on 0 of 12 entries. The guest DTO can carry an author ID, handle, display name, and avatar reference. Those values and the raw response bodies were omitted from the receipt and recording output. Recipe identifiers were compared in memory to verify Back behavior. |
| Credentials and preferences | A fresh browser profile had no signed-in account. The allowed guest API requests carried neither authorization nor cookie headers. The recording declined optional cookies through the app and kept the resulting preference in the disposable profile. |
| Network and telemetry | The capture allowed localhost frontend reads and the required read-only recipe and search API routes. It blocked service workers, WebSocket connections, other API requests, and off-origin browser traffic. The capture blocked 22 other local requests and 1 WebSocket connection. Deployment telemetry and logs were outside the browser capture. |
| Artifacts | The saved evidence is one silent tab video and this metadata receipt. The camera, microphone, desktop, other tabs, raw replies, HAR, and application logs were outside the recording. |

## Local monolith log inventory

Scanned stdout, stderr, and the matching process section of the application log for the guest recording run. The console and application log each had 443 INFO entries and 54 explicitly masked assignments. Stderr was empty. The scan found zero matches for credential-shaped authorization values, signed tokens, provider or private keys, credentials in URIs, cookie values, email-like values, signed query credentials, unmasked secret assignments, raw database-command markers, and structured restricted research fields. The scan exercised its matchers with synthetic positive and negative controls and emitted only counts and hashes.

The stdout SHA-256 is `860a552c5bb2a77f33ef64643355e4683646716c0e2b800cc0fec960501ab137`. The matching application log section has text SHA-256 `11c8dea5b82bb00076950d4d095c30596fa8fc1487bf5ba1e656709755c0ab09`. The raw logs stayed local. Deployment logs and telemetry still need the authorized environment.

This verifies the T56 local guest fallback and its scoped T11 inventory. The authorized Act 3 backup still requires the staged graph and authenticated browser walkthrough. Target runtime failure/recovery, physical-device QA, the complete demo privacy inventory, and the timed presenter/listener rehearsal remain open. See the [staging handoff](member-t395-staging-configuration-20261122.md) and [acceptance checklist](member-backlog-checklist-20261104.md#m-e).
