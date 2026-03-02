package com.chefkix.social.chat.listener;

import java.security.Principal;

import com.chefkix.identity.api.ProfileProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ProfileProvider profileProvider;

    // 1. ONLINE
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            log.info("User Online: {}", user.getName());
            try {
                profileProvider.updateUserOnlineStatus(user.getName(), true);
            } catch (Exception e) {
                log.error("Failed to update ONLINE status: {}", e.getMessage());
            }
        }
    }

    // 2. OFFLINE
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            log.info("User Offline: {}", user.getName());
            try {
                profileProvider.updateUserOnlineStatus(user.getName(), false);
            } catch (Exception e) {
                log.error("Failed to update OFFLINE status: {}", e.getMessage());
            }
        }
    }
}
