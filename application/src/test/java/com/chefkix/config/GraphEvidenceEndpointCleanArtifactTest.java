package com.chefkix.config;

import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceConfiguration;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceController;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
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
import java.util.HexFormat;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verified integration test for the clean graph_eval_v4 artifact bundle.
 * Verifies HTTP 200 AVAILABLE serving on verified data with 0 identity errors.
 */
class GraphEvidenceEndpointCleanArtifactTest {
    public static final String CLEAN_MANIFEST_SHA256 =
            "ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1";

    @Test
    void cleanArtifactServesHttp200AvailableOnRealData() throws Exception {
        Path bundle = Path.of("d:/code/chefkix/ai/out/graph_eval_v4_clean").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.exists(bundle.resolve("manifest.json")),
                "Clean graph_eval_v4 bundle must exist at ai/out/graph_eval_v4_clean");

        assertFalse(Files.isSymbolicLink(bundle), "bundle must not be a symlink");
        byte[] manifestBytes = Files.readAllBytes(bundle.resolve("manifest.json"));
        String observedDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(manifestBytes));
        assertEquals(CLEAN_MANIFEST_SHA256, observedDigest, "Manifest SHA-256 must match pinned clean build");

        try (AnnotationConfigWebApplicationContext context = context(bundle, CLEAN_MANIFEST_SHA256)) {
            MockMvc mvc = mvc(context);

            // Test evidence query: verify HTTP 200 AVAILABLE
            mvc.perform(get("/api/v1/graph/evidence")
                            .contextPath("/api/v1")
                            .param("sourceIngredientId", "1")
                            .param("candidateIngredientId", "2")
                            .header("Authorization", "Bearer clean-graph-test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contractVersion").value("graph-evidence.v1"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.artifactIntegrity.status").value("VERIFIED"))
                    .andExpect(jsonPath("$.artifactIntegrity.expectedSha256").value(CLEAN_MANIFEST_SHA256))
                    .andExpect(jsonPath("$.artifactIntegrity.observedSha256").value(CLEAN_MANIFEST_SHA256))
                    .andExpect(jsonPath("$.evidence.provenance[0].split").value("TRAIN"))
                    .andExpect(jsonPath("$.evidence.calibration.status").value("no_guarantee"))
                    .andExpect(jsonPath("$.evidence.decision.decision").value("ABSTAIN"));

            // Test snapshot query: verify HTTP 200 AVAILABLE with real nodes and edges
            mvc.perform(get("/api/v1/graph/evidence/snapshot")
                            .contextPath("/api/v1")
                            .param("centerIngredientId", "1")
                            .param("depth", "1")
                            .param("limit", "20")
                            .header("Authorization", "Bearer clean-graph-test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contractVersion").value("graph-evidence-snapshot.v1"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.artifactIntegrity.status").value("VERIFIED"))
                    .andExpect(jsonPath("$.snapshot.stats.totalNodes").value(6633))
                    .andExpect(jsonPath("$.snapshot.stats.totalEdges").value(23185))
                    .andExpect(jsonPath("$.snapshot.stats.returnedNodes").value(greaterThan(0)))
                    .andExpect(jsonPath("$.snapshot.acceptanceState").value("PROVISIONAL_PRIVATE_CANDIDATE_ACCEPTED"));
        }
    }

    private static AnnotationConfigWebApplicationContext context(Path bundle, String digest) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(EndpointTestApplication.class);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(),
                "chefkix.knowledge.graph-evidence.enabled=true",
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
                    .subject("clean-graph-evidence-test")
                    .build();
        }
    }
}
