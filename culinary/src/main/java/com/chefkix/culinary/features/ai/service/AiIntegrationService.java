package com.chefkix.culinary.features.ai.service;

import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.features.ai.dto.internal.*;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
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

    @Transactional
    public RecipeDetailResponse createRecipeFromText(String rawText, String userId) {
        log.info("Starting AI Process for user: {}", userId);

        AIProcessResponse aiData = aiRestClient.processRecipe(
                AIProcessRequest.builder().rawText(rawText).userId(userId).build()
        );

        Recipe draft = mapAiResponseToRecipe(userId, aiData);

        Recipe savedDraft = recipeRepository.save(draft);
        log.info("Draft saved with ID: {}", savedDraft.getId());

        return recipeMapper.toRecipeDetailResponse(savedDraft);
    }

    /**
     */
    private Recipe mapAiResponseToRecipe(String userId, AIProcessResponse aiData) {
        Recipe draft = new Recipe();
        draft.setUserId(userId);
        draft.setStatus(RecipeStatus.DRAFT);

        draft.setTitle(aiData.getTitle());
        draft.setDescription(aiData.getDescription());
        draft.setDifficulty(parseDifficulty(aiData.getDifficulty()));

        draft.setPrepTimeMinutes(aiData.getPrepTimeMinutes());
        draft.setCookTimeMinutes(aiData.getCookTimeMinutes());
        draft.setTotalTimeMinutes(aiData.getTotalTimeMinutes());
        draft.setServings(aiData.getServings());
        draft.setCuisineType(aiData.getCuisineType());
        draft.setDietaryTags(aiData.getDietaryTags());
        draft.setCaloriesPerServing(aiData.getCaloriesPerServing());

        if (aiData.getFullIngredientList() != null) {
            draft.setFullIngredientList(aiData.getFullIngredientList().stream()
                    .map(this::mapIngredientDtoToEntity)
                    .collect(Collectors.toList()));
        } else {
            draft.setFullIngredientList(new ArrayList<>());
        }

        if (aiData.getSteps() != null) {
            List<Step> steps = aiData.getSteps().stream()
                    .map(dto -> {
                        Step step = new Step();

                        step.setStepNumber(dto.getStepNumber());
                        step.setTitle(dto.getTitle());
                        step.setDescription(dto.getDescription());
                        step.setAction(dto.getAction());
                        step.setTimerSeconds(dto.getTimerSeconds());
                        step.setImageUrl(dto.getImageUrl());

                        step.setTips(dto.getTips());

                        if (dto.getIngredients() != null) {
                            step.setIngredients(dto.getIngredients().stream()
                                    .map(this::mapIngredientDtoToEntity)
                                    .collect(Collectors.toList()));
                        }

                        step.setChefTip(dto.getChefTip());
                        step.setTechniqueExplanation(dto.getTechniqueExplanation());
                        step.setCommonMistake(dto.getCommonMistake());
                        step.setEstimatedHandsOnTime(dto.getEstimatedHandsOnTime());
                        step.setVisualCues(dto.getVisualCues());
                        step.setEquipmentNeeded(dto.getEquipmentNeeded());
                        step.setGoal(dto.getGoal());
                        step.setMicroSteps(dto.getMicroSteps());

                        return step;
                    })
                    .collect(Collectors.toList());
            draft.setSteps(steps);
        } else {
            draft.setSteps(new ArrayList<>());
        }

        draft.setXpReward(aiData.getXpReward());
        draft.setDifficultyMultiplier(aiData.getDifficultyMultiplier());
        draft.setRewardBadges(aiData.getBadges());
        draft.setSkillTags(aiData.getSkillTags());

        if (aiData.getXpBreakdown() != null) {
            Recipe.XpBreakdown breakdown = new Recipe.XpBreakdown();
            breakdown.setBase(aiData.getXpBreakdown().getBase());
            breakdown.setBaseReason(aiData.getXpBreakdown().getBaseReason());
            breakdown.setSteps(aiData.getXpBreakdown().getSteps());
            breakdown.setStepsReason(aiData.getXpBreakdown().getStepsReason());
            breakdown.setTime(aiData.getXpBreakdown().getTime());
            breakdown.setTimeReason(aiData.getXpBreakdown().getTimeReason());
            breakdown.setTechniques(aiData.getXpBreakdown().getTechniques());
            breakdown.setTechniquesReason(aiData.getXpBreakdown().getTechniquesReason());
            breakdown.setTotal(aiData.getXpBreakdown().getTotal());
            draft.setXpBreakdown(breakdown);
        }

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

    private Ingredient mapIngredientDtoToEntity(AIProcessResponse.AiIngredientDto dto) {
        Ingredient ing = new Ingredient();
        ing.setName(dto.getName());
        ing.setQuantity(dto.getQuantity());
        ing.setUnit(dto.getUnit());
        return ing;
    }

    private Difficulty parseDifficulty(String diffStr) {
        try {
            if (diffStr == null) return Difficulty.INTERMEDIATE;
            return Difficulty.valueOf(diffStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Difficulty.INTERMEDIATE;
        }
    }

    @Transactional
    public RecipeDetailResponse calculateAndEnrichRecipe(String recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        log.info("Calculating metas for recipe: {}", recipe.getTitle());

        AIMetaRequest request = buildMetaRequest(recipe);

        AIMetaResponse response = aiRestClient.calculateMetas(request);

        updateRecipeWithMetas(recipe, response);

        Recipe savedRecipe = recipeRepository.save(recipe);
        return recipeMapper.toRecipeDetailResponse(savedRecipe);
    }

    private AIMetaRequest buildMetaRequest(Recipe recipe) {
        return AIMetaRequest.builder()
                .includeEquipment(true)
                .title(recipe.getTitle())
                .description(recipe.getDescription())
.difficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().getValue() : "Intermediate")
                .cuisineType(recipe.getCuisineType() != null ? recipe.getCuisineType() : "General")
                .dietaryTags(recipe.getDietaryTags())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .servings(recipe.getServings())
                .caloriesPerServing(recipe.getCaloriesPerServing())
                .fullIngredientList(recipe.getFullIngredientList().stream()
                        .map(i -> AIMetaRequest.MetaIngredientDto.builder()
                                .name(i.getName())
                                .quantity(i.getQuantity())
                                .unit(i.getUnit())
                                .build())
                        .collect(Collectors.toList()))
                .steps(recipe.getSteps().stream()
                        .map(s -> AIMetaRequest.MetaStepDto.builder()
                                .stepNumber(s.getStepNumber())
                                .description(s.getDescription())
                                .action(s.getAction())
                                .timerSeconds(s.getTimerSeconds())

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
.includeEnrichment(true)
                .build();
    }

    private void updateRecipeWithMetas(Recipe recipe, AIMetaResponse res) {
        recipe.setXpReward(res.getXpReward());
        recipe.setDifficultyMultiplier(res.getDifficultyMultiplier());
        recipe.setRewardBadges(res.getBadges());
        recipe.setSkillTags(res.getSkillTags());

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

        Recipe.ValidationMetadata validation = Recipe.ValidationMetadata.builder()
                .xpValidated(res.isXpValidated())
                .validationConfidence(res.getValidationConfidence())
                .validationIssues(res.getValidationIssues())
                .xpAdjusted(res.isXpAdjusted())
                .aiUsed(res.isAiUsed())
                .build();
        recipe.setValidation(validation);

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


    /**
     *
     */
    public AIValidationResponse validateRecipeForPublish(Recipe recipe) {
        log.info("Validating recipe for publish: {} (id={})", recipe.getTitle(), recipe.getId());

        List<String> ingredientNames = recipe.getFullIngredientList() != null
                ? recipe.getFullIngredientList().stream()
                    .map(Ingredient::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toList())
                : List.of();

        List<String> stepDescriptions = recipe.getSteps() != null
                ? recipe.getSteps().stream()
                    .map(Step::getDescription)
                    .filter(desc -> desc != null && !desc.isBlank())
                    .collect(Collectors.toList())
                : List.of();

        AIValidationRequest request = AIValidationRequest.builder()
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .ingredients(ingredientNames)
                .steps(stepDescriptions)
                .checkSafety(true)
                .build();

        AIValidationResponse response = aiRestClient.validateRecipe(request);

        if (!response.isContentSafe()) {
            log.warn("Recipe {} failed content safety validation: {}", recipe.getId(), response.getIssues());
            throw new AppException(ErrorCode.RECIPE_VALIDATION_FAILED);
        }

        log.info("Recipe {} passed content safety validation (legitimacy={})",
                recipe.getId(), response.getLegitimacyScore());
        return response;
    }

    /**
     *
     */
    public AIModerationResponse moderateRecipeContent(Recipe recipe) {
        log.info("Moderating recipe content: {} (id={})", recipe.getTitle(), recipe.getId());

        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("Title: ").append(recipe.getTitle()).append("\n");
        if (recipe.getDescription() != null) {
            textBuilder.append("Description: ").append(recipe.getDescription()).append("\n");
        }
        if (recipe.getSteps() != null) {
            for (Step step : recipe.getSteps()) {
                if (step.getDescription() != null) {
                    textBuilder.append("Step: ").append(step.getDescription()).append("\n");
                }
                if (step.getTips() != null) {
                    textBuilder.append("Tips: ").append(step.getTips()).append("\n");
                }
            }
        }

        AIModerationRequest request = AIModerationRequest.builder()
                .content(textBuilder.toString())
                .contentType("recipe")
                .build();

        AIModerationResponse response = aiRestClient.moderateContent(request);

        if (response.isBlocked()) {
            log.warn("Recipe {} blocked by moderation: {} (severity={}, reason={})",
                    recipe.getId(), response.getCategory(), response.getSeverity(), response.getReason());
            throw new AppException(ErrorCode.RECIPE_MODERATION_FAILED);
        }

        if ("flag".equalsIgnoreCase(response.getAction())) {
            log.info("Recipe {} flagged by moderation but allowed (severity={}, reason={})",
                    recipe.getId(), response.getSeverity(), response.getReason());
        }

        log.info("Recipe {} passed moderation (action={}, confidence={})",
                recipe.getId(), response.getAction(), response.getConfidence());
        return response;
    }

    /**
     */
    public boolean scoreRecipeQuality(Recipe recipe) {
        log.info("Scoring recipe quality: {} (id={})", recipe.getTitle(), recipe.getId());
        try {
            List<java.util.Map<String, Object>> stepMaps = new ArrayList<>();
            if (recipe.getSteps() != null) {
                for (Step step : recipe.getSteps()) {
                    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("step_number", step.getStepNumber());
                    map.put("title", step.getTitle());
                    map.put("description", step.getDescription());
                    map.put("action", step.getAction());
                    map.put("timer", step.getTimerSeconds());
                    map.put("tips", step.getTips());
                    map.put("chef_tip", step.getChefTip());
                    map.put("visual_cues", step.getVisualCues());
                    map.put("common_mistake", step.getCommonMistake());
                    map.put("goal", step.getGoal());
                    map.put("micro_steps", step.getMicroSteps());
                    if (step.getIngredients() != null) {
                        map.put("ingredients", step.getIngredients().stream()
                                .map(ing -> {
                                    java.util.Map<String, Object> ingMap = new java.util.LinkedHashMap<>();
                                    ingMap.put("name", ing.getName());
                                    ingMap.put("quantity", ing.getQuantity());
                                    ingMap.put("unit", ing.getUnit());
                                    return ingMap;
                                }).collect(Collectors.toList()));
                    }
                    stepMaps.add(map);
                }
            }

            List<java.util.Map<String, Object>> ingredientMaps = new ArrayList<>();
            if (recipe.getFullIngredientList() != null) {
                for (Ingredient ing : recipe.getFullIngredientList()) {
                    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("name", ing.getName());
                    map.put("quantity", ing.getQuantity());
                    map.put("unit", ing.getUnit());
                    ingredientMaps.add(map);
                }
            }

            boolean hasCover = recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty();
            int stepImages = recipe.getSteps() == null ? 0 :
                    (int) recipe.getSteps().stream().filter(s -> s.getImageUrl() != null).count();
            boolean hasVideo = recipe.getSteps() != null &&
                    recipe.getSteps().stream().anyMatch(s -> s.getVideoUrl() != null);

            AIQualityScoreRequest request = AIQualityScoreRequest.builder()
                    .title(recipe.getTitle())
                    .description(recipe.getDescription())
                    .difficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().getValue() : "Intermediate")
                    .steps(stepMaps)
                    .ingredients(ingredientMaps)
                    .prepTimeMinutes(recipe.getPrepTimeMinutes())
                    .cookTimeMinutes(recipe.getCookTimeMinutes())
                    .hasCoverImage(hasCover)
                    .hasStepImages(stepImages)
                    .hasVideo(hasVideo)
                    .build();

            AIQualityScoreResponse response = aiRestClient.scoreRecipeQuality(request);
            if (response != null) {
                recipe.setQualityScore(response.getOverallScore());
                recipe.setQualityTier(
                        com.chefkix.culinary.common.enums.QualityTier.fromValue(response.getTier()));
                log.info("Recipe {} scored: {} ({})", recipe.getId(),
                        response.getOverallScore(), response.getTier());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("RQS scoring failed for recipe {} — continuing without score: {}",
                    recipe.getId(), e.getMessage());
            return false;
        }
    }
}