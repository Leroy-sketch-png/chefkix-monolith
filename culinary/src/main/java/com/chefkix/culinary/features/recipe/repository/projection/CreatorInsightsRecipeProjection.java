package com.chefkix.culinary.features.recipe.repository.projection;

import com.chefkix.culinary.common.enums.Difficulty;
import java.util.List;

public interface CreatorInsightsRecipeProjection {
    String getId();

    String getTitle();

    long getCookCount();

    Double getCreatorXpEarned();

    List<String> getCoverImageUrl();

    Integer getCookTimeMinutes();

    Difficulty getDifficulty();

    Double getAverageRating();
}