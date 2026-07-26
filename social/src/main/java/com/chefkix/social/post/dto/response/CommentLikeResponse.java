package com.chefkix.social.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentLikeResponse {
    @JsonProperty("isLiked")
    private boolean isLiked;
    private int likes;
}
