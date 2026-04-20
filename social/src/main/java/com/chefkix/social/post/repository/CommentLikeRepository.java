package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.CommentLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends MongoRepository<CommentLike, String> {
    Optional<CommentLike> findByCommentIdAndUserId(String commentId, String userId);

    boolean existsByCommentIdAndUserId(String commentId, String userId);

    void deleteByCommentIdAndUserId(String commentId, String userId);

    void deleteAllByCommentId(String commentId);

    void deleteAllByCommentIdIn(Collection<String> commentIds);

    long countByCommentId(String commentId);
}
