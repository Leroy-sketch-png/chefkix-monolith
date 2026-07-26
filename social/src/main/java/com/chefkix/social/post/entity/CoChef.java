package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
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
