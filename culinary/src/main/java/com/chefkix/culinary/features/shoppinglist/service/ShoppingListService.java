package com.chefkix.culinary.features.shoppinglist.service;

import com.chefkix.culinary.features.mealplan.entity.MealPlan;
import com.chefkix.culinary.features.mealplan.entity.PlannedDay;
import com.chefkix.culinary.features.mealplan.repository.MealPlanRepository;
import com.chefkix.culinary.features.pantry.entity.PantryItem;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.entity.Ingredient;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.shoppinglist.dto.request.AddCustomItemRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateCustomListRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromMealPlanRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromRecipeRequest;
import com.chefkix.culinary.features.shoppinglist.dto.response.ShoppingListResponse;
import com.chefkix.culinary.features.shoppinglist.dto.response.ShoppingListSummaryResponse;
import com.chefkix.culinary.features.shoppinglist.entity.ShoppingList;
import com.chefkix.culinary.features.shoppinglist.entity.ShoppingListItem;
import com.chefkix.culinary.features.shoppinglist.entity.ShoppingListSource;
import com.chefkix.culinary.features.shoppinglist.repository.ShoppingListRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepo;
    private final MealPlanRepository mealPlanRepo;
    private final RecipeRepository recipeRepo;
    private final PantryItemRepository pantryRepo;

    // ── Create from Meal Plan ───────────────────────────────────────

    public ShoppingListResponse createFromMealPlan(String userId, CreateFromMealPlanRequest req) {
        MealPlan plan = mealPlanRepo.findByIdAndUserId(req.getMealPlanId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_PLAN_NOT_FOUND));

        Set<String> pantryNormals = getPantryNormals(userId);
        List<String> recipeIds = extractRecipeIds(plan.getDays());
        List<Recipe> recipes = recipeIds.isEmpty() ? List.of() : recipeRepo.findAllByIdIn(recipeIds);
        Map<String, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r, (a, b) -> a));

        Map<String, AggregatedIngredient> aggregated = new LinkedHashMap<>();
        for (PlannedDay day : plan.getDays()) {
            Stream.of(day.getBreakfast(), day.getLunch(), day.getDinner())
                    .filter(Objects::nonNull)
                    .forEach(meal -> {
                        if (meal.getRecipeId() == null) return;
                        Recipe recipe = recipeMap.get(meal.getRecipeId());
                        if (recipe == null || recipe.getFullIngredientList() == null) return;
                        aggregateIngredients(aggregated, recipe.getFullIngredientList(), recipe.getTitle(), pantryNormals);
                    });
        }

        String name = "Week of " + plan.getWeekStartDate().getMonth().name().substring(0, 3)
                + " " + plan.getWeekStartDate().getDayOfMonth();

        ShoppingList list = ShoppingList.builder()
                .userId(userId)
                .name(name)
                .items(toShoppingItems(aggregated))
                .source(ShoppingListSource.MEAL_PLAN)
                .sourceMealPlanId(plan.getId())
                .shareToken(generateShareToken())
                .build();

        return toResponse(shoppingListRepo.save(list));
    }

    // ── Create from Recipe ──────────────────────────────────────────

    public ShoppingListResponse createFromRecipe(String userId, CreateFromRecipeRequest req) {
        Recipe recipe = recipeRepo.findById(req.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        Set<String> pantryNormals = getPantryNormals(userId);
        Map<String, AggregatedIngredient> aggregated = new LinkedHashMap<>();

        if (recipe.getFullIngredientList() != null) {
            aggregateIngredients(aggregated, recipe.getFullIngredientList(), recipe.getTitle(), pantryNormals);
        }

        ShoppingList list = ShoppingList.builder()
                .userId(userId)
                .name(recipe.getTitle() + " ingredients")
                .items(toShoppingItems(aggregated))
                .source(ShoppingListSource.RECIPE)
                .sourceRecipeId(recipe.getId())
                .shareToken(generateShareToken())
                .build();

        return toResponse(shoppingListRepo.save(list));
    }

    // ── Create Custom ───────────────────────────────────────────────

    public ShoppingListResponse createCustom(String userId, CreateCustomListRequest req) {
        ShoppingList list = ShoppingList.builder()
                .userId(userId)
                .name(req.getName())
                .items(new ArrayList<>())
                .source(ShoppingListSource.CUSTOM)
                .shareToken(generateShareToken())
                .build();

        return toResponse(shoppingListRepo.save(list));
    }

    // ── Read ────────────────────────────────────────────────────────

    public List<ShoppingListSummaryResponse> getUserLists(String userId) {
        return shoppingListRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    public ShoppingListResponse getById(String userId, String id) {
        ShoppingList list = shoppingListRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND));
        return toResponse(list);
    }

    public ShoppingListResponse getByShareToken(String shareToken) {
        ShoppingList list = shoppingListRepo.findByShareToken(shareToken)
                .orElseThrow(() -> new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND));
        return toResponse(list);
    }

    // ── Item Operations ─────────────────────────────────────────────

    public ShoppingListResponse toggleItem(String userId, String listId, String itemId) {
        ShoppingList list = shoppingListRepo.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND));

        boolean found = false;
        for (ShoppingListItem item : list.getItems()) {
            if (itemId.equals(item.getItemId())) {
                item.setChecked(!item.isChecked());
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND);
        }
        return toResponse(shoppingListRepo.save(list));
    }

    public ShoppingListResponse addCustomItem(String userId, String listId, AddCustomItemRequest req) {
        ShoppingList list = shoppingListRepo.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND));

        ShoppingListItem item = ShoppingListItem.builder()
                .itemId(UUID.randomUUID().toString())
                .ingredient(req.getIngredient().trim())
                .quantity(req.getQuantity())
                .unit(req.getUnit())
                .category(req.getCategory() != null ? req.getCategory() : categorize(req.getIngredient()))
                .addedManually(true)
                .build();

        list.getItems().add(item);
        return toResponse(shoppingListRepo.save(list));
    }

    public ShoppingListResponse removeItem(String userId, String listId, String itemId) {
        ShoppingList list = shoppingListRepo.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND));

        list.getItems().removeIf(item -> itemId.equals(item.getItemId()));
        return toResponse(shoppingListRepo.save(list));
    }

    // ── Delete ──────────────────────────────────────────────────────

    public void delete(String userId, String id) {
        shoppingListRepo.deleteByIdAndUserId(id, userId);
    }

    // ── Regenerate Share Token ───────────────────────────────────────

    public ShoppingListResponse regenerateShareToken(String userId, String id) {
        ShoppingList list = shoppingListRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOPPING_LIST_NOT_FOUND));
        list.setShareToken(generateShareToken());
        return toResponse(shoppingListRepo.save(list));
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Set<String> getPantryNormals(String userId) {
        return pantryRepo.findByUserId(userId, Sort.unsorted()).stream()
                .map(PantryItem::getNormalizedName)
                .collect(Collectors.toSet());
    }

    private List<String> extractRecipeIds(List<PlannedDay> days) {
        Set<String> ids = new LinkedHashSet<>();
        for (PlannedDay day : days) {
            Stream.of(day.getBreakfast(), day.getLunch(), day.getDinner())
                    .filter(Objects::nonNull)
                    .filter(meal -> meal.getRecipeId() != null)
                    .forEach(meal -> ids.add(meal.getRecipeId()));
        }
        return new ArrayList<>(ids);
    }

    private void aggregateIngredients(Map<String, AggregatedIngredient> map,
                                       List<Ingredient> ingredients,
                                       String recipeTitle,
                                       Set<String> pantryNormals) {
        for (Ingredient ing : ingredients) {
            if (ing.getName() == null) continue;
            String norm = ing.getName().toLowerCase().trim();
            boolean inPantry = pantryNormals.stream()
                    .anyMatch(pn -> pn.equals(norm) || norm.startsWith(pn + " ") || norm.endsWith(" " + pn) || norm.contains(" " + pn + " "));
            if (inPantry) continue;

            String key = norm;
            AggregatedIngredient agg = map.computeIfAbsent(key, k ->
                    new AggregatedIngredient(ing.getName(), new ArrayList<>(), new LinkedHashSet<>()));
            if (ing.getQuantity() != null && !ing.getQuantity().isBlank()) {
                String qty = ing.getUnit() != null && !ing.getUnit().isBlank()
                        ? ing.getQuantity() + " " + ing.getUnit()
                        : ing.getQuantity();
                agg.quantities.add(qty);
            }
            agg.recipes.add(recipeTitle);
        }
    }

    private List<ShoppingListItem> toShoppingItems(Map<String, AggregatedIngredient> aggregated) {
        return aggregated.values().stream()
                .map(agg -> ShoppingListItem.builder()
                        .itemId(UUID.randomUUID().toString())
                        .ingredient(agg.name)
                        .quantity(agg.quantities.isEmpty() ? null : String.join(" + ", agg.quantities))
                        .category(categorize(agg.name))
                        .recipes(new ArrayList<>(agg.recipes))
                        .build())
                .toList();
    }

    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    // ── Ingredient Categorization ───────────────────────────────────

    private static final Map<String, String> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        // Protein (check before produce — "chicken breast" should be Protein)
        for (String kw : List.of("chicken", "beef", "pork", "fish", "salmon", "shrimp", "prawn",
                "tofu", "turkey", "lamb", "sausage", "bacon", "steak", "tuna", "cod", "tilapia",
                "duck", "veal", "venison", "crab", "lobster", "scallop", "anchovy", "tempeh", "seitan")) {
            CATEGORY_KEYWORDS.put(kw, "Protein");
        }
        // Dairy
        for (String kw : List.of("milk", "cheese", "cream", "butter", "yogurt", "yoghurt",
                "egg", "mozzarella", "parmesan", "cheddar", "ricotta", "feta", "sour cream",
                "whipping cream", "ghee", "mascarpone")) {
            CATEGORY_KEYWORDS.put(kw, "Dairy");
        }
        // Produce
        for (String kw : List.of("tomato", "onion", "garlic", "lettuce", "spinach", "carrot",
                "pepper", "cucumber", "potato", "mushroom", "broccoli", "celery", "avocado",
                "lemon", "lime", "apple", "banana", "berry", "basil", "cilantro", "parsley",
                "ginger", "zucchini", "kale", "cabbage", "corn", "pea", "bean sprout",
                "spring onion", "scallion", "shallot", "eggplant", "squash", "pumpkin",
                "beet", "radish", "asparagus", "artichoke", "fennel", "leek", "chive",
                "mint", "dill", "thyme", "rosemary", "sage", "oregano", "tarragon")) {
            CATEGORY_KEYWORDS.put(kw, "Produce");
        }
        // Grains & Bakery
        for (String kw : List.of("flour", "rice", "pasta", "bread", "noodle", "oat", "cereal",
                "tortilla", "couscous", "quinoa", "barley", "bulgur", "cornmeal", "semolina",
                "panko", "breadcrumb", "pita", "wrap", "baguette", "crouton")) {
            CATEGORY_KEYWORDS.put(kw, "Grains");
        }
        // Spices & Seasonings
        for (String kw : List.of("salt", "cumin", "paprika", "cinnamon", "turmeric", "nutmeg",
                "cayenne", "chili powder", "black pepper", "white pepper", "clove", "cardamom",
                "coriander", "saffron", "curry powder", "garam masala", "bay leaf",
                "star anise", "vanilla", "allspice", "fenugreek", "sumac",
                "smoked paprika", "red pepper flake")) {
            CATEGORY_KEYWORDS.put(kw, "Spices");
        }
        // Condiments & Oils
        for (String kw : List.of("oil", "vinegar", "soy sauce", "ketchup", "mustard", "mayonnaise",
                "honey", "syrup", "hot sauce", "worcestershire", "fish sauce", "oyster sauce",
                "teriyaki", "sriracha", "tahini", "miso", "hoisin", "bbq sauce",
                "sesame oil", "olive oil", "coconut oil", "balsamic")) {
            CATEGORY_KEYWORDS.put(kw, "Condiments");
        }
        // Canned & Preserved
        for (String kw : List.of("canned", "broth", "stock", "tomato paste", "tomato sauce",
                "coconut milk", "chickpea", "lentil", "kidney bean", "black bean",
                "diced tomato", "crushed tomato", "pinto bean", "marinara")) {
            CATEGORY_KEYWORDS.put(kw, "Canned");
        }
        // Baking
        for (String kw : List.of("sugar", "baking powder", "baking soda", "yeast",
                "cocoa", "chocolate", "cornstarch", "gelatin", "food coloring",
                "cream of tartar", "powdered sugar", "brown sugar", "confectioner")) {
            CATEGORY_KEYWORDS.put(kw, "Baking");
        }
    }

    static String categorize(String ingredientName) {
        if (ingredientName == null) return "Other";
        String lower = ingredientName.toLowerCase().trim();
        // Longest keyword first to avoid "tomato" matching before "tomato paste"
        return CATEGORY_KEYWORDS.entrySet().stream()
                .filter(entry -> lower.contains(entry.getKey()))
                .max(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse("Other");
    }

    // ── Response Mappers ────────────────────────────────────────────

    private ShoppingListResponse toResponse(ShoppingList list) {
        return ShoppingListResponse.builder()
                .id(list.getId())
                .name(list.getName())
                .items(list.getItems())
                .source(list.getSource() != null ? list.getSource().getValue() : null)
                .sourceMealPlanId(list.getSourceMealPlanId())
                .sourceRecipeId(list.getSourceRecipeId())
                .shareToken(list.getShareToken())
                .totalItems(list.getItems().size())
                .checkedItems((int) list.getItems().stream().filter(ShoppingListItem::isChecked).count())
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }

    private ShoppingListSummaryResponse toSummary(ShoppingList list) {
        return ShoppingListSummaryResponse.builder()
                .id(list.getId())
                .name(list.getName())
                .source(list.getSource() != null ? list.getSource().getValue() : null)
                .totalItems(list.getItems().size())
                .checkedItems((int) list.getItems().stream().filter(ShoppingListItem::isChecked).count())
                .createdAt(list.getCreatedAt())
                .build();
    }

    // ── Aggregation Helper Class ────────────────────────────────────

    private record AggregatedIngredient(String name, List<String> quantities, Set<String> recipes) {}
}
