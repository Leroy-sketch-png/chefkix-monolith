package com.chefkix.identity.repository;

import com.chefkix.identity.entity.VerificationRequest;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRequestRepository extends MongoRepository<VerificationRequest, String> {

  Optional<VerificationRequest> findByUserIdAndStatus(String userId, String status);

  Optional<VerificationRequest> findTopByUserIdOrderByRequestedAtDesc(String userId);

  boolean existsByUserIdAndStatus(String userId, String status);
}
