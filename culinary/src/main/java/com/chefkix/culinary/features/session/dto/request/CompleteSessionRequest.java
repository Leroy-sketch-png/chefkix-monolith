package com.chefkix.culinary.features.session.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteSessionRequest {
    @Min(value = 1, message = "rating must be at least 1")
    @Max(value = 5, message = "rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "notes must be at most 1000 characters")
    private String notes;
}