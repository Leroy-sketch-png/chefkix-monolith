package com.chefkix.culinary.features.recipe.dto.response;

import com.chefkix.culinary.common.enums.QualityTier;
import com.chefkix.culinary.common.enums.RecipeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipePublishResponse {
    Boolean isPublished;
    RecipeStatus moderationStatus;
    Integer qualityScore;
    QualityTier qualityTier;
}
