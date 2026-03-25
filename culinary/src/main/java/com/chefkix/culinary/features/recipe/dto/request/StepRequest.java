package com.chefkix.culinary.features.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepRequest {
    @Min(0) @Max(100)
    private int stepNumber;

    @Size(max = 200, message = "Step title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Step description cannot be blank")
    @Size(max = 10000, message = "Step description must be at most 10000 characters")
    private String description;

    @Size(max = 200, message = "Action must be at most 200 characters")
    private String action;

    @Valid
    @Size(max = 30, message = "Maximum 30 ingredients per step")
    private List<IngredientRequest> ingredients;

    @Min(0) @Max(86400)
    private Integer timerSeconds;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    private String imageUrl;

    @Size(max = 500, message = "Video URL must be at most 500 characters")
    private String videoUrl;

    @Size(max = 500, message = "Video thumbnail URL must be at most 500 characters")
    private String videoThumbnailUrl;

    @Min(0) @Max(86400)
    private Integer videoDurationSec;

    @Size(max = 2000, message = "Tips must be at most 2000 characters")
    private String tips;
}