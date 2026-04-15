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

  // Social
  @Builder.Default Long followerCount = 0L;
  @Builder.Default Long followingCount = 0L;
  @Builder.Default Long friendCount = 0L;
  @Builder.Default Long friendRequestCount = 0L;

  // Content
  @Builder.Default Long recipeCount = 0L;
  @Builder.Default Long postCount = 0L;
  @Builder.Default Long favouriteCount = 0L;

  // Gamification
  @Builder.Default Integer currentLevel = 1;
  @Builder.Default Integer currentXP = 0;
  @Builder.Default Integer currentXPGoal = 1000;
  @Builder.Default Title title = Title.BEGINNER;
  @Builder.Default Integer streakCount = 0;
  @Builder.Default Integer challengeStreak = 0;
  @Builder.Default Long completionCount = 0L;
  @Builder.Default Long reputation = 0L;
  @Builder.Default List<String> badges = Arrays.asList();
  /** Badge name → ISO timestamp when earned. Null for badges earned before tracking was added. */
  @Builder.Default Map<String, Instant> badgeTimestamps = new HashMap<>();

  // Cooking depth metrics
  /** Historical best cooking streak */
  @Builder.Default Integer longestStreak = 0;
  /** Distinct recipes user has completed */
  @Builder.Default Long recipesCooked = 0L;
  /** Recipes cooked 5+ times (mastery) */
  @Builder.Default Long recipesMastered = 0L;

  // Temporal XP metrics (for leaderboard context)
  /** XP earned this week (resets Monday) */
  @Builder.Default Double xpWeekly = 0.0;
  /** XP earned this month (resets 1st) */
  @Builder.Default Double xpMonthly = 0.0;

  // Creator stats
  /** Total times other users cooked your recipes */
  @Builder.Default Long totalCooksOfYourRecipes = 0L;
  /** XP earned from others cooking your recipes */
  @Builder.Default Long xpEarnedAsCreator = 0L;
  /** Cooks of your recipes this week */
  @Builder.Default Long weeklyCreatorCooks = 0L;
  /** Creator XP this week */
  @Builder.Default Long weeklyCreatorXp = 0L;
  
  // Streak tracking - computed from lastCookAt
  /** Whether user has cooked within the streak window (today for daily, 72h for cooking) */
  @Builder.Default Boolean cookedToday = false;
  /** Timestamp of last cooking session completion */
  Instant lastCookAt;
  /** Hours remaining until streak breaks (0 if already broken) */
  @Builder.Default Integer hoursUntilStreakBreaks = 0;
}
