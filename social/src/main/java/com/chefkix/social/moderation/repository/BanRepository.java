package com.chefkix.social.moderation.repository;

import com.chefkix.social.moderation.entity.Ban;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BanRepository extends MongoRepository<Ban, String> {

    List<Ban> findByUserIdAndActiveTrue(String userId);

    long countByUserIdAndActiveTrue(String userId);

    long countByUserId(String userId);

    Optional<Ban> findFirstByUserIdAndActiveTrueOrderByIssuedAtDesc(String userId);

    List<Ban> findByActiveTrueAndExpiresAtBefore(Instant now);

    List<Ban> findByUserIdOrderByIssuedAtDesc(String userId);
}
