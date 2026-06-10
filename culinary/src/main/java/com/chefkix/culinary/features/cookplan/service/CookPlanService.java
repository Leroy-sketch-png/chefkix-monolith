package com.chefkix.culinary.features.cookplan.service;

import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.MealRole;
import com.chefkix.culinary.features.cookplan.dto.request.CreateCookPlanRequest;
import com.chefkix.culinary.features.cookplan.entity.CookPlan;
import com.chefkix.culinary.features.cookplan.entity.CookPlan.CookBatch;
import com.chefkix.culinary.features.cookplan.entity.CookPlan.CookPlanDish;
import com.chefkix.culinary.features.cookplan.entity.CookPlan.EatingOccasion;
import com.chefkix.culinary.features.cookplan.entity.CookPlan.ShoppingItem;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CookPlanService {

    private static final int MIN_DISHES = 2;
    private static final int MAX_DISHES = 4;

    private final CookPlanRepository cookPlanRepository;
    private final RecipeRepository recipeRepository;
    private final PantryItemRepository pantryItemRepository;
    private final ProfileProvider profileProvider;

    public CookPlanService(
            CookPlanRepository cookPlanRepository,
            RecipeRepository recipeRepository,
            PantryItemRepository pantryItemRepository,
            ProfileProvider profileProvider) {
        this.cookPlanRepository = cookPlanRepository;
        this.recipeRepository = recipeRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.profileProvider = profileProvider;
    }

    @Transactional
    public CookPlan create(String userId, CreateCookPlanRequest request) {
        PlanningPreferences preferences = profileProvider.getPlanningPreferences(userId);
        List<PantryItem> pantry = pantryItemRepository.findByUserId(userId, Sort.unsorted());
        PantryContext pantryContext = pantryContext(pantry, request.getPlanDate());

        List<RecipeCandidate> candidates = recipeRepository.findPublishedForIngredientMatching().stream()
                .filter(recipe -> isSafe(recipe, preferences, request.getMaxActiveMinutes()))
                .map(recipe -> candidate(recipe, preferences, pantryContext, request.isPantryFirst()))
                .sorted(Comparator.comparingDouble(RecipeCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.recipe().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        int targetDishCount = request.getMode() == CookPlanMode.COOK_ONCE_TODAY ? 3 : 2;
        List<RecipeCandidate> selected = selectCompatible(
                candidates,
                Math.min(targetDishCount, MAX_DISHES),
                request.getMaxActiveMinutes());

        if (selected.size() < MIN_DISHES) {
            return noPlan(userId, request, preferences, candidates.size());
        }

        String batchId = UUID.randomUUID().toString();
        int plannedServings = request.getHouseholdSize() * 2;
        List<CookPlanDish> dishes = selected.stream()
                .map(candidate -> toDish(candidate, plannedServings))
                .toList();

        CookBatch batch = CookBatch.builder()
                .id(batchId)
                .title(request.getMode() == CookPlanMode.COOK_ONCE_TODAY
                        ? "Cook once for today"
                        : "Dinner with planned leftovers")
                .activeMinutes(selected.stream().mapToInt(RecipeCandidate::activeMinutes).sum())
                .totalMinutes(selected.stream()
                        .mapToInt(candidate -> candidate.recipe().getTotalTimeMinutes())
                        .max()
                        .orElse(0))
                .dishes(dishes)
                .build();

        CookPlan plan = CookPlan.builder()
                .userId(userId)
                .planDate(request.getPlanDate())
                .mode(request.getMode())
                .householdSize(request.getHouseholdSize())
                .maxActiveMinutes(request.getMaxActiveMinutes())
                .pantryFirst(request.isPantryFirst())
                .cookBatches(List.of(batch))
                .eatingOccasions(eatingOccasions(request, batchId))
                .shoppingList(buildShoppingList(
                        selected.stream().map(RecipeCandidate::recipe).toList(),
                        plannedServings,
                        pantryContext.availableNames()))
                .build();

        cookPlanRepository.deleteByUserIdAndPlanDate(userId, request.getPlanDate());
        return cookPlanRepository.save(plan);
    }

    public CookPlan current(String userId, LocalDate planDate) {
        LocalDate effectiveDate = planDate != null ? planDate : LocalDate.now(ZoneOffset.UTC);
        return cookPlanRepository.findTopByUserIdAndPlanDateOrderByCreatedAtDesc(userId, effectiveDate)
                .orElse(null);
    }

    public CookPlan get(String userId, String planId) {
        return cookPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.COOK_PLAN_NOT_FOUND));
    }

    @Transactional
    public CookPlan swap(
            String userId,
            String planId,
            String batchId,
            String dishRecipeId,
            String replacementRecipeId) {
        CookPlan plan = get(userId, planId);
        PlanningPreferences preferences = profileProvider.getPlanningPreferences(userId);
        Recipe replacement = recipeRepository.findById(replacementRecipeId)
                .filter(recipe -> recipe.getStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (!isSafe(replacement, preferences, plan.getMaxActiveMinutes())) {
            throw new AppException(
                    ErrorCode.NO_VALID_COOK_PLAN,
                    "That recipe conflicts with an allergy, dietary restriction, disliked ingredient, or active-time limit.");
        }

        CookBatch batch = plan.getCookBatches().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), batchId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COOK_PLAN_NOT_FOUND));

        CookPlanDish existing = batch.getDishes().stream()
                .filter(dish -> Objects.equals(dish.getRecipeId(), dishRecipeId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        boolean duplicate = batch.getDishes().stream()
                .anyMatch(dish -> Objects.equals(dish.getRecipeId(), replacementRecipeId));
        if (duplicate) {
            throw new AppException(ErrorCode.INVALID_INPUT, "That recipe is already in this cooking batch.");
        }

        int replacementActiveMinutes = estimateActiveMinutes(replacement);
        int newBatchActiveMinutes = batch.getActiveMinutes()
                - existing.getActiveMinutes()
                + replacementActiveMinutes;
        if (newBatchActiveMinutes > plan.getMaxActiveMinutes()) {
            throw new AppException(
                    ErrorCode.NO_VALID_COOK_PLAN,
                    "That swap would exceed the plan's active-time budget.");
        }

        List<PantryItem> pantry = pantryItemRepository.findByUserId(userId, Sort.unsorted());
        PantryContext pantryContext = pantryContext(pantry, plan.getPlanDate());
        RecipeCandidate replacementCandidate = candidate(
                replacement,
                preferences,
                pantryContext,
                plan.isPantryFirst());

        int index = batch.getDishes().indexOf(existing);
        CookPlanDish replacementDish = toDish(replacementCandidate, existing.getPlannedServings());
        List<CookPlanDish> proposedDishes = new ArrayList<>(batch.getDishes());
        proposedDishes.set(index, replacementDish);
        if (!isCompatibleBatch(proposedDishes)) {
            throw new AppException(
                    ErrorCode.NO_VALID_COOK_PLAN,
                    "That swap would make the cooking batch incoherent. Keep one main dish and compatible courses from the same cuisine.");
        }

        batch.getDishes().set(index, replacementDish);
        batch.setActiveMinutes(newBatchActiveMinutes);
        batch.setTotalMinutes(batch.getDishes().stream()
                .mapToInt(CookPlanDish::getTotalTimeMinutes)
                .max()
                .orElse(0));

        List<String> recipeIds = batch.getDishes().stream().map(CookPlanDish::getRecipeId).toList();
        List<Recipe> recipes = recipeRepository.findAllByIdIn(recipeIds);
        plan.setShoppingList(buildShoppingList(
                recipes,
                existing.getPlannedServings(),
                pantryContext.availableNames()));
        return cookPlanRepository.save(plan);
    }

    private CookPlan noPlan(
            String userId,
            CreateCookPlanRequest request,
            PlanningPreferences preferences,
            int safeCandidateCount) {
        List<String> constraints = new ArrayList<>();
        constraints.add(safeCandidateCount == 0
                ? "No published recipes satisfy every active safety and effort constraint."
                : "Fewer than two compatible recipes fit the active-time budget.");
        if (!safe(preferences.getAllergies()).isEmpty()) {
            constraints.add("Allergies were enforced: " + String.join(", ", preferences.getAllergies()));
        }
        if (!safe(preferences.getDietaryRestrictions()).isEmpty()) {
            constraints.add("Dietary restrictions were enforced: "
                    + String.join(", ", preferences.getDietaryRestrictions()));
        }
        constraints.add("Increase the active-time budget or publish more compatible recipes; ChefKix will not insert an unsafe fallback.");

        return CookPlan.builder()
                .userId(userId)
                .planDate(request.getPlanDate())
                .mode(request.getMode())
                .householdSize(request.getHouseholdSize())
                .maxActiveMinutes(request.getMaxActiveMinutes())
                .pantryFirst(request.isPantryFirst())
                .unmetConstraints(constraints)
                .build();
    }

    private List<RecipeCandidate> selectCompatible(
            List<RecipeCandidate> candidates,
            int targetCount,
            int maxActiveMinutes) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<RecipeCandidate> best = List.of();
        double bestScore = Double.NEGATIVE_INFINITY;

        for (RecipeCandidate anchor : candidates) {
            if (anchor.mealRole() != MealRole.MAIN) {
                continue;
            }

            List<RecipeCandidate> cluster = new ArrayList<>();
            cluster.add(anchor);
            Set<String> selectedIngredients = new HashSet<>(anchor.ingredientNames());

            List<RecipeCandidate> companions = candidates.stream()
                    .filter(candidate -> candidate != anchor)
                    .filter(candidate -> isCompanionRole(candidate.mealRole()))
                    .filter(candidate -> sameCuisine(anchor.recipe(), candidate.recipe()))
                    .sorted(Comparator.comparingDouble((RecipeCandidate candidate) ->
                                    candidate.score()
                                            + compatibilityScore(candidate, cluster, selectedIngredients))
                            .reversed())
                    .toList();

            for (RecipeCandidate companion : companions) {
                if (cluster.size() >= targetCount) {
                    break;
                }
                if (activeMinutes(cluster) + companion.activeMinutes() > maxActiveMinutes) {
                    continue;
                }
                cluster.add(companion);
                selectedIngredients.addAll(companion.ingredientNames());
            }

            if (cluster.size() < MIN_DISHES) {
                continue;
            }

            double clusterScore = cluster.stream().mapToDouble(RecipeCandidate::score).sum()
                    + cluster.size() * 80
                    + cluster.stream().map(RecipeCandidate::mealRole).distinct().count() * 20;
            if (cluster.size() > best.size()
                    || (cluster.size() == best.size() && clusterScore > bestScore)) {
                best = List.copyOf(cluster);
                bestScore = clusterScore;
            }
        }

        return best;
    }

    private int activeMinutes(List<RecipeCandidate> candidates) {
        return candidates.stream().mapToInt(RecipeCandidate::activeMinutes).sum();
    }

    private double compatibilityScore(
            RecipeCandidate candidate,
            List<RecipeCandidate> selected,
            Set<String> selectedIngredients) {
        if (selected.isEmpty()) {
            return 0;
        }
        long sharedIngredients = candidate.ingredientNames().stream()
                .filter(selectedIngredients::contains)
                .count();
        boolean sharedCuisine = selected.stream().anyMatch(existing ->
                normalize(existing.recipe().getCuisineType())
                        .equals(normalize(candidate.recipe().getCuisineType())));
        return sharedIngredients * 18 + (sharedCuisine ? 14 : 0);
    }

    private boolean isCompanionRole(MealRole role) {
        return role == MealRole.SIDE
                || role == MealRole.SOUP
                || role == MealRole.DESSERT
                || role == MealRole.BREAD;
    }

    private boolean sameCuisine(Recipe first, Recipe second) {
        String firstCuisine = normalize(first.getCuisineType());
        return !firstCuisine.isBlank()
                && firstCuisine.equals(normalize(second.getCuisineType()));
    }

    private RecipeCandidate candidate(
            Recipe recipe,
            PlanningPreferences preferences,
            PantryContext pantry,
            boolean pantryFirst) {
        Set<String> ingredientNames = ingredients(recipe).stream()
                .map(Ingredient::getName)
                .map(this::normalize)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long pantryMatches = ingredientNames.stream().filter(pantry.availableNames()::contains).count();
        long expiringMatches = ingredientNames.stream().filter(pantry.expiringNames()::contains).count();
        long shoppingCount = ingredientNames.size() - pantryMatches;
        boolean preferredCuisine = safe(preferences.getPreferredCuisines()).stream()
                .map(this::normalize)
                .anyMatch(cuisine -> cuisine.equals(normalize(recipe.getCuisineType())));

        double pantryWeight = pantryFirst ? 65 : 35;
        double score = pantryMatches * pantryWeight
                + expiringMatches * 30
                + (preferredCuisine ? 24 : 0)
                - shoppingCount * 5
                - estimateActiveMinutes(recipe) * 0.35;

        return new RecipeCandidate(
                recipe,
                estimateActiveMinutes(recipe),
                ingredientNames,
                resolveMealRole(recipe),
                (int) pantryMatches,
                (int) shoppingCount,
                score);
    }

    private boolean isSafe(
            Recipe recipe,
            PlanningPreferences preferences,
            int maxActiveMinutes) {
        if (recipe == null
                || recipe.getId() == null
                || recipe.getTitle() == null
                || recipe.getTitle().isBlank()
                || estimateActiveMinutes(recipe) > maxActiveMinutes) {
            return false;
        }

        List<String> ingredientNames = ingredients(recipe).stream()
                .map(Ingredient::getName)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .toList();

        List<String> exclusions = new ArrayList<>(safe(preferences.getAllergies()));
        exclusions.addAll(safe(preferences.getDislikedIngredients()));
        boolean containsExcludedIngredient = exclusions.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .anyMatch(exclusion -> ingredientNames.stream()
                        .anyMatch(ingredient -> ingredient.contains(exclusion)));
        if (containsExcludedIngredient) {
            return false;
        }

        Set<String> recipeTags = safe(recipe.getDietaryTags()).stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
        return safe(preferences.getDietaryRestrictions()).stream()
                .map(this::normalize)
                .allMatch(recipeTags::contains);
    }

    private CookPlanDish toDish(RecipeCandidate candidate, int plannedServings) {
        Recipe recipe = candidate.recipe();
        String image = safe(recipe.getCoverImageUrl()).stream().findFirst().orElse(null);
        return CookPlanDish.builder()
                .recipeId(recipe.getId())
                .title(recipe.getTitle())
                .coverImageUrl(image)
                .cuisineType(recipe.getCuisineType())
                .mealRole(candidate.mealRole())
                .activeMinutes(candidate.activeMinutes())
                .totalTimeMinutes(recipe.getTotalTimeMinutes())
                .sourceServings(Math.max(1, recipe.getServings()))
                .plannedServings(plannedServings)
                .pantryIngredientCount(candidate.pantryIngredientCount())
                .shoppingIngredientCount(candidate.shoppingIngredientCount())
                .build();
    }

    private List<EatingOccasion> eatingOccasions(
            CreateCookPlanRequest request,
            String batchId) {
        if (request.getMode() == CookPlanMode.COOK_ONCE_TODAY) {
            return List.of(
                    EatingOccasion.builder()
                            .name("Lunch today")
                            .batchId(batchId)
                            .servings(request.getHouseholdSize())
                            .build(),
                    EatingOccasion.builder()
                            .name("Dinner tonight")
                            .batchId(batchId)
                            .servings(request.getHouseholdSize())
                            .build());
        }
        return List.of(
                EatingOccasion.builder()
                        .name("Dinner tonight")
                        .batchId(batchId)
                        .servings(request.getHouseholdSize())
                        .build(),
                EatingOccasion.builder()
                        .name("Lunch tomorrow")
                        .batchId(batchId)
                        .servings(request.getHouseholdSize())
                        .build());
    }

    private List<ShoppingItem> buildShoppingList(
            List<Recipe> recipes,
            int plannedServings,
            Set<String> pantryNames) {
        Map<String, ShoppingAccumulator> shopping = new LinkedHashMap<>();
        for (Recipe recipe : recipes) {
            int sourceServings = Math.max(1, recipe.getServings());
            BigDecimal scale = BigDecimal.valueOf(plannedServings)
                    .divide(BigDecimal.valueOf(sourceServings), 2, RoundingMode.HALF_UP);

            for (Ingredient ingredient : ingredients(recipe)) {
                String normalizedName = normalize(ingredient.getName());
                if (normalizedName.isBlank() || pantryNames.contains(normalizedName)) {
                    continue;
                }
                String unit = normalize(ingredient.getUnit());
                String key = normalizedName + "|" + unit;
                shopping.computeIfAbsent(
                                key,
                                ignored -> new ShoppingAccumulator(ingredient.getName(), ingredient.getUnit()))
                        .add(ingredient.getQuantity(), scale, recipe.getTitle());
            }
        }
        return shopping.values().stream().map(ShoppingAccumulator::toItem).toList();
    }

    private PantryContext pantryContext(List<PantryItem> pantry, LocalDate planDate) {
        Set<String> available = pantry.stream()
                .map(item -> Optional.ofNullable(item.getNormalizedName())
                        .orElse(item.getIngredientName()))
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        Set<String> expiring = pantry.stream()
                .filter(item -> item.getExpiryDate() != null)
                .filter(item -> !item.getExpiryDate().isAfter(planDate.plusDays(3)))
                .map(item -> Optional.ofNullable(item.getNormalizedName())
                        .orElse(item.getIngredientName()))
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        return new PantryContext(available, expiring);
    }

    private int estimateActiveMinutes(Recipe recipe) {
        int prep = Math.max(0, recipe.getPrepTimeMinutes());
        int cook = Math.max(0, recipe.getCookTimeMinutes());
        if (prep > 0 || cook > 0) {
            return Math.max(5, prep + Math.min(cook, 10));
        }
        return Math.max(5, Math.min(Math.max(0, recipe.getTotalTimeMinutes()), 30));
    }

    private List<Ingredient> ingredients(Recipe recipe) {
        return recipe.getFullIngredientList() != null ? recipe.getFullIngredientList() : List.of();
    }

    private MealRole resolveMealRole(Recipe recipe) {
        if (recipe.getMealRole() != null) {
            return recipe.getMealRole();
        }
        return inferMealRole(recipe.getTitle());
    }

    private MealRole resolveMealRole(CookPlanDish dish) {
        return dish.getMealRole() != null ? dish.getMealRole() : inferMealRole(dish.getTitle());
    }

    private MealRole inferMealRole(String title) {
        String normalizedTitle = normalize(title);
        if (normalizedTitle.contains("overnight oats")) {
            return MealRole.BREAKFAST;
        }
        if (normalizedTitle.contains("creme brulee")
                || normalizedTitle.contains("banana bread")) {
            return MealRole.DESSERT;
        }
        if (normalizedTitle.contains("sourdough")
                || normalizedTitle.equals("arepas")) {
            return MealRole.BREAD;
        }
        if (normalizedTitle.contains("egg drop soup")
                || normalizedTitle.contains("french onion soup")) {
            return MealRole.SOUP;
        }
        if (normalizedTitle.contains("gyoza")
                || normalizedTitle.contains("dumpling")
                || normalizedTitle.contains("dal makhani")
                || normalizedTitle.contains("biryani")
                || normalizedTitle.contains("falafel")
                || normalizedTitle.contains("risotto")
                || normalizedTitle.contains("fried rice")
                || normalizedTitle.contains("morning glory")) {
            return MealRole.SIDE;
        }
        return MealRole.MAIN;
    }

    private boolean isCompatibleBatch(List<CookPlanDish> dishes) {
        if (dishes.size() < MIN_DISHES) {
            return false;
        }
        Set<String> cuisines = dishes.stream()
                .map(CookPlanDish::getCuisineType)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        long mainCount = dishes.stream()
                .map(this::resolveMealRole)
                .filter(role -> role == MealRole.MAIN)
                .count();
        boolean unsupportedRole = dishes.stream()
                .map(this::resolveMealRole)
                .anyMatch(role -> role == MealRole.BREAKFAST);
        return cuisines.size() == 1 && mainCount == 1 && !unsupportedRole;
    }

    private <T> List<T> safe(List<T> values) {
        return values != null ? values : List.of();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record RecipeCandidate(
            Recipe recipe,
            int activeMinutes,
            Set<String> ingredientNames,
            MealRole mealRole,
            int pantryIngredientCount,
            int shoppingIngredientCount,
            double score) {
    }

    private record PantryContext(Set<String> availableNames, Set<String> expiringNames) {
    }

    private static final class ShoppingAccumulator {
        private final String ingredient;
        private final String unit;
        private final Set<String> sourceRecipes = new LinkedHashSet<>();
        private BigDecimal numericTotal = BigDecimal.ZERO;
        private boolean allNumeric = true;
        private final List<String> scaledText = new ArrayList<>();

        private ShoppingAccumulator(String ingredient, String unit) {
            this.ingredient = ingredient;
            this.unit = unit;
        }

        private void add(String quantity, BigDecimal scale, String recipeTitle) {
            sourceRecipes.add(recipeTitle);
            BigDecimal numeric = parseQuantity(quantity);
            if (numeric != null) {
                BigDecimal scaled = numeric.multiply(scale);
                numericTotal = numericTotal.add(scaled);
                scaledText.add(compact(scaled));
            } else {
                allNumeric = false;
                String sourceQuantity = quantity == null || quantity.isBlank() ? "as needed" : quantity;
                scaledText.add(sourceQuantity + " x" + compact(scale));
            }
        }

        private ShoppingItem toItem() {
            String quantity = allNumeric
                    ? compact(numericTotal)
                    : String.join(" + ", scaledText);
            return ShoppingItem.builder()
                    .ingredient(ingredient)
                    .quantity(quantity)
                    .unit(unit)
                    .sourceRecipes(new ArrayList<>(sourceRecipes))
                    .build();
        }

        private static BigDecimal parseQuantity(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String trimmed = value.trim();
            try {
                if (trimmed.contains("/")) {
                    String[] fraction = trimmed.split("/");
                    if (fraction.length == 2) {
                        return new BigDecimal(fraction[0].trim())
                                .divide(new BigDecimal(fraction[1].trim()), 4, RoundingMode.HALF_UP);
                    }
                }
                return new BigDecimal(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String compact(BigDecimal value) {
            return value.stripTrailingZeros().toPlainString();
        }
    }
}
