package com.chefkix.social.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Appeal request from a banned user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealRequest {
    @NotBlank(message = "banId is required")
    String banId;

    @NotBlank(message = "reason is required")
    @Size(max = 1000, message = "reason must be at most 1000 characters")
    String reason;

    @Size(max = 5, message = "evidenceUrls must contain at most 5 items")
    List<String> evidenceUrls;
}
