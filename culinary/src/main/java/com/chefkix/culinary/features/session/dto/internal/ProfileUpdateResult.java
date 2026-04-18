package com.chefkix.culinary.features.session.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// This annotation is CRITICALLY IMPORTANT:
// It prevents Recipe Service from erroring if Profile Service returns extra fields
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileUpdateResult {

    String userId;

    Integer currentLevel;

    Integer currentXP;

    Integer currentXPGoal;

    // Note: Profile Service (StatisticsService) returns a field named "recipeCount"
    // so this field must be named identically to receive the data.
    Long completionCount;

    // Level-up tracking for frontend celebration
    Boolean leveledUp;
    Integer oldLevel;
    Integer newLevel;
    Integer xpToNextLevel;
}