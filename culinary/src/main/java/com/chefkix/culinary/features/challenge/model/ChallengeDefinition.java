package com.chefkix.culinary.features.challenge.model;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Class định nghĩa cấu trúc của một thử thách (Challenge).
 * Class này chỉ tồn tại trong RAM (Memory) để phục vụ logic, không lưu vào DB.
 */
@Data
@Builder
public class ChallengeDefinition {

    // ID định danh (VD: "noodle-day", "quick-meal-1")
    // Dùng để lưu vào log lịch sử khi user hoàn thành
    private String id;

    // Tên hiển thị (VD: "Noodle Day 🍜")
    private String title;

    // Mô tả ngắn gọn
    private String description;

    // Số XP thưởng thêm
    private int bonusXp;

    // Metadata dành cho Frontend hiển thị UI
    // Ví dụ: Frontend cần biết tag nào để highlight, hoặc icon gì
    // VD: { "cuisine": "Italian", "icon": "🍝", "color": "#FF5733" }
    private Map<String, Object> criteriaMetadata;

    // LOGIC CỐT LÕI (Functional Interface)
    // Đây là hàm kiểm tra xem món ăn (Recipe) có đạt yêu cầu không.
    // Input: Recipe -> Output: true/false
    private Predicate<Recipe> validationLogic;

    /**
     * Helper method để kiểm tra nhanh.
     * @param recipe Món ăn user vừa nấu
     * @return true nếu hoàn thành thử thách
     */
    public boolean isSatisfiedBy(Recipe recipe) {
        if (validationLogic == null) return false;
        return validationLogic.test(recipe);
    }
}