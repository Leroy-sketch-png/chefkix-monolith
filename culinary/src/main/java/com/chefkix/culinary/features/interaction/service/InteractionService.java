package com.chefkix.culinary.features.interaction.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.features.interaction.dto.response.RecipeLikeResponse;
import com.chefkix.culinary.features.interaction.dto.response.RecipeSaveResponse;
import com.chefkix.culinary.features.interaction.entity.RecipeLike;
import com.chefkix.culinary.features.interaction.entity.RecipeSave;
import com.chefkix.culinary.features.interaction.repository.RecipeLikeRepository;
import com.chefkix.culinary.features.interaction.repository.RecipeSaveRepository;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InteractionService {

    RecipeLikeRepository recipeLikeRepository;
    RecipeSaveRepository recipeSaveRepository;
    RecipeRepository recipeRepository; // Needed to update count and fetch recipe detail
    RecipeMapper recipeMapper;

    @Transactional
    public RecipeLikeResponse toggleLike(String recipeId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<RecipeLike> existingLike = recipeLikeRepository.findByRecipeIdAndUserId(recipeId, userId);
        boolean isLiked;
        int changeAmount;

        if (existingLike.isPresent()) {
            recipeLikeRepository.delete(existingLike.get());
            changeAmount = -1;
            isLiked = false;
        } else {
            recipeLikeRepository.save(RecipeLike.builder().recipeId(recipeId).userId(userId).build());
            changeAmount = 1;
            isLiked = true;
        }

        Recipe updatedRecipe = recipeRepository.updateLikeCount(recipeId, changeAmount);
        if (updatedRecipe == null) throw new AppException(ErrorCode.RECIPE_NOT_FOUND);

        return RecipeLikeResponse.builder()
                .id(recipeId)
                .likeCount(updatedRecipe.getLikeCount())
                .isLiked(isLiked)
                .build();
    }

    @Transactional
    public RecipeSaveResponse toggleSave(String recipeId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<RecipeSave> existingSave = recipeSaveRepository.findByRecipeIdAndUserId(recipeId, userId);
        boolean isSaved;
        int changeAmount;

        if (existingSave.isPresent()) {
            recipeSaveRepository.delete(existingSave.get());
            changeAmount = -1;
            isSaved = false;
        } else {
            recipeSaveRepository.save(RecipeSave.builder().recipeId(recipeId).userId(userId).build());
            changeAmount = 1;
            isSaved = true;
        }

        Recipe updatedRecipe = recipeRepository.updateSaveCount(recipeId, changeAmount);
        if (updatedRecipe == null) throw new AppException(ErrorCode.RECIPE_NOT_FOUND);

        return RecipeSaveResponse.builder()
                .id(recipeId)
                .saveCount(updatedRecipe.getSaveCount())
                .isSaved(isSaved)
                .build();
    }

    public Page<RecipeSummaryResponse> getSavedRecipes(int page, int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<RecipeSave> savesPage = recipeSaveRepository.findAllByUserId(userId, pageable);
        List<String> recipeIds = savesPage.getContent().stream().map(RecipeSave::getRecipeId).toList();

        if (recipeIds.isEmpty()) return Page.empty(pageable);

        List<Recipe> recipes = recipeRepository.findAllByIdIn(recipeIds);
        Map<String, Recipe> recipeMap = recipes.stream().collect(Collectors.toMap(Recipe::getId, r -> r));

        List<RecipeSummaryResponse> responses = recipeIds.stream()
                .map(recipeMap::get)
                .filter(Objects::nonNull)
                .map(recipe -> {
                    RecipeSummaryResponse response = recipeMapper.toRecipeSummaryResponse(recipe);
                    response.setIsSaved(true);
                    response.setIsLiked(recipeLikeRepository.existsByRecipeIdAndUserId(recipe.getId(), userId));
                    return response;
                })
                .toList();

        return new org.springframework.data.domain.PageImpl<>(responses, pageable, savesPage.getTotalElements());
    }

    public Page<RecipeSummaryResponse> getLikedRecipes(int page, int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<RecipeLike> likesPage = recipeLikeRepository.findAllByUserId(userId, pageable);
        List<String> recipeIds = likesPage.getContent().stream().map(RecipeLike::getRecipeId).toList();

        if (recipeIds.isEmpty()) return Page.empty(pageable);

        List<Recipe> recipes = recipeRepository.findAllByIdIn(recipeIds);
        Map<String, Recipe> recipeMap = recipes.stream().collect(Collectors.toMap(Recipe::getId, r -> r));

        List<RecipeSummaryResponse> responses = recipeIds.stream()
                .map(recipeMap::get)
                .filter(Objects::nonNull)
                .map(recipe -> {
                    RecipeSummaryResponse response = recipeMapper.toRecipeSummaryResponse(recipe);
                    response.setIsLiked(true);
                    response.setIsSaved(recipeSaveRepository.existsByRecipeIdAndUserId(recipe.getId(), userId));
                    return response;
                })
                .toList();

        return new org.springframework.data.domain.PageImpl<>(responses, pageable, likesPage.getTotalElements());
    }

    // --- HELPER METHODS FOR RECIPE SERVICE ---

    public boolean isLiked(String recipeId, String userId) {
        return recipeLikeRepository.existsByRecipeIdAndUserId(recipeId, userId);
    }

    public boolean isSaved(String recipeId, String userId) {
        return recipeSaveRepository.existsByRecipeIdAndUserId(recipeId, userId);
    }
}