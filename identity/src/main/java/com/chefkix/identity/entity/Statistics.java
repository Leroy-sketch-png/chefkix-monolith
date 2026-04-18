package com.chefkix.identity.entity;

import com.chefkix.identity.enums.Title;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "statistics")
public class Statistics {

  // Social
  @Builder.Default Long followerCount = 0L;

  @Builder.Default Long followingCount = 0L;

  @Builder.Default Long friendCount = 0L;
  @Builder.Default Long friendRequestCount = 0L;

  // Content
  @Builder.Default Long totalRecipesPublished = 0L;

  @Builder.Default Long postCount = 0L;

  @Builder.Default Long favouriteCount = 0L;

  // Gamification
  // NOTE: Level starts at 1, not 0. "Level up to Level 1" is psychologically wrong.
  // Users should start as "Level 1 Beginner" and level up to "Level 2".
  @Min(1) @Builder.Default Integer currentLevel = 1;

  @Min(0) @Builder.Default Double currentXP = 0.0;

  @Min(0) @Builder.Default Double currentXPGoal = 1000.0;

  /** Weekly XP earned (reset every Monday) - used for leaderboard ranking */
  @Min(0) @Builder.Default Double xpWeekly = 0.0;

  /** Monthly XP earned (reset 1st of each month) - used for monthly leaderboard */
  @Min(0) @Builder.Default Double xpMonthly = 0.0;

  @Builder.Default Title title = Title.BEGINNER;

  @Builder.Default Integer streakCount = 0;

  /** Historical best cooking streak — updated whenever streakCount reaches a new max */
  @Builder.Default Integer longestStreak = 0;

  /** Timestamp of last cooking session completion - used for 72-hour streak window */
  Instant lastCookAt;

  /** Separate streak for daily challenge completion */
  @Builder.Default Integer challengeStreak = 0;

  /** Timestamp of last challenge completion - used for 24-hour challenge streak window */
  Instant lastChallengeAt;

  @Builder.Default Long completionCount = 0L;

  @Builder.Default Long reputation = 0L;

  @Builder.Default List<String> badges = new ArrayList<>();

  /** Badge name → timestamp when earned. Additive alongside badges list for backwards compat. */
  @Builder.Default Map<String, Instant> badgeTimestamps = new HashMap<>();

  // Cooking depth metrics
  /** Distinct recipes user has completed (not recipe count published) */
  @Builder.Default Long recipesCooked = 0L;

  /** Recipes the user has cooked 5+ times (mastery threshold) */
  @Builder.Default Long recipesMastered = 0L;

  /** RecipeId → cook count — tracks per-recipe mastery. Used to compute recipesCooked/recipesMastered. */
  @Builder.Default Map<String, Integer> recipeCookCounts = new HashMap<>();

  // Creator stats
  @Builder.Default Long totalCooksOfYourRecipes = 0L;

  @Builder.Default Long xpEarnedAsCreator = 0L;

  @Builder.Default Long weeklyCreatorCooks = 0L;

  @Builder.Default Long weeklyCreatorXp = 0L;
}
