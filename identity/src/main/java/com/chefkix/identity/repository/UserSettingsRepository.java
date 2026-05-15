package com.chefkix.identity.repository;

import com.chefkix.identity.entity.UserSettings;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {

  Optional<UserSettings> findByUserId(String userId);

  List<UserSettings> findAllByUserIdIn(Collection<String> userIds);

  boolean existsByUserId(String userId);

  long deleteByUserId(String userId);
}
