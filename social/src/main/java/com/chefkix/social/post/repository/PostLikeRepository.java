package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends MongoRepository<PostLike, String> {
  PostLike findByPostIdAndUserId(String postId, String userId);

  boolean existsByPostIdAndUserId(String postId, String userId);
}
