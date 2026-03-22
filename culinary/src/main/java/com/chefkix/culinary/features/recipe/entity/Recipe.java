package com.chefkix.culinary.features.recipe.entity;

import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.RecipeVisibility;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "recipes")
// Index kép để query News Feed nhanh: Lấy các bài đã published, sắp xếp mới nhất
@CompoundIndexes({
        @CompoundIndex(name = "published_date_idx", def = "{'isPublished': 1, 'createdAt': -1}")
})
public class Recipe {

    // --- CORE IDENTITY ---
    @Id
    String id;

    @Indexed // Index đơn để tìm recipe theo userId nhanh
    String userId;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;

    Instant publishedAt;
    RecipeVisibility recipeVisibility;
//    @Builder.Default
//    Boolean isPublished = false;

    @Indexed
    @Builder.Default
    RecipeStatus status = RecipeStatus.DRAFT;


    // --- MEDIA ---
    @Builder.Default
    List<String> coverImageUrl = new ArrayList<>();

    @Builder.Default
    List<String> videoUrl = new ArrayList<>();

    // --- BASIC INFO ---
    @TextIndexed(weight = 10)
    String title;

    @TextIndexed(weight = 5)
    String description;

    Difficulty difficulty; // Enum: BEGINNER, INTERMEDIATE...

    int prepTimeMinutes;
    int cookTimeMinutes;
    int totalTimeMinutes;
    int servings;
    @Indexed(direction = IndexDirection.DESCENDING)
    @Builder.Default
    Double trendingScore = 0.0;

    @Indexed
    @TextIndexed(weight = 3)
    String cuisineType;

    @Builder.Default
    List<String> dietaryTags = new ArrayList<>();

    Integer caloriesPerServing;

    // --- STRUCTURE ---
    @Builder.Default
    List<Ingredient> fullIngredientList = new ArrayList<>();

    @Builder.Default
    List<Step> steps = new ArrayList<>();

    // --- GAMIFICATION CORE ---
    int xpReward;
    double difficultyMultiplier;

    @Builder.Default
    List<String> rewardBadges = new ArrayList<>(); // JSON: "badges"

    @Builder.Default
    List<String> skillTags = new ArrayList<>();

    // --- [NEW] AI METADATA: XP BREAKDOWN ---
    // Giải thích chi tiết tại sao user nhận được mức XP này
    XpBreakdown xpBreakdown;

    // --- [NEW] ANTI-CHEAT VALIDATION ---
    // Kết quả kiểm duyệt từ AI
    ValidationMetadata validation;

    // --- [NEW] AI ENRICHMENT ---
    // Dữ liệu bổ sung do AI tạo ra (Story, Tips, Context...)
    EnrichmentMetadata enrichment;

    // --- SOCIAL COUNTERS ---
    @Builder.Default
    long likeCount = 0;
    @Builder.Default
    long saveCount = 0;
    @Builder.Default
    long viewCount = 0;

    // --- [NEW] BACKEND TRACKED STATS ---
    @Builder.Default
    long cookCount = 0;         // Số người hoàn thành món này

    @Builder.Default
    long masteredByCount = 0;   // Số người đạt level Master món này

    @Builder.Default
    Double averageRating = 0.0;    // Điểm trung bình (1.0 - 5.0)

    @Builder.Default
    Integer creatorXpEarned = 0; // Tổng XP tác giả kiếm được từ món này

    // ==========================================
    // INNER CLASSES (POJOs)
    // ==========================================

    // --- [IMPLEMENTED] XP BREAKDOWN ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class XpBreakdown {
        int base;
        String baseReason;
        int steps;
        String stepsReason;
        int time;
        String timeReason;

        Integer techniques;
        String techniquesReason;

        int total;
    }

    // --- [IMPLEMENTED] VALIDATION METADATA ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationMetadata {
        boolean xpValidated;
        double validationConfidence; // 0.0 - 1.0

        @Builder.Default
        List<String> validationIssues = new ArrayList<>();

        boolean xpAdjusted;

        // [Optional] Thêm field này để biết bài này có dùng AI không
        boolean aiUsed;
    }

    // --- [IMPLEMENTED] ENRICHMENT METADATA ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrichmentMetadata {
        @Builder.Default
        List<String> equipmentNeeded = new ArrayList<>();

        @Builder.Default
        List<String> techniqueGuides = new ArrayList<>(); // List tên kỹ thuật

        @Builder.Default
        List<String> seasonalTags = new ArrayList<>();

        // Map: "Ingredient Name" -> ["Sub1", "Sub2"]
        @Builder.Default
        Map<String, List<String>> ingredientSubstitutions = new HashMap<>();

        // [MỚI] Field này để hứng data "regionalOrigin" từ AI trả về (String)
        String regionalOrigin;

        // Field này có thể giữ lại nếu sau này muốn mở rộng sâu hơn
        CulturalContext culturalContext;

        String recipeStory;
        String chefNotes;
        boolean aiEnriched;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CulturalContext {
        String region;
        String background;
        String significance;
    }
}