package com.chefkix.culinary.features.challenge.repository;

import com.chefkix.culinary.features.challenge.entity.SeasonalChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonalChallengeRepository extends MongoRepository<SeasonalChallenge, String> {

    List<SeasonalChallenge> findByStatusIn(List<String> statuses);
}
