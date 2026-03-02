package com.chefkix.social.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đơn giản chứa thông tin người dùng được @tag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaggedUserInfo {
    private String userId;
    private String displayName;
}