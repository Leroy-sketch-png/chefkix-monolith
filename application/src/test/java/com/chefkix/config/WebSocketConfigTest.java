package com.chefkix.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.social.chat.service.ConversationLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Validator;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class WebSocketConfigTest {

    @Test
    void usesConfiguredCorsOriginsForStompHandshake() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(endpoint);
        when(endpoint.setAllowedOriginPatterns("http://localhost:3000", "http://localhost:3001"))
                .thenReturn(endpoint);
        WebSocketConfig config = new WebSocketConfig(
                mock(JwtDecoder.class),
                mock(JwtAuthenticationConverter.class),
                mock(MongoTemplate.class),
                mock(StringRedisTemplate.class),
                mock(ObjectMapper.class),
                mock(ConversationLookupService.class),
                mock(Validator.class));
        ReflectionTestUtils.setField(config, "allowedOriginPatterns",
                "http://localhost:3000, http://localhost:3001,");

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
        verify(endpoint).setAllowedOriginPatterns("http://localhost:3000", "http://localhost:3001");
    }
}
