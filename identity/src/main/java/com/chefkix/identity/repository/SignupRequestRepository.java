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
   * Deletes expired SignUpTemp documents using MongoDB's query language. The query targets
   * documents where the 'expiresAt' field is less than the current time ('?0'). The 'delete = true'
   * flag executes the removal operation.
   */
  @Query(value = "{ 'expiresAt' : { $lt : ?0 } }", delete = true)
  int deleteExpired(Instant now);
}
