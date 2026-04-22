package com.chefkix.culinary.common.scheduled;

import com.chefkix.culinary.features.report.dto.internal.RecipeStatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.chefkix.culinary.features.interaction.entity.RecipeLike;
import com.chefkix.culinary.features.recipe.entity.RecipeCompletion;
import com.chefkix.culinary.features.recipe.entity.Recipe;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeScheduled {

    private final MongoTemplate mongoTemplate;

    // Runs every 30 minutes (in ms)
    @Scheduled(fixedRate = 1800000)
    public void updateTrendingScores() {
        try {
            // 1. Define time window (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);

            // 2. Map to store temporary scores: Key = recipeId, Value = Score
            Map<String, Double> scoreMap = new HashMap<>();

            // --- STEP A: CALCULATE LIKE SCORE (Weight = 1) ---
            List<RecipeStatDto> likeStats = aggregateCount(
                    RecipeLike.class, "createdAt", sevenDaysAgo, "recipeId"
            );

            likeStats.forEach(stat ->
                    scoreMap.merge(stat.getRecipeId(), (double) stat.getCount() * 1.0, (a, b) -> a + b)
            );

            // --- STEP B: CALCULATE COMPLETION SCORE (Weight = 5) ---
            List<RecipeStatDto> completionStats = aggregateCount(
                    RecipeCompletion.class, "completedAt", sevenDaysAgo, "recipeId"
            );

            completionStats.forEach(stat ->
                    scoreMap.merge(stat.getRecipeId(), (double) stat.getCount() * 5.0, (a, b) -> a + b)
            );

            // --- STEP C: UPDATE DATABASE (Bulk Update) ---
            // Use Bulk Operations to update thousands of records efficiently
            var bulkOps = mongoTemplate.bulkOps(org.springframework.data.mongodb.core.BulkOperations.BulkMode.UNORDERED, Recipe.class);

            // Reset ALL trending scores to 0 first so recipes that lost activity decay naturally
            mongoTemplate.updateMulti(new Query(), new Update().set("trendingScore", 0.0), Recipe.class);

            for (Map.Entry<String, Double> entry : scoreMap.entrySet()) {
                Query query = new Query(Criteria.where("_id").is(entry.getKey()));
                Update update = new Update().set("trendingScore", entry.getValue());
                bulkOps.updateOne(query, update);
            }

            // Execute update
            if (!scoreMap.isEmpty()) {
                bulkOps.execute();
            }
        } catch (Exception e) {
            log.error("Error updating recipe trending scores. Task will retry on next schedule.", e);
        }
    }

    // Helper method to group and count
    private List<RecipeStatDto> aggregateCount(Class<?> collectionClass, String dateField, LocalDateTime fromDate, String groupField) {
        Aggregation aggregation = Aggregation.newAggregation(
                // 1. Filter by date
                Aggregation.match(Criteria.where(dateField).gte(fromDate)),
                // 2. Group by recipeId and count
                Aggregation.group(groupField).count().as("count"),
                // 3. Map _id field (which is recipeId) to the "recipeId" field of DTO
                Aggregation.project("count").and("_id").as("recipeId")
        );

        return mongoTemplate.aggregate(aggregation, collectionClass, RecipeStatDto.class).getMappedResults();
    }
}