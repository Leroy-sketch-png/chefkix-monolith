package com.chefkix.culinary.features.achievement.repository;

import com.chefkix.culinary.features.achievement.entity.Achievement;
import com.chefkix.culinary.features.achievement.entity.AchievementCategory;
import com.chefkix.culinary.features.achievement.entity.CriteriaType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends MongoRepository<Achievement, String> {
    Optional<Achievement> findByCode(String code);

    List<Achievement> findByCategory(AchievementCategory category);

    List<Achievement> findByPathId(String pathId);

    List<Achievement> findByCriteriaType(CriteriaType criteriaType);

    List<Achievement> findByCriteriaTypeAndCriteriaTarget(CriteriaType criteriaType, String criteriaTarget);

    boolean existsByCode(String code);
}
