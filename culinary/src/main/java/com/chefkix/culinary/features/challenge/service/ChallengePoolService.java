package com.chefkix.culinary.features.challenge.service;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChallengePoolService {

    private final List<ChallengeDefinition> pool = new ArrayList<>();
    private final List<ChallengeDefinition> weeklyPool = new ArrayList<>();

    @PostConstruct
    public void initPool() {

        // 1. Italian Day 🍝
        // criteria: {"cuisineType": ["Italian"]}
        pool.add(ChallengeDefinition.builder()
                .id("italian-day")
                .title("Italian Day 🍝")
                .description("Cook any Italian dish")
                .bonusXp(50)
                .criteriaMetadata(Map.of("cuisineType", List.of("Italian")))
                .validationLogic(r -> checkCuisine(r, "Italian"))
                .build());

        // 2. Quick Meal ⚡
        // criteria: {"maxTimeMinutes": 30}
        pool.add(ChallengeDefinition.builder()
                .id("quick-meal")
                .title("Quick Meal ⚡")
                .description("Cook a meal in under 30 minutes")
                .bonusXp(25)
                .criteriaMetadata(Map.of("maxTimeMinutes", 30))
                .validationLogic(r -> r.getTotalTimeMinutes() <= 30)
                .build());

        // 3. Spice It Up 🌶️
        // criteria: {"ingredientContains": ["chili", "pepper", "spicy"]}
        pool.add(ChallengeDefinition.builder()
                .id("spice-it-up")
                .title("Spice It Up 🌶️")
                .description("Use spicy ingredients like chili or pepper")
                .bonusXp(50)
                .criteriaMetadata(Map.of("ingredientContains", List.of("chili", "pepper", "spicy")))
                .validationLogic(r -> checkIngredients(r, "chili", "pepper", "spicy"))
                .build());

        // 4. Asian Fusion 🥢
        // criteria: {"cuisineType": ["Japanese", "Chinese", "Thai", "Korean", "Vietnamese"]}
        pool.add(ChallengeDefinition.builder()
                .id("asian-fusion")
                .title("Asian Fusion 🥢")
                .description("Cook a dish from Asian cuisine")
                .bonusXp(50)
                .criteriaMetadata(Map.of("cuisineType", List.of("Japanese", "Chinese", "Thai", "Korean", "Vietnamese")))
                .validationLogic(r -> checkCuisine(r, "Japanese", "Chinese", "Thai", "Korean", "Vietnamese"))
                .build());

        // 5. Comfort Food 🍲 (Combines 2 conditions)
        // criteria: {"cuisineType": ["American", "British"], "difficulty": ["BEGINNER", "INTERMEDIATE"]}
        pool.add(ChallengeDefinition.builder()
                .id("comfort-food")
                .title("Comfort Food 🍲")
                .description("Classic American or British comfort food")
                .bonusXp(40)
                .criteriaMetadata(Map.of(
                        "cuisineType", List.of("American", "British"),
                        "difficulty", List.of("BEGINNER", "INTERMEDIATE")
                ))
                .validationLogic(r -> checkCuisine(r, "American", "British")
                        && checkDifficulty(r, "BEGINNER", "INTERMEDIATE"))
                .build());

        // 6. Expert Challenge 👨‍🍳
        // criteria: {"difficulty": ["EXPERT"]}
        pool.add(ChallengeDefinition.builder()
                .id("expert-challenge")
                .title("Expert Challenge 👨‍🍳")
                .description("Only for the brave! Cook an Expert level dish.")
                .bonusXp(100)
                .criteriaMetadata(Map.of("difficulty", List.of("EXPERT")))
                .validationLogic(r -> checkDifficulty(r, "EXPERT"))
                .build());

        // 7. Baking Day 🍰
        // criteria: {"skillTags": ["baking"]}
        pool.add(ChallengeDefinition.builder()
                .id("baking-day")
                .title("Baking Day 🍰")
                .description("Time to bake something sweet or savory")
                .bonusXp(75)
                .criteriaMetadata(Map.of("skillTags", List.of("baking")))
                .validationLogic(r -> checkTags(r, "baking", "cake", "oven"))
                .build());

        // =============================================
        // WEEKLY CHALLENGES — 4 rotating, multi-target
        // =============================================

        weeklyPool.add(ChallengeDefinition.builder()
                .id("weekly-italian-week")
                .title("Italian Week \uD83C\uDDEE\uD83C\uDDF9")
                .description("Cook 3 Italian recipes this week")
                .bonusXp(150)
                .target(3)
                .criteriaMetadata(Map.of("cuisineType", List.of("Italian")))
                .validationLogic(r -> checkCuisine(r, "Italian"))
                .build());

        weeklyPool.add(ChallengeDefinition.builder()
                .id("weekly-variety-chef")
                .title("Variety Chef \uD83C\uDF0D")
                .description("Cook recipes from 3 different cuisines this week")
                .bonusXp(200)
                .target(3)
                .criteriaMetadata(Map.of("cuisineType", List.of("ANY")))
                .validationLogic(r -> r.getCuisineType() != null && !r.getCuisineType().isBlank())
                .build());

        weeklyPool.add(ChallengeDefinition.builder()
                .id("weekly-speed-runner")
                .title("Speed Runner \u26A1")
                .description("Complete 5 quick meals (under 30 min) this week")
                .bonusXp(175)
                .target(5)
                .criteriaMetadata(Map.of("maxTimeMinutes", 30))
                .validationLogic(r -> r.getTotalTimeMinutes() <= 30)
                .build());

        weeklyPool.add(ChallengeDefinition.builder()
                .id("weekly-master-baker")
                .title("Master Baker \uD83C\uDF82")
                .description("Bake 2 recipes this week")
                .bonusXp(175)
                .target(2)
                .criteriaMetadata(Map.of("skillTags", List.of("baking")))
                .validationLogic(r -> checkTags(r, "baking", "cake", "oven", "bread", "pastry"))
                .build());
    }

    /**
     * Daily challenge rotation by day of year (UTC).
     */
    public ChallengeDefinition getTodayChallenge() {
        if (pool.isEmpty()) return null;
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        int index = (today.getDayOfYear() - 1) % pool.size();
        return pool.get(index);
    }

    /**
     * Weekly challenge rotation by ISO week number (UTC).
     * Resets every Monday 00:00 UTC.
     */
    public ChallengeDefinition getThisWeekChallenge() {
        if (weeklyPool.isEmpty()) return null;
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        int weekOfYear = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int index = (weekOfYear - 1) % weeklyPool.size();
        return weeklyPool.get(index);
    }

    // =========================================================================
    // HELPER METHODS (Keep code clean and prevent NullPointerException)
    // =========================================================================

    // 1. Check Cuisine (Case-insensitive string comparison)
    private boolean checkCuisine(Recipe r, String... allowedCuisines) {
        if (r.getCuisineType() == null) return false;
        String recipeCuisine = r.getCuisineType().toLowerCase();

        for (String allowed : allowedCuisines) {
            if (recipeCuisine.contains(allowed.toLowerCase())) return true;
        }
        return false;
    }

    // 2. Check Difficulty
    private boolean checkDifficulty(Recipe r, String... allowedLevels) {
        if (r.getDifficulty() == null) return false;
        String recipeDiff = r.getDifficulty().toString().toUpperCase(); // Assuming Difficulty is Enum or String

        for (String allowed : allowedLevels) {
            if (recipeDiff.equals(allowed.toUpperCase())) return true;
        }
        return false;
    }

    // 3. Check Ingredients (Search within ingredient list)
    private boolean checkIngredients(Recipe r, String... keywords) {
        // Depending on how your Recipe stores ingredients (String or List<Object>)
        // Common approach: convert everything to string for search
        String ingredientsStr = "";

        if (r.getFullIngredientList() != null) {
            // Assuming r.getIngredients() returns List<Ingredient>
            ingredientsStr = r.getFullIngredientList().toString().toLowerCase();
        } else if (r.getDescription() != null) {
            // Fallback if no ingredients, search in description
            ingredientsStr = r.getDescription().toLowerCase();
        }

        for (String keyword : keywords) {
            if (ingredientsStr.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    // 4. Check Tags/Skills
    private boolean checkTags(Recipe r, String... keywords) {
        // Assuming Recipe has field List<String> tags or categories
        if (r.getDietaryTags() == null) return false;

        for (String tag : r.getDietaryTags()) {
            for (String keyword : keywords) {
                if (tag.toLowerCase().contains(keyword.toLowerCase())) return true;
            }
        }
        return false;
    }
}