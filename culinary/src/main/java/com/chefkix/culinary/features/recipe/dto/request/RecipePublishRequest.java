package com.chefkix.culinary.features.recipe.dto.request;

import com.chefkix.culinary.common.enums.RecipeVisibility;
import lombok.Data;

@Data
public class RecipePublishRequest {
    RecipeVisibility visibility = RecipeVisibility.PUBLIC;
}