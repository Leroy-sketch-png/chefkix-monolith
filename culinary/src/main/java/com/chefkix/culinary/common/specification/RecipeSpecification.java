package com.chefkix.culinary.common.specification;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RecipeSpecification {

    /**
     */
    public static Criteria getCriteria(RecipeSearchQuery queryDto) {
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("status").is("PUBLISHED"));

        if (queryDto.getCurrentUserId() != null) {
            List<Criteria> visibilityCriteria = new ArrayList<>();
            visibilityCriteria.add(Criteria.where("recipeVisibility").is("PUBLIC"));
            visibilityCriteria.add(Criteria.where("recipeVisibility").is(null));
            visibilityCriteria.add(Criteria.where("userId").is(queryDto.getCurrentUserId()));
            if (queryDto.getFriendIds() != null && !queryDto.getFriendIds().isEmpty()) {
                visibilityCriteria.add(new Criteria().andOperator(
                        Criteria.where("recipeVisibility").is("FRIENDS_ONLY"),
                        Criteria.where("userId").in(queryDto.getFriendIds())
                ));
            }
            criteriaList.add(new Criteria().orOperator(visibilityCriteria.toArray(new Criteria[0])));
        } else {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("recipeVisibility").is("PUBLIC"),
                    Criteria.where("recipeVisibility").is(null)
            ));
        }

        if (StringUtils.hasText(queryDto.getQuery())) {
            String escaped = Pattern.quote(queryDto.getQuery().trim());
            String regex = ".*" + escaped + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("fullIngredientList.name").regex(regex, "i")
            ));
        }

        if (queryDto.getDifficulty() != null) {
            criteriaList.add(Criteria.where("difficulty").is(queryDto.getDifficulty()));
        }

        if (StringUtils.hasText(queryDto.getCuisineType())) {
            criteriaList.add(Criteria.where("cuisineType").is(queryDto.getCuisineType()));
        }

        if (queryDto.getMaxTimeMinutes() != null) {
            criteriaList.add(Criteria.where("totalTimeMinutes").lte(queryDto.getMaxTimeMinutes()));
        }

        if (queryDto.getDietaryTags() != null && !queryDto.getDietaryTags().isEmpty()) {
            criteriaList.add(Criteria.where("dietaryTags").all(queryDto.getDietaryTags()));
        }

        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}