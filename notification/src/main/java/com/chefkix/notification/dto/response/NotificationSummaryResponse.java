package com.chefkix.notification.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response for GET /notification/summary-since.
 * Powers the "Welcome Back" card on dashboard — aggregated activity since last visit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationSummaryResponse {
    int newLikes;
    int newFollowers;
    int newComments;
    int newMentions;
    int challengesAvailable;
    int xpAwarded;
    int levelsGained;
    int badgesEarned;
    int roomInvites;
    int totalNotifications;
    Instant since;
}
