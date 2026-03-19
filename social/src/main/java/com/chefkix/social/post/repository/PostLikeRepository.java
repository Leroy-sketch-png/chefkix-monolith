package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostLikeRepository extends MongoRepository<PostLike, String> {
  PostLike findByPostIdAndUserId(String postId, String userId);

  boolean existsByPostIdAndUserId(String postId, String userId);

  /**
   * Batch: find all likes by a user for a set of posts (eliminates N+1).
   */
  List<PostLike> findByUserIdAndPostIdIn(String userId, List<String> postIds);
}
