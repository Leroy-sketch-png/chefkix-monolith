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
  
  // Streak tracking - computed from lastCookAt
  /** Whether user has cooked within the streak window (today for daily, 72h for cooking) */
  @Builder.Default Boolean cookedToday = false;
  /** Timestamp of last cooking session completion */
  Instant lastCookAt;
  /** Hours remaining until streak breaks (0 if already broken) */
  @Builder.Default Integer hoursUntilStreakBreaks = 0;
}
