package com.chefkix.culinary.features.ai.service;

import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.features.ai.dto.internal.AIMetaRequest;
import com.chefkix.culinary.features.ai.dto.internal.AIProcessRequest;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.ai.dto.internal.AIMetaResponse;
import com.chefkix.culinary.features.ai.dto.internal.AIProcessResponse;
import com.chefkix.culinary.features.recipe.entity.Ingredient;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.Step;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.common.client.AIRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIntegrationService {

    private final AIRestClient aiRestClient;
    private final RecipeMapper recipeMapper;
    private final RecipeRepository recipeRepository;

    // 1. MAIN FLOW: Raw Text -> Draft Recipe
    @Transactional
    public RecipeDetailResponse createRecipeFromText(String rawText, String userId) {
        log.info("Starting AI Process for user: {}", userId);

        // B1: Gọi AI lấy DTO
        AIProcessResponse aiData = aiRestClient.processRecipe(
                AIProcessRequest.builder().rawText(rawText).userId(userId).build()
        );

        // B2: Map DTO -> Entity
        Recipe draft = mapAiResponseToRecipe(userId, aiData);

        // B3: Save DB & Return
        Recipe savedDraft = recipeRepository.save(draft);
        log.info("Draft saved with ID: {}", savedDraft.getId());

        return recipeMapper.toRecipeDetailResponse(savedDraft);
    }

    /**
     * Logic Mapping chi tiết để đảm bảo không field nào bị NULL
     */
    private Recipe mapAiResponseToRecipe(String userId, AIProcessResponse aiData) {
        Recipe draft = new Recipe();
        draft.setUserId(userId);
        draft.setStatus(RecipeStatus.DRAFT);

        // --- 1. CORE INFO ---
        draft.setTitle(aiData.getTitle());
        draft.setDescription(aiData.getDescription());
        draft.setDifficulty(parseDifficulty(aiData.getDifficulty()));

        // --- 2. METADATA ---
        draft.setPrepTimeMinutes(aiData.getPrepTimeMinutes());
        draft.setCookTimeMinutes(aiData.getCookTimeMinutes());
        draft.setTotalTimeMinutes(aiData.getTotalTimeMinutes());
        draft.setServings(aiData.getServings());
        draft.setCuisineType(aiData.getCuisineType());
        draft.setDietaryTags(aiData.getDietaryTags());
        draft.setCaloriesPerServing(aiData.getCaloriesPerServing());

        // --- 3. INGREDIENTS (LIST TỔNG) ---
        if (aiData.getFullIngredientList() != null) {
            draft.setFullIngredientList(aiData.getFullIngredientList().stream()
                    .map(this::mapIngredientDtoToEntity)
                    .collect(Collectors.toList()));
        } else {
            draft.setFullIngredientList(new ArrayList<>());
        }

        // --- 4. STEPS (CHI TIẾT) ---
        if (aiData.getSteps() != null) {
            List<Step> steps = aiData.getSteps().stream()
                    .map(dto -> {
                        Step step = new Step();

                        // Map Core Step
                        step.setStepNumber(dto.getStepNumber());
                        step.setTitle(dto.getTitle());
                        step.setDescription(dto.getDescription());
                        step.setAction(dto.getAction());
                        step.setTimerSeconds(dto.getTimerSeconds());
                        step.setImageUrl(dto.getImageUrl());

                        // [FIX QUAN TRỌNG] Map Tips
                        step.setTips(dto.getTips());

                        // [FIX QUAN TRỌNG] Map Ingredients Con trong Step
                        if (dto.getIngredients() != null) {
                            step.setIngredients(dto.getIngredients().stream()
                                    .map(this::mapIngredientDtoToEntity)
                                    .collect(Collectors.toList()));
                        }

                        // Map Enrichment Fields (Phẳng hóa)
                        step.setChefTip(dto.getChefTip());
                        step.setTechniqueExplanation(dto.getTechniqueExplanation());
                        step.setCommonMistake(dto.getCommonMistake());
                        step.setEstimatedHandsOnTime(dto.getEstimatedHandsOnTime());
                        step.setVisualCues(dto.getVisualCues());
                        step.setEquipmentNeeded(dto.getEquipmentNeeded());

                        return step;
                    })
                    .collect(Collectors.toList());
            draft.setSteps(steps);
        } else {
            draft.setSteps(new ArrayList<>());
        }

        // --- 5. GAMIFICATION ---
        draft.setXpReward(aiData.getXpReward());
        draft.setDifficultyMultiplier(aiData.getDifficultyMultiplier());
        draft.setRewardBadges(aiData.getBadges());
        draft.setSkillTags(aiData.getSkillTags());

        // --- 6. ENRICHMENT METADATA ---
        // Xử lý techniqueGuides (Map -> List Key)
        List<String> techniques = null;
        if (aiData.getTechniqueGuides() != null) {
            techniques = new ArrayList<>(aiData.getTechniqueGuides().keySet());
        }

        Recipe.EnrichmentMetadata enrichment = Recipe.EnrichmentMetadata.builder()
                .recipeStory(aiData.getRecipeStory())
                .chefNotes(aiData.getChefNotes())
                .equipmentNeeded(aiData.getEquipmentNeeded())
                .techniqueGuides(techniques)
                .seasonalTags(aiData.getSeasonalTags())
                .ingredientSubstitutions(aiData.getIngredientSubstitutions())
                .regionalOrigin(aiData.getRegionalOrigin())
                .aiEnriched(true)
                .build();
        draft.setEnrichment(enrichment);

        return draft;
    }

    // Helper: Map Ingredient DTO -> Entity
    private Ingredient mapIngredientDtoToEntity(AIProcessResponse.AiIngredientDto dto) {
        Ingredient ing = new Ingredient();
        ing.setName(dto.getName());
        ing.setQuantity(dto.getQuantity());
        ing.setUnit(dto.getUnit());
        return ing;
    }

    // Helper: Parse Enum Difficulty
    private Difficulty parseDifficulty(String diffStr) {
        try {
            if (diffStr == null) return Difficulty.INTERMEDIATE;
            // AI trả về: "Beginner", "Intermediate"... -> Chuyển thành UPPERCASE để khớp Enum
            return Difficulty.valueOf(diffStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Difficulty.INTERMEDIATE;
        }
    }

    @Transactional
    public RecipeDetailResponse calculateAndEnrichRecipe(String recipeId) {
        // 1. Lấy Recipe từ DB
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        log.info("Calculating metas for recipe: {}", recipe.getTitle());

        // 2. Build Request gửi sang AI
        AIMetaRequest request = buildMetaRequest(recipe);

        // 3. Gọi AI
        AIMetaResponse response = aiRestClient.calculateMetas(request);

        // 4. Update lại Recipe với dữ liệu mới
        updateRecipeWithMetas(recipe, response);

        // 5. Lưu & Trả về
        Recipe savedRecipe = recipeRepository.save(recipe);
        return recipeMapper.toRecipeDetailResponse(savedRecipe);
    }

    // --- Helper 1: Convert Entity -> AI Request ---
    private AIMetaRequest buildMetaRequest(Recipe recipe) {
        return AIMetaRequest.builder()
                .includeEquipment(true)
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .difficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().getValue() : "Intermediate") // Enum -> String
                .cuisineType(recipe.getCuisineType() != null ? recipe.getCuisineType() : "General")
                .dietaryTags(recipe.getDietaryTags())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .servings(recipe.getServings())
                .caloriesPerServing(recipe.getCaloriesPerServing())
                // Map Ingredients
                .fullIngredientList(recipe.getFullIngredientList().stream()
                        .map(i -> AIMetaRequest.MetaIngredientDto.builder()
                                .name(i.getName())
                                .quantity(i.getQuantity())
                                .unit(i.getUnit())
                                .build())
                        .collect(Collectors.toList()))
                // Map Steps
                .steps(recipe.getSteps().stream()
                        .map(s -> AIMetaRequest.MetaStepDto.builder()
                                .stepNumber(s.getStepNumber())
                                .description(s.getDescription())
                                .action(s.getAction())
                                .timerSeconds(s.getTimerSeconds())

                                // --- [FIX] Đổ dữ liệu Ingredient từ DB vào Request ---
                                .ingredients(s.getIngredients() != null ?
                                        s.getIngredients().stream()
                                                .map(i -> AIMetaRequest.MetaIngredientDto.builder()
                                                        .name(i.getName())
                                                        .quantity(i.getQuantity())
                                                        .unit(i.getUnit())
                                                        .build())
                                                .collect(Collectors.toList())
                                        : null)
                                .build())
                        .collect(Collectors.toList()))
                .includeEnrichment(true) // Luôn bật để lấy story
                .build();
    }

    // --- Helper 2: Update Entity từ AI Response ---
    private void updateRecipeWithMetas(Recipe recipe, AIMetaResponse res) {
        // 1. Gamification
        recipe.setXpReward(res.getXpReward());
        recipe.setDifficultyMultiplier(res.getDifficultyMultiplier());
        recipe.setRewardBadges(res.getBadges());
        recipe.setSkillTags(res.getSkillTags());

        // 2. XP Breakdown (Quan trọng để hiển thị minh bạch)
        if (res.getXpBreakdown() != null) {
            Recipe.XpBreakdown breakdown = new Recipe.XpBreakdown();
            breakdown.setBase(res.getXpBreakdown().getBase());
            breakdown.setBaseReason(res.getXpBreakdown().getBaseReason());
            breakdown.setSteps(res.getXpBreakdown().getSteps());
            breakdown.setStepsReason(res.getXpBreakdown().getStepsReason());
            breakdown.setTime(res.getXpBreakdown().getTime());
            breakdown.setTimeReason(res.getXpBreakdown().getTimeReason());
            breakdown.setTotal(res.getXpBreakdown().getTotal());
            recipe.setXpBreakdown(breakdown);
        }

        // 3. Validation (Anti-cheat)
        Recipe.ValidationMetadata validation = Recipe.ValidationMetadata.builder()
                .xpValidated(res.isXpValidated())
                .validationConfidence(res.getValidationConfidence())
                .validationIssues(res.getValidationIssues())
                .xpAdjusted(res.isXpAdjusted())
                .aiUsed(res.isAiUsed())
                .build();
        recipe.setValidation(validation);

        // 4. Enrichment Metadata
        Recipe.CulturalContext culture = null;
        if (res.getCulturalContext() != null) {
            culture = new Recipe.CulturalContext(
                    res.getCulturalContext().getRegion(),
                    res.getCulturalContext().getBackground(),
                    res.getCulturalContext().getSignificance()
            );
        }

        Recipe.EnrichmentMetadata enrichment = Recipe.EnrichmentMetadata.builder()
                .equipmentNeeded(res.getEquipmentNeeded())
                .techniqueGuides(res.getTechniqueGuides())
                .seasonalTags(res.getSeasonalTags())
                .ingredientSubstitutions(res.getIngredientSubstitutions())
                .recipeStory(res.getRecipeStory())
                .chefNotes(res.getChefNotes())
                .aiEnriched(res.isAiEnriched())
                .culturalContext(culture)
                .build();
        recipe.setEnrichment(enrichment);
    }
}