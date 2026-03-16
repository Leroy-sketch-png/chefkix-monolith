package com.chefkix.social.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Admin request to manually ban a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BanUserRequest {
    @NotBlank(message = "reason is required")
    @Size(max = 1000, message = "reason must be at most 1000 characters")
    String reason;

    /**
     * Scope: "post", "comment", "all"
     */
    @NotBlank(message = "scope is required")
    @Pattern(regexp = "post|comment|all", message = "scope must be one of: post, comment, all")
    String scope;
}
