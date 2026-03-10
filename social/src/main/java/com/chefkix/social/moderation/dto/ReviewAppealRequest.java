package com.chefkix.social.moderation.dto;

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
    String decision;

    String notes;
}
