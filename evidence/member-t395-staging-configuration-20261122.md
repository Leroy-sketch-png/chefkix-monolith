# Member T395 staging configuration handoff

Infrastructure revision f97723608da36f35ceacd083d64f3f8acb9dbb60 publishes the [opt-in Compose overlay](https://github.com/NotTisn/chefkix-infrastructure/blob/f97723608da36f35ceacd083d64f3f8acb9dbb60/compose.staging.t395.yaml) and [Linux manifest preflight](https://github.com/NotTisn/chefkix-infrastructure/blob/f97723608da36f35ceacd083d64f3f8acb9dbb60/scripts/verify-t395-bundle.sh) for the [Lead-selected graph_eval_v4_clean bundle](iron-chef-lead-member-handoff-20261105/l46-scan-return-live-check-20261116.md#l46t395-graph-evidence-consumer-handoff--2026-11-20).

The overlay adds one read-only existing-directory bind to the monolith at /opt/chefkix/graph_eval_v4_clean and sets GRAPH_EVIDENCE_BUNDLE_PATH and GRAPH_EVIDENCE_EXPECTED_MANIFEST_SHA256. The manifest pin is ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1. Staging image, authentication and feature settings retain their existing definitions. The separate accepted M-A preflight and provisional preview keep their own bindings.

## Verified locally

- Docker Compose 2.39.2 rendered both base staging and opt-in staging using the published placeholder environment. Removing the one mount and its two environment entries from the opt-in render produced the same base configuration. The opt-in render rejected an unset host-path variable.
- A disposable network-isolated container read an existing synthetic fixture through the overlay and could not write to the bind. Windows Docker Desktop created an empty directory for the missing synthetic path despite the Compose request, so presence and exact manifest hash must be checked before applying the overlay.
- In a network-isolated Linux shell, the preflight rejected unset and relative paths, a missing manifest, and an incorrect manifest digest. No staging bundle bytes, deployment credentials, or authenticated staging responses were used for these checks.

## Explorer contract verification

Frontend `6d8513cd6b98b95fe21960e4b7d819763e3ddaf5` sends `centerIngredientId=1&depth=0&limit=1`. The canonical center comes from the existing clean-artifact integration test. The client requires `graph-evidence-snapshot.v1`, a matching resolved center, and the same valid SHA-256 in expected, observed, and snapshot provenance. The footer shows the evidence artifact hash and nested aggregate counts separately from the preview graph. Mock mode omits the evidence footer.

Controlled Chromium checks showed the full footer hash for a verified response, kept it hidden for a provenance mismatch and HTTP 503, and displayed it again when reopening the route with a healthy response. The graph preview remained available throughout. Frontend suites passed 93 tests with one opt-in historical replay skipped. The monolith controller and artifact round-trip suites passed all 28 tests. The production Docker build completed with both graph flags enabled. The built image labels are true and the network-isolated container served the enabled Explorer route. The four controlled Chromium cases also passed against this production image. Its local image ID is sha256:3c8550fe0db05a8261db1a3eddc59324a11919ce60976b8f13e68564fd32e358.

## Remaining owner operation

On the authorized host, export T395_GRAPH_EVIDENCE_HOST_PATH for the original existing directory. Run the manifest preflight and apply the optional overlay with the existing staging files only after it passes. Build the immutable frontend image from the published revision with the existing approved origin arguments and `--build-arg NEXT_PUBLIC_GRAPH_EXPLORER_ENABLED=true --build-arg NEXT_PUBLIC_GRAPH_EVIDENCE_ENABLED=true`. Both graph flags default to false. Verify the image labels `com.chefkix.graph-explorer-enabled` and `com.chefkix.graph-evidence-enabled` are true for this opt-in image. Use the existing non-mock frontend configuration and the existing authenticated session.

Verify HTTP 200 and VERIFIED for the authenticated snapshot with the same bounded query. Confirm that expectedSha256, observedSha256, and snapshot.provenance.manifestSha256 equal the exact pin. Capture the full hash from the Evidence artifact footer and the deployed revisions without publishing graph rows or credentials.

The configuration is ready for the deployment owner. The actual mount, authenticated API/Explorer continuity, and T03/T52/C5 acceptance remain open.
