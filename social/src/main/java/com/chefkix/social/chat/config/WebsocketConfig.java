package com.chefkix.social.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                log.debug("Interceptor received command: {}", accessor.getCommand()); // Log command

                // 1. Xác thực khi CONNECT
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    log.debug("Interceptor CONNECT: Authorization header found? {}", StringUtils.hasText(authHeader));

                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            Jwt jwt = jwtDecoder.decode(token);
                            Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                            accessor.setUser(authentication); // Lưu user vào session
                            log.info("STOMP user connected: {}", authentication.getName());
                        } catch (Exception e) {
                            log.error("STOMP Authentication failed: {}", e.getMessage(), e); // Log cả exception
                        }
                    } else {
                        log.warn("Interceptor CONNECT: No valid Authorization header found.");
                    }
                }

                // --- PHẦN SỬA LỖI QUAN TRỌNG NHẤT (VỚI DEBUG LOG) ---
                // 2. Đặt Authentication vào SecurityContext cho TẤT CẢ các tin nhắn
                Authentication authentication = (Authentication) accessor.getUser();
                if (authentication != null) {
                    log.debug(
                            "Interceptor (preSend): Setting Authentication in SecurityContextHolder for user: {}",
                            authentication.getName());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // Kiểm tra ngay sau khi set
                    Authentication checkAuth =
                            SecurityContextHolder.getContext().getAuthentication();
                    log.debug(
                            "Interceptor (preSend): SecurityContextHolder NOW HAS Authentication? {}, User: {}",
                            (checkAuth != null),
                            (checkAuth != null ? checkAuth.getName() : "NULL"));
                } else if (accessor.getUser() == null && !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Log nếu user bị null sau khi connect (cho các lệnh SEND, SUBSCRIBE...)
                    log.warn(
                            "Interceptor (preSend): Authentication is NULL in session for non-CONNECT command '{}'. Session: {}",
                            accessor.getCommand(),
                            accessor.getSessionId());
                } else if (accessor.getUser() == null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Log nếu user bị null ngay cả khi CONNECT (thường do token lỗi)
                    log.warn(
                            "Interceptor (preSend): Authentication is NULL after CONNECT processing. Session: {}",
                            accessor.getSessionId());
                }
                // ----------------------------------------------------

                return message;
            }
        });
    }
}
