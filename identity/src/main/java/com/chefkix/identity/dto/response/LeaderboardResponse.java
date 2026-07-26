package com.chefkix.identity.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardResponse {

    /**
     */
    String type;

    /**
     */
    String timeframe;

    /**
     */
    List<LeaderboardEntry> entries;

    /**
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
        @Builder.Default
        List<String> topBadges = new ArrayList<>();
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
Long recipesCooked;
    }
}
