package com.chefkix.culinary.features.achievement.controller;

import com.chefkix.culinary.features.achievement.dto.SkillTreeResponse;
import com.chefkix.culinary.features.achievement.entity.Achievement;
import com.chefkix.culinary.features.achievement.entity.UserAchievement;
import com.chefkix.culinary.features.achievement.service.AchievementService;
import com.chefkix.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /**
     */
    @GetMapping("/my-skill-tree")
    public ApiResponse<SkillTreeResponse> getMySkillTree() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(achievementService.getSkillTree(userId));
    }

    /**
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<SkillTreeResponse> getUserSkillTree(@PathVariable String userId) {
        return ApiResponse.success(achievementService.getSkillTree(userId));
    }

    /**
     */
    @GetMapping
    public ApiResponse<List<Achievement>> getAllAchievements() {
        return ApiResponse.success(achievementService.getAllAchievements());
    }

    /**
     */
    @GetMapping("/my-unlocked")
    public ApiResponse<List<UserAchievement>> getMyUnlocked() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(achievementService.getUnlockedAchievements(userId));
    }
}
