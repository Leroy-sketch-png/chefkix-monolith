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

    // Find user's log for a specific date
    // challengeDate format: "YYYY-MM-DD"
    Optional<ChallengeLog> findByUserIdAndChallengeDate(String userId, String challengeDate);

    // Quick check if already completed (returns true/false)
    boolean existsByUserIdAndChallengeDate(String userId, String challengeDate);

    // 1. Get PAGINATED history (Pageable)
    // Spring Data Mongo automatically applies limit, offset and sort from the Pageable object
    Page<ChallengeLog> findByUserId(String userId, Pageable pageable);

    // 2. Get ALL completed dates (No pagination)
    // Used for streak calculation algorithm (needs full history)
    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$project': { 'date': '$challengeDate', '_id': 0 } }"
    })
    List<String> findCompletedDatesByUserId(String userId);

    // 3. Calculate total XP (Using Aggregation for speed, no Java object loading)
    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$group': { '_id': null, 'totalXp': { '$sum': '$bonusXp' } } }"
    })
    // You need a wrapper class to receive this result, or use Document
    // This is example logic; in practice you could use MongoTemplate
    SumResult sumBonusXpByUserId(String userId);

    // Wrapper class to receive sum result
    // Batch check: get all userIds who have completed a specific challenge date
    @Aggregation(pipeline = {
            "{ '$match': { 'challengeDate': ?0, 'userId': { '$in': ?1 } } }",
            "{ '$project': { 'userId': 1, '_id': 0 } }"
    })
    List<String> findUserIdsWithChallengeDate(String challengeDate, List<String> userIds);

        void deleteAllByUserId(String userId);

    class SumResult {
        public long totalXp;
    }}