package com.chefkix.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Executable filter-chain regression for the additive graph-evidence route.
 * The probe controller is synthetic and exists only in this minimal test context.
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

            // If this exact research path ever enters PUBLIC_ENDPOINTS or GUEST_GET_ENDPOINTS,
            // the anonymous request reaches the synthetic endpoint (204) instead of 401.
            mvc.perform(get("/api/v1/graph/evidence"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/graph/evidence").header("Authorization", "Bearer synthetic-token"))
                    .andExpect(status().isNoContent());

            // Controls prove the neighboring legacy public and guest-GET rules still match
            // their own paths and that the GET-only guest rule does not permit mutations.
            mvc.perform(get("/knowledge/probe"))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/knowledge-graph/probe"))
                    .andExpect(status().isNoContent());
            mvc.perform(post("/knowledge-graph/probe"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfig.class)
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
        @GetMapping("/api/v1/graph/evidence")
        ResponseEntity<Void> graphEvidence() {
            return ResponseEntity.noContent().build();
        }

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
