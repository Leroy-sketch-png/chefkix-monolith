package com.chefkix.culinary.features.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PhotoSubstitutionRequest(
        @NotBlank @Size(max = 80) String ingredientToReplace,
        @Size(max = 120) String dishName,
        @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 80) String> dishIngredients,
        @Size(max = 10) List<@NotBlank @Size(max = 80) String> allergenConstraints,
        @Size(max = 8) List<@NotBlank @Size(max = 80) String> dietaryConstraints) {
}
