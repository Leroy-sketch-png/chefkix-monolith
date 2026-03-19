package com.chefkix.config;

import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import com.chefkix.social.chat.repository.ConversationRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
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
import org.springframework.validation.Validator;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified WebSocket/STOMP configuration with mandatory JWT auth.
 * Unauthenticated CONNECT attempts are rejected.
 * User-scoped subscriptions are validated to prevent cross-user eavesdropping.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ConversationRepository conversationRepository;
    private final CookingRoomRedisRepository cookingRoomRedisRepository;
    private final Validator validator;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "https://*.chefkix.com")
                .withSockJS();
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
                // Prevent authentication leakage between pooled broker threads.
                SecurityContextHolder.clearContext();

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                StompCommand command = accessor.getCommand();

                // --- CONNECT: Mandatory JWT authentication ---
                if (StompCommand.CONNECT.equals(command)) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                        log.warn("STOMP CONNECT rejected: missing or invalid Authorization header");
                        throw new MessageDeliveryException("Authentication required for WebSocket connection");
                    }

                    String token = authHeader.substring(7);
                    try {
                        Jwt jwt = jwtDecoder.decode(token);
                        Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                        accessor.setUser(authentication);
                        log.info("STOMP user connected: {}", authentication.getName());
                    } catch (Exception e) {
                        log.error("STOMP CONNECT rejected: JWT validation failed: {}", e.getMessage());
                        throw new MessageDeliveryException("Invalid or expired authentication token");
                    }
                }

                // --- SUBSCRIBE: Validate user-scoped destinations ---
                if (StompCommand.SUBSCRIBE.equals(command)) {
                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth == null) {
                        throw new MessageDeliveryException("Authentication required for subscription");
                    }

                    String destination = accessor.getDestination();
                    if (destination != null) {
                        String userId = auth.getName();
                        // Guard user-specific queues: /queue/notifications-{userId}
                        if (destination.startsWith("/queue/notifications-")) {
                            String targetUserId = destination.substring("/queue/notifications-".length());
                            if (!userId.equals(targetUserId)) {
                                log.warn("STOMP subscription blocked: user {} tried to subscribe to {}", userId, destination);
                                throw new MessageDeliveryException("Cannot subscribe to another user's notifications");
                            }
                        }
                        // Guard user-specific queues: /queue/messages-{userId}
                        if (destination.startsWith("/queue/messages-")) {
                            String targetUserId = destination.substring("/queue/messages-".length());
                            if (!userId.equals(targetUserId)) {
                                log.warn("STOMP subscription blocked: user {} tried to subscribe to {}", userId, destination);
                                throw new MessageDeliveryException("Cannot subscribe to another user's messages");
                            }
                        }

                        // Guard conversation topics: /topic/conversation/{conversationId}
                        if (destination.startsWith("/topic/conversation/")) {
                            String conversationId = destination.substring("/topic/conversation/".length());
                            boolean isParticipant = conversationRepository.findById(conversationId)
                                    .map(conversation -> conversation.getParticipants() != null
                                            && conversation.getParticipants().stream()
                                            .anyMatch(p -> userId.equals(p.getUserId())))
                                    .orElse(false);

                            if (!isParticipant) {
                                log.warn("STOMP subscription blocked: user {} is not participant of conversation {}", userId, conversationId);
                                throw new MessageDeliveryException("Cannot subscribe to a conversation you are not part of");
                            }
                        }

                        // Guard room topics: /topic/room/{roomCode}
                        if (destination.startsWith("/topic/room/")) {
                            String roomCode = destination.substring("/topic/room/".length());
                            boolean isRoomParticipant = cookingRoomRedisRepository.findByRoomCode(roomCode)
                                    .map(room -> room.getParticipants() != null
                                            && room.getParticipants().stream()
                                            .anyMatch(p -> userId.equals(p.getUserId())))
                                    .orElse(false);

                            if (!isRoomParticipant) {
                                log.warn("STOMP subscription blocked: user {} is not participant of room {}", userId, roomCode);
                                throw new MessageDeliveryException("Cannot subscribe to a room you are not part of");
                            }
                        }
                    }
                }

                // --- SEND: Require authentication for all messages ---
                if (StompCommand.SEND.equals(command)) {
                    if (accessor.getUser() == null) {
                        throw new MessageDeliveryException("Authentication required to send messages");
                    }
                }

                // Propagate Authentication to SecurityContext for @MessageMapping handlers
                Authentication auth = (Authentication) accessor.getUser();
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

                return message;
            }
        });
    }

    /**
     * Expose the injected Validator as a bean-style accessor.
     * Not an interface override — WebSocketMessageBrokerConfigurer
     * does not define getValidator() in newer Spring versions.
     */
    public Validator getValidator() {
        return validator;
    }
}
