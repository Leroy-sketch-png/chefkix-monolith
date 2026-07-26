package com.chefkix.culinary.features.achievement.dto;

import com.chefkix.culinary.features.achievement.entity.AchievementCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillTreeResponse {
    List<SkillPath> paths;
    int totalUnlocked;
    int totalAchievements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SkillPath {
        String pathId;
String pathName;
        AchievementCategory category;
        List<AchievementNode> nodes;
        int unlockedCount;
        int totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AchievementNode {
        String code;
        String name;
        String description;
        String icon;
        int tier;
        AchievementCategory category;
        boolean hidden;
        boolean premium;

        int currentProgress;
        int requiredProgress;
        boolean unlocked;
        Instant unlockedAt;

        String prerequisiteCode;
boolean prerequisiteMet;
    }
}
