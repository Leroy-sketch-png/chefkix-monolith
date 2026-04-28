package com.chefkix.identity.repository;

import com.chefkix.identity.entity.UserActivity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends MongoRepository<UserActivity, String> {
  Optional<UserActivity> findByKeycloakId(String id);

  long deleteByKeycloakId(String id);
}
