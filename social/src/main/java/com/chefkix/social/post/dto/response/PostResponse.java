package com.chefkix.social.post.dto.response;

import com.chefkix.social.post.entity.CoChef;
import com.chefkix.social.post.entity.PollData;
import com.chefkix.social.post.enums.PostType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class PostResponse {
    String id;
    String userId;
    String groupId;
String displayName;
String avatarUrl;
    @JsonProperty("isVerified")
    boolean verified;
    String content;
    String slug;
    List<String> photoUrls;
    String videoUrl;
    String postUrl;
    List<String> tags;

    PostType postType;

String sessionId;
String recipeId;
String recipeTitle;
    @JsonProperty("isPrivateRecipe")
boolean isPrivateRecipe;
Double xpEarned;

String roomCode;
List<CoChef> coChefs;

List<String> taggedUserIds;

    Integer likes;
    Integer commentCount;
    
    @JsonProperty("isLiked")
Boolean isLiked;
    @JsonProperty("isSaved")
Boolean isSaved;

    Instant createdAt;
    Instant updatedAt;

    PollData pollData;
String userVote;

Integer reviewRating;

    String battleRecipeIdA;
    String battleRecipeIdB;
    String battleRecipeTitleA;
    String battleRecipeTitleB;
    String battleRecipeImageA;
    String battleRecipeImageB;
    Integer battleVotesA;
    Integer battleVotesB;
    Instant battleEndsAt;
String userBattleVote;

    Integer fireCount;
    Integer cringeCount;
String userPlateRating;
}