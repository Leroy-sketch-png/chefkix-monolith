package com.chefkix.social.post.repository;

import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    // --- Feed queries (exclude hidden posts) ---
    Page<Post> findByHiddenFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByHiddenFalseOrderByHotScoreDesc(Pageable pageable);

    Page<Post> findByHiddenFalse(Pageable pageable);

    Page<Post> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Post> findByUserIdInAndHiddenFalseOrderByCreatedAtDesc(List<String> userIds, Pageable pageable);

    Page<Post> findByUserIdInAndHiddenFalseOrderByHotScoreDesc(List<String> userIds, Pageable pageable);

    // --- Non-feed queries (include all) ---
    List<Post> findByCreatedAtAfter(Instant since);

    long countByUserIdAndHiddenFalse(String userId);

    // Legacy (kept for backward compat, prefer hidden-aware variants above)
    Page<Post> findAllByUserId(String userId, Pageable pageable);
    long countByUserId(String userId);
}
