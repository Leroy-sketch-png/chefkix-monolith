package com.chefkix.culinary.features.challenge.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ChallengeHistoryResponse.java


@Data
@Builder
public class ChallengeHistoryResponse {

    // ✅ Dùng List để JSON ra dạng mảng: [ {...}, {...} ]
    private List<ChallengeItemDto> challenges;

    // ✅ Object stats bình thường
    private StatsDto stats;

    @Data
    @Builder
    public static class ChallengeItemDto {
        private String id;
        private String title;
        private LocalDate date;         // Hoặc LocalDate
        private boolean completed;
        private LocalDateTime completedAt;  // Hoặc LocalDateTime
        private int bonusXpEarned;
        private RecipeShortInfo recipeCooked;
    }

    @Data
    @Builder
    public static class StatsDto {
        private long totalCompleted;
        private int currentStreak;
        private int longestStreak;
        private long totalBonusXp;
    }

    @Data @Builder
    public static class RecipeShortInfo {
        private String id;
        private String title;
    }
}