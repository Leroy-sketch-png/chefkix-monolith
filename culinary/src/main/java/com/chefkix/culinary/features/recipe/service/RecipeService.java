package com.chefkix.culinary.features.recipe.service;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.culinary.features.report.dto.internal.InternalCreatorInsightsResponse;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.features.recipe.dto.response.CreatorPerformanceResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecentCookResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSocialProofResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.dto.response.StepHeatmapResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.RecipeVisibility;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.features.recipe.events.RecipeIndexEvent;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.identity.api.ProfileProvider;
import org.springframework.context.ApplicationEventPublisher;
import com.chefkix.culinary.features.interaction.service.InteractionService; // Import Service mới
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.enums.TimerEventType;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecipeService {

    private static final int MIN_SESSIONS_FOR_STRUGGLE = 3;
    private static final double SKIP_RATE_THRESHOLD = 30.0;
    private static final double COMPLETION_RATE_THRESHOLD = 60.0;

    AsyncHelper asyncHelper;
    ProfileProvider profileProvider;
    RecipeMapper recipeMapper;
    RecipeRepository recipeRepository;
    RecipeHelper recipeHelper;
    InteractionService interactionService;
    CookingSessionRepository cookingSessionRepository;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public RecipeDetailResponse updateRecipe(String recipeId, RecipeRequest request) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[RECIPE_UPDATE] User {} requested update for recipe {}", currentUserId, recipeId);

        try {
            CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(currentUserId);

            Recipe existingRecipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

            if (!existingRecipe.getUserId().equals(currentUserId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            recipeMapper.updateRecipeFromRequest(existingRecipe, request);

            // NOTE: isPublished bypass removed — publishing only via DraftService.publishRecipe()
            // which enforces mandatory field validation + AI safety checks

            authorFuture.join();
            existingRecipe = recipeRepository.save(existingRecipe);

            RecipeDetailResponse response = recipeMapper.toRecipeDetailResponse(existingRecipe);
            response.setAuthor(authorFuture.get());

            // Check like/save status via InteractionService
            response.setIsLiked(interactionService.isLiked(recipeId, currentUserId));
            response.setIsSaved(interactionService.isSaved(recipeId, currentUserId));

            return response;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RECIPE_UPDATE] Failed to update recipe", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to update recipe");
        }
    }

    // --- TOGGLE METHODS MOVED TO INTERACTION SERVICE ---

    @Transactional
    public void deleteRecipe(String recipeId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Prevent deleting recipes that have active cooking sessions
        long activeSessions = cookingSessionRepository.countByRecipeIdAndStatus(recipeId, SessionStatus.IN_PROGRESS);
        if (activeSessions > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Soft-delete: archive instead of hard delete to preserve data integrity
        recipe.setStatus(RecipeStatus.ARCHIVED);
        recipeRepository.save(recipe);
        // Real-time Typesense removal — archived recipes must not appear in search
        eventPublisher.publishEvent(RecipeIndexEvent.remove(recipeId));
        log.info("[RECIPE_DELETE] User {} archived recipe {}", currentUserId, recipeId);
    }

    public RecipeDetailResponse getRecipeById(String recipeId) {
        String viewerId = SecurityContextHolder.getContext().getAuthentication().getName();

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // Prevent unauthorized access to non-published recipes
        boolean isOwner = recipe.getUserId().equals(viewerId);
        if (!isOwner) {
            if (recipe.getStatus() == RecipeStatus.DRAFT || recipe.getStatus() == RecipeStatus.ARCHIVED) {
                throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
            }
            if (recipe.getRecipeVisibility() == RecipeVisibility.PRIVATE) {
                throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
            }
        }

        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(recipe.getUserId())
                .exceptionally(ex -> {
                    log.error("[AUTHOR] Failed to fetch profile: {}", ex.getMessage());
                    return AuthorResponse.builder().userId(recipe.getUserId()).displayName("Unknown Chef").build();
                });

        boolean isLiked = false;
        boolean isSaved = false;

        if (!"anonymousUser".equals(viewerId)) {
            // Thay vì gọi Repository, ta gọi qua Service
            isLiked = interactionService.isLiked(recipeId, viewerId);
            isSaved = interactionService.isSaved(recipeId, viewerId);
        }

        RecipeDetailResponse response = recipeMapper.toRecipeDetailResponse(recipe);
        response.setAuthor(authorFuture.join());
        response.setIsLiked(isLiked);
        response.setIsSaved(isSaved);

        recipeHelper.incrementRecipeStats(recipeId, Map.of("viewCount", 1));

        return response;
    }

    public Page<RecipeDetailResponse> searchRecipes(RecipeSearchQuery query, Pageable pageable) {
        // Populate visibility context for privacy-aware search
        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            query.setCurrentUserId(currentUserId);
            query.setFriendIds(profileProvider.getFriendIds(currentUserId));
        } catch (Exception e) {
            log.debug("Could not resolve user context for recipe search: {}", e.getMessage());
        }
        return recipeRepository.searchRecipes(query, pageable).map(recipeMapper::toRecipeDetailResponse);
    }

    public Page<RecipeDetailResponse> getTrendingRecipes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "trendingScore"));
        return recipeRepository.findByStatus(RecipeStatus.PUBLISHED, pageable).map(recipeMapper::toRecipeDetailResponse);
    }

    public Page<RecipeSummaryResponse> getFriendsFeed(int page, int size) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<String> friendIds;
        try {
            friendIds = profileProvider.getFriendIds(currentUserId);
        } catch (Exception e) {
            log.error("Failed friend list {}", currentUserId, e);
            throw new AppException(ErrorCode.EMPTY);
        }

        if (friendIds == null || friendIds.isEmpty()) {
            throw new AppException(ErrorCode.NO_FRIEND_YET);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Recipe> pageResult = recipeRepository.findAllByUserIdInOrderByCreatedAtDesc(friendIds, pageable);

        if (pageResult.isEmpty()) return Page.empty(pageable);

        Set<String> uniqueAuthorIds = pageResult.getContent().stream()
                .map(Recipe::getUserId)
                .collect(Collectors.toSet());

        Map<String, CompletableFuture<AuthorResponse>> profileFutureMap = uniqueAuthorIds.stream()
                .collect(Collectors.toMap(id -> id, asyncHelper::getProfileAsync));

        CompletableFuture.allOf(profileFutureMap.values().toArray(new CompletableFuture[0])).join();

        return pageResult.map(recipe -> {
            RecipeSummaryResponse response = recipeMapper.toRecipeSummaryResponse(recipe);

            // Gọi qua InteractionService
            response.setIsLiked(interactionService.isLiked(response.getId(), currentUserId));
            response.setIsSaved(interactionService.isSaved(response.getId(), currentUserId));

            CompletableFuture<AuthorResponse> future = profileFutureMap.get(recipe.getUserId());
            try {
                AuthorResponse authorProfile = future.getNow(null);
                response.setAuthor(authorProfile != null ? authorProfile :
                        AuthorResponse.builder().userId(recipe.getUserId()).displayName("Unknown Chef").build());
            } catch (Exception e) {
                log.error("Error mapping author {}", recipe.getId());
            }
            return response;
        });
    }

    // --- CÁC HÀM GET LIKED/SAVED RECIPES ĐÃ CHUYỂN SANG INTERACTION SERVICE ---

    public Page<RecipeSummaryResponse> getRecipesByUser(String targetUserId, int page, int size) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Recipe> recipesPage = recipeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                targetUserId, RecipeStatus.PUBLISHED, pageable);

        return recipesPage.map(recipe -> {
            RecipeSummaryResponse response = recipeMapper.toRecipeSummaryResponse(recipe);
            // Gọi qua InteractionService
            response.setIsLiked(interactionService.isLiked(recipe.getId(), currentUserId));
            response.setIsSaved(interactionService.isSaved(recipe.getId(), currentUserId));
            return response;
        });
    }

    // ===============================================
    // RECOMMENDATIONS
    // ===============================================

    /**
     * Tonight's Pick — personalized daily recipe recommendation.
     * Algorithm: user's cooking history (cuisine prefs) + trending + easy-to-start recipes.
     * Cold start: returns highest trending score published recipe.
     */
    @Transactional(readOnly = true)
    public RecipeDetailResponse getTonightsPick() {
        String rawUserId;
        try {
            rawUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            rawUserId = null;
        }
        final String currentUserId = rawUserId;

        Recipe pick = null;

        // Try personalized recommendation based on cooking history
        if (currentUserId != null && !"anonymousUser".equals(currentUserId)) {
            List<CookingSession> recentSessions = cookingSessionRepository
                    .findTop20ByUserIdOrderByStartedAtDesc(currentUserId);

            if (!recentSessions.isEmpty()) {
                // Extract preferred cuisines from cooking history
                Set<String> cookedRecipeIds = recentSessions.stream()
                        .map(CookingSession::getRecipeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                List<Recipe> cookedRecipes = recipeRepository.findAllByIdIn(
                        new ArrayList<>(cookedRecipeIds));

                List<String> preferredCuisines = cookedRecipes.stream()
                        .map(Recipe::getCuisineType)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(5)
                        .toList();

                if (!preferredCuisines.isEmpty()) {
                    // Find a matching recipe user hasn't cooked, sorted by trending
                    List<Recipe> candidates = recipeRepository
                            .findTop5ByCuisineTypeInIgnoreCase(preferredCuisines);

                    pick = candidates.stream()
                            .filter(r -> r.getStatus() == RecipeStatus.PUBLISHED)
                            .filter(r -> !cookedRecipeIds.contains(r.getId()))
                            .filter(r -> !r.getUserId().equals(currentUserId))
                            .max(Comparator.comparingDouble(r -> r.getTrendingScore() != null ? r.getTrendingScore() : 0.0))
                            .orElse(null);
                }
            }
        }

        // Fallback: highest trending score
        if (pick == null) {
            Page<Recipe> trending = recipeRepository.findByStatus(
                    RecipeStatus.PUBLISHED,
                    PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "trendingScore")));
            if (!trending.isEmpty()) {
                pick = trending.getContent().get(0);
            }
        }

        if (pick == null) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }

        RecipeDetailResponse response = recipeMapper.toRecipeDetailResponse(pick);
        try {
            response.setAuthor(asyncHelper.getProfileAsync(pick.getUserId()).join());
        } catch (Exception e) {
            response.setAuthor(AuthorResponse.builder()
                    .userId(pick.getUserId()).displayName("Chef").build());
        }
        return response;
    }

    /**
     * Similar Recipes — content-based recommendations.
     * Matches by cuisine type, difficulty, and dietary tags.
     * Excludes the source recipe itself.
     */
    @Transactional(readOnly = true)
    public Page<RecipeDetailResponse> getSimilarRecipes(String recipeId, int size) {
        Recipe source = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // Build search query matching cuisine and/or difficulty
        RecipeSearchQuery query = new RecipeSearchQuery();
        if (source.getCuisineType() != null) {
            query.setCuisineType(source.getCuisineType());
        }
        if (source.getDifficulty() != null) {
            query.setDifficulty(source.getDifficulty());
        }

        Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "trendingScore"));
        Page<RecipeDetailResponse> results = recipeRepository.searchRecipes(query, pageable)
                .map(recipeMapper::toRecipeDetailResponse);

        // Filter out the source recipe
        List<RecipeDetailResponse> filtered = results.getContent().stream()
                .filter(r -> !r.getId().equals(recipeId))
                .limit(size)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(filtered, PageRequest.of(0, size), filtered.size());
    }

    public InternalCreatorInsightsResponse getRecipeWithAboveTenCooks(String userId) {
        // Get all published recipes for this user
        List<Recipe> allRecipes = recipeRepository.findByUserIdAndStatus(userId, RecipeStatus.PUBLISHED);
        
        // Handle case where user has no recipes
        if (allRecipes.isEmpty()) {
            return InternalCreatorInsightsResponse.builder()
                    .topRecipe(null)
                    .highPerformingRecipes(List.of())
                    .avgRating(null)
                    .build();
        }
        
        // Find top recipe by cook count
        Recipe top = allRecipes.stream()
                .max((r1, r2) -> Long.compare(r1.getCookCount(), r2.getCookCount()))
                .orElse(allRecipes.get(0));
        
        // Filter high-performing recipes (10+ cooks)
        List<Recipe> performantRecipes = allRecipes.stream()
                .filter(r -> r.getCookCount() >= 10)
                .toList();
        
        // Calculate average rating across all recipes (only count those with ratings > 0)
        Double avgRating = allRecipes.stream()
                .filter(r -> r.getAverageRating() != null && r.getAverageRating() > 0)
                .mapToDouble(Recipe::getAverageRating)
                .average()
                .orElse(0.0);

        return InternalCreatorInsightsResponse.builder()
                .topRecipe(recipeMapper.toRecipeDto(top))
                .highPerformingRecipes(performantRecipes.stream().map(recipeMapper::toRecipeDto).toList())
                .avgRating(avgRating > 0 ? avgRating : null)
                .build();
    }

    // ===============================================
    // CREATOR ANALYTICS
    // ===============================================

    /**
     * Per-recipe performance metrics for the creator dashboard.
     * Returns all published recipes with their individual stats + an aggregate summary.
     */
    @Transactional(readOnly = true)
    public CreatorPerformanceResponse getCreatorPerformance() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Recipe> recipes = recipeRepository.findByUserIdAndStatus(userId, RecipeStatus.PUBLISHED);

        List<CreatorPerformanceResponse.RecipePerformanceItem> items = recipes.stream()
                .map(r -> CreatorPerformanceResponse.RecipePerformanceItem.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .coverImageUrl(r.getCoverImageUrl())
                        .difficulty(r.getDifficulty() != null ? r.getDifficulty().toString() : null)
                        .xpReward(r.getXpReward())
                        .cookCount(r.getCookCount())
                        .masteredByCount(r.getMasteredByCount())
                        .averageRating(r.getAverageRating())
                        .creatorXpEarned(r.getCreatorXpEarned())
                        .likeCount(r.getLikeCount())
                        .saveCount(r.getSaveCount())
                        .viewCount(r.getViewCount())
                        .build())
                .sorted((a, b) -> Long.compare(b.getCookCount(), a.getCookCount()))
                .toList();

        long totalCooks = recipes.stream().mapToLong(Recipe::getCookCount).sum();
        long totalViews = recipes.stream().mapToLong(Recipe::getViewCount).sum();
        long totalLikes = recipes.stream().mapToLong(Recipe::getLikeCount).sum();
        double avgRating = recipes.stream()
                .filter(r -> r.getAverageRating() != null && r.getAverageRating() > 0)
                .mapToDouble(Recipe::getAverageRating)
                .average().orElse(0.0);

        return CreatorPerformanceResponse.builder()
                .recipes(items)
                .summary(CreatorPerformanceResponse.CreatorSummary.builder()
                        .totalRecipes(recipes.size())
                        .totalCooks(totalCooks)
                        .totalViews(totalViews)
                        .totalLikes(totalLikes)
                        .averageRating(Math.round(avgRating * 10.0) / 10.0)
                        .build())
                .build();
    }

    /**
     * Who recently cooked the creator's recipes.
     * Returns paginated cooking sessions with cooker profile info.
     */
    @Transactional(readOnly = true)
    public RecentCookResponse getRecentCooksOfMyRecipes(int page, int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Recipe> myRecipes = recipeRepository.findByUserIdAndStatus(userId, RecipeStatus.PUBLISHED);

        if (myRecipes.isEmpty()) {
            return RecentCookResponse.builder().cooks(List.of()).totalCount(0).build();
        }

        List<String> recipeIds = myRecipes.stream().map(Recipe::getId).toList();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));

        Page<CookingSession> sessionsPage = cookingSessionRepository
                .findByRecipeIdInAndStatus(recipeIds, SessionStatus.COMPLETED, pageable);

        // Resolve cooker profiles — batch-friendly approach
        List<RecentCookResponse.RecentCookItem> cooks = sessionsPage.getContent().stream()
                .filter(s -> !userId.equals(s.getUserId())) // Exclude self-cooks
                .map(session -> {
                    RecentCookResponse.RecentCookItem.RecentCookItemBuilder item =
                            RecentCookResponse.RecentCookItem.builder()
                                    .sessionId(session.getId())
                                    .recipeId(session.getRecipeId())
                                    .recipeTitle(session.getRecipeTitle())
                                    .coverImageUrl(session.getCoverImageUrl())
                                    .cookUserId(session.getUserId())
                                    .completedAt(session.getCompletedAt())
                                    .rating(session.getRating());

                    // Resolve cooker profile (fail-safe)
                    try {
                        BasicProfileInfo cookerProfile = profileProvider.getBasicProfile(session.getUserId());
                        if (cookerProfile != null) {
                            item.cookDisplayName(cookerProfile.getDisplayName())
                                .cookAvatarUrl(cookerProfile.getAvatarUrl())
                                .cookUsername(cookerProfile.getUsername());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to resolve profile for cooker {}: {}", session.getUserId(), e.getMessage());
                        item.cookDisplayName("Chef");
                    }

                    return item.build();
                })
                .toList();

        return RecentCookResponse.builder()
                .cooks(cooks)
                .totalCount(sessionsPage.getTotalElements())
                .build();
    }

    // ===============================================
    // STEP HEATMAP (Wave 4 — Creator unique value)
    // ===============================================

    /**
     * Step-level analytics for a recipe: completion rate, skip rate, avg time, struggle points.
     * Only the recipe owner can access this (creator-only endpoint).
     * Aggregates data from all terminal sessions (COMPLETED, POSTED, ABANDONED).
     */
    @Transactional(readOnly = true)
    public StepHeatmapResponse getStepHeatmap(String recipeId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (!recipe.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        int totalSteps = recipe.getSteps().size();
        if (totalSteps == 0) {
            return StepHeatmapResponse.builder()
                    .recipeId(recipeId)
                    .recipeTitle(recipe.getTitle())
                    .totalSessions(0)
                    .steps(List.of())
                    .build();
        }

        List<CookingSession> sessions = cookingSessionRepository.findByRecipeIdAndStatusIn(
                recipeId, List.of(SessionStatus.COMPLETED, SessionStatus.POSTED, SessionStatus.ABANDONED));

        int totalSessions = sessions.size();
        if (totalSessions == 0) {
            // Return step structure with zero data
            List<StepHeatmapResponse.StepAnalytics> emptySteps = recipe.getSteps().stream()
                    .map(step -> StepHeatmapResponse.StepAnalytics.builder()
                            .stepNumber(step.getStepNumber())
                            .title(step.getTitle() != null ? step.getTitle() : "Step " + step.getStepNumber())
                            .completionRate(0)
                            .skipRate(0)
                            .avgTimeSeconds(null)
                            .estimatedTimeSeconds(step.getTimerSeconds())
                            .strugglePoint(false)
                            .abandonedAtCount(0)
                            .build())
                    .toList();

            return StepHeatmapResponse.builder()
                    .recipeId(recipeId)
                    .recipeTitle(recipe.getTitle())
                    .totalSessions(0)
                    .steps(emptySteps)
                    .build();
        }

        // Per-step counters
        int[] completionCount = new int[totalSteps + 1]; // 1-indexed
        int[] skipCount = new int[totalSteps + 1];
        long[] totalTimeMs = new long[totalSteps + 1];
        int[] timeEntryCount = new int[totalSteps + 1];
        int[] abandonedAt = new int[totalSteps + 1];

        for (CookingSession session : sessions) {
            // Track step completions
            if (session.getCompletedSteps() != null) {
                for (Integer stepNum : session.getCompletedSteps()) {
                    if (stepNum >= 1 && stepNum <= totalSteps) {
                        completionCount[stepNum]++;
                    }
                }
            }

            // Track timer events (skip/complete + timing)
            if (session.getTimerEvents() != null) {
                // Group timer events by step to compute duration per step
                Map<Integer, List<CookingSession.TimerEvent>> eventsByStep = session.getTimerEvents().stream()
                        .filter(e -> e.getStepNumber() != null && e.getStepNumber() >= 1 && e.getStepNumber() <= totalSteps)
                        .collect(Collectors.groupingBy(CookingSession.TimerEvent::getStepNumber));

                for (var entry : eventsByStep.entrySet()) {
                    int stepNum = entry.getKey();
                    List<CookingSession.TimerEvent> events = entry.getValue();

                    CookingSession.TimerEvent startEvent = events.stream()
                            .filter(e -> e.getEvent() == TimerEventType.START)
                            .findFirst().orElse(null);
                    CookingSession.TimerEvent endEvent = events.stream()
                            .filter(e -> e.getEvent() == TimerEventType.COMPLETE || e.getEvent() == TimerEventType.SKIP)
                            .findFirst().orElse(null);

                    if (endEvent != null && endEvent.getEvent() == TimerEventType.SKIP) {
                        skipCount[stepNum]++;
                    }

                    if (startEvent != null && endEvent != null
                            && startEvent.getServerTimestamp() != null && endEvent.getServerTimestamp() != null) {
                        long durationMs = java.time.Duration.between(
                                startEvent.getServerTimestamp(), endEvent.getServerTimestamp()).toMillis();
                        if (durationMs > 0 && durationMs < 7200000) { // Cap at 2 hours to exclude outliers
                            totalTimeMs[stepNum] += durationMs;
                            timeEntryCount[stepNum]++;
                        }
                    }
                }
            }

            // Track where sessions were abandoned
            if (session.getStatus() == SessionStatus.ABANDONED && session.getCurrentStep() != null) {
                int step = session.getCurrentStep();
                if (step >= 1 && step <= totalSteps) {
                    abandonedAt[step]++;
                }
            }
        }

        // Build analytics per step
        List<StepHeatmapResponse.StepAnalytics> stepAnalytics = recipe.getSteps().stream()
                .map(step -> {
                    int sn = step.getStepNumber();
                    double compRate = totalSessions > 0 ? (completionCount[sn] * 100.0 / totalSessions) : 0;
                    double skRate = totalSessions > 0 ? (skipCount[sn] * 100.0 / totalSessions) : 0;
                    Double avgTime = timeEntryCount[sn] > 0
                            ? (totalTimeMs[sn] / (double) timeEntryCount[sn]) / 1000.0
                            : null;

                    // Struggle point: skip rate > 30% OR completion rate < 60% (with meaningful data)
                    boolean struggle = totalSessions >= MIN_SESSIONS_FOR_STRUGGLE
                            && (skRate > SKIP_RATE_THRESHOLD || compRate < COMPLETION_RATE_THRESHOLD);

                    return StepHeatmapResponse.StepAnalytics.builder()
                            .stepNumber(sn)
                            .title(step.getTitle() != null ? step.getTitle() : "Step " + sn)
                            .completionRate(Math.round(compRate * 10.0) / 10.0)
                            .skipRate(Math.round(skRate * 10.0) / 10.0)
                            .avgTimeSeconds(avgTime != null ? Math.round(avgTime * 10.0) / 10.0 : null)
                            .estimatedTimeSeconds(step.getTimerSeconds())
                            .strugglePoint(struggle)
                            .abandonedAtCount(abandonedAt[sn])
                            .build();
                })
                .toList();

        return StepHeatmapResponse.builder()
                .recipeId(recipeId)
                .recipeTitle(recipe.getTitle())
                .totalSessions(totalSessions)
                .steps(stepAnalytics)
                .build();
    }

    // ===============================================
    // SOCIAL PROOF (spec: 09-posts.txt / vision)
    // ===============================================

    /**
     * Community validation for a recipe: cook count, rating, recent cookers, post count.
     * Powers the "12 people made this" widget on recipe detail page.
     */
    @Transactional(readOnly = true)
    public RecipeSocialProofResponse getRecipeSocialProof(String recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // Post count = sessions that successfully linked a post
        long postCount = cookingSessionRepository.countByRecipeIdAndStatus(recipeId, SessionStatus.POSTED);

        // Recent cookers (last 5 who completed or posted)
        Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "completedAt"));
        Page<CookingSession> recentSessions = cookingSessionRepository
                .findByRecipeIdAndStatusIn(recipeId, List.of(SessionStatus.COMPLETED, SessionStatus.POSTED), top5);

        List<RecipeSocialProofResponse.RecentCooker> recentCookers = recentSessions.getContent().stream()
                .map(session -> {
                    RecipeSocialProofResponse.RecentCooker.RecentCookerBuilder cooker =
                            RecipeSocialProofResponse.RecentCooker.builder()
                                    .userId(session.getUserId())
                                    .completedAt(session.getCompletedAt());
                    try {
                        BasicProfileInfo profile = profileProvider.getBasicProfile(session.getUserId());
                        if (profile != null) {
                            cooker.username(profile.getUsername())
                                  .displayName(profile.getDisplayName())
                                  .avatarUrl(profile.getAvatarUrl());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to resolve profile for cooker {}: {}", session.getUserId(), e.getMessage());
                        cooker.displayName("Chef");
                    }
                    return cooker.build();
                })
                .toList();

        return RecipeSocialProofResponse.builder()
                .cookCount(recipe.getCookCount())
                .postCount(postCount)
                .averageRating(recipe.getAverageRating())
                .recentCookers(recentCookers)
                .build();
    }
}