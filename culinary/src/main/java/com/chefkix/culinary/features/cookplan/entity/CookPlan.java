package com.chefkix.culinary.features.cookplan.entity;

import com.chefkix.culinary.common.enums.MealRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cook_plans")
@CompoundIndex(name = "user_plan_date_idx", def = "{'userId': 1, 'planDate': -1, 'createdAt': -1}")
public class CookPlan {

    @Id
    private String id;
    private String userId;
    private LocalDate planDate;
    private CookPlanMode mode;
    private int householdSize;
    private int maxActiveMinutes;
    private boolean pantryFirst;

    @Builder.Default
    private List<CookBatch> cookBatches = new ArrayList<>();

    @Builder.Default
    private List<EatingOccasion> eatingOccasions = new ArrayList<>();

    @Builder.Default
    private List<ShoppingItem> shoppingList = new ArrayList<>();

    @Builder.Default
    private List<String> unmetConstraints = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CookBatch {
        private String id;
        private String title;
        private int activeMinutes;
        private int totalMinutes;

        @Builder.Default
        private List<CookPlanDish> dishes = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CookPlanDish {
        private String recipeId;
        private String title;
        private String coverImageUrl;
        private String cuisineType;
        private MealRole mealRole;
        private int activeMinutes;
        private int totalTimeMinutes;
        private int sourceServings;
        private int plannedServings;
        private int pantryIngredientCount;
        private int shoppingIngredientCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EatingOccasion {
        private String name;
        private String batchId;
        private int servings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShoppingItem {
        private String ingredient;
        private String quantity;
        private String unit;

        @Builder.Default
        private List<String> sourceRecipes = new ArrayList<>();
    }
}
