package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.enums.PostStatus;
import com.chefkix.social.post.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    // --- Feed queries (exclude hidden posts) ---
    Page<Post> findByHiddenFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByHiddenFalseOrderByHotScoreDesc(Pageable pageable);

    Page<Post> findByHiddenFalse(Pageable pageable);

    Page<Post> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Post> findByUserIdInAndHiddenFalseOrderByCreatedAtDesc(List<String> userIds, Pageable pageable);

    Page<Post> findByUserIdInAndHiddenFalseOrderByHotScoreDesc(List<String> userIds, Pageable pageable);

    // --- Recipe reviews: all reviews for a specific recipe ---
    Page<Post> findByRecipeIdAndPostTypeAndHiddenFalseOrderByCreatedAtDesc(
            String recipeId, PostType postType, Pageable pageable);

        Page<Post> findByGroupIdAndPostTypeAndStatusAndHiddenFalseOrderByCreatedAtDesc(
            String groupId, PostType postType, PostStatus status, Pageable pageable);

    long countByRecipeIdAndPostTypeAndHiddenFalse(String recipeId, PostType postType);

    // --- Recipe battles: active battles (not yet ended) ---
    Page<Post> findByPostTypeAndBattleEndsAtAfterAndHiddenFalseOrderByBattleEndsAtAsc(
            PostType postType, Instant now, Pageable pageable);

    // --- Non-feed queries (include all) ---
    List<Post> findByCreatedAtAfter(Instant since);

        List<Post> findAllByUserId(String userId);

    long countByUserIdAndHiddenFalse(String userId);

    // Legacy (kept for backward compat, prefer hidden-aware variants above)
    Page<Post> findAllByUserId(String userId, Pageable pageable);
    long countByUserId(String userId);

    /**
     * Lean fetch for feed/saved views.
     * Excludes commentIds, which can grow large and are not used in PostResponse.
     */
    @Query(value = "{ '_id': { '$in': ?0 } }", fields = "{ 'commentIds': 0 }")
    List<Post> findLeanByIdIn(List<String> ids);
}
