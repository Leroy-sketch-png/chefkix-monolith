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

    @Scheduled(fixedRate = 1800000)
    public void updateTrendingScores() {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);

            Map<String, Double> scoreMap = new HashMap<>();

            List<RecipeStatDto> likeStats = aggregateCount(
                    RecipeLike.class, "createdAt", sevenDaysAgo, "recipeId"
            );

            likeStats.forEach(stat ->
                    scoreMap.merge(stat.getRecipeId(), (double) stat.getCount() * 1.0, (a, b) -> a + b)
            );

            List<RecipeStatDto> completionStats = aggregateCount(
                    RecipeCompletion.class, "completedAt", sevenDaysAgo, "recipeId"
            );

            completionStats.forEach(stat ->
                    scoreMap.merge(stat.getRecipeId(), (double) stat.getCount() * 5.0, (a, b) -> a + b)
            );

            var bulkOps = mongoTemplate.bulkOps(org.springframework.data.mongodb.core.BulkOperations.BulkMode.UNORDERED, Recipe.class);

            mongoTemplate.updateMulti(new Query(), new Update().set("trendingScore", 0.0), Recipe.class);

            for (Map.Entry<String, Double> entry : scoreMap.entrySet()) {
                Query query = new Query(Criteria.where("_id").is(entry.getKey()));
                Update update = new Update().set("trendingScore", entry.getValue());
                bulkOps.updateOne(query, update);
            }

            if (!scoreMap.isEmpty()) {
                bulkOps.execute();
            }
        } catch (Exception e) {
            log.error("Error updating recipe trending scores. Task will retry on next schedule.", e);
        }
    }

    private List<RecipeStatDto> aggregateCount(Class<?> collectionClass, String dateField, LocalDateTime fromDate, String groupField) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(dateField).gte(fromDate)),
                Aggregation.group(groupField).count().as("count"),
                Aggregation.project("count").and("_id").as("recipeId")
        );

        return mongoTemplate.aggregate(aggregation, collectionClass, RecipeStatDto.class).getMappedResults();
    }
}