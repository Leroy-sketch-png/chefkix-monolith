package com.chefkix.social.post.entity;

import com.chefkix.shared.util.SlugUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "post")
@CompoundIndex(def = "{'userId': 1, 'createdAt': -1}", name = "idx_userId_createdAt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
  @Id String id;
  String userId;
  List<String> tags;
  String displayName;
  String content;
  String avatarUrl;
  List<String> photoUrls;
  String videoUrl;
  String slug;
  String postUrl;
  @Indexed String sessionId; // Liên kết với Cooking Session
  @Indexed String recipeId;  // ID công thức đã nấu

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
  List<String> commentIds;

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
