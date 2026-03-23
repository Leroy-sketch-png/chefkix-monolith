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
    String displayName; // Tên hiển thị của người post
    String avatarUrl;   // Avatar của người post
    String content;
    String slug;
    List<String> photoUrls;
    String videoUrl;
    String postUrl;
    List<String> tags;

    PostType postType;

    // --- NEW FIELDS: GAMIFICATION & SESSION INFO ---
    String sessionId;       // Để FE link ngược về trang chi tiết session nấu ăn
    String recipeId;        // Để FE dẫn link sang trang công thức gốc
    String recipeTitle;     // Để FE hiển thị header: "Đã nấu thành công món: [Tên Món]"
    @JsonProperty("isPrivateRecipe")
    boolean isPrivateRecipe; // Nếu true -> FE ẩn nút "Xem công thức"
    Double xpEarned;        // Để FE chạy animation chúc mừng "+180 XP"

    // --- CO-COOKING ATTRIBUTION ---
    String roomCode;                // Co-cooking room this post originated from
    List<CoChef> coChefs;           // Co-chefs who cooked together (userId, displayName, avatarUrl)

    List<String> taggedUserIds;      // @mentioned users in this post

    Integer likes;
    Integer commentCount;
    
    // --- USER-SPECIFIC FIELDS: Like/Save status for current user ---
    @JsonProperty("isLiked")
    Boolean isLiked;        // Để FE biết user đã like post này chưa
    @JsonProperty("isSaved")
    Boolean isSaved;        // Để FE biết user đã save post này chưa

    Instant createdAt;
    Instant updatedAt;

    // Poll data (present when postType == POLL)
    PollData pollData;
    String userVote; // "A", "B", or null (current user's vote)

    // Rate This Plate data (for posts with photos)
    Integer fireCount;
    Integer cringeCount;
    String userPlateRating; // "FIRE", "CRINGE", or null
}