package com.chefkix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceConfiguration;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Request-level round trip for the real pinned-byte loader, provider, MVC serializer,
 * and authentication chain. The bundle is synthetic and proves no research data claim.
 */
class GraphEvidenceEndpointArtifactRoundTripTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private static final String EXPECTED_MANIFEST_SHA256 =
            "e31541510745ec5707951508f31aea1f9df64b9f73fba4977268d0cc6b685ad1";
    private static final String TRAIN = "a".repeat(64);
    private static final String SOURCE_MANIFEST = "b".repeat(64);
    private static final String TRANSFORM = "c".repeat(64);
    private static final String CONTEXT = "d".repeat(64);

    @TempDir
    Path tempDir;

    @Test
    void authenticatedRequestReturnsTheDigestRecomputedFromPinnedManifestBytes() throws Exception {
        Path bundle = writeBundle(tempDir.resolve("bundle"));
        String manifestDigest = sha256(Files.readAllBytes(bundle.resolve("manifest.json")));
        org.junit.jupiter.api.Assertions.assertEquals(EXPECTED_MANIFEST_SHA256, manifestDigest);

        try (AnnotationConfigWebApplicationContext context = context(bundle, manifestDigest, true)) {
            MockMvc mvc = mvc(context);

            mvc.perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "1")
                            .param("candidateIngredientId", "2")
                            .header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contractVersion").value("graph-evidence.v1"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.artifactIntegrity.status").value("VERIFIED"))
                    .andExpect(jsonPath("$.artifactIntegrity.expectedSha256").value(EXPECTED_MANIFEST_SHA256))
                    .andExpect(jsonPath("$.artifactIntegrity.observedSha256").value(EXPECTED_MANIFEST_SHA256))
                    .andExpect(jsonPath("$.evidence.provenance[0].artifactSha256").value(TRAIN))
                    .andExpect(jsonPath("$.evidence.provenance[0].split").value("TRAIN"))
                    .andExpect(jsonPath("$.evidence.trainPairEvidence.trainCount").value(1))
                    .andExpect(jsonPath("$.evidence.calibration.status").value("no_guarantee"))
                    .andExpect(jsonPath("$.evidence.decision.decision").value("ABSTAIN"));

            mvc.perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "1")
                            .param("candidateIngredientId", "2"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void disabledFeatureDoesNotLoadOrExposeTheConfiguredSyntheticBundle() throws Exception {
        Path bundle = writeBundle(tempDir.resolve("disabled-bundle"));
        String manifestDigest = sha256(Files.readAllBytes(bundle.resolve("manifest.json")));

        try (AnnotationConfigWebApplicationContext context = context(bundle, manifestDigest, false)) {
            mvc(context).perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "1")
                            .param("candidateIngredientId", "2")
                            .header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                    .andExpect(jsonPath("$.unavailableReason").value("FEATURE_DISABLED"))
                    .andExpect(jsonPath("$.artifactIntegrity").doesNotExist());
        }
    }

    @Test
    void tamperedOutputIsRefusedAtRequestBoundaryWithoutLegacyFallback() throws Exception {
        Path bundle = writeBundle(tempDir.resolve("tampered-bundle"));
        String manifestDigest = sha256(Files.readAllBytes(bundle.resolve("manifest.json")));
        Files.writeString(bundle.resolve("edges.jsonl"), "tampered\n", StandardCharsets.UTF_8);

        try (AnnotationConfigWebApplicationContext context = context(bundle, manifestDigest, true)) {
            mvc(context).perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "1")
                            .param("candidateIngredientId", "2")
                            .header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("REFUSED"))
                    .andExpect(jsonPath("$.artifactIntegrity.status").value("INVALID"))
                    .andExpect(jsonPath("$.artifactIntegrity.expectedSha256").value(manifestDigest))
                    .andExpect(jsonPath("$.artifactIntegrity.observedSha256").doesNotExist())
                    .andExpect(jsonPath("$.evidence").doesNotExist());
        }
    }

    private static AnnotationConfigWebApplicationContext context(Path bundle, String digest, boolean enabled) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(EndpointTestApplication.class);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(),
                "chefkix.knowledge.graph-evidence.enabled=" + enabled,
                "chefkix.knowledge.graph-evidence.bundle-path="
                        + bundle.toAbsolutePath().toString().replace('\\', '/'),
                "chefkix.knowledge.graph-evidence.expected-manifest-sha256=" + digest);
        context.refresh();
        return context;
    }

    private static MockMvc mvc(AnnotationConfigWebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", FilterChainProxy.class))
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, GraphEvidenceConfiguration.class, GraphEvidenceController.class})
    static class EndpointTestApplication {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("synthetic-graph-evidence-test")
                    .build();
        }
    }

    private static Path writeBundle(Path root) throws Exception {
        Files.createDirectories(root);
        String evidenceId = sha256(JSON.writeValueAsBytes(List.of(
                "1", "2", "substitution", TRAIN, TRAIN + ":0", "synthetic-transform", TRANSFORM, CONTEXT)));
        byte[] nodes = jsonl(List.of(
                Map.of("canonical_id", "1", "display_name", "one", "identity_status", "resolved",
                        "node_type", "ingredient", "source_refs", List.of("gismo_vocabulary", "gismo_node_catalog")),
                Map.of("canonical_id", "2", "display_name", "two", "identity_status", "resolved",
                        "node_type", "ingredient", "source_refs", List.of("gismo_vocabulary", "gismo_node_catalog"))));
        byte[] edges = jsonl(List.of(Map.of(
                "distinct_context_count", 1, "edge_ref", "1:2:substitution", "evidence_refs", List.of(evidenceId),
                "relation", "substitution", "seen", true, "source_id", "1", "target_id", "2", "train_pair_count", 1)));
        byte[] evidence = jsonl(List.of(Map.ofEntries(
                Map.entry("context_sha256", CONTEXT), Map.entry("edge_ref", "1:2:substitution"),
                Map.entry("evidence_id", evidenceId), Map.entry("relation", "substitution"),
                Map.entry("source_artifact_sha256", TRAIN), Map.entry("source_id", "1"),
                Map.entry("source_row_key", TRAIN + ":0"), Map.entry("split", "train"),
                Map.entry("target_id", "2"), Map.entry("transformation_id", "synthetic-transform"),
                Map.entry("transformation_sha256", TRANSFORM))));
        byte[] rowErrors = new byte[0];
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("nodes.jsonl", sha256(nodes));
        outputs.put("edges.jsonl", sha256(edges));
        outputs.put("edge_evidence.jsonl", sha256(evidence));
        outputs.put("row_errors.jsonl", sha256(rowErrors));
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema_id", "chefkix.graph_eval_v4");
        manifest.put("schema_version", 4);
        manifest.put("outputs_sha256", outputs);
        manifest.put("source", Map.of("source_manifest_sha256", SOURCE_MANIFEST,
                "train_csv_sha256", TRAIN, "split", "train"));
        manifest.put("counts", Map.of("identity_error_rows", 0, "nodes", 2, "edges", 1, "evidence_records", 1));
        manifest.put("state", "PROVISIONAL_PRIVATE_CANDIDATE_NOT_ACCEPTED");
        manifest.put("transformations", List.of(Map.of("id", "synthetic-transform", "sha256", TRANSFORM)));
        Files.write(root.resolve("manifest.json"), JSON.writeValueAsBytes(manifest));
        Files.write(root.resolve("nodes.jsonl"), nodes);
        Files.write(root.resolve("edges.jsonl"), edges);
        Files.write(root.resolve("edge_evidence.jsonl"), evidence);
        Files.write(root.resolve("row_errors.jsonl"), rowErrors);
        return root;
    }

    private static byte[] jsonl(List<Map<String, Object>> rows) throws Exception {
        StringBuilder content = new StringBuilder();
        for (Map<String, Object> row : rows) content.append(JSON.writeValueAsString(row)).append('\n');
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
