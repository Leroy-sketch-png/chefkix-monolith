# M-C Product Explorer validation and Lead handoff

**Date:** 2026-11-25

**Status:** source and service checks passed; authenticated local browser-to-API verification remains open.

## Source and artifact pins

- FE base commit: `7d34509` (`origin/main` at validation time); the source and test changes listed below are local working-tree edits and are not included in that commit.
- Monolith commit: `764c28ec449714280c1e613e9ceaf47d101aed08`.
- Product package: `evidence/member-foodb-native-output-20261111/`.
- Manifest SHA-256: `a187fe108dc2e716169344f3cb120c881a8e481b923351acfd49881cf5a6a01f`.
- `nodes.jsonl` SHA-256: `ca26f6001a50c0ebe668019541fe392e6aee87cecd78c89b2429c3090ddbac94`.
- `edges.jsonl` SHA-256: `d611e9effb7a0b2b7fd2d5bd9e09d78bb8822681d2955bb1f024cec39a8f403f`.
- Local source archive: `evidence/member-foodb-source-20261114/foodb_2020_04_07_json.zip`; its manifest-pinned SHA-256 is `3a37305b2a46d7fbd41969bdc5f3c9233bd837cde7c6f46e34316d08879a999c`.

## Checks completed

| Check | Result | Scope |
| --- | --- | --- |
| FE focused Jest: product service, product Explorer, Explorer wrapper | 3 suites, 17 tests passed | Includes wrong-Food-ID refusal, pinned-manifest refusal, unknown status, and visible Food/Compound IDs plus manifest/node/edge hashes. FE auth and API calls are mocked. |
| FE TypeScript | Passed | `npm run typecheck -- --pretty false` |
| FE formatting | Passed | Prettier check on the four changed FE files |
| Monolith `FoodbProductGraphServiceTest` | 5 tests passed, 0 skipped | Ran with `-Dchefkix.productGraph.realArtifactTest=true`; loaded the actual local package and source archive and returned a non-empty typed service result. Also covers wrong manifest/output bytes and unknown Food identity. |

The test commands and affected FE paths are recorded in the current working tree. The generated Surefire summary is `culinary/target/surefire-reports/com.chefkix.culinary.features.knowledge.product.FoodbProductGraphServiceTest.txt`.

## Remaining items for LEAD review and provision

**For LEAD to review:**

1. Review the FE diff in `chefkix-fe/src/features/graph-explorer/services/productGraphService.ts`, `components/ProductCompositionExplorer.tsx`, and their two focused test files. The change rejects a response whose Food ID differs from the requested ID and visibly renders source IDs plus the three pinned hashes.
2. Review the test results and the scope limits above. These checks do not extend acceptance to complete graph coverage, ingredient identity bridging, nutrition, allergen/safety, substitution, T-09, or C5.

**For LEAD/the local environment owner to provide or arrange:**

1. An available local Explorer URL and confirmation that the frontend is running from the reviewed FE worktree, with the monolith product endpoint available locally. Supply the FE and monolith runtime revision identifiers (or local image digests) so the browser observation is bound to code.
2. A redacted effective local configuration showing `NEXT_PUBLIC_GRAPH_EXPLORER_ENABLED=true`, `chefkix.knowledge.product.foodb.enabled=true`, `chefkix.knowledge.product.foodb.bundle-path=<local package path>`, `chefkix.knowledge.product.foodb.source-archive-path=<local archive path>`, and `chefkix.knowledge.product.foodb.expected-manifest-sha256=a187fe108dc2e716169344f3cb120c881a8e481b923351acfd49881cf5a6a01f`. Keep absolute local paths private; do not add credentials or private paths to Git.
3. An existing authenticated local browser session for a read-only check, or arrange for the account owner to open/sign in to the local app. Do not send passwords, cookies, access tokens, or other credentials.
4. LEAD's narrow review disposition on the sanitized local receipt once Member captures it.

## Required local runtime receipt

Once the local session is available, capture a sanitized receipt containing:

- Authenticated `GET` search and composition requests for Apple FooDB Food `105`, with HTTP status only and no auth headers or tokens.
- The returned typed Food ID, Compound IDs, `AVAILABLE` status, source/release, exact manifest/node/edge hashes, and the four export-filtered known relations reported for the staging Apple record.
- An out-of-scope Food ID check showing explicit unknown status rather than absence.
- Explorer-visible IDs, source/scope/export disclosure, and hashes; bind the capture to the exact local FE/monolith revisions and package pins above.
- A reference to the existing wrong-manifest and changed-output refusal tests; do not alter the active app's configuration to manufacture a tamper result.

Keep this operation local and noncommercial under the saved manifest's boundary. The staging Apple observation remains a separate receipt and is not evidence for the newer local FE source or this local runtime path.
