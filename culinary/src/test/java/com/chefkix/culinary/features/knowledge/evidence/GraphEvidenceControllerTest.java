package com.chefkix.culinary.features.knowledge.evidence;

import com.chefkix.culinary.features.knowledge.controller.KnowledgeGraphController;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphEvidenceControllerTest {
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Test
    void defaultPropertiesAreDisabledAndEndpointReturnsTypedUnavailableWithoutCallingProvider() {
        GraphEvidenceProperties properties = new GraphEvidenceProperties();
        ObjectProvider<GraphEvidenceProvider> providers = mock(ObjectProvider.class);
        GraphEvidenceController controller = new GraphEvidenceController(properties, providers);

        var response = controller.getEvidence("ingredient-a", "ingredient-b");

        assertFalse(properties.isEnabled());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(ResponseStatus.UNAVAILABLE, response.getBody().status());
        assertEquals(UnavailableReason.FEATURE_DISABLED, response.getBody().unavailableReason());
        assertEquals(GraphEvidenceContractV1.VERSION, response.getBody().contractVersion());
        verifyNoInteractions(providers);
    }

    @Test
    void snapshotRejectsInvalidBoundsBeforeProviderAndRemainsDisabledByDefault() {
        ObjectProvider<GraphEvidenceProvider> providers = mock(ObjectProvider.class);
        GraphEvidenceController controller = new GraphEvidenceController(new GraphEvidenceProperties(), providers);

        var invalid = controller.getSnapshot("1", 3, 100);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        assertEquals(SnapshotUnavailableReason.INVALID_QUERY, invalid.getBody().unavailableReason());
        assertEquals(GraphEvidenceContractV1.SNAPSHOT_VERSION, invalid.getBody().contractVersion());

        var invalidCenter = controller.getSnapshot("01", 1, 100);
        assertEquals(HttpStatus.BAD_REQUEST, invalidCenter.getStatusCode());
        assertEquals(SnapshotUnavailableReason.INVALID_QUERY, invalidCenter.getBody().unavailableReason());

        var disabled = controller.getSnapshot("1", 1, 100);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, disabled.getStatusCode());
        assertEquals(SnapshotUnavailableReason.FEATURE_DISABLED, disabled.getBody().unavailableReason());
        verifyNoInteractions(providers);
    }

    @Test
    void snapshotRouteReturnsOnlyPinnedAggregateGraphAndExplicitlyUnpinnedC3Status() {
        GraphSnapshot snapshot = validSnapshot(new SnapshotStats(2, 1, 1, 0, 0, 10, true),
                new SnapshotProvenance("recipe1msubs-train", "UNKNOWN_NOT_REVIEWED", "TRAIN",
                        "PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED", HASH_B, HASH_A));

        var response = controllerWith(snapshotProvider(snapshot), HASH_B).getSnapshot("1", 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SnapshotStatus.AVAILABLE, response.getBody().status());
        assertEquals(C3CellStatus.NOT_SEPARATELY_PINNED, response.getBody().snapshot().c3CellStatus());
        assertEquals(1, response.getBody().snapshot().stats().returnedNodes());
        String json = assertDoesNotThrow(() -> new ObjectMapper().writeValueAsString(response.getBody()));
        assertFalse(json.contains("source_row_key"));
        assertFalse(json.contains("evidence_id"));
        assertFalse(json.contains("artifactSha256"));
        assertTrue(json.contains("sourceManifestSha256"));
        assertFalse(json.contains("confidencePercentage"));
    }

    @Test
    void snapshotRejectsProviderTotalsBelowReturnedInventory() {
        GraphSnapshot snapshot = validSnapshot(new SnapshotStats(0, 0, 1, 0, 0, 10, false),
                validSnapshotProvenance());

        var response = controllerWith(snapshotProvider(snapshot), HASH_B).getSnapshot("1", 0, 10);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().snapshot());
    }

    @Test
    void snapshotRejectsMalformedSourceManifestHash() {
        GraphSnapshot snapshot = validSnapshot(new SnapshotStats(2, 1, 1, 0, 0, 10, true),
                new SnapshotProvenance("recipe1msubs-train", "UNKNOWN_NOT_REVIEWED", "TRAIN",
                        "PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED", HASH_B, "not-a-sha256"));

        var response = controllerWith(snapshotProvider(snapshot), HASH_B).getSnapshot("1", 0, 10);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().snapshot());
    }

    @Test
    void snapshotRejectsValidButMismatchedBundleManifestProvenanceHash() {
        GraphSnapshot snapshot = validSnapshot(new SnapshotStats(2, 1, 1, 0, 0, 10, true),
                new SnapshotProvenance("recipe1msubs-train", "UNKNOWN_NOT_REVIEWED", "TRAIN",
                        "PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED", HASH_A, HASH_A));

        var response = controllerWith(snapshotProvider(snapshot), HASH_B).getSnapshot("1", 0, 10);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().snapshot());
    }

    @Test
    void enabledWithoutProviderFailsClosedRatherThanUsingLegacySeedGraph() {
        GraphEvidenceProperties properties = enabledProperties();
        ObjectProvider<GraphEvidenceProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(null);

        var response = new GraphEvidenceController(properties, providers)
                .getEvidence("ingredient-a", "ingredient-b");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.PROVIDER_NOT_REGISTERED, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
        verify(providers).getIfAvailable();
        assertNoLegacyServiceDependency();
    }

    @Test
    void missingArtifactReturnsUnavailable() {
        GraphEvidenceProvider provider = request -> null;
        var response = controllerWith(provider).getEvidence("ingredient-a", "ingredient-b");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.ARTIFACT_MISSING, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
    }

    @Test
    void mismatchedOrMalformedArtifactHashIsRefused() {
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_B), sampleEvidence());
        var response = controllerWith(provider).getEvidence("ingredient-a", "ingredient-b");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(ResponseStatus.REFUSED, response.getBody().status());
        assertEquals(UnavailableReason.ARTIFACT_UNVERIFIED, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
    }

    @Test
    void verifiedArtifactWithNullEvidenceIsProviderResultFailureNotIntegrityFailure() {
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), null);
        var response = controllerWith(provider).getEvidence("ingredient-a", "ingredient-b");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(ResponseStatus.UNAVAILABLE, response.getBody().status());
        assertEquals(UnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().artifactIntegrity());
    }

    @Test
    void trainPairSeenFlagIsDerivedFromNonnegativeCount() {
        assertFalse(new TrainPairEvidence(true, 0L, "TRAIN_PAIRS").seenInTrain());
        assertTrue(new TrainPairEvidence(false, 4L, "TRAIN_PAIRS").seenInTrain());
        assertNull(new TrainPairEvidence(true, null, "TRAIN_PAIRS").seenInTrain());
        assertThrows(IllegalArgumentException.class,
                () -> new TrainPairEvidence(false, -1L, "TRAIN_PAIRS"));
    }

    @Test
    void missingTrainPairCountReturnsTypedUnavailable() {
        Evidence evidence = evidenceWithTrainPair(null);
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);
        var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.TRAIN_PAIR_EVIDENCE_UNAVAILABLE, response.getBody().unavailableReason());
        assertEquals(IdentityStatus.RESOLVED, response.getBody().identityResolution().status());
    }

    @Test
    void trainPairCountMustDeclareTheTrainPairUnit() {
        for (String unit : new String[]{null, "PAIR_ROWS", ""}) {
            Evidence evidence = evidenceWithTrainPair(new TrainPairEvidence(true, 3L, unit));
            GraphEvidenceProvider provider = request -> new ProviderResult(
                    new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);

            var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals(UnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
            assertNull(response.getBody().evidence());
        }
    }

    @Test
    void frozenCalibrationStatusesRemainDistinctAndGraphEvidenceAlwaysAbstains() throws Exception {
        for (CalibrationStatus status : List.of(
                CalibrationStatus.OK,
                CalibrationStatus.UNINFORMATIVE,
                CalibrationStatus.VACUOUS,
                CalibrationStatus.NO_GUARANTEE,
                CalibrationStatus.ABSTAIN)) {
            Evidence evidence = evidenceWithCalibration(status, Decision.ABSTAIN);
            GraphEvidenceProvider provider = request -> new ProviderResult(
                    new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);
            var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(status, response.getBody().evidence().calibration().status());
            assertEquals(Decision.ABSTAIN, response.getBody().evidence().decision().decision());
            String json = new ObjectMapper().writeValueAsString(response.getBody());
            assertEquals(status.wireValue(), new ObjectMapper().readTree(json)
                    .path("evidence").path("calibration").path("status").asText());
        }

        Evidence show = evidenceWithCalibration(CalibrationStatus.OK, Decision.SHOW);
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), show);
        var response = controllerWith(provider).getEvidence("source-x", "candidate-y");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
    }

    @Test
    void unavailableCalibrationUsesUnavailableReasonAndNonOkShowIsRejected() {
        for (CalibrationStatus status : List.of(
                CalibrationStatus.UNAVAILABLE, CalibrationStatus.NOT_APPLICABLE)) {
            Evidence evidence = evidenceWithCalibration(status, Decision.ABSTAIN);
            GraphEvidenceProvider provider = request -> new ProviderResult(
                    new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);
            var response = controllerWith(provider).getEvidence("source-x", "candidate-y");
            assertEquals(UnavailableReason.CALIBRATION_UNAVAILABLE, response.getBody().unavailableReason());
        }

        Evidence invalid = evidenceWithCalibration(CalibrationStatus.NO_GUARANTEE, Decision.SHOW);
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), invalid);
        var response = controllerWith(provider).getEvidence("source-x", "candidate-y");
        assertEquals(UnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
    }

    @Test
    void nullCalibrationAndNullCalibrationStatusReturnTypedUnavailable() {
        for (Evidence evidence : List.of(
                evidenceWithCalibration((Calibration) null), evidenceWithNullCalibrationStatus())) {
            GraphEvidenceProvider provider = request -> new ProviderResult(
                    new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);
            var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals(ResponseStatus.UNAVAILABLE, response.getBody().status());
            assertEquals(UnavailableReason.CALIBRATION_UNAVAILABLE, response.getBody().unavailableReason());
            assertNull(response.getBody().evidence());
        }
    }

    @Test
    void verifiedSyntheticProviderProducesExplicitVersionedFields() {
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), sampleEvidence());
        var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.AVAILABLE, response.getBody().status());
        assertEquals(GraphEvidenceContractV1.VERSION, response.getBody().contractVersion());
        Evidence evidence = response.getBody().evidence();
        assertEquals(IdentityStatus.RESOLVED, evidence.identityResolution().status());
        assertEquals("source-x", evidence.identityResolution().requestedSourceIngredientId());
        assertEquals("candidate-y", evidence.identityResolution().requestedCandidateIngredientId());
        assertTrue(evidence.trainPairEvidence().seenInTrain());
        assertEquals(3L, evidence.trainPairEvidence().trainCount());
        assertEquals("TRAIN_PAIRS", evidence.trainPairEvidence().unit());
        assertEquals("UNKNOWN", evidence.provenance().getFirst().licenseStatus());
        assertEquals(AvailabilityStatus.NOT_IN_ARTIFACT, evidence.nutrientAvailability().status());
        assertEquals(ScreeningStatus.UNAVAILABLE, evidence.declaredGroupScreening().status());
        assertEquals(CalibrationStatus.OK, evidence.calibration().status());
        assertEquals(Decision.ABSTAIN, evidence.decision().decision());
        String json = assertDoesNotThrow(() -> new ObjectMapper().writeValueAsString(response.getBody()));
        assertFalse(json.contains("confidencePercentage"));
    }

    @Test
    void unknownAndAmbiguousIdentitiesAreNotReturnedAsEvidence() {
        for (IdentityStatus identityStatus : List.of(IdentityStatus.UNKNOWN, IdentityStatus.AMBIGUOUS)) {
            Evidence evidence = evidenceWithIdentityStatus(identityStatus);
            GraphEvidenceProvider provider = request -> new ProviderResult(
                    new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);
            var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals(ResponseStatus.UNAVAILABLE, response.getBody().status());
            assertEquals(identityStatus == IdentityStatus.UNKNOWN
                            ? UnavailableReason.IDENTITY_UNKNOWN : UnavailableReason.IDENTITY_AMBIGUOUS,
                    response.getBody().unavailableReason());
            assertEquals(identityStatus, response.getBody().identityResolution().status());
            assertNull(response.getBody().evidence());
        }
    }

    @Test
    void providerExceptionDetailsDoNotLeakIntoTheResponse() throws Exception {
        String secretSentinel = "CHEFKIX_T11_SECRET_SENTINEL_20261023";
        GraphEvidenceProvider provider = request -> { throw new IllegalStateException(secretSentinel); };
        var response = controllerWith(provider).getEvidence("ingredient-a", "ingredient-b");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.PROVIDER_ERROR, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
        String responseJson = new ObjectMapper().writeValueAsString(response.getBody());
        assertFalse(responseJson.contains(secretSentinel));
        assertFalse(responseJson.contains("IllegalStateException"));
    }

    @Test
    void ambiguousProviderRegistryReturnsTypedUnavailable() {
        ObjectProvider<GraphEvidenceProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenThrow(new IllegalStateException("synthetic ambiguity"));
        var response = new GraphEvidenceController(enabledProperties(), providers)
                .getEvidence("ingredient-a", "ingredient-b");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.PROVIDER_ERROR, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
    }

    @Test
    void responseForDifferentProviderRequestIsProviderResultFailure() {
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), sampleEvidence());
        var response = controllerWith(provider).getEvidence("other-source", "candidate-y");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(ResponseStatus.UNAVAILABLE, response.getBody().status());
        assertEquals(UnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
    }

    @Test
    void resolvedCanonicalIdsMustMatchTheRequestedPair() {
        Evidence original = sampleEvidence();
        IdentityResolution mismatched = new IdentityResolution(
                IdentityStatus.RESOLVED, "source-x", "different-source", List.of(),
                "candidate-y", "different-candidate", List.of());
        Evidence evidence = new Evidence(mismatched, original.trainPairEvidence(), original.provenance(),
                original.compoundAvailability(), original.nutrientAvailability(), original.declaredGroupScreening(),
                original.calibration(), original.decision());
        GraphEvidenceProvider provider = request -> new ProviderResult(
                new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_A, HASH_A), evidence);

        var response = controllerWith(provider).getEvidence("source-x", "candidate-y");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(UnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().evidence());
    }

    @Test
    void evidenceRouteIsAdditiveAndSeparateFromLegacyKnowledgeGraphRoute() throws Exception {
        GetMapping evidenceMapping = GraphEvidenceController.class
                .getMethod("getEvidence", String.class, String.class).getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/graph/evidence"}, evidenceMapping.value());
        GetMapping snapshotMapping = GraphEvidenceController.class
                .getMethod("getSnapshot", String.class, int.class, int.class).getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/graph/evidence/snapshot"}, snapshotMapping.value());

        RequestMapping legacyPrefix = KnowledgeGraphController.class.getAnnotation(RequestMapping.class);
        GetMapping legacyGraph = KnowledgeGraphController.class
                .getMethod("getGraph", String.class, String.class, int.class, int.class)
                .getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/knowledge"}, legacyPrefix.value());
        assertArrayEquals(new String[]{"/graph"}, legacyGraph.value());
        assertNotEquals(GraphEvidenceController.ROUTE,
                legacyPrefix.value()[0] + legacyGraph.value()[0]);
        assertNoLegacyServiceDependency();
    }

    private static GraphEvidenceController controllerWith(GraphEvidenceProvider provider) {
        ObjectProvider<GraphEvidenceProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(provider);
        return new GraphEvidenceController(enabledProperties(), providers);
    }

    private static GraphEvidenceController controllerWith(GraphEvidenceProvider provider, String expectedManifestHash) {
        ObjectProvider<GraphEvidenceProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(provider);
        GraphEvidenceProperties properties = enabledProperties();
        properties.setExpectedManifestSha256(expectedManifestHash);
        return new GraphEvidenceController(properties, providers);
    }

    private static GraphEvidenceProvider snapshotProvider(GraphSnapshot snapshot) {
        return new GraphEvidenceProvider() {
            @Override
            public ProviderResult findEvidence(Request request) {
                return null;
            }

            @Override
            public SnapshotResult getSnapshot(SnapshotRequest request) {
                return new SnapshotResult(new ArtifactIntegrity(ArtifactIntegrityStatus.VERIFIED, HASH_B, HASH_B),
                        new SnapshotCenter(IdentityStatus.RESOLVED, request.centerIngredientId(),
                                request.centerIngredientId()), snapshot);
            }
        };
    }

    @Test
    void snapshotRejectsNodeMetadataOutsideManifestBackedVocabularyContract() {
        List<SnapshotNode> invalidNodes = List.of(
                new SnapshotNode("1", "one", "guessed", "ingredient", List.of("gismo_vocabulary", "gismo_node_catalog")),
                new SnapshotNode("1", "one", "resolved", "dish", List.of("gismo_vocabulary", "gismo_node_catalog")),
                new SnapshotNode("1", "one", "resolved", "ingredient", List.of("unverified_alias")),
                new SnapshotNode("1", " ", "resolved", "ingredient", List.of("gismo_vocabulary", "gismo_node_catalog")));

        for (SnapshotNode node : invalidNodes) {
            GraphSnapshot snapshot = new GraphSnapshot("PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED",
                    List.of(node), List.of(), new SnapshotStats(1, 0, 1, 0, 0, 10, false),
                    validSnapshotProvenance(), C3CellStatus.NOT_SEPARATELY_PINNED, "No C3 artifact is pinned.");
            var response = controllerWith(snapshotProvider(snapshot), HASH_B).getSnapshot("1", 0, 10);
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
            assertNull(response.getBody().snapshot());
        }
    }

    @Test
    void snapshotRejectsZeroCountEdgeInconsistentWithVerifiedArtifactContract() {
        GraphSnapshot snapshot = new GraphSnapshot("PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED",
                List.of(new SnapshotNode("1", "one", "resolved", "ingredient", List.of("gismo_vocabulary", "gismo_node_catalog"))),
                List.of(new SnapshotEdge("1", "1", "substitution", true, 0, 0)),
                new SnapshotStats(1, 1, 1, 1, 0, 10, false), validSnapshotProvenance(),
                C3CellStatus.NOT_SEPARATELY_PINNED, "No C3 artifact is pinned.");

        var response = controllerWith(snapshotProvider(snapshot), HASH_B).getSnapshot("1", 0, 10);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(SnapshotUnavailableReason.PROVIDER_RESULT_INVALID, response.getBody().unavailableReason());
        assertNull(response.getBody().snapshot());
    }

    private static GraphSnapshot validSnapshot(SnapshotStats stats, SnapshotProvenance provenance) {
        return new GraphSnapshot("PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED",
                List.of(new SnapshotNode("1", "one", "resolved", "ingredient",
                        List.of("gismo_vocabulary", "gismo_node_catalog"))),
                List.of(), stats, provenance, C3CellStatus.NOT_SEPARATELY_PINNED, "No C3 artifact is pinned.");
    }

    private static SnapshotProvenance validSnapshotProvenance() {
        return new SnapshotProvenance("recipe1msubs-train", "UNKNOWN_NOT_REVIEWED", "TRAIN",
                "PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED", HASH_B, HASH_A);
    }

    private static GraphEvidenceProperties enabledProperties() {
        GraphEvidenceProperties properties = new GraphEvidenceProperties();
        properties.setEnabled(true);
        return properties;
    }

    private static Evidence sampleEvidence() {
        return new Evidence(
                new IdentityResolution(IdentityStatus.RESOLVED, "source-x", "source-x", List.of(),
                        "candidate-y", "candidate-y", List.of()),
                new TrainPairEvidence(true, 3L, "TRAIN_PAIRS"),
                List.of(new Provenance("synthetic-fixture", "UNKNOWN", "TRAIN", "SYNTHETIC",
                        HASH_A, HASH_A)),
                new Availability(AvailabilityStatus.AVAILABLE, 2L),
                new Availability(AvailabilityStatus.NOT_IN_ARTIFACT, null),
                new DeclaredGroupScreening(ScreeningStatus.UNAVAILABLE,
                        "Declared-group evidence is not an allergen safety or clinical claim."),
                new Calibration("arm-a", 0.2, CalibrationStatus.OK, 3.0),
                new DecisionResult(Decision.ABSTAIN, "Synthetic fixture has no approved decision rule."));
    }

    private static Evidence evidenceWithIdentityStatus(IdentityStatus status) {
        Evidence evidence = sampleEvidence();
        IdentityResolution identity = evidence.identityResolution();
        return new Evidence(
                new IdentityResolution(status, identity.requestedSourceIngredientId(),
                        status == IdentityStatus.RESOLVED ? identity.sourceIngredientId() : null,
                        status == IdentityStatus.AMBIGUOUS ? List.of("source-x-1", "source-x-2") : List.of(),
                        identity.requestedCandidateIngredientId(),
                        status == IdentityStatus.RESOLVED ? identity.candidateIngredientId() : null,
                        status == IdentityStatus.AMBIGUOUS ? List.of("candidate-y-1", "candidate-y-2") : List.of()),
                evidence.trainPairEvidence(), evidence.provenance(), evidence.compoundAvailability(),
                evidence.nutrientAvailability(), evidence.declaredGroupScreening(), evidence.calibration(),
                evidence.decision());
    }

    private static Evidence evidenceWithTrainPair(TrainPairEvidence trainPairEvidence) {
        Evidence evidence = sampleEvidence();
        return new Evidence(evidence.identityResolution(), trainPairEvidence, evidence.provenance(),
                evidence.compoundAvailability(), evidence.nutrientAvailability(),
                evidence.declaredGroupScreening(), evidence.calibration(), evidence.decision());
    }

    private static Evidence evidenceWithCalibration(CalibrationStatus status, Decision decision) {
        Evidence evidence = sampleEvidence();
        return new Evidence(evidence.identityResolution(), evidence.trainPairEvidence(), evidence.provenance(),
                evidence.compoundAvailability(), evidence.nutrientAvailability(),
                evidence.declaredGroupScreening(), new Calibration("arm-a", 0.2, status, 3.0),
                new DecisionResult(decision, "synthetic fixture"));
    }

    private static Evidence evidenceWithCalibration(Calibration calibration) {
        Evidence evidence = sampleEvidence();
        return new Evidence(evidence.identityResolution(), evidence.trainPairEvidence(), evidence.provenance(),
                evidence.compoundAvailability(), evidence.nutrientAvailability(),
                evidence.declaredGroupScreening(), calibration, evidence.decision());
    }

    private static Evidence evidenceWithNullCalibrationStatus() {
        return evidenceWithCalibration(new Calibration("arm-a", 0.2, null, 3.0));
    }

    private static void assertNoLegacyServiceDependency() {
        for (Field field : GraphEvidenceController.class.getDeclaredFields()) {
            assertFalse(field.getType().getName().equals(
                    "com.chefkix.culinary.features.knowledge.service.KnowledgeGraphService"));
        }
    }
}
