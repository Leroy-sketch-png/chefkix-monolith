package com.chefkix.culinary.features.recipe.mapper;

import com.chefkix.culinary.features.recipe.dto.request.IngredientRequest;
import com.chefkix.culinary.features.recipe.dto.response.IngredientResponse;
import com.chefkix.culinary.features.recipe.entity.Ingredient; // Giả sử entity của bạn ở đây
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IngredientMapper {
    Ingredient toIngredient(IngredientRequest request);

    IngredientResponse toIngredientResponse(Ingredient ingredient);
}