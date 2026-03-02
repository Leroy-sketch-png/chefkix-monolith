package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.identity.api.dto.CompletionResult;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletionResponse {

    String completionId;
    String recipeId;

    int xpEarned;          // Số XP thực nhận (đã tính toán 50% hay 100%)
    List<String> newBadges; // Danh sách badge mới nhận được (nếu có)
    CompletionResult userProfile; // Thông tin profile cập nhật để update UI

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @FieldDefaults(level = AccessLevel.PRIVATE)
//    public static class UserProfileSummary {
//        String userId;
//
//        Integer currentLevel;
//        Integer currentXP;
//        Integer currentXPGoal;
//
//        int completionCount;
//    }
}