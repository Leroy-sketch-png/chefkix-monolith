package com.chefkix.identity.repository;

import com.chefkix.identity.entity.CreatorTipSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CreatorTipSettingsRepository extends MongoRepository<CreatorTipSettings, String> {

    Optional<CreatorTipSettings> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
