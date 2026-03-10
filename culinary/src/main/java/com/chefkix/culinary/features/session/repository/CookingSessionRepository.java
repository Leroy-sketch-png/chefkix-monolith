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

    // Tìm session đang chạy của user (để chặn không cho start 2 cái cùng lúc)
    Optional<CookingSession> findByUserIdAndStatus(String userId, SessionStatus status);

    // Đếm số lần user đã nấu xong món này (để tính Mastery)
    long countByUserIdAndRecipeIdAndStatus(String userId, String recipeId, SessionStatus status);

    Optional<CookingSession> findFirstByUserIdAndStatus(String userId, SessionStatus sessionStatus);

    Page<CookingSession> findAllByUserIdAndStatusIn(String userId, List<SessionStatus> statuses, Pageable pageable);

    /**
     * Find completed sessions for a set of recipe IDs, sorted by completedAt DESC.
     * Used by creator analytics to show who recently cooked the creator's recipes.
     */
    Page<CookingSession> findByRecipeIdInAndStatus(List<String> recipeIds, SessionStatus status, Pageable pageable);

    /**
     * Count completed sessions for a user within a date range for specific recipe IDs.
     * Used by weekly challenge progress computation.
     */
    long countByUserIdAndRecipeIdInAndStatusAndCompletedAtBetween(
            String userId, List<String> recipeIds, SessionStatus status,
            LocalDateTime start, LocalDateTime end);

    /**
     * Find recent cooking sessions for a single recipe by status.
     * Used by social proof to show "recent cookers" of a recipe.
     */
    Page<CookingSession> findByRecipeIdAndStatusIn(String recipeId, List<SessionStatus> statuses, Pageable pageable);

    /**
     * Count sessions for a recipe with a specific status.
     * Used to count posts linked to a recipe (status = POSTED).
     */
    long countByRecipeIdAndStatus(String recipeId, SessionStatus status);
}