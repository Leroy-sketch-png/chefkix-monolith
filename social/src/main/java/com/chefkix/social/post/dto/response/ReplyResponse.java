package com.chefkix.social.post.dto.response;

import com.chefkix.social.post.entity.TaggedUserInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyResponse {

    private String id;
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String content;
    @Builder.Default
    private Integer likes = 0;
    
    /** Whether the current user has liked this reply */
    @JsonProperty("isLiked")
    @Builder.Default
    private Boolean isLiked = false;
    
    private Instant createdAt;
    private Instant updatedAt;

    private List<TaggedUserInfo> taggedUsers;

    private String parentCommentId;
}