package com.chefkix.identity.repository;

import com.chefkix.identity.entity.ReferralRedemption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralRedemptionRepository extends MongoRepository<ReferralRedemption, String> {

    List<ReferralRedemption> findByReferrerUserId(String referrerUserId);

    Optional<ReferralRedemption> findByReferredUserId(String referredUserId);

    boolean existsByReferredUserId(String referredUserId);

    long countByReferrerUserId(String referrerUserId);
}
