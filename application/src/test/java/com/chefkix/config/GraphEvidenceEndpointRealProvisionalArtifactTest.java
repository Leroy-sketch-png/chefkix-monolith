package com.chefkix.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceConfiguration;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceController;
import com.chefkix.culinary.features.knowledge.evidence.VerifiedGraphArtifactLoader;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opt-in local request-boundary check for the real provisional T154 bundle.
 * It intentionally verifies refusal; the artifact has unresolved identity rows.
 */
class GraphEvidenceEndpointRealProvisionalArtifactTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String T154_MANIFEST_SHA256 =
            "60e5c26144d162ea79e79cb54e7a3e6052c9fe35f4c454d8ff8f5d882ce47140";
    private static final String BUNDLE_PROPERTY = "chefkix.test.real-provisional-bundle";

    @Test
    void refusesRealProvisionalBundleAndReturnsOnlyItsRecomputedManifestDigest() throws Exception {
        String configuredBundle = System.getProperty(BUNDLE_PROPERTY);
        Assumptions.assumeTrue(configuredBundle != null && !configuredBundle.isBlank(),
                "Set the explicit local bundle property to run this data-boundary test");

        Path bundle = Path.of(configuredBundle).toAbsolutePath().normalize();
        Path manifestPath = bundle.resolve("manifest.json");
        assertFalse(Files.isSymbolicLink(bundle), "the source bundle must not be a symlink");
        assertFalse(Files.isSymbolicLink(manifestPath), "the source manifest must not be a symlink");
        byte[] manifestBytes = Files.readAllBytes(manifestPath);
        String actualManifestDigest = sha256(manifestBytes);
        assertEquals(T154_MANIFEST_SHA256, actualManifestDigest);
        JsonNode manifest = JSON.readTree(manifestBytes);
        assertEquals(10, manifest.path("counts").path("identity_error_rows").intValue());

        try (AnnotationConfigWebApplicationContext context = context(bundle, actualManifestDigest)) {
            MockMvc mvc = mvc(context);
            mvc.perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "synthetic-source")
                            .param("candidateIngredientId", "synthetic-candidate")
                            .header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("REFUSED"))
                    .andExpect(jsonPath("$.artifactIntegrity.status").value("INVALID"))
                    .andExpect(jsonPath("$.artifactIntegrity.expectedSha256").value(actualManifestDigest))
                    .andExpect(jsonPath("$.artifactIntegrity.observedSha256").value(actualManifestDigest))
                    .andExpect(jsonPath("$.evidence").doesNotExist())
                    .andExpect(jsonPath("$.identityResolution").doesNotExist());
        }
    }

    @Test
    void refusesTamperedCopyOfRealProvisionalBundleWithoutReturningEvidence(@TempDir Path tempDir)
            throws Exception {
        String configuredBundle = System.getProperty(BUNDLE_PROPERTY);
        Assumptions.assumeTrue(configuredBundle != null && !configuredBundle.isBlank(),
                "Set the explicit local bundle property to run this data-boundary test");

        Path bundle = Path.of(configuredBundle).toAbsolutePath().normalize();
        assertFalse(Files.isSymbolicLink(bundle), "the source bundle must not be a symlink");
        VerifiedGraphArtifactLoader loader = new VerifiedGraphArtifactLoader();
        VerifiedGraphArtifactLoader.Result sourceResult = loader.load(bundle, T154_MANIFEST_SHA256);
        assertInstanceOf(VerifiedGraphArtifactLoader.Loaded.class, sourceResult);

        Path copy = Files.createDirectory(tempDir.resolve("tampered-bundle"));
        List<String> allowlistedFiles = List.of(
                "manifest.json", "nodes.jsonl", "edges.jsonl", "edge_evidence.jsonl", "row_errors.jsonl");
        for (String name : allowlistedFiles) {
            Path source = bundle.resolve(name);
            assertFalse(Files.isSymbolicLink(source), "source bundle entries must not be symlinks");
            Files.copy(source, copy.resolve(name));
        }

        Path sourceEdges = bundle.resolve("edges.jsonl");
        byte[] sourceBytes = Files.readAllBytes(sourceEdges);
        assertFalse(sourceBytes.length == 0, "the pinned edge artifact must be nonempty");
        byte[] tamperedBytes = sourceBytes.clone();
        tamperedBytes[0] ^= 1;
        Files.write(copy.resolve("edges.jsonl"), tamperedBytes);

        VerifiedGraphArtifactLoader.Result tamperedResult = loader.load(copy, T154_MANIFEST_SHA256);
        VerifiedGraphArtifactLoader.Refused refusal = assertInstanceOf(
                VerifiedGraphArtifactLoader.Refused.class, tamperedResult);
        assertEquals(VerifiedGraphArtifactLoader.Refusal.OUTPUT_HASH_MISMATCH, refusal.reason());
        assertEquals(sha256(sourceBytes), sha256(Files.readAllBytes(sourceEdges)),
                "the source artifact digest must remain unchanged");

        try (AnnotationConfigWebApplicationContext context = context(copy, T154_MANIFEST_SHA256)) {
            MockMvc mvc = mvc(context);
            mvc.perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "synthetic-source")
                            .param("candidateIngredientId", "synthetic-candidate")
                            .header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("REFUSED"))
                    .andExpect(jsonPath("$.artifactIntegrity.status").value("INVALID"))
                    .andExpect(jsonPath("$.artifactIntegrity.expectedSha256").value(T154_MANIFEST_SHA256))
                    .andExpect(jsonPath("$.evidence").doesNotExist())
                    .andExpect(jsonPath("$.identityResolution").doesNotExist());
        }
    }

    private static AnnotationConfigWebApplicationContext context(Path bundle, String digest) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(EndpointTestApplication.class);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(),
                "chefkix.knowledge.graph-evidence.enabled=true",
                "chefkix.knowledge.graph-evidence.bundle-path="
                        + bundle.toString().replace('\\', '/'),
                "chefkix.knowledge.graph-evidence.expected-manifest-sha256=" + digest);
        context.refresh();
        return context;
    }

    private static MockMvc mvc(AnnotationConfigWebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", FilterChainProxy.class))
                .build();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, GraphEvidenceConfiguration.class, GraphEvidenceController.class})
    static class EndpointTestApplication {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("real-provisional-artifact-test")
                    .build();
        }
    }
}
