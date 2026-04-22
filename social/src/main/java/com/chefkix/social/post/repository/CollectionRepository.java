package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Collection;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends MongoRepository<Collection, String> {

    List<Collection> findAllByUserId(String userId, Sort sort);

    List<Collection> findAllByUserIdAndIsPublicTrue(String userId, Sort sort);

    @Query("{ 'postIds': ?0 }")
    List<Collection> findAllByPostIdsContaining(String postId);

    long countByUserId(String userId);

    void deleteAllByUserId(String userId);

    boolean existsByUserIdAndName(String userId, String name);

    List<Collection> findAllByIsFeaturedTrueAndIsPublicTrue(Sort sort);

    List<Collection> findAllByIsFeaturedTrueAndSeasonTag(String seasonTag, Sort sort);
}
