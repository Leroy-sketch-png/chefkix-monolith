package com.chefkix.culinary.features.session.repository;

import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.session.repository.custom.CookingSessionCustomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CookingSessionRepository extends MongoRepository<CookingSession, String>, CookingSessionCustomRepository {

    Optional<CookingSession> findByUserIdAndStatus(String userId, SessionStatus status);

    List<CookingSession> findAllByUserId(String userId);

    long countByUserIdAndRecipeIdAndStatus(String userId, String recipeId, SessionStatus status);

    Optional<CookingSession> findFirstByUserIdAndStatus(String userId, SessionStatus sessionStatus);

    Optional<CookingSession> findFirstByUserIdAndStatusIn(String userId, List<SessionStatus> statuses);

        List<CookingSession> findTop20ByUserIdAndStatusAndPostIdIsNullOrderByCompletedAtDesc(
            String userId, SessionStatus status);

    List<CookingSession> findAllByPostIdAndStatus(String postId, SessionStatus status);

    Page<CookingSession> findAllByUserIdAndStatusIn(String userId, List<SessionStatus> statuses, Pageable pageable);

    /**
     */
    Page<CookingSession> findByRecipeIdInAndStatus(List<String> recipeIds, SessionStatus status, Pageable pageable);

    /**
     */
    long countByUserIdAndRecipeIdInAndStatusAndCompletedAtBetween(
            String userId, List<String> recipeIds, SessionStatus status,
            LocalDateTime start, LocalDateTime end);

    List<CookingSession> findByUserIdAndStatusAndCompletedAtBetween(
            String userId, SessionStatus status, LocalDateTime start, LocalDateTime end);

    /**
     */
    Page<CookingSession> findByRecipeIdAndStatusIn(String recipeId, List<SessionStatus> statuses, Pageable pageable);

    /**
     */
    long countByRecipeIdAndStatus(String recipeId, SessionStatus status);

    /**
     */
    List<CookingSession> findTop20ByUserIdOrderByStartedAtDesc(String userId);

    /**
     */
    List<CookingSession> findByRecipeIdAndStatusIn(String recipeId, List<SessionStatus> statuses);
}
