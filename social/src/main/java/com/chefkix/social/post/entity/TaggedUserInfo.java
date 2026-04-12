package com.chefkix.social.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO containing information about a @tagged user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaggedUserInfo {
    private String userId;
    private String displayName;
}