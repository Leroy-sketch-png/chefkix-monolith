package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.CollectionProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionProgressRepository extends MongoRepository<CollectionProgress, String> {

    Optional<CollectionProgress> findByUserIdAndCollectionId(String userId, String collectionId);

    List<CollectionProgress> findAllByUserId(String userId);

    void deleteAllByUserId(String userId);

    List<CollectionProgress> findAllByCollectionId(String collectionId);

    long countByCollectionId(String collectionId);

    @Query(value = "{ 'collectionId': ?0, 'completedRecipeIds': { $size: ?1 } }", count = true)
    long countByCollectionIdAndCompletedRecipeIdsSize(String collectionId, int size);

    boolean existsByUserIdAndCollectionId(String userId, String collectionId);
}
