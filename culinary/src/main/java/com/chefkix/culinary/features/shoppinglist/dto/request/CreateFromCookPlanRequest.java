package com.chefkix.culinary.features.shoppinglist.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFromCookPlanRequest {
    @NotBlank
    private String cookPlanId;
}
