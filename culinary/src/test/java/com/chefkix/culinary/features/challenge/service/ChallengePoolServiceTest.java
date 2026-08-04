package com.chefkix.culinary.features.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import com.chefkix.culinary.features.recipe.entity.Ingredient;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChallengePoolServiceTest {

    private final ChallengePoolService poolService = new ChallengePoolService();

    @BeforeEach
    void setUp() {
        poolService.initPool();
    }

    @Test
    void rotatesFourteenDistinctWeekdayChallengesAcrossYearBoundary() {
        LocalDate start = LocalDate.of(2026, 12, 24);
        List<ChallengeDefinition> rotation = java.util.stream.IntStream.range(0, 14)
                .mapToObj(offset -> poolService.getChallengeForDate(start.plusDays(offset)))
                .toList();

        assertThat(rotation).extracting(ChallengeDefinition::getId)
                .doesNotHaveDuplicates()
                .doesNotContain("expert-challenge");
        assertThat(poolService.getChallengeForDate(start.plusDays(14)).getId())
                .isEqualTo(rotation.getFirst().getId());
    }

    @Test
    void namedWeekdayChallengesOnlyAppearOnTheirNamedWeekdays() {
        LocalDate tacoDate = LocalDate.of(2026, 1, 6);
        LocalDate veggieDate = LocalDate.of(2026, 1, 2);

        assertThat(poolService.getChallengeForDate(tacoDate).getId()).isEqualTo("taco-tuesday");
        assertThat(tacoDate.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(poolService.getChallengeForDate(tacoDate.plusDays(14)).getId()).isEqualTo("taco-tuesday");

        assertThat(poolService.getChallengeForDate(veggieDate).getId()).isEqualTo("veggie-friday");
        assertThat(veggieDate.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(poolService.getChallengeForDate(veggieDate.plusDays(14)).getId()).isEqualTo("veggie-friday");
    }

    @Test
    void everyActiveChallengeRequiresPositiveTimeWithinItsPublishedLimit() {
        LocalDate start = LocalDate.of(2026, 12, 24);

        for (int offset = 0; offset < 14; offset++) {
            ChallengeDefinition challenge = poolService.getChallengeForDate(start.plusDays(offset));
            int maxMinutes = ((Number) challenge.getCriteriaMetadata().get("maxTimeMinutes")).intValue();
            Recipe matchingRecipe = matchingRecipe(challenge.getCriteriaMetadata(), maxMinutes);

            assertThat(maxMinutes).isBetween(1, 60);
            assertThat(challenge.isSatisfiedBy(matchingRecipe))
                    .as("%s accepts its exact time boundary", challenge.getId())
                    .isTrue();

            matchingRecipe.setTotalTimeMinutes(0);
            assertThat(challenge.isSatisfiedBy(matchingRecipe))
                    .as("%s rejects unknown time", challenge.getId())
                    .isFalse();

            matchingRecipe.setTotalTimeMinutes(-1);
            assertThat(challenge.isSatisfiedBy(matchingRecipe))
                    .as("%s rejects negative time", challenge.getId())
                    .isFalse();

            matchingRecipe.setTotalTimeMinutes(maxMinutes + 1);
            assertThat(challenge.isSatisfiedBy(matchingRecipe))
                    .as("%s rejects over-limit time", challenge.getId())
                    .isFalse();
        }
    }

    @Test
    void retiredDefinitionsRemainResolvableWithoutReturningToRotation() {
        ChallengeDefinition retiredExpert = poolService.findDailyChallengeById("expert-challenge")
                .orElseThrow();
        ChallengeDefinition retiredBaking = poolService.findDailyChallengeById("baking-day")
                .orElseThrow();
        Recipe bakingRecipe = Recipe.builder()
                .totalTimeMinutes(45)
                .skillTags(List.of("baking"))
                .dietaryTags(List.of())
                .build();

        assertThat(retiredExpert.getTitle()).startsWith("Expert Challenge");
        assertThat(retiredBaking.isSatisfiedBy(bakingRecipe)).isTrue();
    }

    private Recipe matchingRecipe(Map<String, Object> criteria, int totalTimeMinutes) {
        Recipe recipe = Recipe.builder().totalTimeMinutes(totalTimeMinutes).build();
        firstString(criteria, "cuisineType").ifPresent(recipe::setCuisineType);
        firstString(criteria, "ingredientContains").ifPresent(ingredient ->
                recipe.setFullIngredientList(List.of(new Ingredient(ingredient, "1", "unit"))));
        firstString(criteria, "dietaryTags").ifPresent(tag -> recipe.setDietaryTags(List.of(tag)));
        firstString(criteria, "skillTags").ifPresent(tag -> recipe.setSkillTags(List.of(tag)));
        return recipe;
    }

    private java.util.Optional<String> firstString(Map<String, Object> criteria, String key) {
        if (!(criteria.get(key) instanceof List<?> values)) return java.util.Optional.empty();
        return values.stream().filter(String.class::isInstance).map(String.class::cast).findFirst();
    }
}
