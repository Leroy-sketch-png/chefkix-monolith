package com.chefkix.culinary.features.recipe.service;

import com.chefkix.culinary.features.recipe.dto.request.RecipePublishRequest;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipePublishResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.entity.Ingredient;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.Step;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.ai.service.AiIntegrationService;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.features.recipe.events.RecipeIndexEvent;
import com.chefkix.culinary.features.recipe.mapper.IngredientMapper;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.mapper.StepMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DraftService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper mapper;
    private final StepMapper stepMapper;
    private final IngredientMapper ingredientMapper;
    private final AsyncHelper asyncHelper;
    private final AiIntegrationService aiIntegrationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RecipeDetailResponse createDraft() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Async Author fetch (utilize DB wait time to get user info)
        // Frontend needs this to display the author avatar immediately when opening the screen
        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(userId);

        // 2. Create EMPTY entity (Bare-bones)
        Recipe draft = Recipe.builder()
                .userId(userId)
                .status(RecipeStatus.DRAFT) // <--- MOST IMPORTANT
                .createdAt(Instant.now())
                .updatedAt(Instant.now())

                // Init empty lists so FE doesn't get null pointer
                .coverImageUrl(new ArrayList<>())
                .fullIngredientList(new ArrayList<>())
                .steps(new ArrayList<>())

                // Init counters to 0
                .likeCount(0)
                .cookCount(0)
                .build();

        // 3. Save to generate ID (Mongo will create _id field)
        draft = recipeRepository.save(draft);

        // 4. Map to Response
        RecipeDetailResponse response = mapper.toRecipeDetailResponse(draft);
        response.setRecipeStatus(RecipeStatus.DRAFT);

        // 5. Join Author (Get Async result)
        // DB save takes ~10-20ms, Identity call takes ~15-30ms
        // -> Running in parallel saves time
        response.setAuthor(authorFuture.join());

        // Draft has no likes/saves yet
        response.setIsLiked(false);
        response.setIsSaved(false);

        return response;
    }

    // =========================================================================
    // 2. AUTO-SAVE (Flexible update logic)
    // =========================================================================
    @Transactional
    public RecipeDetailResponse autoSaveDraft(String id, RecipeRequest request) {
        // 1. Find the recipe
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 2. Check ownership (Security)
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2b. CRITICAL: Only allow auto-save on DRAFT recipes
        // Without this guard, published/archived recipes could be silently modified
        if (recipe.getStatus() != RecipeStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        // 3. Map data (IMPORTANT: Only map non-null fields)
        // If FE only sends title, description, steps etc. must remain unchanged
        mapper.updateRecipeFromRequest(recipe, request);

        // 4. EXPLICIT COLLECTION HANDLING (MapStruct doesn't handle list replacement well)
        // Steps: REPLACE the entire list if provided
        if (request.getSteps() != null) {
            var newSteps = request.getSteps().stream()
                    .map(stepMapper::toStep)
                    .toList();
            recipe.getSteps().clear();
            recipe.getSteps().addAll(newSteps);
            log.debug("Updated {} steps for draft {}", newSteps.size(), id);
        }

        // Ingredients: REPLACE the entire list if provided  
        if (request.getFullIngredientList() != null) {
            var newIngredients = request.getFullIngredientList().stream()
                    .map(ingredientMapper::toIngredient)
                    .toList();
            recipe.getFullIngredientList().clear();
            recipe.getFullIngredientList().addAll(newIngredients);
            log.debug("Updated {} ingredients for draft {}", newIngredients.size(), id);
        }

        // 5. Update Metadata
        recipe.setUpdatedAt(Instant.now());

        // 6. Save (No status change, no business rule validation)
        recipe = recipeRepository.save(recipe);

        return mapper.toRecipeDetailResponse(recipe);
    }

    // =========================================================================
    // 3. GET DRAFTS & DELETE
    // =========================================================================
    public List<RecipeSummaryResponse> getMyDrafts() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Fire Async Profile request IMMEDIATELY
        // (Run in parallel while DB query is executing in step 2)
        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(currentUserId);

        // 2. Query Database (IO Blocking)
        List<Recipe> drafts = recipeRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(currentUserId, RecipeStatus.DRAFT);

        if (drafts.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Resolve Author result (JOIN)
        // By now DB query is done, and authorFuture likely has result too -> No long wait
        AuthorResponse authorProfile = authorFuture.join();

        // 4. Map & Enrich
        return drafts.stream()
                .map(draft -> {
                    // Basic mapping
                    RecipeSummaryResponse response = mapper.toRecipeSummaryResponse(draft);

                    // Set Author (Reuse same authorProfile object for all items -> Saves memory)
                    if (authorProfile != null) {
                        response.setAuthor(AuthorResponse.builder()
                                .userId(authorProfile.getUserId())
                                .displayName(authorProfile.getDisplayName())
                                .avatarUrl(authorProfile.getAvatarUrl())
                                .username(authorProfile.getUsername())
                                .build());
                    } else {
                        // Fallback (In case Identity service errors)
                        response.setAuthor(AuthorResponse.builder()
                                .userId(currentUserId)
                                .displayName("Me")
                                .build());
                    }

                    return response;
                })
                .toList();
    }

    public void discardDraft(String id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // CRITICAL: Only allow discarding DRAFT recipes
        // Without this guard, published recipes could be hard-deleted via this endpoint
        if (recipe.getStatus() != RecipeStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        // Hard Delete (Remove entirely from DB)
        recipeRepository.delete(recipe);
    }

    @Transactional
    public RecipePublishResponse publishRecipe(String id, RecipePublishRequest request) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Fetch & authorize
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. MANDATORY FIELD VALIDATION (local — no AI needed)
        validateMandatoryFields(recipe);

        // 3. AI RECIPE VALIDATION (fail-closed: if AI is down, block publish)
        // Checks: content safety, legitimacy, is-real-food, dangerous combinations
        aiIntegrationService.validateRecipeForPublish(recipe);

        // 4. AI CONTENT MODERATION (fail-closed: if AI is down, block publish)
        // Checks: toxic content, spam, off-topic (hybrid rules + AI)
        aiIntegrationService.moderateRecipeContent(recipe);

        // 5. RQS SCORING (fail-open: if AI is down, publish continues without score)
        aiIntegrationService.scoreRecipeQuality(recipe);

        // 6. All gates passed — publish
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setPublishedAt(Instant.now());
        recipe.setRecipeVisibility(request.getVisibility());
        recipe.setUpdatedAt(Instant.now());

        // Init stats if needed
        if (recipe.getLikeCount() == 0) recipe.setLikeCount(0);

        var savedRecipe = recipeRepository.save(recipe);

        // Real-time Typesense indexing — fires synchronously after MongoDB save
        eventPublisher.publishEvent(RecipeIndexEvent.index(savedRecipe));

        log.info("Recipe {} published successfully by user {}", id, currentUserId);

        return RecipePublishResponse.builder()
                .moderationStatus(savedRecipe.getStatus())
                .isPublished(true)
                .qualityScore(savedRecipe.getQualityScore())
                .qualityTier(savedRecipe.getQualityTier())
                .build();
    }

    /**
     * Duplicate any owned recipe (draft or published) as a new DRAFT.
     * Deep-copies all content fields; resets identity, status, and social counters.
     */
    @Transactional
    public RecipeDetailResponse duplicateDraft(String sourceId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Recipe source = recipeRepository.findById(sourceId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (!source.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Deep copy content via builder — new lists to avoid shared references
        Recipe duplicate = Recipe.builder()
                .userId(userId)
                .status(RecipeStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(null)
                .recipeVisibility(null)
                // Content
                .title(source.getTitle() != null ? source.getTitle() + " (Copy)" : "Untitled (Copy)")
                .description(source.getDescription())
                .difficulty(source.getDifficulty())
                .prepTimeMinutes(source.getPrepTimeMinutes())
                .cookTimeMinutes(source.getCookTimeMinutes())
                .totalTimeMinutes(source.getTotalTimeMinutes())
                .servings(source.getServings())
                .cuisineType(source.getCuisineType())
                .caloriesPerServing(source.getCaloriesPerServing())
                // Media — new lists with same URLs (Cloudinary URLs are shareable)
                .coverImageUrl(source.getCoverImageUrl() != null ? new ArrayList<>(source.getCoverImageUrl()) : new ArrayList<>())
                .videoUrl(source.getVideoUrl() != null ? new ArrayList<>(source.getVideoUrl()) : new ArrayList<>())
                // Tags
                .dietaryTags(source.getDietaryTags() != null ? new ArrayList<>(source.getDietaryTags()) : new ArrayList<>())
                .skillTags(source.getSkillTags() != null ? new ArrayList<>(source.getSkillTags()) : new ArrayList<>())
                .rewardBadges(source.getRewardBadges() != null ? new ArrayList<>(source.getRewardBadges()) : new ArrayList<>())
                // Structure — deep copy via streams
                .fullIngredientList(deepCopyIngredients(source.getFullIngredientList()))
                .steps(deepCopySteps(source.getSteps()))
                // Gamification (preserve AI data so user doesn't have to re-process)
                .xpReward(source.getXpReward())
                .difficultyMultiplier(source.getDifficultyMultiplier())
                .xpBreakdown(source.getXpBreakdown())
                .validation(source.getValidation())
                .enrichment(source.getEnrichment())
                // Social counters — reset to 0
                .likeCount(0)
                .saveCount(0)
                .viewCount(0)
                .cookCount(0)
                .masteredByCount(0)
                .averageRating(0.0)
                .creatorXpEarned(0)
                .trendingScore(0.0)
                .build();

        duplicate = recipeRepository.save(duplicate);

        // Build response with author info
        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(userId);
        RecipeDetailResponse response = mapper.toRecipeDetailResponse(duplicate);
        response.setAuthor(authorFuture.join());
        response.setIsLiked(false);
        response.setIsSaved(false);

        log.info("Recipe {} duplicated as new draft {} by user {}", sourceId, duplicate.getId(), userId);

        return response;
    }

    private List<Ingredient> deepCopyIngredients(List<Ingredient> source) {
        if (source == null || source.isEmpty()) return new ArrayList<>();
        return source.stream().map(i -> {
            Ingredient copy = new Ingredient();
            copy.setName(i.getName());
            copy.setQuantity(i.getQuantity());
            copy.setUnit(i.getUnit());
            return copy;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<Step> deepCopySteps(List<Step> source) {
        if (source == null || source.isEmpty()) return new ArrayList<>();
        return source.stream().map(s -> {
            Step copy = new Step();
            copy.setStepNumber(s.getStepNumber());
            copy.setTitle(s.getTitle());
            copy.setDescription(s.getDescription());
            copy.setAction(s.getAction());
            copy.setTimerSeconds(s.getTimerSeconds());
            copy.setImageUrl(s.getImageUrl());
            copy.setTips(s.getTips());
            copy.setIngredients(s.getIngredients() != null ? deepCopyIngredients(s.getIngredients()) : null);
            copy.setChefTip(s.getChefTip());
            copy.setTechniqueExplanation(s.getTechniqueExplanation());
            copy.setCommonMistake(s.getCommonMistake());
            copy.setEstimatedHandsOnTime(s.getEstimatedHandsOnTime());
            copy.setEquipmentNeeded(s.getEquipmentNeeded() != null ? new ArrayList<>(s.getEquipmentNeeded()) : null);
            copy.setVisualCues(s.getVisualCues());
            return copy;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Validate all mandatory fields required for publishing.
     * Uses English error messages per spec §3.
     */
    private void validateMandatoryFields(Recipe recipe) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(recipe.getTitle())) {
            errors.add("Title is required");
        }

        if (recipe.getCoverImageUrl() == null || recipe.getCoverImageUrl().isEmpty()) {
            errors.add("At least one cover image is required");
        }

        if (recipe.getFullIngredientList() == null || recipe.getFullIngredientList().isEmpty()) {
            errors.add("Ingredient list cannot be empty");
        }

        if (recipe.getSteps() == null || recipe.getSteps().isEmpty()) {
            errors.add("At least one cooking step is required");
        }

        if (recipe.getServings() <= 0) {
            errors.add("Servings must be greater than 0");
        }

        if (!errors.isEmpty()) {
            log.warn("Draft {} failed mandatory validation: {}", recipe.getId(), errors);
            throw new AppException(ErrorCode.DRAFT_VALIDATION_FAILED, String.join("; ", errors));
        }
    }
}