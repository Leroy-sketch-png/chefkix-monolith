package com.chefkix.culinary.features.recipe.service;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.culinary.features.report.dto.internal.InternalCreatorInsightsResponse;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.culinary.features.interaction.service.InteractionService; // Import Service mới
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

    AsyncHelper asyncHelper;
    ProfileProvider profileProvider;
    RecipeMapper recipeMapper;
    RecipeRepository recipeRepository;
    RecipeHelper recipeHelper;
    InteractionService interactionService; // Inject Service mới

    @Transactional
    public RecipeDetailResponse createRecipe(RecipeRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(userId);

        Recipe recipe = recipeMapper.toRecipe(request);
        recipe.setUserId(userId);
        recipe.setStatus(RecipeStatus.PUBLISHED);

        recipe = recipeRepository.save(recipe);

        RecipeDetailResponse response = recipeMapper.toRecipeDetailResponse(recipe);
        response.setAuthor(authorFuture.join());
        response.setIsLiked(false);
        response.setIsSaved(false);

        return response;
    }

    @Transactional
    public RecipeDetailResponse updateRecipe(String recipeId, RecipeRequest request) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[RECIPE_UPDATE] User {} yêu cầu cập nhật recipe {}", currentUserId, recipeId);

        try {
            CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(currentUserId);

            Recipe existingRecipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

            if (!existingRecipe.getUserId().equals(currentUserId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            recipeMapper.updateRecipeFromRequest(existingRecipe, request);

            if (request.getIsPublished() != null) {
                existingRecipe.setStatus(RecipeStatus.PUBLISHED);
            }

            authorFuture.join();
            existingRecipe = recipeRepository.save(existingRecipe);

            RecipeDetailResponse response = recipeMapper.toRecipeDetailResponse(existingRecipe);
            response.setAuthor(authorFuture.get());

            // Logic cũ: Author tự like bài mình thì sao? -> Gọi InteractionService check cho chắc
            response.setIsLiked(interactionService.isLiked(recipeId, currentUserId));
            response.setIsSaved(interactionService.isSaved(recipeId, currentUserId));

            return response;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RECIPE_UPDATE] Lỗi", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi cập nhật recipe");
        }
    }

    // --- CÁC HÀM TOGGLE ĐÃ CHUYỂN SANG INTERACTION SERVICE ---

    public RecipeDetailResponse getRecipeById(String recipeId) {
        String viewerId = SecurityContextHolder.getContext().getAuthentication().getName();

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(recipe.getUserId())
                .exceptionally(ex -> {
                    log.error("[AUTHOR] Lỗi Profile Service: {}", ex.getMessage());
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
}