package com.chefkix.social.moderation.dto;

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
    String reason;

    /**
     * Scope: "post", "comment", "all"
     */
    String scope;
}
