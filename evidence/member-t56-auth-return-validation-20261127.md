# Member T56 auth-return follow-through — 2026-11-27

## Scope

Continue the Member T56/T10 mobile sign-in and recipe-preview path using the latest Lead staging artifact. Keep this separate from M-C FooDB product-graph acceptance and from any live account mutation.

## Lead artifact available

The [26 November Lead receipt](iron-chef-lead-member-handoff-20261105/l46-scan-return-live-check-20261116.md) records Funnel HTTPS at https://desktop-buiavs5.tail3c6ec8.ts.net/, a public GET /auth/sign-up returning HTTP 200, and matching nottisn issuer, authorization, token, and JWKS origins. That establishes route/origin alignment only. It does not establish successful credential authentication, an authenticated session, consent persistence, or account creation.

The last documented staging FE image is OCI index sha256:b2608bd9bc37dc05f07a94545853b93c9cac5a4042f37b58f0d7bcc5a0f52d0c, built from FE commit 1eb7a7f. Current FE default is 69d7971, so the Lead receipt does not prove the later auth-intent preservation and browser-test changes are present in staging.

## Member code change

The FE E2E config now launches node .next/standalone/server.js with HOSTNAME=localhost and PORT=3200. The prior command used next start, which Next reports as unsupported for this repository's output: standalone configuration. The new cross-platform scripts/prepare-standalone-e2e.mjs copies public and .next/static into the standalone directory, matching the staging Dockerfile. The npm run test:e2e script and CI now prepare those assets; CI installs Chromium and runs the post-auth return case on the mobile project after its production build.

## Local validation

- npm test -- --runInBand src/constants/auth.test.ts src/components/providers/__tests__/AuthProvider.test.tsx: 2 suites, 20 tests passed.
- npm test -- --runInBand recipe-detail-mobile-actions.test.ts: 1 suite, 3 tests passed.
- npm run typecheck: passed.
- npx playwright test tests/e2e/mobile-phone-moments.spec.ts --list --config=playwright.e2e.config.ts: 8 browser cases discovered across desktop Chromium and mobile emulation.
- node --check scripts/prepare-standalone-e2e.mjs passed; node scripts/prepare-standalone-e2e.mjs and Prettier checks also passed. The asset helper copied public and static output successfully; this did not establish standalone server startup.
- npm run build: Next compiled successfully, completed lint/type checking, and generated all 62 static pages. It exited 1 during standalone packaging with EPERM: operation not permitted, mkdir at .next/standalone/node_modules/@webassemblyjs/wasm-edit/lib. No standalone server.js was produced, so the focused successful fixture sign-in returns to the intended recipe without starting a cook browser case was not executed locally.

No real credentials, account state, signup request, or cooking-session write was used. The passing Jest suites and Playwright discovery validate code/tests only; they do not establish staging authentication or the live product-graph path. The newly wired CI browser case has not run yet.

## Remaining evidence

1. Run the newly wired CI browser case from a clean build environment and retain its sanitized result.
2. Deployment owner supplies the current FE and monolith runtime revisions/digests and redacted effective product-graph settings matching the saved manifest pins.
3. An existing authorized account owner completes sign-in on the current staging image; Member captures the read-only Food 105 result, unknown-ID refusal, provenance hashes, scope/filter counts, and absence of writes.

Do not treat the public signup route as authenticated proof, create an account for this check, or send credentials.
