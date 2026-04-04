package com.chefkix.social.config;

import com.chefkix.social.chat.controller.VideoSignalingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class VideoWebSocketConfig implements WebSocketConfigurer {

    private final VideoSignalingHandler signalingHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000,https://*.chefkix.com}")
    private String allowedOriginPatterns;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws/video-signaling")
                .setAllowedOriginPatterns(allowedOriginPatterns.split(","));
    }
}
