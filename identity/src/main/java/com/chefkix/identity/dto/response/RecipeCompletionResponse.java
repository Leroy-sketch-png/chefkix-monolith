package com.chefkix.identity.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeCompletionResponse {
  String userId;
  Integer currentLevel;
  Double currentXP;
  Double currentXPGoal;

  Long completionCount;

  // Level-up tracking for frontend celebration
  Boolean leveledUp;
  Integer oldLevel;
  Integer newLevel;
  Integer xpToNextLevel;
}
