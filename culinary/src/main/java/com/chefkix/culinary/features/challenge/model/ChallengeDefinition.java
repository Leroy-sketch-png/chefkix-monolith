package com.chefkix.culinary.features.challenge.model;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.function.Predicate;

/**
 */
@Data
@Builder
public class ChallengeDefinition {

    private String id;

    private String title;

    private String description;

    private int bonusXp;

    @Builder.Default
    private int target = 1;

    private Map<String, Object> criteriaMetadata;

    private Predicate<Recipe> validationLogic;

    /**
     */
    public boolean isSatisfiedBy(Recipe recipe) {
        if (validationLogic == null) return false;
        return validationLogic.test(recipe);
    }
}