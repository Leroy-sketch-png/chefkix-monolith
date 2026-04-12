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
        // 1. Use Specification to build the filter (Criteria)
        // Filtering logic is encapsulated in the Specification file, keeping this file clean
        Criteria criteria = RecipeSpecification.getCriteria(queryDto);

        // 2. Create Query from Criteria and apply Pageable
        Query query = new Query(criteria).with(pageable);

        // 3. Handle Custom Sort (If there's special sort logic not available in Pageable)
        // Example: Sort by "Trending" (View + Like)
        applyCustomSorting(query, queryDto.getSortBy());

        // 4. Execute count query to get total records (for calculating total pages)
        // Note: count() can be slow with large datasets, can optimize with estimateCount() if needed
        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), Recipe.class);

        // 5. Execute query to fetch data
        List<Recipe> recipes = mongoTemplate.find(query, Recipe.class);

        return new PageImpl<>(recipes, pageable, total);
    }

    @Override
    public void incrementViewCount(String recipeId) {
        // Use updateFirst for atomic increment, no need to load the object and save it back
        Query query = Query.query(Criteria.where("id").is(recipeId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Recipe.class);
    }

    // Helper method for handling special sorting
    private void applyCustomSorting(Query query, String sortBy) {
        if ("trending".equalsIgnoreCase(sortBy)) {
            // Trending logic: Prioritize high Views, then high Likes
            query.with(Sort.by(Sort.Direction.DESC, "viewCount", "likeCount"));
        }
        else if ("xpReward".equalsIgnoreCase(sortBy)) {
            query.with(Sort.by(Sort.Direction.DESC, "xpReward"));
        }
        // Basic sort cases (createdAt, title...) are already handled by Pageable
    }

    @Override
    public Recipe updateLikeCount(String recipeId, int amount) {
        return updateCounter(recipeId, "likeCount", amount);
    }

    @Override
    public Recipe updateSaveCount(String recipeId, int amount) {
        return updateCounter(recipeId, "saveCount", amount);
    }

    // Helper method reusing the update logic
    private Recipe updateCounter(String recipeId, String fieldName, int amount) {
        Query query = Query.query(Criteria.where("id").is(recipeId));

        if (amount < 0) {
            // For decrements, add a floor guard: only decrement if current value > 0
            // This prevents counters from going negative (e.g., likeCount = -1)
            query.addCriteria(Criteria.where(fieldName).gt(0));
        }

        Update update = new Update().inc(fieldName, amount);

        // returnNew(true): Return the object AFTER the increment
        return mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                Recipe.class
        );
    }

    @Override
    public List<Recipe> findPublishedForIngredientMatching() {
        Query query = Query.query(Criteria.where("status").is(RecipeStatus.PUBLISHED));
        // Project only the fields needed for ingredient matching — avoids loading
        // steps, enrichment, validation, and other heavy nested objects.
        query.fields()
                .include("id")
                .include("title")
                .include("coverImageUrl")
                .include("totalTimeMinutes")
                .include("difficulty")
                .include("fullIngredientList");
        return mongoTemplate.find(query, Recipe.class);
    }
}