package com.chefkix.identity.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response DTO for leaderboard endpoint.
 * Contains ranked list of users and current user's position.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardResponse {

    /**
     * Type of leaderboard: global, friends, or league
     */
    String type;

    /**
     * Timeframe: weekly, monthly, or all_time
     */
    String timeframe;

    /**
     * List of top entries (max 50 by default)
     */
    List<LeaderboardEntry> entries;

    /**
     * Current user's rank info (always included even if not in top entries)
     */
    MyRank myRank;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LeaderboardEntry {
        int rank;
        String userId;
        String username;
        String displayName;
        String avatarUrl;
        int level;
        double xpThisWeek;
        long recipesCooked;
        int streak;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MyRank {
        int rank;
        double xpThisWeek;
        Double xpToNextRank;
        Integer nextRankPosition;
        Long recipesCooked; // User's total completed cooking sessions
    }
}
