package com.chefkix.social.story.repository;

import com.chefkix.social.story.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {

    // Lấy Story đang hoạt động (cho chính chủ hoặc bạn bè)
    List<Story> findByUserIdAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(
            String userId, Instant now);

    // Lấy Story đã vào kho lưu trữ (chỉ chính chủ)
    Page<Story> findByUserIdAndIsDeletedFalseAndExpiresAtBeforeOrderByCreatedAtDesc(
            String userId, Instant now, Pageable pageable);

    // Kiểm tra tồn tại để validate trước khi xóa
    Optional<Story> findByIdAndUserIdAndIsDeletedFalse(String id, String userId);

    List<Story> findByUserIdInAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(List<String> followingIds, Instant now);

    long countByIdInAndUserId(List<String> storyIds, String userId);
}