package com.chefkix.social.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentLikeResponse {
    @JsonProperty("isLiked")
    private boolean isLiked;
    /** Number of likes on the comment (field name matches CommentResponse.likes) */
    private int likes;
}
