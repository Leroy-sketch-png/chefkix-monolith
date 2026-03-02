package com.chefkix.culinary.common.scheduled;

import com.chefkix.culinary.features.report.dto.internal.RecipeStatDto;
import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecipeScheduled {

    private final MongoTemplate mongoTemplate;

    // Chạy mỗi 30 phút (đơn vị ms)
    @Scheduled(fixedRate = 1800000)
    public void updateTrendingScores() {
        // 1. Xác định khung thời gian (7 ngày qua)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 2. Map lưu trữ điểm số tạm tính: Key = recipeId, Value = Score
        Map<String, Double> scoreMap = new HashMap<>();

        // --- BƯỚC A: TÍNH ĐIỂM LIKE (Weight = 1) ---
        List<RecipeStatDto> likeStats = aggregateCount(
                RecipeLike.class, "createdAt", sevenDaysAgo, "recipeId"
        );

        likeStats.forEach(stat ->
                scoreMap.merge(stat.getRecipeId(), (double) stat.getCount() * 1.0, Double::sum)
        );

        // --- BƯỚC B: TÍNH ĐIỂM COMPLETION (Weight = 5) ---
        List<RecipeStatDto> completionStats = aggregateCount(
                RecipeCompletion.class, "completedAt", sevenDaysAgo, "recipeId"
        );

        completionStats.forEach(stat ->
                scoreMap.merge(stat.getRecipeId(), (double) stat.getCount() * 5.0, Double::sum)
        );

        // --- BƯỚC C: UPDATE VÀO DATABASE (Bulk Update) ---
        // Dùng Bulk Operations để update hàng nghìn record cực nhanh
        var bulkOps = mongoTemplate.bulkOps(org.springframework.data.mongodb.core.BulkOperations.BulkMode.UNORDERED, Recipe.class);

        // Reset điểm của TOÀN BỘ Recipe về 0 trước (để những món hết hot sẽ rớt hạng)
        // Lưu ý: Cách này đơn giản nhưng có thể gây flick nhẹ. 
        // Cách tốt hơn là update những món có trong map, và set 0 cho những món không có trong map.
        // Ở đây mình demo cách update những món CÓ điểm.

        for (Map.Entry<String, Double> entry : scoreMap.entrySet()) {
            Query query = new Query(Criteria.where("_id").is(entry.getKey()));
            Update update = new Update().set("trendingScore", entry.getValue());
            bulkOps.updateOne(query, update);
        }

        // Thực thi update
        if (!scoreMap.isEmpty()) {
            bulkOps.execute();
        }
    }

    // Hàm helper để gom nhóm và đếm
    private List<RecipeStatDto> aggregateCount(Class<?> collectionClass, String dateField, LocalDateTime fromDate, String groupField) {
        Aggregation aggregation = Aggregation.newAggregation(
                // 1. Lọc theo ngày
                Aggregation.match(Criteria.where(dateField).gte(fromDate)),
                // 2. Gom nhóm theo recipeId và đếm
                Aggregation.group(groupField).count().as("count"),
                // 3. Map field _id (là recipeId) ra field "id" của DTO
                Aggregation.project("count").and("_id").as("id")
        );

        return mongoTemplate.aggregate(aggregation, collectionClass, RecipeStatDto.class).getMappedResults();
    }
}