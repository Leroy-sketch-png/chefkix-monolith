package com.chefkix.identity.mapper;

import com.chefkix.identity.dto.response.StatisticResponse;
import com.chefkix.identity.entity.Statistics;
import java.time.Duration;
import java.time.Instant;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StatisticsMapper {

  /** The streak window is 72 hours for cooking streaks (ChefKix design) */
  long STREAK_WINDOW_HOURS = 72;

  @Mapping(source = "totalRecipesPublished", target = "recipeCount")
  @Mapping(target = "cookedToday", ignore = true) // Computed in afterMapping
  @Mapping(target = "hoursUntilStreakBreaks", ignore = true) // Computed in afterMapping
  StatisticResponse toStatisticResponse(Statistics statistics);

  /**
   * Computes streak-related fields after initial mapping:
   * - cookedToday: true if lastCookAt is within the streak window (72h)
   * - hoursUntilStreakBreaks: hours remaining until streak breaks (0 if broken/no streak)
   */
  @AfterMapping
  default void computeStreakFields(Statistics source, @MappingTarget StatisticResponse target) {
    Instant now = Instant.now();
    Instant lastCookAt = source.getLastCookAt();
    
    if (lastCookAt == null || source.getStreakCount() == null || source.getStreakCount() <= 0) {
      target.setCookedToday(false);
      target.setHoursUntilStreakBreaks(0);
      return;
    }

    // Calculate time since last cook
    Duration sinceLastCook = Duration.between(lastCookAt, now);
    long hoursSinceLastCook = sinceLastCook.toHours();
    
    // "Cooked today" means within the streak window (72 hours for cooking streaks)
    boolean withinWindow = hoursSinceLastCook < STREAK_WINDOW_HOURS;
    target.setCookedToday(withinWindow);
    
    // Calculate hours until streak breaks
    if (withinWindow) {
      long hoursRemaining = STREAK_WINDOW_HOURS - hoursSinceLastCook;
      target.setHoursUntilStreakBreaks((int) Math.max(0, hoursRemaining));
    } else {
      target.setHoursUntilStreakBreaks(0);
    }
  }
}
