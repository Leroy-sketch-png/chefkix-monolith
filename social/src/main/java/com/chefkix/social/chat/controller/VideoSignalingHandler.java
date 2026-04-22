package com.chefkix.social.chat.controller;

import com.chefkix.social.chat.dto.request.SignalMessage;
import com.chefkix.social.chat.repository.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSignalingHandler extends TextWebSocketHandler {

    private static final String SESSION_USER_ID = "userId";

    private final ObjectMapper objectMapper;
    private final ConversationRepository conversationRepository;

    // conversationId -> list of active user sessions
    private final ConcurrentHashMap<String, List<WebSocketSession>> conversations = new ConcurrentHashMap<>();

    // Keep track of which session belongs to which conversation for easy cleanup
    private final ConcurrentHashMap<WebSocketSession, String> sessionConversationMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getAuthenticatedUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
            return;
        }

        log.info("New video WebSocket connection: session={}, user={}", session.getId(), userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String userId = getAuthenticatedUserId(session);
            if (userId == null) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
                return;
            }

            SignalMessage signal = objectMapper.readValue(message.getPayload(), SignalMessage.class);
            String conversationId = signal.getConversationId();

            if (conversationId == null || conversationId.isEmpty()) {
                log.warn("Received message without conversationId");
                return;
            }

            signal.setSenderId(userId);

            switch (signal.getType()) {
                case "join":
                    handleJoin(session, conversationId);
                    break;
                case "ring":
                case "offer":
                case "answer":
                case "ice-candidate":
                    handleSignalingMessage(session, signal, conversationId);
                    break;
                case "leave":
                    handleLeave(session, conversationId);
                    break;
                default:
                    log.warn("Unknown message type: {}", signal.getType());
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }

    private void handleJoin(WebSocketSession session, String conversationId) throws IOException {
        String userId = getAuthenticatedUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
            return;
        }

        boolean isParticipant = conversationRepository.findById(conversationId)
                .map(conversation -> conversation.getParticipants() != null
                        && conversation.getParticipants().stream()
                        .anyMatch(participant -> userId.equals(participant.getUserId())))
                .orElse(false);

        if (!isParticipant) {
            log.warn("User {} attempted to join unauthorized video conversation {}", userId, conversationId);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Not a participant of this conversation"));
            return;
        }

        String existingConversationId = sessionConversationMap.get(session);
        if (existingConversationId != null && !existingConversationId.equals(conversationId)) {
            log.warn("Session {} attempted to join a second conversation: {} -> {}", session.getId(), existingConversationId, conversationId);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Session already joined to another conversation"));
            return;
        }

        conversations.putIfAbsent(conversationId, new CopyOnWriteArrayList<>());
        List<WebSocketSession> roomSessions = conversations.get(conversationId);

        // Limit to 2 people per conversation for 1-on-1 calls
        if (roomSessions.size() >= 2 && !roomSessions.contains(session)) {
            log.warn("Conversation {} is full. Rejecting session {}", conversationId, session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Conversation full"));
            return;
        }

        if (!roomSessions.contains(session)) {
            roomSessions.add(session);
            sessionConversationMap.put(session, conversationId);
            log.info("Session {} joined conversation {}. Total participants: {}", session.getId(), conversationId, roomSessions.size());
            
            // Optional: Notify others that someone joined
            // If there's exactly 1 other person, we could notify them
            if (roomSessions.size() == 2) {
                log.info("Conversation {} now has 2 participants", conversationId);
            }
        }
    }

    private void handleSignalingMessage(WebSocketSession senderSession, SignalMessage signal, String conversationId) throws IOException {
        String joinedConversationId = sessionConversationMap.get(senderSession);
        if (joinedConversationId == null || !joinedConversationId.equals(conversationId)) {
            log.warn("Session {} attempted signaling without joining conversation {}", senderSession.getId(), conversationId);
            senderSession.close(CloseStatus.POLICY_VIOLATION.withReason("Join the conversation before signaling"));
            return;
        }

        List<WebSocketSession> roomSessions = conversations.get(conversationId);
        if (roomSessions == null) return;

        log.debug("Routing {} message in conversation {}", signal.getType(), conversationId);

        // Forward to the OTHER participant
        for (WebSocketSession s : roomSessions) {
            if (s.isOpen() && !s.getId().equals(senderSession.getId())) {
                s.sendMessage(new TextMessage(objectMapper.writeValueAsString(signal)));
            }
        }
    }

    private void handleLeave(WebSocketSession session, String conversationId) {
        removeSessionFromConversation(session, conversationId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String conversationId = sessionConversationMap.get(session);
        if (conversationId != null) {
            removeSessionFromConversation(session, conversationId);
        }
        sessionConversationMap.remove(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }

    private void removeSessionFromConversation(WebSocketSession session, String conversationId) {
        List<WebSocketSession> roomSessions = conversations.get(conversationId);
        if (roomSessions != null) {
            roomSessions.remove(session);
            
            // Notify the remaining participant that the other left
            SignalMessage leaveSignal = SignalMessage.builder()
                .type("leave")
                .conversationId(conversationId)
                .build();
                
            try {
                for (WebSocketSession s : roomSessions) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(objectMapper.writeValueAsString(leaveSignal)));
                    }
                }
            } catch (IOException e) {
                log.error("Error sending leave notification", e);
            }

            if (roomSessions.isEmpty()) {
                conversations.remove(conversationId);
                log.info("Removed empty conversation {}", conversationId);
            } else {
                log.info("Session {} left conversation {}. Remaining participants: {}", session.getId(), conversationId, roomSessions.size());
            }
        }
    }

    private String getAuthenticatedUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(SESSION_USER_ID);
        return userId instanceof String && !((String) userId).isBlank() ? (String) userId : null;
    }
}
