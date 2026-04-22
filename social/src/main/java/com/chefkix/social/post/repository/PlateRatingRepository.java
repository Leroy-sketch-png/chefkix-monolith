package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.PlateRating;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlateRatingRepository extends MongoRepository<PlateRating, String> {
    Optional<PlateRating> findByPostIdAndUserId(String postId, String userId);

    List<PlateRating> findByPostIdInAndUserId(List<String> postIds, String userId);

    List<PlateRating> findAllByUserId(String userId);

    void deleteAllByPostId(String postId);
}
