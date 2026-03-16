package com.chefkix.social.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Admin review decision for an appeal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewAppealRequest {
    /**
     * Decision: "approved" or "rejected"
     */
    @NotBlank(message = "decision is required")
    @Pattern(regexp = "approved|rejected", message = "decision must be one of: approved, rejected")
    String decision;

    @Size(max = 1000, message = "notes must be at most 1000 characters")
    String notes;
}
