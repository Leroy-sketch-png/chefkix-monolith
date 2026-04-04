package com.chefkix.culinary.common.dto.query;

import com.chefkix.culinary.common.enums.Difficulty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeSearchQuery {

    // --- Search & Filter ---
    String query;
    Difficulty difficulty;
    String cuisineType;
    List<String> dietaryTags;
    Integer maxTimeMinutes;

    // --- Visibility context (set by service layer, NOT from user input) ---
    /** Current authenticated user's ID. Used to allow owners to see their own private recipes. */
    String currentUserId;
    /** Friend IDs of the current user. Used for FRIENDS_ONLY visibility filter. */
    List<String> friendIds;

    // --- Removed page, size ---
    // --- Removed sortBy (If using Spring's default sort via ?sort=createdAt,desc) ---
    // OR KEEP sortBy (If custom logic like "trending" is needed)
    @Builder.Default
    String sortBy = "newest";
}