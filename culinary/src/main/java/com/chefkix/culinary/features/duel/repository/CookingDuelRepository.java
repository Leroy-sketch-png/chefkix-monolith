package com.chefkix.culinary.features.duel.repository;

import com.chefkix.culinary.features.duel.entity.CookingDuel;
import com.chefkix.culinary.features.duel.entity.DuelStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CookingDuelRepository extends MongoRepository<CookingDuel, String> {

    @Query("{ '$or': [ {'challengerId': ?0}, {'opponentId': ?0} ], 'status': { '$in': ?1 } }")
    List<CookingDuel> findByParticipantAndStatusIn(String userId, List<DuelStatus> statuses);

    @Query("{ '$or': [ {'challengerId': ?0}, {'opponentId': ?0} ] }")
    List<CookingDuel> findByParticipant(String userId);

    List<CookingDuel> findByOpponentIdAndStatus(String opponentId, DuelStatus status);

    List<CookingDuel> findByChallengerIdAndStatus(String challengerId, DuelStatus status);

    @Query("{ '$or': [ {'challengerId': ?0, 'opponentId': ?1}, {'challengerId': ?1, 'opponentId': ?0} ], 'recipeId': ?2, 'status': { '$in': ['PENDING', 'ACCEPTED', 'IN_PROGRESS'] } }")
    Optional<CookingDuel> findActiveBetween(String challengerId, String opponentId, String recipeId);

    Optional<CookingDuel> findByChallengerSessionId(String sessionId);
    Optional<CookingDuel> findByOpponentSessionId(String sessionId);

}
