package com.chefkix.culinary.features.mealplan.repository;

import com.chefkix.culinary.features.mealplan.entity.MealPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanRepository extends MongoRepository<MealPlan, String> {

    List<MealPlan> findByUserIdOrderByWeekStartDateDesc(String userId);

    Optional<MealPlan> findByIdAndUserId(String id, String userId);

    Optional<MealPlan> findByUserIdAndWeekStartDate(String userId, LocalDate weekStartDate);

    void deleteByIdAndUserId(String id, String userId);

    void deleteAllByUserId(String userId);
}
