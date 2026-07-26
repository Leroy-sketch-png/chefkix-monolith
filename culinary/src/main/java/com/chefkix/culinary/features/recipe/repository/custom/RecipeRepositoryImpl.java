package com.chefkix.culinary.features.recipe.repository.custom;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.specification.RecipeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RecipeRepositoryImpl implements RecipeRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Recipe> searchRecipes(RecipeSearchQuery queryDto, Pageable pageable) {
        Criteria criteria = RecipeSpecification.getCriteria(queryDto);

        Query query = new Query(criteria).with(pageable);

        applyCustomSorting(query, queryDto.getSortBy());

        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), Recipe.class);

        List<Recipe> recipes = mongoTemplate.find(query, Recipe.class);

        return new PageImpl<>(recipes, pageable, total);
    }

    @Override
    public void incrementViewCount(String recipeId) {
        Query query = Query.query(Criteria.where("id").is(recipeId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Recipe.class);
    }

    private void applyCustomSorting(Query query, String sortBy) {
        if ("trending".equalsIgnoreCase(sortBy)) {
            query.with(Sort.by(Sort.Direction.DESC, "viewCount", "likeCount"));
        }
        else if ("xpReward".equalsIgnoreCase(sortBy)) {
            query.with(Sort.by(Sort.Direction.DESC, "xpReward"));
        }
    }

    @Override
    public Recipe updateLikeCount(String recipeId, int amount) {
        return updateCounter(recipeId, "likeCount", amount);
    }

    @Override
    public Recipe updateSaveCount(String recipeId, int amount) {
        return updateCounter(recipeId, "saveCount", amount);
    }

    private Recipe updateCounter(String recipeId, String fieldName, int amount) {
        Query query = Query.query(Criteria.where("id").is(recipeId));

        if (amount < 0) {
            query.addCriteria(Criteria.where(fieldName).gt(0));
        }

        Update update = new Update().inc(fieldName, amount);

        return mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                Recipe.class
        );
    }

    @Override
    public List<Recipe> findPublishedForIngredientMatching() {
        Query query = Query.query(Criteria.where("status").is(RecipeStatus.PUBLISHED));
        query.fields()
                .include("id")
                .include("title")
                .include("coverImageUrl")
                .include("prepTimeMinutes")
                .include("cookTimeMinutes")
                .include("totalTimeMinutes")
                .include("servings")
                .include("difficulty")
                .include("cuisineType")
                .include("dietaryTags")
                .include("mealRole")
                .include("fullIngredientList");
        return mongoTemplate.find(query, Recipe.class);
    }
}
