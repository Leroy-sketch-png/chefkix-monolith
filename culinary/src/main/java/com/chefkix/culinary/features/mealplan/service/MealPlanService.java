package com.chefkix.culinary.features.mealplan.service;

import com.chefkix.culinary.features.mealplan.dto.request.GenerateMealPlanRequest;
import com.chefkix.culinary.features.mealplan.dto.request.SwapMealRequest;
import com.chefkix.culinary.features.mealplan.dto.response.MealPlanResponse;
import com.chefkix.culinary.features.mealplan.entity.*;
import com.chefkix.culinary.features.mealplan.repository.MealPlanRepository;
import com.chefkix.culinary.features.pantry.entity.PantryItem;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
// Error codes: MEAL_PLAN_NOT_FOUND, EMPTY, INVALID_INPUT
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Meal plan service — local generation (no external AI call for now, uses existing recipes).
 * When AI service endpoint is ready, replace the local generation with an API call.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §6-§7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepo;
    private final RecipeRepository recipeRepo;
    private final PantryItemRepository pantryRepo;

    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    // ── Generate ────────────────────────────────────────────────────

    public MealPlanResponse generate(String userId, GenerateMealPlanRequest req) {
        // Enrich with pantry items if not provided
        List<String> pantryNames = req.getPantryItems();
        if (pantryNames == null || pantryNames.isEmpty()) {
            pantryNames = pantryRepo.findByUserId(userId, Sort.unsorted()).stream()
                    .map(PantryItem::getNormalizedName)
                    .toList();
        }

        // Cap at 500 recipes to prevent OOM as the catalog grows.
        // TODO: Replace with a MongoDB text-search or aggregation pipeline for true scalability.
        List<Recipe> recipes = recipeRepo.findByStatus(RecipeStatus.PUBLISHED, PageRequest.of(0, 500)).getContent();
        if (recipes.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY);
        }

        // Shuffle for variety
        List<Recipe> shuffled = new ArrayList<>(recipes);
        Collections.shuffle(shuffled);

        int days = Math.min(req.getDays(), 7);
        List<PlannedDay> plannedDays = new ArrayList<>();
        Set<String> usedRecipeIds = new HashSet<>();

        for (int i = 0; i < days; i++) {
            PlannedMeal breakfast = pickMeal(shuffled, usedRecipeIds, 20);
            PlannedMeal lunch = pickMeal(shuffled, usedRecipeIds, 45);
            PlannedMeal dinner = pickMeal(shuffled, usedRecipeIds, 90);

            plannedDays.add(PlannedDay.builder()
                    .dayOfWeek(DAY_NAMES[i])
                    .breakfast(breakfast)
                    .lunch(lunch)
                    .dinner(dinner)
                    .build());
        }

        // Build shopping list from selected recipe ingredients vs pantry
        List<ShoppingItem> shoppingList = buildShoppingList(plannedDays, recipes, new HashSet<>(pantryNames));

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        MealPlan plan = MealPlan.builder()
                .userId(userId)
                .weekStartDate(weekStart)
                .days(plannedDays)
                .shoppingList(shoppingList)
                .build();

        plan = mealPlanRepo.save(plan);
        return toResponse(plan);
    }

    // ── CRUD ────────────────────────────────────────────────────────

    public MealPlanResponse getCurrent(String userId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        MealPlan plan = mealPlanRepo.findByUserIdAndWeekStartDate(userId, weekStart)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_PLAN_NOT_FOUND));
        return toResponse(plan);
    }

    public MealPlanResponse getById(String userId, String planId) {
        MealPlan plan = mealPlanRepo.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_PLAN_NOT_FOUND));
        return toResponse(plan);
    }

    public void delete(String userId, String planId) {
        mealPlanRepo.deleteByIdAndUserId(planId, userId);
    }

    // ── Swap Meal ───────────────────────────────────────────────────

    public MealPlanResponse swapMeal(String userId, String planId, String day, String mealType, SwapMealRequest req) {
        MealPlan plan = mealPlanRepo.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_PLAN_NOT_FOUND));

        PlannedMeal meal = PlannedMeal.builder()
                .recipeId(req.getRecipeId())
                .title(req.getTitle())
                .totalTimeMinutes(req.getTotalTimeMinutes())
                .servings(req.getServings())
                .aiGenerated(req.isAiGenerated())
                .build();

        for (PlannedDay d : plan.getDays()) {
            if (d.getDayOfWeek().equalsIgnoreCase(day)) {
                switch (mealType.toLowerCase()) {
                    case "breakfast" -> d.setBreakfast(meal);
                    case "lunch" -> d.setLunch(meal);
                    case "dinner" -> d.setDinner(meal);
                    default -> throw new AppException(ErrorCode.INVALID_INPUT);
                }
                break;
            }
        }

        return toResponse(mealPlanRepo.save(plan));
    }

    // ── Shopping List ───────────────────────────────────────────────

    public List<ShoppingItem> getShoppingList(String userId, String planId) {
        MealPlan plan = mealPlanRepo.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_PLAN_NOT_FOUND));
        return plan.getShoppingList();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private PlannedMeal pickMeal(List<Recipe> pool, Set<String> used, int maxTime) {
        // Try to find unique recipe under max time
        for (Recipe r : pool) {
            if (!used.contains(r.getId()) && r.getTotalTimeMinutes() <= maxTime) {
                used.add(r.getId());
                return PlannedMeal.builder()
                        .recipeId(r.getId())
                        .title(r.getTitle())
                        .totalTimeMinutes(r.getTotalTimeMinutes())
                        .servings(r.getServings())
                        .aiGenerated(false)
                        .build();
            }
        }
        // Fallback: any recipe under max time (allow duplicates)
        for (Recipe r : pool) {
            if (r.getTotalTimeMinutes() <= maxTime) {
                return PlannedMeal.builder()
                        .recipeId(r.getId())
                        .title(r.getTitle())
                        .totalTimeMinutes(r.getTotalTimeMinutes())
                        .servings(r.getServings())
                        .aiGenerated(false)
                        .build();
            }
        }
        // Last resort: first recipe regardless of time
        Recipe fallback = pool.get(0);
        return PlannedMeal.builder()
                .recipeId(fallback.getId())
                .title(fallback.getTitle())
                .totalTimeMinutes(fallback.getTotalTimeMinutes())
                .servings(fallback.getServings())
                .aiGenerated(false)
                .build();
    }

    private List<ShoppingItem> buildShoppingList(List<PlannedDay> days, List<Recipe> allRecipes, Set<String> pantryNormals) {
        Map<String, Recipe> recipeMap = allRecipes.stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r, (a, b) -> a));

        // Ingredient -> list of recipe titles
        Map<String, Set<String>> ingredientRecipes = new LinkedHashMap<>();

        for (PlannedDay day : days) {
            Stream.of(day.getBreakfast(), day.getLunch(), day.getDinner())
                    .filter(Objects::nonNull)
                    .forEach(meal -> {
                        if (meal.getRecipeId() == null) return;
                        Recipe recipe = recipeMap.get(meal.getRecipeId());
                        if (recipe == null || recipe.getFullIngredientList() == null) return;
                        for (var ing : recipe.getFullIngredientList()) {
                            if (ing.getName() == null || ing.getName().isBlank()) continue;
                            String norm = ing.getName().toLowerCase().trim();
                            boolean inPantry = pantryNormals.stream().anyMatch(pn -> pn.contains(norm) || norm.contains(pn));
                            if (!inPantry) {
                                ingredientRecipes.computeIfAbsent(ing.getName(), k -> new LinkedHashSet<>())
                                        .add(recipe.getTitle());
                            }
                        }
                    });
        }

        return ingredientRecipes.entrySet().stream()
                .map(e -> ShoppingItem.builder()
                        .ingredient(e.getKey())
                        .recipes(new ArrayList<>(e.getValue()))
                        .build())
                .toList();
    }

    private MealPlanResponse toResponse(MealPlan plan) {
        return MealPlanResponse.builder()
                .id(plan.getId())
                .weekStartDate(plan.getWeekStartDate())
                .days(plan.getDays())
                .shoppingList(plan.getShoppingList())
                .build();
    }
}
