package com.chefkix.identity.dto.response;

import com.chefkix.identity.enums.Title;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticResponse {

  @Builder.Default Long followerCount = 0L;
  @Builder.Default Long followingCount = 0L;
  @Builder.Default Long friendCount = 0L;
  @Builder.Default Long friendRequestCount = 0L;

  @Builder.Default Long recipeCount = 0L;
  @Builder.Default Long postCount = 0L;
  @Builder.Default Long favouriteCount = 0L;

  @Builder.Default Integer currentLevel = 1;
  @Builder.Default Integer currentXP = 0;
  @Builder.Default Integer currentXPGoal = 1000;
  @Builder.Default Title title = Title.BEGINNER;
  @Builder.Default Integer streakCount = 0;
  @Builder.Default Integer challengeStreak = 0;
  @Builder.Default Long completionCount = 0L;
  @Builder.Default Long reputation = 0L;
  @Builder.Default List<String> badges = Arrays.asList();
  @Builder.Default Map<String, Instant> badgeTimestamps = new HashMap<>();

  @Builder.Default Integer longestStreak = 0;
  @Builder.Default Long recipesCooked = 0L;
  @Builder.Default Long recipesMastered = 0L;

  @Builder.Default Double xpWeekly = 0.0;
  @Builder.Default Double xpMonthly = 0.0;
  @Builder.Default Double totalXpAllTime = 0.0;

  @Builder.Default Long totalCooksOfYourRecipes = 0L;
  @Builder.Default Long xpEarnedAsCreator = 0L;
  @Builder.Default Long weeklyCreatorCooks = 0L;
  @Builder.Default Long weeklyCreatorXp = 0L;
  
  @Builder.Default Boolean cookedToday = false;
  Instant lastCookAt;
  @Builder.Default Integer hoursUntilStreakBreaks = 0;
}
