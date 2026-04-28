package com.chefkix.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chefkix.notification.dto.request.RegisterPushTokenRequest;
import com.chefkix.notification.entity.PushToken;
import com.chefkix.notification.repository.PushTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending push notifications via Firebase Cloud Messaging (FCM).
 * 
 * <p>Handles token registration, deregistration, and multi-device push delivery.
 * Implements automatic token cleanup when FCM reports invalid tokens.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushNotificationService {

    PushTokenRepository pushTokenRepository;
    @org.springframework.beans.factory.annotation.Qualifier("taskExecutor") Executor taskExecutor;

    @Value("${chefkix.push.enabled:false}")
    @NonFinal
    boolean pushEnabled;

    // ===============================================
    // TOKEN MANAGEMENT
    // ===============================================

    /**
     * Register or update an FCM push token for a user's device.
     * If the device already has a token, it will be updated.
     */
    public PushToken registerToken(String userId, RegisterPushTokenRequest request) {
        Optional<PushToken> existing = pushTokenRepository.findByUserIdAndDeviceId(userId, request.getDeviceId());

        PushToken token;
        if (existing.isPresent()) {
            token = existing.get();
            token.setFcmToken(request.getFcmToken());
            token.setPlatform(request.getPlatform());
            token.setDeviceName(request.getDeviceName());
            token.setActive(true);
            log.info("Updated FCM token for user={}, device={}", userId, request.getDeviceId());
        } else {
            token = PushToken.builder()
                    .userId(userId)
                    .deviceId(request.getDeviceId())
                    .fcmToken(request.getFcmToken())
                    .platform(request.getPlatform())
                    .deviceName(request.getDeviceName())
                    .active(true)
                    .build();
            log.info("Registered new FCM token for user={}, device={}, platform={}", 
                    userId, request.getDeviceId(), request.getPlatform());
        }

        return pushTokenRepository.save(token);
    }

    /**
     * Unregister a specific device's token.
     */
    public void unregisterToken(String userId, String deviceId) {
        pushTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(token -> {
                    token.setActive(false);
                    pushTokenRepository.save(token);
                    log.info("Unregistered FCM token for user={}, device={}", userId, deviceId);
                });
    }

    /**
     * Unregister all tokens for a user (logout from all devices).
     */
    public void unregisterAllTokens(String userId) {
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndActiveTrue(userId);
        tokens.forEach(token -> token.setActive(false));
        pushTokenRepository.saveAll(tokens);
        log.info("Unregistered all {} FCM tokens for user={}", tokens.size(), userId);
    }

    /**
     * Hard-delete all push tokens after an account is deleted.
     */
    public long cleanupDeletedUserTokens(String userId) {
        long tokenCount = pushTokenRepository.countByUserId(userId);
        if (tokenCount == 0) {
            return 0;
        }

        pushTokenRepository.deleteByUserId(userId);
        log.info("Deleted {} push tokens for deleted user={}", tokenCount, userId);
        return tokenCount;
    }

    // ===============================================
    // PUSH NOTIFICATION SENDING
    // ===============================================

    /**
     * Send a push notification to all of a user's devices.
     * 
     * @param userId Recipient user ID
     * @param title Notification title
     * @param body Notification body
     * @param data Optional data payload for app handling
     */
    public CompletableFuture<Void> sendToUser(String userId, String title, String body, Map<String, String> data) {
        if (!pushEnabled) {
            log.debug("Push notifications disabled, skipping send to user={}", userId);
            return CompletableFuture.completedFuture(null);
        }

        List<PushToken> tokens = pushTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("No active push tokens for user={}", userId);
            return CompletableFuture.completedFuture(null);
        }

        List<String> fcmTokens = tokens.stream()
                .map(PushToken::getFcmToken)
                .toList();

        return sendMulticast(fcmTokens, title, body, data, userId);
    }

    /**
     * Send to multiple FCM tokens and handle failures.
     */
    private CompletableFuture<Void> sendMulticast(
            List<String> fcmTokens, 
            String title, 
            String body, 
            Map<String, String> data,
            String userId) {

        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized, skipping push notification");
            return CompletableFuture.completedFuture(null);
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(fcmTokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data != null ? data : Map.of())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setIcon("/icon-192.png")
                                .build())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setIcon("ic_notification")
                                .setColor("#ff5a36")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .setSound("default")
                                .build())
                        .build())
                .build();

        return CompletableFuture.runAsync(() -> {
            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("Push sent to user={}: {}/{} successful", 
                        userId, response.getSuccessCount(), fcmTokens.size());

                // Handle invalid tokens
                handleFailedTokens(fcmTokens, response, userId);
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send push to user={}: {}", userId, e.getMessage());
            }
        }, taskExecutor);
    }

    /**
     * Mark failed/invalid tokens as inactive.
     */
    private void handleFailedTokens(List<String> fcmTokens, BatchResponse response, String userId) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                String errorCode = exception != null ? exception.getMessagingErrorCode().name() : "UNKNOWN";

                // Token is invalid or unregistered - deactivate it
                if (isTokenInvalid(exception)) {
                    String invalidToken = fcmTokens.get(i);
                    pushTokenRepository.findByFcmToken(invalidToken)
                            .ifPresent(token -> {
                                token.setActive(false);
                                pushTokenRepository.save(token);
                                log.info("Deactivated invalid FCM token for user={}, error={}", userId, errorCode);
                            });
                }
            } else {
                // Update last used timestamp for successful sends
                String successToken = fcmTokens.get(i);
                pushTokenRepository.findByFcmToken(successToken)
                        .ifPresent(token -> {
                            token.setLastUsedAt(Instant.now());
                            pushTokenRepository.save(token);
                        });
            }
        }
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        if (e == null) return false;
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
