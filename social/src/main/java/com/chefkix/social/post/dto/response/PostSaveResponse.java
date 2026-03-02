package com.chefkix.social.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostSaveResponse {
    @JsonProperty("isSaved")
    private boolean isSaved;
    private long saveCount;
}
