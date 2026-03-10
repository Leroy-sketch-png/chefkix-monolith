package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents a co-cooking participant who cooked together with the post author.
 * Embedded in Post document for co-attribution display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoChef {
    String userId;
    String displayName;
    String avatarUrl;
}
