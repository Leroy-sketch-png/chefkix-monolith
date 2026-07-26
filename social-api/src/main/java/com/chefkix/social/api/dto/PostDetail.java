package com.chefkix.social.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostDetail {

    String id;

    String displayName;

    String content;

    List<String> photoUrls;


    String recipeTitle;

    String recipeId;

    String sessionId;

    boolean privateRecipe;
}
