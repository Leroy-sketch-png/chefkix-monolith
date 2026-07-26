package com.chefkix.culinary.features.challenge.repository;

import com.chefkix.culinary.features.challenge.entity.ChallengeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeLogRepository extends MongoRepository<ChallengeLog, String> {

    Optional<ChallengeLog> findByUserIdAndChallengeDate(String userId, String challengeDate);

    boolean existsByUserIdAndChallengeDate(String userId, String challengeDate);

    Page<ChallengeLog> findByUserId(String userId, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$project': { 'date': '$challengeDate', '_id': 0 } }"
    })
    List<String> findCompletedDatesByUserId(String userId);

    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$group': { '_id': null, 'totalXp': { '$sum': '$bonusXp' } } }"
    })
    SumResult sumBonusXpByUserId(String userId);

    @Aggregation(pipeline = {
            "{ '$match': { 'challengeDate': ?0, 'userId': { '$in': ?1 } } }",
            "{ '$project': { 'userId': 1, '_id': 0 } }"
    })
    List<String> findUserIdsWithChallengeDate(String challengeDate, List<String> userIds);

        void deleteAllByUserId(String userId);

    class SumResult {
        public long totalXp;
    }}