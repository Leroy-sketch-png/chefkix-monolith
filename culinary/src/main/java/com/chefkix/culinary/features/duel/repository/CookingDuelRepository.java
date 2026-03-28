package com.chefkix.culinary.features.duel.repository;

import com.chefkix.culinary.features.duel.entity.CookingDuel;
import com.chefkix.culinary.features.duel.entity.DuelStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CookingDuelRepository extends MongoRepository<CookingDuel, String> {

    // My duels (as challenger or opponent)
    @Query("{ '$or': [ {'challengerId': ?0}, {'opponentId': ?0} ], 'status': { '$in': ?1 } }")
    List<CookingDuel> findByParticipantAndStatusIn(String userId, List<DuelStatus> statuses);

    @Query("{ '$or': [ {'challengerId': ?0}, {'opponentId': ?0} ] }")
    List<CookingDuel> findByParticipant(String userId);

    // Pending duels sent TO me
    List<CookingDuel> findByOpponentIdAndStatus(String opponentId, DuelStatus status);

    // Pending duels I sent
    List<CookingDuel> findByChallengerIdAndStatus(String challengerId, DuelStatus status);

    // Active between two users on same recipe (prevent duplicates) — bidirectional
    @Query("{ '$or': [ {'challengerId': ?0, 'opponentId': ?1}, {'challengerId': ?1, 'opponentId': ?0} ], 'recipeId': ?2, 'status': { '$in': ['PENDING', 'ACCEPTED', 'IN_PROGRESS'] } }")
    Optional<CookingDuel> findActiveBetween(String challengerId, String opponentId, String recipeId);

    // Link session to duel
    Optional<CookingDuel> findByChallengerSessionId(String sessionId);
    Optional<CookingDuel> findByOpponentSessionId(String sessionId);

    // Expired pending duels (for cleanup scheduler)
    List<CookingDuel> findByStatusAndAcceptDeadlineBefore(DuelStatus status, Instant now);

    // Expired accepted duels (cook deadline passed)
    List<CookingDuel> findByStatusInAndCookDeadlineBefore(List<DuelStatus> statuses, Instant now);
}
