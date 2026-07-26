package com.chefkix.social.post.entity;

import com.chefkix.shared.util.SlugUtils;
import com.chefkix.social.post.enums.PostStatus;
import com.chefkix.social.post.enums.PostType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "post")
@CompoundIndexes({
        @CompoundIndex(def = "{'userId': 1, 'createdAt': -1}", name = "idx_userId_createdAt"),
        @CompoundIndex(def = "{'groupId': 1, 'createdAt': -1}", name = "idx_groupId_createdAt"),
        @CompoundIndex(def = "{'hidden': 1, 'hotScore': -1}", name = "idx_hidden_hotScore"),
        @CompoundIndex(def = "{'hidden': 1, 'createdAt': -1}", name = "idx_hidden_createdAt"),
        @CompoundIndex(def = "{'recipeId': 1, 'postType': 1, 'createdAt': -1}", name = "idx_recipeId_postType_createdAt"),
        @CompoundIndex(def = "{'postType': 1, 'battleEndsAt': 1}", name = "idx_postType_battleEndsAt"),
        @CompoundIndex(def = "{'userId': 1, 'hidden': 1, 'createdAt': -1}", name = "idx_user_hidden_createdAt"),
        @CompoundIndex(def = "{'userId': 1, 'hidden': 1, 'postType': 1, 'createdAt': -1}", name = "idx_user_hidden_postType_createdAt")
})@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
  @Id String id;
  String userId;
  @TextIndexed(weight = 3)
  List<String> tags;
  String displayName;
  @TextIndexed(weight = 5)
  String content;
  String avatarUrl;
  boolean verified;
  List<String> photoUrls;
  String videoUrl;
  String slug;
  String postUrl;
@Indexed String sessionId;
@Indexed String recipeId;

  @TextIndexed(weight = 7)
String recipeTitle;
@Builder.Default boolean isPrivateRecipe = false;
double xpEarned;

String roomCode;
List<CoChef> coChefs;

@Builder.Default boolean hidden = false;

  @Builder.Default Integer likes = 0;
  @Builder.Default Integer commentCount = 0;
  @Builder.Default Double hotScore = 0.0;
  @CreatedDate Instant createdAt;
  @LastModifiedDate Instant updatedAt;

  List<String> taggedUserIds;
  List<String> commentIds;


PostType postType;
    @Indexed
    String groupId;
    @Builder.Default
    PostStatus status = PostStatus.ACTIVE;

  PollData pollData;

Integer reviewRating;

String battleRecipeIdA;
String battleRecipeIdB;
  String battleRecipeTitleA;
  String battleRecipeTitleB;
String battleRecipeImageA;
String battleRecipeImageB;
  @Builder.Default Integer battleVotesA = 0;
  @Builder.Default Integer battleVotesB = 0;
Instant battleEndsAt;

  @Builder.Default Integer fireCount = 0;
  @Builder.Default Integer cringeCount = 0;

  public void generateSlug() {
    if (this.content != null) {
      this.slug =
          SlugUtils.toSlug(
              this.content.length() > 50 ? this.content.substring(0, 50) : this.content);
    } else {
      this.slug = SlugUtils.toSlug(this.id);
    }
  }
}
