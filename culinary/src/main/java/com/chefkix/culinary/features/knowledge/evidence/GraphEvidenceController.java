package com.chefkix.culinary.features.knowledge.evidence;

import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.Request;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.Response;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.UnavailableReason;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.ProviderResult;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.IdentityResolution;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.IdentityStatus;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.Calibration;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.CalibrationStatus;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.Decision;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.DecisionResult;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.SnapshotRequest;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.SnapshotResult;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.SnapshotResponse;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.SnapshotUnavailableReason;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.TrainPairEvidence;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Additive v1 shadow endpoint. It never calls the legacy seed graph or suggestion service. */
@RestController
public class GraphEvidenceController {
    // application.yml supplies the external /api/v1 servlet context path.
    public static final String ROUTE = "/graph/evidence";
    public static final String SNAPSHOT_ROUTE = "/graph/evidence/snapshot";

    private final GraphEvidenceProperties properties;
    private final ObjectProvider<GraphEvidenceProvider> provider;

    public GraphEvidenceController(
            GraphEvidenceProperties properties,
            ObjectProvider<GraphEvidenceProvider> provider) {
        this.properties = properties;
        this.provider = provider;
    }

    @GetMapping(ROUTE)
    public ResponseEntity<Response> getEvidence(
            @RequestParam("sourceIngredientId") String sourceIngredientId,
            @RequestParam("candidateIngredientId") String candidateIngredientId) {
        if (!properties.isEnabled()) {
            return unavailable(UnavailableReason.FEATURE_DISABLED);
        }

        try {
            GraphEvidenceProvider evidenceProvider = provider.getIfAvailable();
            if (evidenceProvider == null) {
                return unavailable(UnavailableReason.PROVIDER_NOT_REGISTERED);
            }
            ProviderResult result = evidenceProvider.findEvidence(
                    new Request(sourceIngredientId, candidateIngredientId));
            if (result == null) {
                return unavailable(UnavailableReason.ARTIFACT_MISSING);
            }
            if (result.artifactIntegrity() == null) {
                return unavailable(UnavailableReason.ARTIFACT_MISSING);
            }
            if (result.artifactIntegrity().status() == GraphEvidenceContractV1.ArtifactIntegrityStatus.MISSING) {
                return unavailable(UnavailableReason.ARTIFACT_MISSING);
            }
            if (!result.artifactIntegrity().isVerified()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Response.refused(result.artifactIntegrity()));
            }
            if (result.evidence() == null) {
                return unavailable(UnavailableReason.PROVIDER_RESULT_INVALID);
            }
            IdentityResolution resolution = result.evidence().identityResolution();
            if (resolution == null || resolution.status() == null) {
                return unavailable(UnavailableReason.PROVIDER_RESULT_INVALID);
            }
            if (resolution.status() == IdentityStatus.UNKNOWN) {
                return unavailable(UnavailableReason.IDENTITY_UNKNOWN, resolution);
            }
            if (resolution.status() == IdentityStatus.AMBIGUOUS) {
                return unavailable(UnavailableReason.IDENTITY_AMBIGUOUS, resolution);
            }
            if (resolution.sourceIngredientId() == null || resolution.candidateIngredientId() == null
                    || !sourceIngredientId.equals(resolution.requestedSourceIngredientId())
                    || !candidateIngredientId.equals(resolution.requestedCandidateIngredientId())
                    || !sourceIngredientId.equals(resolution.sourceIngredientId())
                    || !candidateIngredientId.equals(resolution.candidateIngredientId())) {
                return unavailable(UnavailableReason.PROVIDER_RESULT_INVALID, resolution);
            }
            TrainPairEvidence trainPair = result.evidence().trainPairEvidence();
            if (trainPair == null || trainPair.trainCount() == null || trainPair.seenInTrain() == null) {
                return unavailable(UnavailableReason.TRAIN_PAIR_EVIDENCE_UNAVAILABLE, resolution);
            }
            if (!GraphEvidenceContractV1.TRAIN_PAIR_UNIT.equals(trainPair.unit())) {
                return unavailable(UnavailableReason.PROVIDER_RESULT_INVALID, resolution);
            }
            Calibration calibration = result.evidence().calibration();
            if (calibration == null || calibration.status() == null
                    || calibration.status() == CalibrationStatus.UNAVAILABLE
                    || calibration.status() == CalibrationStatus.NOT_APPLICABLE) {
                return unavailable(UnavailableReason.CALIBRATION_UNAVAILABLE, resolution);
            }
            DecisionResult decisionResult = result.evidence().decision();
            if (decisionResult == null || decisionResult.decision() == null
                    // This graph-only shadow endpoint reports evidence, never a
                    // substitution recommendation. A future calibrated decision
                    // belongs to the separately reviewed substitution route.
                    || decisionResult.decision() != Decision.ABSTAIN) {
                return unavailable(UnavailableReason.PROVIDER_RESULT_INVALID, resolution);
            }
            return ResponseEntity.ok(Response.available(result));
        } catch (RuntimeException ignored) {
            // Do not leak provider internals or substitute legacy graph data on failure.
            return unavailable(UnavailableReason.PROVIDER_ERROR);
        }
    }

    @GetMapping(SNAPSHOT_ROUTE)
    public ResponseEntity<SnapshotResponse> getSnapshot(
            @RequestParam("centerIngredientId") String centerIngredientId,
            @RequestParam(defaultValue = "1") int depth,
            @RequestParam(defaultValue = "100") int limit) {
        if (depth < 0 || depth > 2 || limit < 1 || limit > 500
                || centerIngredientId == null || !centerIngredientId.matches("0|[1-9][0-9]*")) {
            return ResponseEntity.badRequest()
                    .body(SnapshotResponse.unavailable(SnapshotUnavailableReason.INVALID_QUERY));
        }
        if (!properties.isEnabled()) {
            return snapshotUnavailable(SnapshotUnavailableReason.FEATURE_DISABLED);
        }
        try {
            GraphEvidenceProvider evidenceProvider = provider.getIfAvailable();
            if (evidenceProvider == null) {
                return snapshotUnavailable(SnapshotUnavailableReason.PROVIDER_NOT_REGISTERED);
            }
            SnapshotResult result = evidenceProvider.getSnapshot(
                    new SnapshotRequest(centerIngredientId, depth, limit));
            if (result == null || result.artifactIntegrity() == null) {
                return snapshotUnavailable(SnapshotUnavailableReason.ARTIFACT_MISSING);
            }
            if (result.artifactIntegrity().status() == GraphEvidenceContractV1.ArtifactIntegrityStatus.MISSING) {
                return snapshotUnavailable(SnapshotUnavailableReason.ARTIFACT_MISSING);
            }
            if (!integrityMatchesConfiguredPin(result.artifactIntegrity())) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(SnapshotResponse.refused(result.artifactIntegrity()));
            }
            if (result.center() == null || result.center().status() == null) {
                return snapshotUnavailable(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID);
            }
            if (result.center().status() == IdentityStatus.UNKNOWN) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(SnapshotResponse.unavailable(
                        SnapshotUnavailableReason.CENTER_IDENTITY_UNKNOWN, result.artifactIntegrity(), result.center()));
            }
            if (result.center().status() != IdentityStatus.RESOLVED
                    || !centerIngredientId.equals(result.center().requestedId())
                    || !centerIngredientId.equals(result.center().canonicalId())
                    || result.snapshot() == null || result.snapshot().stats() == null
                    || result.snapshot().provenance() == null
                    || result.snapshot().c3CellStatus() != GraphEvidenceContractV1.C3CellStatus.NOT_SEPARATELY_PINNED
                    || result.snapshot().stats().returnedNodes() != result.snapshot().nodes().size()
                    || result.snapshot().stats().returnedEdges() != result.snapshot().edges().size()
                    || result.snapshot().stats().returnedNodes() > limit
                    || result.snapshot().stats().returnedEdges() > limit
                    || result.snapshot().stats().totalNodes() < result.snapshot().stats().returnedNodes()
                    || result.snapshot().stats().totalNodes() < 1
                    || result.snapshot().stats().totalEdges() < result.snapshot().stats().returnedEdges()
                    || result.snapshot().stats().totalEdges() < 0
                    || result.snapshot().stats().depth() != depth
                    || result.snapshot().stats().limit() != limit
                    || !validSnapshot(result.snapshot(), centerIngredientId, result.artifactIntegrity())) {
                return snapshotUnavailable(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID);
            }
            return ResponseEntity.ok(SnapshotResponse.available(result));
        } catch (RuntimeException ignored) {
            return snapshotUnavailable(SnapshotUnavailableReason.PROVIDER_ERROR);
        }
    }

    private static ResponseEntity<Response> unavailable(UnavailableReason reason) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Response.unavailable(reason));
    }

    private static ResponseEntity<Response> unavailable(
            UnavailableReason reason, IdentityResolution resolution) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Response.unavailable(reason, resolution));
    }

    private static ResponseEntity<SnapshotResponse> snapshotUnavailable(SnapshotUnavailableReason reason) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(SnapshotResponse.unavailable(reason));
    }

    private boolean integrityMatchesConfiguredPin(GraphEvidenceContractV1.ArtifactIntegrity integrity) {
        String configuredPin = properties.getExpectedManifestSha256();
        return integrity != null && integrity.isVerified() && configuredPin != null
                && configuredPin.matches("(?i)[0-9a-f]{64}")
                && configuredPin.equalsIgnoreCase(integrity.expectedSha256())
                && configuredPin.equalsIgnoreCase(integrity.observedSha256());
    }

    private static boolean validSnapshot(
            GraphEvidenceContractV1.GraphSnapshot snapshot,
            String centerId,
            GraphEvidenceContractV1.ArtifactIntegrity integrity) {
        GraphEvidenceContractV1.SnapshotProvenance provenance = snapshot.provenance();
        if (snapshot.acceptanceState() == null || snapshot.acceptanceState().isBlank()
                || snapshot.c3CellCaveat() == null || snapshot.c3CellCaveat().isBlank()
                || provenance == null
                || !"recipe1msubs-train".equals(provenance.sourceId())
                || !"UNKNOWN_NOT_REVIEWED".equals(provenance.licenseStatus())
                || !"TRAIN".equals(provenance.split())
                || !snapshot.acceptanceState().equals(provenance.evidenceTier())
                || !validSha256(provenance.manifestSha256())
                || !integrity.expectedSha256().equalsIgnoreCase(provenance.manifestSha256())
                || !integrity.observedSha256().equalsIgnoreCase(provenance.manifestSha256())
                || !validSha256(provenance.sourceManifestSha256())) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (GraphEvidenceContractV1.SnapshotNode node : snapshot.nodes()) {
            if (node == null || node.canonicalId() == null || !node.canonicalId().matches("0|[1-9][0-9]*")
                    || node.displayName() == null || node.displayName().isBlank()
                    || !"resolved".equals(node.identityStatus()) || !"ingredient".equals(node.nodeType())
                    || !List.of("gismo_vocabulary", "gismo_node_catalog").equals(node.sourceRefs())
                    || !ids.add(node.canonicalId())) return false;
        }
        if (!ids.contains(centerId)) return false;
        for (GraphEvidenceContractV1.SnapshotEdge edge : snapshot.edges()) {
            if (edge == null || !ids.contains(edge.sourceId()) || !ids.contains(edge.targetId())
                    || !"substitution".equals(edge.relation()) || !edge.seen()
                    || edge.trainPairCount() <= 0 || edge.distinctContextCount() <= 0
                    || edge.distinctContextCount() > edge.trainPairCount()) return false;
        }
        return true;
    }

    private static boolean validSha256(String value) {
        return value != null && value.matches("(?i)[0-9a-f]{64}");
    }
}
