package com.chefkix.culinary.features.challenge.service;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class ChallengePoolService {

    private static final int DAILY_MAX_MINUTES = 60;

    private final List<ChallengeDefinition> pool = new ArrayList<>();
    private final List<ChallengeDefinition> weeklyPool = new ArrayList<>();
    private final Map<String, ChallengeDefinition> dailyCatalog = new LinkedHashMap<>();

    @PostConstruct
    public void initPool() {
        pool.clear();
        weeklyPool.clear();
        dailyCatalog.clear();

        registerDaily(dailyChallenge(
                "italian-day", "Italian Day \uD83C\uDF5D", "Cook an Italian dish in an hour or less",
                50, DAILY_MAX_MINUTES, Map.of("cuisineType", List.of("Italian")),
                recipe -> checkCuisine(recipe, "Italian")));
        registerDaily(dailyChallenge(
                "veggie-friday", "Veggie Friday \uD83E\uDD57", "Cook a vegetarian dish",
                50, DAILY_MAX_MINUTES, Map.of("dietaryTags", List.of("vegetarian")),
                recipe -> checkDietaryTags(recipe, "vegetarian")));
        registerDaily(dailyChallenge(
                "french-flair", "French Flair \uD83E\uDD50", "Cook a French dish in an hour or less",
                60, DAILY_MAX_MINUTES, Map.of("cuisineType", List.of("French")),
                recipe -> checkCuisine(recipe, "French")));
        registerDaily(dailyChallenge(
                "mediterranean-vibes", "Mediterranean Vibes \uD83E\uDED2", "Cook a Mediterranean favorite",
                50, DAILY_MAX_MINUTES,
                Map.of("cuisineType", List.of("Greek", "Mediterranean", "Lebanese")),
                recipe -> checkCuisine(recipe, "Greek", "Mediterranean", "Lebanese")));
        registerDaily(dailyChallenge(
                "quick-meal", "Quick Meal \u26A1", "Cook a meal in 30 minutes or less",
                25, 30, Map.of(), recipe -> true));
        registerDaily(dailyChallenge(
                "taco-tuesday", "Taco Tuesday \uD83C\uDF2E", "Cook a Mexican dish in an hour or less",
                50, DAILY_MAX_MINUTES, Map.of("cuisineType", List.of("Mexican")),
                recipe -> checkCuisine(recipe, "Mexican")));
        registerDaily(dailyChallenge(
                "asian-fusion", "Asian Fusion \uD83E\uDD62", "Cook a dish from an Asian cuisine",
                50, DAILY_MAX_MINUTES,
                Map.of("cuisineType", List.of("Japanese", "Chinese", "Thai", "Korean", "Vietnamese")),
                recipe -> checkCuisine(recipe, "Japanese", "Chinese", "Thai", "Korean", "Vietnamese")));
        registerDaily(dailyChallenge(
                "indian-spices", "Indian Spices \uD83C\uDF5B", "Cook an Indian dish in an hour or less",
                50, DAILY_MAX_MINUTES, Map.of("cuisineType", List.of("Indian")),
                recipe -> checkCuisine(recipe, "Indian")));
        registerDaily(dailyChallenge(
                "plant-power", "Plant Power \uD83C\uDF31", "Cook a vegan dish",
                60, DAILY_MAX_MINUTES, Map.of("dietaryTags", List.of("vegan")),
                recipe -> checkDietaryTags(recipe, "vegan")));
        registerDaily(dailyChallenge(
                "chicken-challenge", "Chicken Challenge \uD83C\uDF57", "Cook with chicken today",
                50, DAILY_MAX_MINUTES, Map.of("ingredientContains", List.of("chicken")),
                recipe -> checkIngredients(recipe, "chicken")));
        registerDaily(dailyChallenge(
                "seafood-day", "Seafood Day \uD83E\uDD90", "Cook with fish or shellfish today",
                60, DAILY_MAX_MINUTES,
                Map.of("ingredientContains", List.of("fish", "shrimp", "salmon", "tuna")),
                recipe -> checkIngredients(recipe, "fish", "shrimp", "salmon", "tuna")));
        registerDaily(dailyChallenge(
                "express-cook", "Express Cook \uD83C\uDFC3", "Cook a meal in 20 minutes or less",
                30, 20, Map.of(), recipe -> true));
        registerDaily(dailyChallenge(
                "spice-it-up", "Spice It Up \uD83C\uDF36\uFE0F", "Cook with chili, pepper, or another spicy ingredient",
                50, DAILY_MAX_MINUTES,
                Map.of("ingredientContains", List.of("chili", "pepper", "sriracha", "jalapeno")),
                recipe -> checkIngredients(recipe, "chili", "pepper", "sriracha", "jalapeno")));
        registerDaily(dailyChallenge(
                "american-classics", "American Classics \uD83C\uDF54", "Cook an American classic",
                40, DAILY_MAX_MINUTES, Map.of("cuisineType", List.of("American")),
                recipe -> checkCuisine(recipe, "American")));

        registerRetired(dailyChallenge(
                "comfort-food", "Comfort Food \uD83C\uDF72", "Classic American or British comfort food",
                40, DAILY_MAX_MINUTES,
                Map.of("cuisineType", List.of("American", "British"),
                        "difficulty", List.of("BEGINNER", "INTERMEDIATE")),
                recipe -> checkCuisine(recipe, "American", "British")
                        && checkDifficulty(recipe, "BEGINNER", "INTERMEDIATE")));
        registerRetired(dailyChallenge(
                "expert-challenge", "Expert Challenge \uD83D\uDC68\u200D\uD83C\uDF73",
                "Only for the brave! Cook an Expert level dish.", 100, DAILY_MAX_MINUTES,
                Map.of("difficulty", List.of("EXPERT")),
                recipe -> checkDifficulty(recipe, "EXPERT")));
        registerRetired(dailyChallenge(
                "baking-day", "Baking Day \uD83C\uDF70", "Bake something sweet or savory",
                75, DAILY_MAX_MINUTES, Map.of("skillTags", List.of("baking")),
                recipe -> checkTags(recipe, "baking", "cake", "oven")));


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
                .validationLogic(r -> hasValidDuration(r, 30))
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
     */
    public ChallengeDefinition getTodayChallenge() {
        return getChallengeForDate(LocalDate.now(ZoneId.of("UTC")));
    }

    ChallengeDefinition getChallengeForDate(LocalDate date) {
        if (pool.isEmpty()) return null;
        int index = Math.floorMod(date.toEpochDay(), pool.size());
        return pool.get(index);
    }

    public Optional<ChallengeDefinition> findDailyChallengeById(String challengeId) {
        return Optional.ofNullable(dailyCatalog.get(challengeId));
    }

    /**
     */
    public ChallengeDefinition getThisWeekChallenge() {
        if (weeklyPool.isEmpty()) return null;
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        int weekOfYear = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int index = (weekOfYear - 1) % weeklyPool.size();
        return weeklyPool.get(index);
    }


    private boolean checkCuisine(Recipe r, String... allowedCuisines) {
        if (r.getCuisineType() == null) return false;
        String recipeCuisine = r.getCuisineType().toLowerCase();

        for (String allowed : allowedCuisines) {
            if (recipeCuisine.contains(allowed.toLowerCase())) return true;
        }
        return false;
    }

    private boolean checkDifficulty(Recipe r, String... allowedLevels) {
        if (r.getDifficulty() == null) return false;
        String recipeDiff = r.getDifficulty().toString().toUpperCase();

        for (String allowed : allowedLevels) {
            if (recipeDiff.equals(allowed.toUpperCase())) return true;
        }
        return false;
    }

    private boolean checkIngredients(Recipe r, String... keywords) {
        String ingredientsStr = r.getFullIngredientList() == null
                ? ""
                : r.getFullIngredientList().stream()
                        .filter(ingredient -> ingredient != null && ingredient.getName() != null)
                        .map(ingredient -> ingredient.getName().toLowerCase())
                        .reduce("", (left, right) -> left + " " + right);

        for (String keyword : keywords) {
            if (ingredientsStr.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    private boolean checkTags(Recipe r, String... keywords) {
        if (r.getSkillTags() == null) return false;

        for (String tag : r.getSkillTags()) {
            if (tag == null) continue;
            for (String keyword : keywords) {
                if (keyword == null) continue;
                if (tag.toLowerCase().contains(keyword.toLowerCase())) return true;
            }
        }
        return false;
    }

    private boolean checkDietaryTags(Recipe recipe, String... allowedTags) {
        if (recipe.getDietaryTags() == null) return false;
        return recipe.getDietaryTags().stream()
                .filter(tag -> tag != null)
                .anyMatch(tag -> List.of(allowedTags).stream()
                        .filter(allowed -> allowed != null)
                        .anyMatch(tag::equalsIgnoreCase));
    }

    private ChallengeDefinition dailyChallenge(
            String id,
            String title,
            String description,
            int bonusXp,
            int maxTimeMinutes,
            Map<String, Object> primaryCriteria,
            Predicate<Recipe> primaryRule) {
        Map<String, Object> criteria = new LinkedHashMap<>(primaryCriteria);
        int boundedMinutes = Math.min(maxTimeMinutes, DAILY_MAX_MINUTES);
        criteria.put("maxTimeMinutes", boundedMinutes);

        return ChallengeDefinition.builder()
                .id(id)
                .title(title)
                .description(description)
                .bonusXp(bonusXp)
                .criteriaMetadata(Map.copyOf(criteria))
                .validationLogic(recipe -> hasValidDuration(recipe, boundedMinutes)
                        && primaryRule.test(recipe))
                .build();
    }

    private void registerDaily(ChallengeDefinition challenge) {
        pool.add(challenge);
        dailyCatalog.put(challenge.getId(), challenge);
    }

    private void registerRetired(ChallengeDefinition challenge) {
        dailyCatalog.putIfAbsent(challenge.getId(), challenge);
    }

    private boolean hasValidDuration(Recipe recipe, int maxTimeMinutes) {
        return recipe != null
                && recipe.getTotalTimeMinutes() > 0
                && recipe.getTotalTimeMinutes() <= maxTimeMinutes;
    }
}
