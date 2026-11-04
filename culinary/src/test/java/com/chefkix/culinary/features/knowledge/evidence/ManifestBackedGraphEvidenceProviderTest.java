package com.chefkix.culinary.features.knowledge.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.*;
import static org.junit.jupiter.api.Assertions.*;

class ManifestBackedGraphEvidenceProviderTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TRAIN = "a".repeat(64);
    private static final String SOURCE_MANIFEST = "b".repeat(64);
    private static final String TRANSFORM = "c".repeat(64);
    private static final String CONTEXT = "d".repeat(64);

    @TempDir
    Path tempDir;

    @Test
    void springProviderBeanIsAbsentByDefaultAndOnlyRegisteredByExplicitOptIn() {
        new ApplicationContextRunner()
                .withUserConfiguration(GraphEvidenceConfiguration.class)
                .run(context -> assertFalse(context.containsBean("manifestBackedGraphEvidenceProvider")));

        new ApplicationContextRunner()
                .withUserConfiguration(GraphEvidenceConfiguration.class)
                .withPropertyValues("chefkix.knowledge.graph-evidence.enabled=true")
                .run(context -> assertFalse(context.containsBean("manifestBackedGraphEvidenceProvider")));

        new ApplicationContextRunner()
                .withUserConfiguration(GraphEvidenceConfiguration.class)
                .withPropertyValues(
                        "chefkix.knowledge.graph-evidence.enabled=true",
                        "chefkix.knowledge.graph-evidence.bundle-path=relative/path",
                        "chefkix.knowledge.graph-evidence.expected-manifest-sha256=" + "a".repeat(64))
                .run(context -> assertFalse(context.containsBean("manifestBackedGraphEvidenceProvider")));

        new ApplicationContextRunner()
                .withUserConfiguration(GraphEvidenceConfiguration.class)
                .withPropertyValues(
                        "chefkix.knowledge.graph-evidence.enabled=true",
                        "chefkix.knowledge.graph-evidence.bundle-path=" + tempDir,
                        "chefkix.knowledge.graph-evidence.expected-manifest-sha256=" + "a".repeat(64))
                .run(context -> {
                    assertTrue(context.containsBean("manifestBackedGraphEvidenceProvider"));
                    assertInstanceOf(ManifestBackedGraphEvidenceProvider.class,
                            context.getBean(GraphEvidenceProvider.class));
                });

        new ApplicationContextRunner()
                .withUserConfiguration(GraphEvidenceConfiguration.class)
                .withPropertyValues(
                        "chefkix.knowledge.graph-evidence.enabled=true",
                        "chefkix.knowledge.graph-evidence.bundle-path=" + tempDir,
                        "chefkix.knowledge.graph-evidence.expected-manifest-sha256=not-a-digest")
                .run(context -> assertFalse(context.containsBean("manifestBackedGraphEvidenceProvider")));
    }

    @Test
    void exactPinnedSnapshotReturnsTrainOnlyPairEvidenceAndExplicitAbstention() throws Exception {
        Path root = writeBundle(tempDir.resolve("valid"), false);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));
        ManifestBackedGraphEvidenceProvider provider = provider(root, pin);

        ProviderResult result = provider.findEvidence(new Request("1", "2"));

        assertTrue(result.artifactIntegrity().isVerified());
        assertEquals(1L, result.evidence().trainPairEvidence().trainCount());
        assertTrue(result.evidence().trainPairEvidence().seenInTrain());
        assertEquals("TRAIN", result.evidence().provenance().getFirst().split());
        assertEquals(TRAIN, result.evidence().provenance().getFirst().artifactSha256());
        assertEquals(SOURCE_MANIFEST, result.evidence().provenance().getFirst().manifestSha256());
        assertEquals(CalibrationStatus.NO_GUARANTEE, result.evidence().calibration().status());
        assertEquals(Decision.ABSTAIN, result.evidence().decision().decision());
        assertEquals(ScreeningStatus.UNAVAILABLE, result.evidence().declaredGroupScreening().status());
        assertNull(result.evidence().calibration().cutoff());
    }

    @Test
    void boundedSnapshotUsesDeterministicCanonicalIdOrderAndOmitsRowEvidenceIds() throws Exception {
        Path root = writeBundle(tempDir.resolve("snapshot"), false);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));
        ManifestBackedGraphEvidenceProvider provider = provider(root, pin);

        SnapshotResult result = provider.getSnapshot(new SnapshotRequest("1", 1, 2));
        SnapshotResult repeated = provider.getSnapshot(new SnapshotRequest("1", 1, 2));

        assertTrue(result.artifactIntegrity().isVerified());
        assertEquals(new SnapshotCenter(IdentityStatus.RESOLVED, "1", "1"), result.center());
        assertEquals(List.of("1", "2"), result.snapshot().nodes().stream().map(SnapshotNode::canonicalId).toList());
        assertEquals(List.of(new SnapshotEdge("1", "2", "substitution", true, 1, 1)),
                result.snapshot().edges());
        assertEquals(new SnapshotStats(2, 1, 2, 1, 1, 2, false), result.snapshot().stats());
        assertEquals(C3CellStatus.NOT_SEPARATELY_PINNED, result.snapshot().c3CellStatus());
        assertEquals(result.artifactIntegrity().observedSha256(), result.snapshot().provenance().manifestSha256());
        assertEquals(SOURCE_MANIFEST, result.snapshot().provenance().sourceManifestSha256());
        assertEquals(result, repeated);
        String json = JSON.writeValueAsString(result);
        assertFalse(json.contains("source_row_key"));
        assertFalse(json.contains("evidence_id"));
    }

    @Test
    void snapshotIsBoundedAndRetainsUnknownCenterWithoutGuessing() throws Exception {
        Path root = writeBundle(tempDir.resolve("snapshot-bounded"), false);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));
        ManifestBackedGraphEvidenceProvider provider = provider(root, pin);

        SnapshotResult bounded = provider.getSnapshot(new SnapshotRequest("1", 1, 1));
        SnapshotResult unknown = provider.getSnapshot(new SnapshotRequest("999", 1, 10));

        assertEquals(List.of("1"), bounded.snapshot().nodes().stream().map(SnapshotNode::canonicalId).toList());
        assertTrue(bounded.snapshot().stats().hasMore());
        assertEquals(1, bounded.snapshot().stats().returnedNodes());
        assertEquals(IdentityStatus.UNKNOWN, unknown.center().status());
        assertNull(unknown.snapshot());
    }

    @Test
    void depthLimitDoesNotReportUnreturnedEdgesBeyondRequestedBfsDepthAsTruncated() throws Exception {
        Path root = writeBundle(tempDir.resolve("snapshot-depth-zero"), false);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));

        SnapshotResult result = provider(root, pin).getSnapshot(new SnapshotRequest("1", 0, 10));

        assertEquals(List.of("1"), result.snapshot().nodes().stream().map(SnapshotNode::canonicalId).toList());
        assertTrue(result.snapshot().edges().isEmpty());
        assertFalse(result.snapshot().stats().hasMore());
        assertEquals(2, result.snapshot().stats().totalNodes());
        assertEquals(1, result.snapshot().stats().totalEdges());
    }

    @Test
    void knownPairAbsentFromArtifactIsAnExplicitZeroNotAnInferredEdge() throws Exception {
        Path root = writeBundle(tempDir.resolve("absent"), false);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));

        ProviderResult result = provider(root, pin).findEvidence(new Request("2", "1"));

        assertTrue(result.artifactIntegrity().isVerified());
        assertEquals(0L, result.evidence().trainPairEvidence().trainCount());
        assertFalse(result.evidence().trainPairEvidence().seenInTrain());
    }

    @Test
    void unknownIdentityIsNotNormalizedOrReturnedAsResolvedEvidence() throws Exception {
        Path root = writeBundle(tempDir.resolve("unknown"), false);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));

        ProviderResult result = provider(root, pin).findEvidence(new Request(" 1", "2"));

        assertTrue(result.artifactIntegrity().isVerified());
        assertEquals(IdentityStatus.UNKNOWN, result.evidence().identityResolution().status());
        assertNull(result.evidence().identityResolution().sourceIngredientId());
        assertNull(result.evidence().trainPairEvidence().trainCount());
    }

    @Test
    void wrongPinAndPostManifestTamperingFailClosed() throws Exception {
        Path wrongPinRoot = writeBundle(tempDir.resolve("wrong-pin"), false);
        String actualPin = sha256(Files.readAllBytes(wrongPinRoot.resolve("manifest.json")));
        ProviderResult wrongPin = provider(wrongPinRoot, "0".repeat(64))
                .findEvidence(new Request("1", "2"));
        assertEquals(ArtifactIntegrityStatus.INVALID, wrongPin.artifactIntegrity().status());
        assertNull(wrongPin.evidence());

        Path tamperedRoot = writeBundle(tempDir.resolve("tampered"), false);
        String pin = sha256(Files.readAllBytes(tamperedRoot.resolve("manifest.json")));
        Files.writeString(tamperedRoot.resolve("edges.jsonl"), "tampered\n");
        ProviderResult tampered = provider(tamperedRoot, pin).findEvidence(new Request("1", "2"));
        assertEquals(ArtifactIntegrityStatus.INVALID, tampered.artifactIntegrity().status());
        assertNull(tampered.evidence());
        assertNotNull(actualPin);
    }

    @Test
    void duplicateOutputJsonKeysAreRejectedEvenWhenTheirBytesAreManifestPinned() throws Exception {
        Path root = writeBundle(tempDir.resolve("duplicate-output-key"), true);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));

        ProviderResult result = provider(root, pin).findEvidence(new Request("1", "2"));

        assertEquals(ArtifactIntegrityStatus.INVALID, result.artifactIntegrity().status());
        assertNull(result.evidence());
    }

    @Test
    void missingConfigurationFailsAsMissingAndIsMemoizedWithoutFallback() {
        GraphEvidenceProperties properties = new GraphEvidenceProperties();
        ManifestBackedGraphEvidenceProvider provider = new ManifestBackedGraphEvidenceProvider(
                properties, new VerifiedGraphArtifactLoader());

        ProviderResult first = provider.findEvidence(new Request("1", "2"));
        properties.setBundlePath(tempDir);
        properties.setExpectedManifestSha256("a".repeat(64));
        ProviderResult second = provider.findEvidence(new Request("1", "2"));

        assertEquals(ArtifactIntegrityStatus.MISSING, first.artifactIntegrity().status());
        assertNull(first.evidence());
        assertEquals(first.artifactIntegrity(), second.artifactIntegrity());
        assertNull(second.evidence());
    }

    private static ManifestBackedGraphEvidenceProvider provider(Path root, String pin) {
        GraphEvidenceProperties properties = new GraphEvidenceProperties();
        properties.setEnabled(true);
        properties.setBundlePath(root);
        properties.setExpectedManifestSha256(pin);
        return new ManifestBackedGraphEvidenceProvider(properties, new VerifiedGraphArtifactLoader());
    }

    private static Path writeBundle(Path root, boolean duplicateNodeField) throws Exception {
        Files.createDirectories(root);
        byte[] nodes = duplicateNodeField
                ? ("{\"canonical_id\":\"1\",\"canonical_id\":\"1\",\"display_name\":\"one\","
                + "\"identity_status\":\"resolved\",\"node_type\":\"ingredient\","
                + "\"source_refs\":[\"gismo_vocabulary\",\"gismo_node_catalog\"]}\n")
                .getBytes(StandardCharsets.UTF_8)
                : jsonl(List.of(node("1", "one"), node("2", "two")));
        String evidenceId = evidenceId();
        byte[] edges = jsonl(List.of(edge(evidenceId)));
        byte[] evidence = jsonl(List.of(evidence(evidenceId)));
        byte[] rowErrors = new byte[0];

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("nodes.jsonl", sha256(nodes));
        outputs.put("edges.jsonl", sha256(edges));
        outputs.put("edge_evidence.jsonl", sha256(evidence));
        outputs.put("row_errors.jsonl", sha256(rowErrors));
        Map<String, Object> source = Map.of(
                "source_manifest_sha256", SOURCE_MANIFEST,
                "train_csv_sha256", TRAIN,
                "split", "train");
        Map<String, Object> counts = Map.of(
                "identity_error_rows", 0,
                "nodes", 2,
                "edges", 1,
                "evidence_records", 1);
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema_id", "chefkix.graph_eval_v4");
        manifest.put("schema_version", 4);
        manifest.put("outputs_sha256", outputs);
        manifest.put("source", source);
        manifest.put("counts", counts);
        manifest.put("state", "PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED");
        manifest.put("transformations", List.of(Map.of("id", "synthetic-transform", "sha256", TRANSFORM)));
        Files.write(root.resolve("manifest.json"), JSON.writeValueAsBytes(manifest));
        Files.write(root.resolve("nodes.jsonl"), nodes);
        Files.write(root.resolve("edges.jsonl"), edges);
        Files.write(root.resolve("edge_evidence.jsonl"), evidence);
        Files.write(root.resolve("row_errors.jsonl"), rowErrors);
        return root;
    }

    private static Map<String, Object> node(String id, String displayName) {
        return Map.of("canonical_id", id, "display_name", displayName,
                "identity_status", "resolved", "node_type", "ingredient",
                "source_refs", List.of("gismo_vocabulary", "gismo_node_catalog"));
    }

    private static Map<String, Object> edge(String evidenceId) {
        return Map.of("distinct_context_count", 1, "edge_ref", "1:2:substitution",
                "evidence_refs", List.of(evidenceId), "relation", "substitution", "seen", true,
                "source_id", "1", "target_id", "2", "train_pair_count", 1);
    }

    private static Map<String, Object> evidence(String evidenceId) {
        return Map.ofEntries(
                Map.entry("context_sha256", CONTEXT),
                Map.entry("edge_ref", "1:2:substitution"),
                Map.entry("evidence_id", evidenceId),
                Map.entry("relation", "substitution"),
                Map.entry("source_artifact_sha256", TRAIN),
                Map.entry("source_id", "1"),
                Map.entry("source_row_key", TRAIN + ":0"),
                Map.entry("split", "train"),
                Map.entry("target_id", "2"),
                Map.entry("transformation_id", "synthetic-transform"),
                Map.entry("transformation_sha256", TRANSFORM));
    }

    private static String evidenceId() throws Exception {
        return sha256(JSON.writeValueAsBytes(List.of("1", "2", "substitution", TRAIN,
                TRAIN + ":0", "synthetic-transform", TRANSFORM, CONTEXT)));
    }

    private static byte[] jsonl(List<Map<String, Object>> rows) throws Exception {
        List<byte[]> encoded = new ArrayList<>();
        for (Map<String, Object> row : rows) encoded.add(JSON.writeValueAsBytes(row));
        int size = encoded.stream().mapToInt(bytes -> bytes.length + 1).sum();
        byte[] output = new byte[size];
        int offset = 0;
        for (byte[] bytes : encoded) {
            System.arraycopy(bytes, 0, output, offset, bytes.length);
            offset += bytes.length;
            output[offset++] = (byte) '\n';
        }
        return output;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
