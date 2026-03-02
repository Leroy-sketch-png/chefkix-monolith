package com.chefkix.culinary.features.recipe.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationMetadata {
    boolean xpValidated;           // Đã qua kiểm duyệt AI chưa?
    double validationConfidence;   // Độ tin cậy (0.0 - 1.0)

    @Builder.Default
    List<String> validationIssues = new ArrayList<>(); // Danh sách lỗi (Vd: "Time too short")

    boolean xpAdjusted;            // Có bị trừ điểm do vi phạm không?
}