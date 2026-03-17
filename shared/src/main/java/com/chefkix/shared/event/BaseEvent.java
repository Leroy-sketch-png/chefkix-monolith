package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract base for all Kafka events in ChefKix.
 * <p>
 * Consolidated from 4 separate {@code BaseEvent} hierarchies (identity, recipe,
 * post, notification) into a single polymorphic type. Jackson uses the
 * {@code eventType} discriminator for deserialization.
 * <p>
 * All concrete event classes must be registered in {@link JsonSubTypes} below.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PostCreatedEvent.class, name = "POST_CREATED_ACTION"),
        @JsonSubTypes.Type(value = PostDeletedEvent.class, name = "POST_DELETED_ACTION"),
        @JsonSubTypes.Type(value = PostLikeEvent.class, name = "POST_LIKE_ACTION"),
        @JsonSubTypes.Type(value = CommentEvent.class, name = "COMMENT_ACTION"),
        @JsonSubTypes.Type(value = UserMentionEvent.class, name = "USER_MENTION"),
        @JsonSubTypes.Type(value = XpRewardEvent.class, name = "XP_REWARDED_ACTION"),
        @JsonSubTypes.Type(value = NewFollowerEvent.class, name = "NEW_FOLLOWER_ACTION"),
        @JsonSubTypes.Type(value = GamificationNotificationEvent.class, name = "GAMIFICATION_ACTION"),
        @JsonSubTypes.Type(value = ReminderEvent.class, name = "REMINDER_ACTION"),
        @JsonSubTypes.Type(value = GroupJoinRequestedEvent.class, name = "GROUP_JOIN_REQUESTED"),
        @JsonSubTypes.Type(value = GroupMemberJoinedEvent.class, name = "GROUP_MEMBER_JOINED"),
        @JsonSubTypes.Type(value = GroupRequestApprovedEvent.class, name = "GROUP_REQUEST_APPROVED"),
        @JsonSubTypes.Type(value = GroupOwnershipTransferredEvent.class, name = "GROUP_OWNERSHIP_TRANSFERRED"),

})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseEvent {

    /**
     * Unique identifier for idempotency checks.
     * Generated on event creation to prevent duplicate processing on Kafka redelivery.
     */
    protected String eventId;
    protected String eventType;
    protected Long timestamp = System.currentTimeMillis();
    protected String userId;

    protected BaseEvent(String eventType, String userId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }
}
