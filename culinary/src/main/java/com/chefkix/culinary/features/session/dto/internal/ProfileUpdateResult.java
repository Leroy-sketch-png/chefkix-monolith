package com.chefkix.culinary.features.session.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileUpdateResult {

    String userId;

    Integer currentLevel;

    Integer currentXP;

    Integer currentXPGoal;

    Long completionCount;

    Boolean leveledUp;
    Integer oldLevel;
    Integer newLevel;
    Integer xpToNextLevel;
}