package com.chefkix.culinary.features.challenge.repository;

import com.chefkix.culinary.features.challenge.entity.CommunityChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CommunityChallengeRepository extends MongoRepository<CommunityChallenge, String> {

    List<CommunityChallenge> findByStatusAndEndsAtAfter(String status, Instant now);
}
