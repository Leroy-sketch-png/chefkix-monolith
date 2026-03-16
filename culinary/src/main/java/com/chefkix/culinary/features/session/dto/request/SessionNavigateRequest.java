package com.chefkix.culinary.features.session.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SessionNavigateRequest {
    @NotBlank(message = "action is required")
    @Pattern(regexp = "next|previous|goto", message = "action must be one of: next, previous, goto")
    private String action; // "next" | "previous" | "goto"

    @Min(value = 1, message = "targetStep must be at least 1")
    private Integer targetStep;
}