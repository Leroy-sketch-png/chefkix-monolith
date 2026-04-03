package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Collection;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends MongoRepository<Collection, String> {

    List<Collection> findAllByUserId(String userId, Sort sort);

    List<Collection> findAllByUserIdAndIsPublicTrue(String userId, Sort sort);

    long countByUserId(String userId);

    boolean existsByUserIdAndName(String userId, String name);
}
