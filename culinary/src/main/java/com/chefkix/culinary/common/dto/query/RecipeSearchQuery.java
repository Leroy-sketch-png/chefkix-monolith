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

    // --- XÓA page, size ---
    // --- XÓA sortBy (Nếu muốn dùng sort mặc định của Spring ?sort=createdAt,desc) ---
    // HOẶC GIỮ sortBy (Nếu muốn custom logic như "trending")
    @Builder.Default
    String sortBy = "newest";
}