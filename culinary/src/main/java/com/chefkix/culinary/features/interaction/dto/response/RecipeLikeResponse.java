package com.chefkix.culinary.features.interaction.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeLikeResponse {
    String id;
    long likeCount;
    @JsonProperty("isLiked")
    boolean isLiked;
}