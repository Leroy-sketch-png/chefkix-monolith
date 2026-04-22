package com.chefkix.identity.repository;

import com.chefkix.identity.entity.UserSubscription;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends MongoRepository<UserSubscription, String> {
    Optional<UserSubscription> findByUserId(String userId);
    boolean existsByUserIdAndActiveTrue(String userId);
    long deleteByUserId(String userId);
}
