package com.chefkix.culinary.features.pantry.service;

import com.chefkix.culinary.features.pantry.dto.request.BulkPantryItemRequest;
import com.chefkix.culinary.features.pantry.dto.request.PantryItemRequest;
import com.chefkix.culinary.features.pantry.dto.response.PantryItemResponse;
import com.chefkix.culinary.features.pantry.dto.response.PantryRecipeMatchResponse;
import com.chefkix.culinary.features.pantry.entity.PantryItem;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pantry service — CRUD + recipe matching.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §1-§3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PantryService {

    private final PantryItemRepository pantryRepo;
    private final RecipeRepository recipeRepo;

    // ── CRUD ────────────────────────────────────────────────────────

    public PantryItemResponse addItem(String userId, PantryItemRequest req) {
        String normalized = normalize(req.getIngredientName());

        // Upsert: if same ingredient exists, increase quantity
        Optional<PantryItem> existing = pantryRepo.findByUserIdAndNormalizedName(userId, normalized);
        if (existing.isPresent()) {
            PantryItem item = existing.get();
            item.setQuantity((item.getQuantity() != null ? item.getQuantity() : 0) + (req.getQuantity() != null ? req.getQuantity() : 0));
            if (req.getExpiryDate() != null) item.setExpiryDate(req.getExpiryDate());
            if (req.getUnit() != null) item.setUnit(req.getUnit());
            return toResponse(pantryRepo.save(item));
        }

        PantryItem item = PantryItem.builder()
                .userId(userId)
                .ingredientName(req.getIngredientName().trim())
                .normalizedName(normalized)
                .quantity(req.getQuantity())
                .unit(req.getUnit())
                .category(req.getCategory() != null ? req.getCategory().toLowerCase() : "other")
                .expiryDate(req.getExpiryDate())
            .addedDate(utcToday())
                .build();

        return toResponse(pantryRepo.save(item));
    }

    public List<PantryItemResponse> bulkAdd(String userId, BulkPantryItemRequest req) {
        return req.getItems().stream()
                .map(r -> addItem(userId, r))
                .toList();
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "category", "ingredientName", "expiryDate", "addedDate", "quantity");

    public List<PantryItemResponse> getAll(String userId, String category, String sortField) {
        String safeSortField = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "category";
        Sort sort = Sort.by(Sort.Direction.ASC, safeSortField);
        List<PantryItem> items = category != null
                ? pantryRepo.findByUserIdAndCategory(userId, category.toLowerCase(), sort)
                : pantryRepo.findByUserId(userId, sort);
        return items.stream().map(this::toResponse).toList();
    }

    public PantryItemResponse updateItem(String userId, String itemId, PantryItemRequest req) {
        PantryItem item = pantryRepo.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PANTRY_ITEM_NOT_FOUND));

        if (req.getIngredientName() != null) {
            item.setIngredientName(req.getIngredientName().trim());
            item.setNormalizedName(normalize(req.getIngredientName()));
        }
        if (req.getQuantity() != null) item.setQuantity(req.getQuantity());
        if (req.getUnit() != null) item.setUnit(req.getUnit());
        if (req.getCategory() != null) item.setCategory(req.getCategory().toLowerCase());
        item.setExpiryDate(req.getExpiryDate());

        return toResponse(pantryRepo.save(item));
    }

    @Transactional
    public void deleteItem(String userId, String itemId) {
        pantryRepo.deleteByIdAndUserId(itemId, userId);
    }

    public long clearExpired(String userId) {
        List<PantryItem> expired = pantryRepo.findByUserIdAndExpiryDateBefore(userId, utcToday());
        pantryRepo.deleteAll(expired);
        return expired.size();
    }

    // ── Recipe Matching (§3) ────────────────────────────────────────

    public List<PantryRecipeMatchResponse> findMatchingRecipes(String userId, double minMatch, boolean prioritizeExpiring) {
        List<PantryItem> pantryItems = pantryRepo.findByUserId(userId, Sort.unsorted());
        if (pantryItems.isEmpty()) return List.of();

        Set<String> pantryNormals = pantryItems.stream()
                .map(PantryItem::getNormalizedName)
                .collect(Collectors.toSet());

        // Items expiring within 3 days
        LocalDate now = utcToday();
        Set<String> expiringNormals = pantryItems.stream()
                .filter(p -> p.getExpiryDate() != null && !p.getExpiryDate().isBefore(now) && p.getExpiryDate().isBefore(now.plusDays(4)))
                .map(PantryItem::getNormalizedName)
                .collect(Collectors.toSet());

        // Use projected query — loads only matching-relevant fields (id, title, coverImage,
        // totalTime, difficulty, fullIngredientList), avoiding heavy step/enrichment data.
        List<Recipe> publishedRecipes = recipeRepo.findPublishedForIngredientMatching();

        List<PantryRecipeMatchResponse> matches = new ArrayList<>();

        for (Recipe recipe : publishedRecipes) {
            if (recipe.getFullIngredientList() == null || recipe.getFullIngredientList().isEmpty()) continue;

            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            List<String> expiringUsed = new ArrayList<>();

            for (var ing : recipe.getFullIngredientList()) {
                String normalizedIng = normalize(ing.getName());
                if (pantryNormals.stream().anyMatch(pn -> pn.contains(normalizedIng) || normalizedIng.contains(pn))) {
                    matched.add(ing.getName());
                    if (expiringNormals.stream().anyMatch(en -> en.contains(normalizedIng) || normalizedIng.contains(en))) {
                        expiringUsed.add(ing.getName());
                    }
                } else {
                    missing.add(ing.getName());
                }
            }

            double matchPct = recipe.getFullIngredientList().isEmpty() ? 0.0
                    : (double) matched.size() / recipe.getFullIngredientList().size();
            if (matchPct >= minMatch) {
                matches.add(PantryRecipeMatchResponse.builder()
                        .recipeId(recipe.getId())
                        .recipeTitle(recipe.getTitle())
                        .coverImageUrl(recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty()
                                ? recipe.getCoverImageUrl().get(0) : null)
                        .totalTimeMinutes(recipe.getTotalTimeMinutes())
                        .difficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().getValue() : "Beginner")
                        .matchPercentage(Math.round(matchPct * 100.0) / 100.0)
                        .matchedIngredients(matched)
                        .missingIngredients(missing)
                        .expiringIngredientsUsed(expiringUsed)
                        .build());
            }
        }

        // Sort
        if (prioritizeExpiring) {
            matches.sort(Comparator
                    .<PantryRecipeMatchResponse, Integer>comparing(m -> m.getExpiringIngredientsUsed().size(), Comparator.reverseOrder())
                    .thenComparing(PantryRecipeMatchResponse::getMatchPercentage, Comparator.reverseOrder()));
        } else {
            matches.sort(Comparator.comparing(PantryRecipeMatchResponse::getMatchPercentage, Comparator.reverseOrder()));
        }

        return matches;
    }

    // ── Helpers ─────────────────────────────────────────────────────

    /**
     * Normalize ingredient name: lowercase, trim, replace spaces with underscore, basic singularization.
     */
    static String normalize(String name) {
        if (name == null) return "";
        String lower = name.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
        // Basic singularization
        if (lower.endsWith("ies")) {
            lower = lower.substring(0, lower.length() - 3) + "y";
        } else if (lower.endsWith("es") && !lower.endsWith("ches") && !lower.endsWith("shes")) {
            lower = lower.substring(0, lower.length() - 2);
        } else if (lower.endsWith("s") && !lower.endsWith("ss")) {
            lower = lower.substring(0, lower.length() - 1);
        }
        return lower;
    }

    private PantryItemResponse toResponse(PantryItem item) {
        String freshness = "fresh";
        if (item.getExpiryDate() != null) {
            LocalDate now = utcToday();
            if (item.getExpiryDate().isBefore(now)) {
                freshness = "expired";
            } else if (item.getExpiryDate().isBefore(now.plusDays(4))) {
                freshness = "expiring_soon";
            }
        }

        return PantryItemResponse.builder()
                .id(item.getId())
                .ingredientName(item.getIngredientName())
                .normalizedName(item.getNormalizedName())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .category(item.getCategory())
                .expiryDate(item.getExpiryDate())
                .addedDate(item.getAddedDate())
                .freshness(freshness)
                .build();
    }

    protected LocalDate utcToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
