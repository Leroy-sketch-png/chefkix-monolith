package com.chefkix.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chefkix.notification.entity.Notification;
import com.chefkix.notification.enums.NotificationType;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Optional<Notification> findByRecipientIdAndTargetEntityIdAndType(
            String recipientId, String targetId, NotificationType notificationType);

    long countByRecipientIdAndIsReadFalse(String recipientId);

    Slice<Notification> findAllByRecipientIdAndIsReadFalse(String userId, Pageable pageable);

    List<Notification> findAllByRecipientIdAndIsReadFalse(String recipientId);

    Slice<Notification> findAllByRecipientId(String userId, Pageable pageable);

    /**
     * Find all notifications for a user created after a given timestamp.
     * Used by "Welcome Back" summary to aggregate activity since last visit.
     * Leverages compound index on {recipientId, isRead, createdAt}.
     */
    List<Notification> findAllByRecipientIdAndCreatedAtAfter(String recipientId, Instant since);
}
