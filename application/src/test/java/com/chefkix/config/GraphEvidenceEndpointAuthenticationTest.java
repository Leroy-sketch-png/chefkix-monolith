package com.chefkix.config;

import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceConfiguration;
import com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Executable filter-chain regression for the additive graph-evidence route.
 */
class GraphEvidenceEndpointAuthenticationTest {

    @Test
    void graphEvidenceRequiresAuthenticationWhileNeighboringAllowlistRulesRemainScoped() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(SecurityTestApplication.class);
            context.refresh();

            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean("springSecurityFilterChain", FilterChainProxy.class))
                    .build();

            // The external URL includes the servlet context path; the controller mapping does not.
            mvc.perform(get("/api/v1/graph/evidence").contextPath("/api/v1"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/graph/evidence").contextPath("/api/v1")
                            .param("sourceIngredientId", "1")
                            .param("candidateIngredientId", "2")
                            .header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.unavailableReason").value("FEATURE_DISABLED"));

            // Controls prove the neighboring legacy public and guest-GET rules still match
            // their own paths and that the GET-only guest rule does not permit mutations.
            mvc.perform(get("/api/v1/knowledge/probe").contextPath("/api/v1"))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/knowledge-graph/probe").contextPath("/api/v1"))
                    .andExpect(status().isNoContent());
            mvc.perform(post("/api/v1/knowledge-graph/probe").contextPath("/api/v1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({
        SecurityConfig.class,
        GraphEvidenceConfiguration.class,
        GraphEvidenceController.class
    })
    static class SecurityTestApplication {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("synthetic-authenticated-test-principal")
                    .build();
        }

        @Bean
        ProbeEndpoints probeEndpoints() {
            return new ProbeEndpoints();
        }
    }

    @RestController
    static class ProbeEndpoints {
        @GetMapping("/knowledge/probe")
        ResponseEntity<Void> publicKnowledgeControl() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/knowledge-graph/probe")
        ResponseEntity<Void> guestKnowledgeGraphGetControl() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/knowledge-graph/probe")
        ResponseEntity<Void> guestKnowledgeGraphPostControl() {
            return ResponseEntity.noContent().build();
        }
    }
}
