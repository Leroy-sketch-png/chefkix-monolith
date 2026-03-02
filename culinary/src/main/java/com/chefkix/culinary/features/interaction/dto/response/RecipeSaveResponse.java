package com.chefkix.culinary.features.interaction.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeSaveResponse {
    String id;
    long saveCount;
    @JsonProperty("isSaved")
    boolean isSaved;
}