package com.chefkix.culinary.features.session.entity;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.enums.TimerEventType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "cooking_sessions")
@CompoundIndexes({
    @CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}"),
    @CompoundIndex(name = "user_recipe_status_idx", def = "{'userId': 1, 'recipeId': 1, 'status': 1}"),
    @CompoundIndex(name = "user_started_idx", def = "{'userId': 1, 'startedAt': -1}"),
    @CompoundIndex(
            name = "user_pending_completed_idx",
            def = "{'userId': 1, 'status': 1, 'postId': 1, 'completedAt': -1}"
    )
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CookingSession {

    @Id
String id;

    @Version
Long version;

String userId;
String recipeId;
    String recipeTitle;
    List<String> coverImageUrl;
SessionStatus status;

String roomCode;

Integer currentStep;

    @Builder.Default
List<Integer> completedSteps = new ArrayList<>();

LocalDateTime startedAt;
LocalDateTime pausedAt;
    @Indexed
    LocalDateTime resumeDeadline;
LocalDateTime completedAt;
LocalDateTime abandonedAt;

    @Builder.Default
List<TimerEvent> timerEvents = new ArrayList<>();

    @Builder.Default
List<ActiveTimer> activeTimers = new ArrayList<>();

Integer rating;
String notes;

Double baseXpAwarded;
Double pendingXp;
Double remainingXpAwarded;

Double xpMultiplier;
String xpMultiplierReason;

String postId;
    @Indexed
LocalDateTime postDeadline;
    LocalDateTime linkedAt;
    LocalDateTime postDeletedAt;

boolean flagged;
String flagReason;

    boolean userDeleted;
    LocalDateTime userDeletedAt;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimerEvent {
Integer stepNumber;
TimerEventType event;
LocalDateTime clientTimestamp;
LocalDateTime serverTimestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveTimer {
Integer stepNumber;
Integer totalSeconds;
LocalDateTime startedAt;
Integer remainingSeconds;
    }
}