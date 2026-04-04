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
    String displayName; // Display name of the post author
    String avatarUrl;   // Avatar of the post author
    @JsonProperty("isVerified")
    boolean verified;
    String content;
    String slug;
    List<String> photoUrls;
    String videoUrl;
    String postUrl;
    List<String> tags;

    PostType postType;

    // --- NEW FIELDS: GAMIFICATION & SESSION INFO ---
    String sessionId;       // For FE to link back to cooking session detail page
    String recipeId;        // For FE to link to the original recipe page
    String recipeTitle;     // For FE to display header: "Successfully cooked: [Recipe Name]"
    @JsonProperty("isPrivateRecipe")
    boolean isPrivateRecipe; // If true -> FE hides the "View Recipe" button
    Double xpEarned;        // For FE to run congratulation animation "+180 XP"

    // --- CO-COOKING ATTRIBUTION ---
    String roomCode;                // Co-cooking room this post originated from
    List<CoChef> coChefs;           // Co-chefs who cooked together (userId, displayName, avatarUrl)

    List<String> taggedUserIds;      // @mentioned users in this post

    Integer likes;
    Integer commentCount;
    
    // --- USER-SPECIFIC FIELDS: Like/Save status for current user ---
    @JsonProperty("isLiked")
    Boolean isLiked;        // For FE to know if user has liked this post
    @JsonProperty("isSaved")
    Boolean isSaved;        // For FE to know if user has saved this post

    Instant createdAt;
    Instant updatedAt;

    // Poll data (present when postType == POLL)
    PollData pollData;
    String userVote; // "A", "B", or null (current user's vote)

    // Recipe Review data (present when postType == RECIPE_REVIEW)
    Integer reviewRating; // 1-5 star rating for the recipe

    // Recipe Battle data (present when postType == RECIPE_BATTLE)
    String battleRecipeIdA;
    String battleRecipeIdB;
    String battleRecipeTitleA;
    String battleRecipeTitleB;
    String battleRecipeImageA;
    String battleRecipeImageB;
    Integer battleVotesA;
    Integer battleVotesB;
    Instant battleEndsAt;
    String userBattleVote; // "A", "B", or null (current user's vote)

    // Rate This Plate data (for posts with photos)
    Integer fireCount;
    Integer cringeCount;
    String userPlateRating; // "FIRE", "CRINGE", or null
}