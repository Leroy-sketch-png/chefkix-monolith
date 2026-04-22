package com.chefkix.social.config;

import com.chefkix.social.chat.controller.VideoSignalingHandler;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class VideoWebSocketConfig implements WebSocketConfigurer {

    private final VideoSignalingHandler signalingHandler;
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,https://*.chefkix.com}")
    private String allowedOriginPatterns;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws/video-signaling")
                .addInterceptors(videoAuthHandshakeInterceptor())
                .setAllowedOriginPatterns(allowedOriginPatterns.split(","));
    }

    private HandshakeInterceptor videoAuthHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes
            ) {
                String token = extractToken(request);
                if (!StringUtils.hasText(token)) {
                    log.warn("Video WebSocket rejected: missing bearer token");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }

                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
                        log.warn("Video WebSocket rejected: token produced no authentication principal");
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return false;
                    }

                    attributes.put("authentication", authentication);
                    attributes.put("userId", authentication.getName());
                    return true;
                } catch (Exception exception) {
                    log.warn("Video WebSocket rejected: JWT validation failed: {}", exception.getMessage());
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            }

            @Override
            public void afterHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception
            ) {
                // No-op
            }
        };
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String accessToken = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("access_token");
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }

        return UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
    }
}
