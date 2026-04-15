package com.chefkix.culinary.features.recipe.mapper;

import com.chefkix.culinary.features.recipe.dto.request.StepRequest;
import com.chefkix.culinary.features.recipe.dto.response.StepResponse;
import com.chefkix.culinary.features.recipe.entity.Step; // Entity import
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {IngredientMapper.class})
public interface StepMapper {
    Step toStep(StepRequest request);
    StepResponse toStepResponse(Step step);
}
