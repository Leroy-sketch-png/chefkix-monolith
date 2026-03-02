package com.chefkix.culinary.features.challenge.repository;

import com.chefkix.culinary.features.challenge.entity.ChallengeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeLogRepository extends MongoRepository<ChallengeLog, String> {

    // Tìm log của user trong một ngày cụ thể
    // challengeDate format: "YYYY-MM-DD"
    Optional<ChallengeLog> findByUserIdAndChallengeDate(String userId, String challengeDate);

    // Kiểm tra nhanh xem đã làm chưa (trả về true/false)
    boolean existsByUserIdAndChallengeDate(String userId, String challengeDate);

    // 1. Lấy lịch sử CÓ PHÂN TRANG (Pageable)
    // Spring Data Mongo tự động apply limit, offset và sort từ đối tượng Pageable
    Page<ChallengeLog> findByUserId(String userId, Pageable pageable);

    // 2. Lấy TOÀN BỘ ngày hoàn thành (Không phân trang)
    // Dùng cho thuật toán tính Streak (cần full lịch sử)
    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$project': { 'date': '$challengeDate', '_id': 0 } }"
    })
    List<String> findCompletedDatesByUserId(String userId);

    // 3. Tính tổng XP (Dùng Aggregation cho nhanh, không load java object)
    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$group': { '_id': null, 'totalXp': { '$sum': '$bonusXp' } } }"
    })
    // Bạn cần class wrapper để hứng kết quả này, hoặc dùng Document
    // Ở đây mình viết ví dụ logic, thực tế có thể dùng MongoTemplate
    SumResult sumBonusXpByUserId(String userId);

    // Class hứng kết quả sum
    class SumResult {
        public long totalXp;
    }}