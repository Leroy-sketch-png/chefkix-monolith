package com.chefkix.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chefkix.notification.entity.PushToken;

@Repository
public interface PushTokenRepository extends MongoRepository<PushToken, String> {

    long countByUserId(String userId);

    List<PushToken> findByUserIdAndActiveTrue(String userId);

    Optional<PushToken> findByFcmToken(String fcmToken);

    Optional<PushToken> findByUserIdAndDeviceId(String userId, String deviceId);

    long countByUserIdAndActiveTrue(String userId);

    void deleteByUserId(String userId);

    default void deactivateByUserId(String userId) {
        List<PushToken> tokens = findByUserIdAndActiveTrue(userId);
        tokens.forEach(token -> token.setActive(false));
        saveAll(tokens);
    }
}
