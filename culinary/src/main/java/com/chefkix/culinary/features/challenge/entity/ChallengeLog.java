package com.chefkix.culinary.features.challenge.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "challenge_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// Quan trọng: Đảm bảo user không thể claim reward 2 lần trong 1 ngày (logic database)
@CompoundIndex(name = "unique_daily_challenge_user", def = "{'userId': 1, 'challengeDate': 1}", unique = true)
public class ChallengeLog {

    @Id
    String id;

    @Indexed
    String userId;

    // ID định danh của challenge (VD: "noodle-day", "quick-meal")
    // Lấy từ file config/enum của bạn
    String challengeId;

    // Snapshot tiêu đề challenge tại thời điểm hoàn thành
    // (Để nếu sau này file config sửa title, lịch sử user không bị lỗi hiển thị)
    String challengeTitle;

    // Recipe user đã nấu để hoàn thành challenge này
    String recipeId;
    String recipeTitle;
    // Key ngày logic: "YYYY-MM-DD" (VD: "2025-01-16")
    // Dùng String thay vì Date để tránh rắc rối về giờ/phút/giây khi check unique index
    @Indexed
    String challengeDate;

    // Số XP thưởng đã nhận (lưu cứng lại để lịch sử chính xác)
    int bonusXp;

    // Thời điểm thực tế record được tạo
    @CreatedDate
    Instant completedAt;
}