package com.chefkix.culinary.features.achievement.repository;

import com.chefkix.culinary.features.achievement.entity.UserAchievement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends MongoRepository<UserAchievement, String> {
    List<UserAchievement> findByUserId(String userId);

    List<UserAchievement> findByUserIdAndUnlocked(String userId, boolean unlocked);

    Optional<UserAchievement> findByUserIdAndAchievementCode(String userId, String achievementCode);

    long countByUserIdAndUnlocked(String userId, boolean unlocked);

    void deleteAllByUserId(String userId);
}
