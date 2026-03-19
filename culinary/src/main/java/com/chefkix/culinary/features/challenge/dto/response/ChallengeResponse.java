package com.chefkix.culinary.features.challenge.dto.response;

import com.chefkix.culinary.common.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {

    // Thông tin cơ bản của Challenge (Lấy từ Pool)
    private String id;
    private String title;
    private String description;
    private String icon;
    private int bonusXp;

    // Metadata giúp Frontend hiển thị điều kiện (VD: cuisineType: ["Italian"])
    private Map<String, Object> criteria;

    // Thời gian hết hạn (Thường là cuối ngày UTC)
    private String endsAt;

    // Trạng thái của User đối với Challenge này
    private boolean completed;

    // Thời điểm hoàn thành (null nếu chưa xong)
    private String completedAt;

    // Danh sách món ăn gợi ý để user nấu
    private List<RecipePreviewDto> matchingRecipes;

    // ==========================================================
    // Inner DTO: Tóm tắt món ăn (Để tránh trả về full Recipe nặng nề)
    // Bạn có thể tách ra file riêng nếu muốn tái sử dụng
    // ==========================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipePreviewDto {
        private String id;
        private String title;
        private int xpReward; // XP gốc của món ăn
        private List<String> coverImageUrl;
        private int totalTime; // phút
        private Difficulty difficulty;
    }
}