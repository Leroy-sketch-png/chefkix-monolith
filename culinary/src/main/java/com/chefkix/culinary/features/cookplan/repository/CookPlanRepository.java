package com.chefkix.culinary.features.cookplan.repository;

import com.chefkix.culinary.features.cookplan.entity.CookPlan;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CookPlanRepository extends MongoRepository<CookPlan, String> {

    Optional<CookPlan> findByIdAndUserId(String id, String userId);

    Optional<CookPlan> findTopByUserIdAndPlanDateOrderByCreatedAtDesc(String userId, LocalDate planDate);

    void deleteByUserIdAndPlanDate(String userId, LocalDate planDate);
}
