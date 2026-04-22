package com.chefkix.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chefkix.notification.entity.PushToken;

@Repository
public interface PushTokenRepository extends MongoRepository<PushToken, String> {

    long countByUserId(String userId);

    /** Find all active tokens for a user (multi-device support) */
    List<PushToken> findByUserIdAndActiveTrue(String userId);

    /** Find a specific token by FCM token value */
    Optional<PushToken> findByFcmToken(String fcmToken);

    /** Find by user + device (for upsert logic) */
    Optional<PushToken> findByUserIdAndDeviceId(String userId, String deviceId);

    /** Count active tokens for a user */
    long countByUserIdAndActiveTrue(String userId);

    /** Delete all tokens for a user (logout from all devices) */
    void deleteByUserId(String userId);

    /** Soft-delete: mark tokens as inactive */
    default void deactivateByUserId(String userId) {
        List<PushToken> tokens = findByUserIdAndActiveTrue(userId);
        tokens.forEach(token -> token.setActive(false));
        saveAll(tokens);
    }
}
