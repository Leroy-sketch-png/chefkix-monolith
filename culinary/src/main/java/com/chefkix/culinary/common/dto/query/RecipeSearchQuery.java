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

    String query;
    Difficulty difficulty;
    List<Difficulty> difficulties;
    String cuisineType;
    List<String> cuisineTypes;
    List<String> dietaryTags;
    Integer maxTimeMinutes;
    Double minRating;
    com.chefkix.culinary.common.enums.QualityTier qualityTier;

    String currentUserId;
    List<String> friendIds;

    @Builder.Default
    String sortBy = "newest";
}
