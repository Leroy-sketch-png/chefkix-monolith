package com.chefkix.culinary.features.cookplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.MealRole;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.cookplan.dto.request.CreateCookPlanRequest;
import com.chefkix.culinary.features.cookplan.entity.CookPlan;
import com.chefkix.culinary.features.cookplan.entity.CookPlanMode;
import com.chefkix.culinary.features.cookplan.repository.CookPlanRepository;
import com.chefkix.culinary.features.pantry.entity.PantryItem;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.entity.Ingredient;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.PlanningPreferences;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CookPlanServiceTest {

    @Mock
    private CookPlanRepository cookPlanRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private PantryItemRepository pantryItemRepository;

    @Mock
    private ProfileProvider profileProvider;

    private CookPlanService service;

    @BeforeEach
    void setUp() {
        service = new CookPlanService(
                cookPlanRepository,
                recipeRepository,
                pantryItemRepository,
                profileProvider);
        lenient().when(profileProvider.getPlanningPreferences("user-1"))
                .thenReturn(PlanningPreferences.builder()
                        .allergies(List.of("peanut"))
                        .dietaryRestrictions(List.of("vegetarian"))
                        .preferredCuisines(List.of("Vietnamese"))
                        .defaultServings(4)
                        .build());
        lenient().when(pantryItemRepository.findByUserId("user-1", Sort.unsorted()))
                .thenReturn(List.of(PantryItem.builder()
                        .ingredientName("Rice")
                        .normalizedName("rice")
                        .expiryDate(LocalDate.of(2026, 6, 11))
                        .build()));
        lenient().when(cookPlanRepository.save(any(CookPlan.class))).thenAnswer(invocation -> {
            CookPlan plan = invocation.getArgument(0);
            plan.setId("plan-1");
            return plan;
        });
    }

    @Test
    void createsCoherentRealRecipeBatchWithSafetyPantryAndScaling() {
        Recipe riceBowl = recipe(
                "rice-bowl",
                "Vietnamese Tofu Rice Bowl",
                10,
                20,
                2,
                List.of(
                        ingredient("Rice", "1", "cup"),
                        ingredient("Tofu", "0.5", "kg")));
        Recipe greens = recipe(
                "greens",
                "Garlic Morning Glory",
                8,
                12,
                2,
                List.of(
                        ingredient("Morning glory", "1", "bunch"),
                        ingredient("Garlic", "2", "cloves")));
        Recipe allergyConflict = recipe(
                "peanut-noodles",
                "Peanut Noodles",
                8,
                10,
                2,
                List.of(ingredient("Peanut butter", "2", "tbsp")));
        Recipe tooMuchEffort = recipe(
                "slow-stew",
                "Slow Stew",
                90,
                120,
                2,
                List.of(ingredient("Potato", "2", "kg")));

        when(recipeRepository.findPublishedForIngredientMatching())
                .thenReturn(List.of(riceBowl, greens, allergyConflict, tooMuchEffort));

        CookPlan plan = service.create("user-1", request(4, 60));

        assertThat(plan.getId()).isEqualTo("plan-1");
        assertThat(plan.getCookBatches()).hasSize(1);
        assertThat(plan.getCookBatches().get(0).getDishes())
                .extracting(CookPlan.CookPlanDish::getRecipeId)
                .containsExactlyInAnyOrder("rice-bowl", "greens")
                .doesNotContain("peanut-noodles", "slow-stew");
        assertThat(plan.getCookBatches().get(0).getDishes())
                .allMatch(dish -> dish.getPlannedServings() == 8);
        assertThat(plan.getCookBatches().get(0).getDishes())
                .extracting(CookPlan.CookPlanDish::getMealRole)
                .containsExactlyInAnyOrder(MealRole.MAIN, MealRole.SIDE);
        assertThat(plan.getEatingOccasions())
                .extracting(CookPlan.EatingOccasion::getName)
                .containsExactly("Lunch today", "Dinner tonight");
        assertThat(plan.getShoppingList())
                .extracting(CookPlan.ShoppingItem::getIngredient)
                .doesNotContain("Rice")
                .contains("Tofu", "Morning glory", "Garlic");
        assertThat(plan.getShoppingList().stream()
                .filter(item -> "Tofu".equals(item.getIngredient()))
                .findFirst()
                .orElseThrow()
                .getQuantity()).isEqualTo("2");
    }

    @Test
    void excludesBreakfastAndMixedCuisineRecipesFromTheCookingBatch() {
        Recipe ramen = recipe(
                "ramen",
                "Tonkotsu Ramen",
                "Japanese",
                MealRole.MAIN,
                10,
                20,
                2,
                List.of(ingredient("Rice", "1", "cup")));
        Recipe gyoza = recipe(
                "gyoza",
                "Japanese Gyoza",
                "Japanese",
                MealRole.SIDE,
                8,
                12,
                2,
                List.of(ingredient("Cabbage", "1", "head")));
        Recipe friedRice = recipe(
                "fried-rice",
                "Garlic Fried Rice",
                "Chinese",
                MealRole.SIDE,
                5,
                10,
                2,
                List.of(ingredient("Rice", "1", "cup")));
        Recipe oats = recipe(
                "oats",
                "Overnight Oats",
                "International",
                MealRole.BREAKFAST,
                5,
                5,
                2,
                List.of(ingredient("Rice", "1", "cup")));

        when(recipeRepository.findPublishedForIngredientMatching())
                .thenReturn(List.of(oats, friedRice, ramen, gyoza));

        CookPlan plan = service.create("user-1", request(2, 60));

        assertThat(plan.getCookBatches()).hasSize(1);
        assertThat(plan.getCookBatches().get(0).getDishes())
                .extracting(CookPlan.CookPlanDish::getRecipeId)
                .containsExactlyInAnyOrder("ramen", "gyoza")
                .doesNotContain("fried-rice", "oats");
        assertThat(plan.getCookBatches().get(0).getDishes())
                .extracting(CookPlan.CookPlanDish::getCuisineType)
                .containsOnly("Japanese");
    }

    @Test
    void returnsActionableNoPlanInsteadOfUnsafeFallback() {
        Recipe onlySafeRecipe = recipe(
                "safe-one",
                "Vegetarian Rice",
                10,
                10,
                2,
                List.of(ingredient("Rice", "1", "cup")));
        Recipe allergyConflict = recipe(
                "unsafe",
                "Peanut Salad",
                5,
                5,
                2,
                List.of(ingredient("Peanuts", "1", "cup")));
        when(recipeRepository.findPublishedForIngredientMatching())
                .thenReturn(List.of(onlySafeRecipe, allergyConflict));

        CookPlan plan = service.create("user-1", request(2, 45));

        assertThat(plan.getId()).isNull();
        assertThat(plan.getCookBatches()).isEmpty();
        assertThat(plan.getUnmetConstraints())
                .anyMatch(message -> message.contains("Fewer than two"))
                .anyMatch(message -> message.contains("unsafe fallback"));
    }

    @Test
    void swapReappliesAllergyConstraints() {
        CookPlan existing = CookPlan.builder()
                .id("plan-1")
                .userId("user-1")
                .planDate(LocalDate.of(2026, 6, 10))
                .mode(CookPlanMode.COOK_ONCE_TODAY)
                .householdSize(2)
                .maxActiveMinutes(60)
                .cookBatches(List.of(CookPlan.CookBatch.builder()
                        .id("batch-1")
                        .activeMinutes(20)
                        .dishes(new java.util.ArrayList<>(List.of(
                                CookPlan.CookPlanDish.builder()
                                        .recipeId("safe-one")
                                        .activeMinutes(20)
                                        .plannedServings(4)
                                        .build())))
                        .build()))
                .build();
        Recipe allergyConflict = recipe(
                "unsafe",
                "Peanut Salad",
                5,
                5,
                2,
                List.of(ingredient("Peanuts", "1", "cup")));

        when(cookPlanRepository.findByIdAndUserId("plan-1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(recipeRepository.findById("unsafe")).thenReturn(Optional.of(allergyConflict));

        assertThatThrownBy(() ->
                service.swap("user-1", "plan-1", "batch-1", "safe-one", "unsafe"))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.NO_VALID_COOK_PLAN);
    }

    @Test
    void swapRejectsARecipeThatWouldBreakBatchCoherence() {
        CookPlan existing = CookPlan.builder()
                .id("plan-1")
                .userId("user-1")
                .planDate(LocalDate.of(2026, 6, 10))
                .mode(CookPlanMode.COOK_ONCE_TODAY)
                .householdSize(2)
                .maxActiveMinutes(60)
                .pantryFirst(true)
                .cookBatches(List.of(CookPlan.CookBatch.builder()
                        .id("batch-1")
                        .activeMinutes(30)
                        .dishes(new java.util.ArrayList<>(List.of(
                                CookPlan.CookPlanDish.builder()
                                        .recipeId("ramen")
                                        .title("Tonkotsu Ramen")
                                        .cuisineType("Japanese")
                                        .mealRole(MealRole.MAIN)
                                        .activeMinutes(20)
                                        .plannedServings(4)
                                        .build(),
                                CookPlan.CookPlanDish.builder()
                                        .recipeId("gyoza")
                                        .title("Japanese Gyoza")
                                        .cuisineType("Japanese")
                                        .mealRole(MealRole.SIDE)
                                        .activeMinutes(10)
                                        .plannedServings(4)
                                        .build())))
                        .build()))
                .build();
        Recipe friedRice = recipe(
                "fried-rice",
                "Garlic Fried Rice",
                "Chinese",
                MealRole.SIDE,
                5,
                10,
                2,
                List.of(ingredient("Rice", "1", "cup")));

        when(cookPlanRepository.findByIdAndUserId("plan-1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(recipeRepository.findById("fried-rice")).thenReturn(Optional.of(friedRice));

        assertThatThrownBy(() ->
                service.swap("user-1", "plan-1", "batch-1", "gyoza", "fried-rice"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("incoherent")
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.NO_VALID_COOK_PLAN);
    }

    private CreateCookPlanRequest request(int householdSize, int maxActiveMinutes) {
        return CreateCookPlanRequest.builder()
                .planDate(LocalDate.of(2026, 6, 10))
                .mode(CookPlanMode.COOK_ONCE_TODAY)
                .householdSize(householdSize)
                .maxActiveMinutes(maxActiveMinutes)
                .pantryFirst(true)
                .build();
    }

    private Recipe recipe(
            String id,
            String title,
            int prepMinutes,
            int cookMinutes,
            int servings,
            List<Ingredient> ingredients) {
        return Recipe.builder()
                .id(id)
                .title(title)
                .status(RecipeStatus.PUBLISHED)
                .prepTimeMinutes(prepMinutes)
                .cookTimeMinutes(cookMinutes)
                .totalTimeMinutes(prepMinutes + cookMinutes)
                .servings(servings)
                .cuisineType("Vietnamese")
                .dietaryTags(List.of("vegetarian"))
                .fullIngredientList(ingredients)
                .build();
    }

    private Recipe recipe(
            String id,
            String title,
            String cuisine,
            MealRole mealRole,
            int prepMinutes,
            int cookMinutes,
            int servings,
            List<Ingredient> ingredients) {
        return Recipe.builder()
                .id(id)
                .title(title)
                .status(RecipeStatus.PUBLISHED)
                .prepTimeMinutes(prepMinutes)
                .cookTimeMinutes(cookMinutes)
                .totalTimeMinutes(prepMinutes + cookMinutes)
                .servings(servings)
                .cuisineType(cuisine)
                .mealRole(mealRole)
                .dietaryTags(List.of("vegetarian"))
                .fullIngredientList(ingredients)
                .build();
    }

    private Ingredient ingredient(String name, String quantity, String unit) {
        return new Ingredient(name, quantity, unit);
    }
}
