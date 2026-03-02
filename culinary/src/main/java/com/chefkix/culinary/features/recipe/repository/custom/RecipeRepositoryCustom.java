package com.chefkix.culinary.features.recipe.repository.custom;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecipeRepositoryCustom {

    /**
     * Tìm kiếm nâng cao với Dynamic Filter
     */
    Page<Recipe> searchRecipes(RecipeSearchQuery query, Pageable pageable);

    /**
     * Tăng viewCount bất đồng bộ (Fire & Forget)
     */
    void incrementViewCount(String recipeId);

    /**
     * Cập nhật số like/save an toàn (Atomic Update)
     * Trả về Recipe mới nhất để lấy số count hiển thị cho UI
     * @param amount: +1 hoặc -1
     */
    Recipe updateLikeCount(String recipeId, int amount);

    Recipe updateSaveCount(String recipeId, int amount);
}