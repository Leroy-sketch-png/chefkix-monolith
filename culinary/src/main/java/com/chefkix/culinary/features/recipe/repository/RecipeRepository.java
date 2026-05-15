package com.chefkix.culinary.features.recipe.repository;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.recipe.repository.custom.RecipeRepositoryCustom;
import com.chefkix.culinary.features.recipe.repository.projection.CreatorInsightsRecipeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends MongoRepository<Recipe,String>, RecipeRepositoryCustom {
    List<Recipe> findTop5ByCuisineTypeInIgnoreCase(List<String> cuisines);

    List<Recipe> findTop5ByFullIngredientListInIgnoreCase(List<String> ingredients);

    Page<Recipe> findAllByUserIdInOrderByCreatedAtDesc(List<String> userIds, Pageable pageable);

    List<Recipe> findByUserIdAndStatusOrderByUpdatedAtDesc(String currentUserId, RecipeStatus recipeStatus);
    
    // Get published recipes by a specific user (for /user/{userId} endpoint)
    Page<Recipe> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, RecipeStatus status, Pageable pageable);
    
    // Get all published recipes by a user (for creator insights)
    List<Recipe> findByUserIdAndStatus(String userId, RecipeStatus status);
        List<CreatorInsightsRecipeProjection> findByUserIdAndStatus(
            String userId,
            RecipeStatus status,
            Class<CreatorInsightsRecipeProjection> projectionType);
    
    // Get published recipes for trending (sorted by trendingScore)
    Page<Recipe> findByStatus(RecipeStatus status, Pageable pageable);
    
    // Get all recipes by IDs (for fetching liked/saved recipes)
    List<Recipe> findAllByIdIn(List<String> ids);

        // Lightweight projection for summary-card flows (liked/saved profile tabs)
        @Query(
            value = "{ '_id': { $in: ?0 } }",
            fields = "{ " +
                "'createdAt': 1, " +
                "'title': 1, " +
                "'description': 1, " +
                "'coverImageUrl': 1, " +
                "'difficulty': 1, " +
                "'totalTimeMinutes': 1, " +
                "'servings': 1, " +
                "'cuisineType': 1, " +
                "'xpReward': 1, " +
                "'rewardBadges': 1, " +
                "'qualityScore': 1, " +
                "'qualityTier': 1, " +
                "'likeCount': 1, " +
                "'saveCount': 1, " +
                "'viewCount': 1 " +
                "}"
        )
        List<Recipe> findSummaryFieldsByIdIn(List<String> ids);

    Page<Recipe> findAllByIdInAndStatus(List<String> ids, RecipeStatus status, Pageable pageable);

    // Creator insights queries
    Optional<Recipe> findTopByUserIdOrderByCookCountDesc(String userId);
    List<Recipe> findByUserIdAndCookCountGreaterThanEqual(String userId, int i);

    // Achievement system queries
    long countByUserIdAndStatus(String userId, RecipeStatus status);
    List<Recipe> findByUserId(String userId);
}
