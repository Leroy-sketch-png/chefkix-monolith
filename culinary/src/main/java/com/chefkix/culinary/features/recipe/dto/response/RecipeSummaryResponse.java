package com.chefkix.culinary.features.recipe.dto.response;

import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.common.dto.response.AuthorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSummaryResponse {
    // --- Identity ---
    private String id;
    private Instant createdAt;

    // --- Content ---
    private String title;
    private String description;
    private List<String> coverImageUrl;

    // --- Metadata ---
    private Difficulty difficulty;
    private int totalTimeMinutes;
    private int servings;
    private String cuisineType;

    // --- Gamification ---
    private int xpReward;
    private List<String> badges;

    // --- Social ---
    private long likeCount;
    private long saveCount;
    private long viewCount;

    // --- DYNAMIC FIELDS (Thêm vào lúc query) ---
    private AuthorResponse author;
    private Boolean isLiked;
    private Boolean isSaved;
}