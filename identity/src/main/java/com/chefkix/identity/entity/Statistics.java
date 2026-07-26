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

  @Builder.Default Long followerCount = 0L;

  @Builder.Default Long followingCount = 0L;

  @Builder.Default Long friendCount = 0L;
  @Builder.Default Long friendRequestCount = 0L;

  @Builder.Default Long totalRecipesPublished = 0L;

  @Builder.Default Long postCount = 0L;

  @Builder.Default Long favouriteCount = 0L;

  @Min(1) @Builder.Default Integer currentLevel = 1;

  @Min(0) @Builder.Default Double currentXP = 0.0;

  @Min(0) @Builder.Default Double currentXPGoal = 1000.0;

  @Min(0) @Builder.Default Double xpWeekly = 0.0;

  @Min(0) @Builder.Default Double xpMonthly = 0.0;

  /**
   */
  @Min(0) @Builder.Default Double totalXpAllTime = 0.0;

  @Builder.Default Title title = Title.BEGINNER;

  @Builder.Default Integer streakCount = 0;

  @Builder.Default Integer longestStreak = 0;

  Instant lastCookAt;

  @Builder.Default Integer challengeStreak = 0;

  Instant lastChallengeAt;

  @Builder.Default Long completionCount = 0L;

  @Builder.Default Long reputation = 0L;

  @Builder.Default List<String> badges = new ArrayList<>();

  @Builder.Default Map<String, Instant> badgeTimestamps = new HashMap<>();

  @Builder.Default Long recipesCooked = 0L;

  @Builder.Default Long recipesMastered = 0L;

  @Builder.Default Map<String, Integer> recipeCookCounts = new HashMap<>();

  @Builder.Default Long totalCooksOfYourRecipes = 0L;

  @Builder.Default Long xpEarnedAsCreator = 0L;

  @Builder.Default Long weeklyCreatorCooks = 0L;

  @Builder.Default Long weeklyCreatorXp = 0L;
}
