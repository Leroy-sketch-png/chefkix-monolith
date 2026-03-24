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
        // 1. For lighting-fast Personal Profile feeds
        @CompoundIndex(def = "{'userId': 1, 'createdAt': -1}", name = "idx_userId_createdAt"),
        // 2. For lightning-fast Group feeds
        @CompoundIndex(def = "{'groupId': 1, 'createdAt': -1}", name = "idx_groupId_createdAt")
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
  @Indexed String sessionId; // Liên kết với Cooking Session
  @Indexed String recipeId;  // ID công thức đã nấu

  @TextIndexed(weight = 7)
  String recipeTitle; // Tên món ăn (VD: "Phở Bò")
  @Builder.Default boolean isPrivateRecipe = false; // Cờ đánh dấu công thức riêng tư
  double xpEarned; // Số XP nhận được từ bài post này

  // Co-cooking attribution (Stream 4)
  String roomCode; // Room code if cooked in co-cooking session
  List<CoChef> coChefs; // Other participants who cooked together

  @Builder.Default boolean hidden = false; // Auto-hidden when report threshold reached

  @Builder.Default Integer likes = 0;
  @Builder.Default Integer commentCount = 0;
  @Builder.Default Double hotScore = 0.0;
  @CreatedDate Instant createdAt;
  @LastModifiedDate Instant updatedAt;

  // List<String> taggedUserIds;
  List<String> taggedUserIds;
  List<String> commentIds;


  // this is for group posts
  PostType postType; // Enum: PERSONAL, GROUP, QUICK, POLL
    @Indexed
    String groupId;
    @Builder.Default
    PostStatus status = PostStatus.ACTIVE;

  // Poll data (only present when postType == POLL)
  PollData pollData;

  // Rate This Plate data (for posts with photos)
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
