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
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.TrainPairEvidence;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Additive v1 shadow endpoint. It never calls the legacy seed graph or suggestion service. */
@RestController
public class GraphEvidenceController {
    // application.yml supplies the external /api/v1 servlet context path.
    public static final String ROUTE = "/graph/evidence";

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
                    || !candidateIngredientId.equals(resolution.requestedCandidateIngredientId())) {
                return unavailable(UnavailableReason.PROVIDER_RESULT_INVALID, resolution);
            }
            TrainPairEvidence trainPair = result.evidence().trainPairEvidence();
            if (trainPair == null || trainPair.trainCount() == null || trainPair.seenInTrain() == null) {
                return unavailable(UnavailableReason.TRAIN_PAIR_EVIDENCE_UNAVAILABLE, resolution);
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

    private static ResponseEntity<Response> unavailable(UnavailableReason reason) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Response.unavailable(reason));
    }

    private static ResponseEntity<Response> unavailable(
            UnavailableReason reason, IdentityResolution resolution) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Response.unavailable(reason, resolution));
    }
}
