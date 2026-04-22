package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByPostId(String postId);

    List<Comment> findAllByUserId(String userId);

    List<Comment> findByPostId(String postId, Pageable pageable);

    void deleteAllByPostId(String postId);
}
