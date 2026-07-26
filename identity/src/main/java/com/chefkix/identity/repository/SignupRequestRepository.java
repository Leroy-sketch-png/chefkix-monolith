package com.chefkix.identity.repository;

import com.chefkix.identity.entity.SignupRequest;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SignupRequestRepository extends MongoRepository<SignupRequest, String> {

  Optional<SignupRequest> findByEmail(String email);

  void deleteByEmail(String email);

  /**
   */
  @Query(value = "{ 'expiresAt' : { $lt : ?0 } }", delete = true)
  int deleteExpired(Instant now);
}
