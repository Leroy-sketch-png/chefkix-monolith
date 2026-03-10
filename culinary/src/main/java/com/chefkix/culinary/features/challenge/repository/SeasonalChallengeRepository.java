package com.chefkix.culinary.features.challenge.repository;

import com.chefkix.culinary.features.challenge.entity.SeasonalChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonalChallengeRepository extends MongoRepository<SeasonalChallenge, String> {

    List<SeasonalChallenge> findByStatus(String status);

    List<SeasonalChallenge> findByStatusAndEndsAtAfter(String status, Instant now);

    Optional<SeasonalChallenge> findFirstByStatusOrderByEndsAtAsc(String status);

    List<SeasonalChallenge> findByStatusIn(List<String> statuses);
}
