package com.chefkix.culinary.features.session.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// Annotation này CỰC KỲ QUAN TRỌNG:
// Nó giúp Recipe Service không bị lỗi nếu Profile Service lỡ trả về thừa trường nào đó
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileUpdateResult {

    String userId;

    Integer currentLevel;

    Integer currentXP;

    Integer currentXPGoal;

    // Lưu ý: Bên Profile Service (StatisticsService) bạn đang trả về field tên là "recipeCount"
    // nên ở đây phải đặt tên giống y hệt để hứng dữ liệu.
    Long completionCount;

    // Level-up tracking for frontend celebration
    Boolean leveledUp;
    Integer oldLevel;
    Integer newLevel;
    Integer xpToNextLevel;
}