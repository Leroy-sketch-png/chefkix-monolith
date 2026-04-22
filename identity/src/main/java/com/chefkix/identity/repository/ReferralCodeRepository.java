package com.chefkix.identity.repository;

import com.chefkix.identity.entity.ReferralCode;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralCodeRepository extends MongoRepository<ReferralCode, String> {

    Optional<ReferralCode> findByUserId(String userId);

    Optional<ReferralCode> findByCode(String code);

    boolean existsByCode(String code);

    long deleteByUserId(String userId);
}
