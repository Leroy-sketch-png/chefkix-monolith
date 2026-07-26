package com.chefkix.social.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostSummary {

    String id;

    String userId;

    String displayName;

    String avatarUrl;

    String content;

    String slug;

    List<String> photoUrls;

    String videoUrl;

    String postUrl;

    List<String> tags;


    String sessionId;

    String recipeId;

    String recipeTitle;

    @JsonProperty("isPrivateRecipe")
    boolean privateRecipe;

    Double xpEarned;


    Integer likes;

    Integer commentCount;


    @JsonProperty("isLiked")
    Boolean liked;

    @JsonProperty("isSaved")
    Boolean saved;


    Instant createdAt;

    Instant updatedAt;
}
