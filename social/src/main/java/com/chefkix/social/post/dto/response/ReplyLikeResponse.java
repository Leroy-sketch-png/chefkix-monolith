package com.chefkix.social.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplyLikeResponse {
    @JsonProperty("isLiked")
    boolean isLiked;
    
    /** Number of likes on the reply (renamed from likeCount for consistency with CommentLikeResponse) */
    long likes;
}
