# Member T395 staging configuration handoff

Infrastructure revision f97723608da36f35ceacd083d64f3f8acb9dbb60 publishes the [opt-in Compose overlay](https://github.com/NotTisn/chefkix-infrastructure/blob/f97723608da36f35ceacd083d64f3f8acb9dbb60/compose.staging.t395.yaml) and [Linux manifest preflight](https://github.com/NotTisn/chefkix-infrastructure/blob/f97723608da36f35ceacd083d64f3f8acb9dbb60/scripts/verify-t395-bundle.sh) for the [Lead-selected graph_eval_v4_clean bundle](iron-chef-lead-member-handoff-20261105/l46-scan-return-live-check-20261116.md#l46t395-graph-evidence-consumer-handoff--2026-11-20).

The overlay adds one read-only existing-directory bind to the monolith at /opt/chefkix/graph_eval_v4_clean and sets GRAPH_EVIDENCE_BUNDLE_PATH and GRAPH_EVIDENCE_EXPECTED_MANIFEST_SHA256. The manifest pin is ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1. Staging image, authentication and feature settings retain their existing definitions. The separate accepted M-A preflight and provisional preview keep their own bindings.

## Verified locally

- Docker Compose 2.39.2 rendered both base staging and opt-in staging using the published placeholder environment. Removing the one mount and its two environment entries from the opt-in render produced the same base configuration. The opt-in render rejected an unset host-path variable.
- A disposable network-isolated container read an existing synthetic fixture through the overlay and could not write to the bind. Windows Docker Desktop created an empty directory for the missing synthetic path despite the Compose request, so presence and exact manifest hash must be checked before applying the overlay.
- In a network-isolated Linux shell, the preflight rejected unset and relative paths, a missing manifest, and an incorrect manifest digest. No staging bundle bytes, deployment credentials, or authenticated staging responses were used for these checks.

## Remaining owner operation

On the authorized host, export T395_GRAPH_EVIDENCE_HOST_PATH for the original existing directory. Run the manifest preflight and apply the optional overlay with the existing staging files only after it passes. Then verify HTTP 200 and VERIFIED from the authenticated snapshot route with expectedSha256, observedSha256, and snapshot.provenance.manifestSha256 equal to the exact pin. Capture the same full hash in Explorer and record the deployed revisions without publishing graph rows or credentials.

The configuration is ready for the deployment owner. The actual mount, authenticated API/Explorer continuity, and T03/T52/C5 acceptance remain open.
