package com.chefkix.culinary.features.session.repository;

import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.session.repository.custom.CookingSessionCustomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

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
}