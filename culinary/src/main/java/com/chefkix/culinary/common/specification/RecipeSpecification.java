package com.chefkix.culinary.common.specification;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RecipeSpecification {

    /**
     * Convert from Search DTO to MongoDB Criteria.
     * Equivalent to writing a WHERE clause in SQL.
     */
    public static Criteria getCriteria(RecipeSearchQuery queryDto) {
        List<Criteria> criteriaList = new ArrayList<>();

        // 1. Base Condition: Always only fetch published recipes
        // NOTE: Entity uses 'status' enum (DRAFT, PUBLISHED, ARCHIVED), NOT boolean 'isPublished'
        criteriaList.add(Criteria.where("status").is("PUBLISHED"));

        // 1b. VISIBILITY FILTER: Respect recipe visibility settings
        // PUBLIC = everyone can see
        // PRIVATE = only the owner can see
        // FRIENDS_ONLY = owner + friends can see
        if (queryDto.getCurrentUserId() != null) {
            List<Criteria> visibilityCriteria = new ArrayList<>();
            // PUBLIC recipes are always visible
            visibilityCriteria.add(Criteria.where("recipeVisibility").is("PUBLIC"));
            // Null visibility defaults to PUBLIC (backward compatibility)
            visibilityCriteria.add(Criteria.where("recipeVisibility").is(null));
            // Owner can always see their own recipes
            visibilityCriteria.add(Criteria.where("userId").is(queryDto.getCurrentUserId()));
            // FRIENDS_ONLY: visible to friends
            if (queryDto.getFriendIds() != null && !queryDto.getFriendIds().isEmpty()) {
                visibilityCriteria.add(new Criteria().andOperator(
                        Criteria.where("recipeVisibility").is("FRIENDS_ONLY"),
                        Criteria.where("userId").in(queryDto.getFriendIds())
                ));
            }
            criteriaList.add(new Criteria().orOperator(visibilityCriteria.toArray(new Criteria[0])));
        } else {
            // No auth context: only show PUBLIC recipes
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("recipeVisibility").is("PUBLIC"),
                    Criteria.where("recipeVisibility").is(null)
            ));
        }

        // 2. Search by keyword (Title or Description)
        // Use Regex for fuzzy matching (LIKE %query%), 'i' is case-insensitive
        if (StringUtils.hasText(queryDto.getQuery())) {
            String escaped = Pattern.quote(queryDto.getQuery().trim());
            String regex = ".*" + escaped + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("fullIngredientList.name").regex(regex, "i")
            ));
        }

        // 3. Filter by Difficulty (Exact Match)
        if (queryDto.getDifficulty() != null) {
            criteriaList.add(Criteria.where("difficulty").is(queryDto.getDifficulty()));
        }

        // 4. Filter by Cuisine Type (Exact Match)
        if (StringUtils.hasText(queryDto.getCuisineType())) {
            criteriaList.add(Criteria.where("cuisineType").is(queryDto.getCuisineType()));
        }

        // 5. Filter by Cook Time (Less Than or Equal)
        // Find recipes that cook faster than or equal to the time the user selected
        if (queryDto.getMaxTimeMinutes() != null) {
            criteriaList.add(Criteria.where("totalTimeMinutes").lte(queryDto.getMaxTimeMinutes()));
        }

        // 6. Filter by Dietary Tags
        // Logic: Recipe must contain ALL tags the user selected (AND logic)
        // Example: Select "Vegan" and "Keto" -> Recipe must have both tags.
        if (queryDto.getDietaryTags() != null && !queryDto.getDietaryTags().isEmpty()) {
            criteriaList.add(Criteria.where("dietaryTags").all(queryDto.getDietaryTags()));
        }

        // 7. Combine all conditions using AND operator
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}