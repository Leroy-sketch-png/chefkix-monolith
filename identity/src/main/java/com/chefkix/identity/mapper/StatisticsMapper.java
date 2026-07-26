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

  long STREAK_WINDOW_HOURS = 72;

  @Mapping(source = "totalRecipesPublished", target = "recipeCount")
@Mapping(target = "cookedToday", ignore = true)
@Mapping(target = "hoursUntilStreakBreaks", ignore = true)
  StatisticResponse toStatisticResponse(Statistics statistics);

  /**
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

    Duration sinceLastCook = Duration.between(lastCookAt, now);
    long hoursSinceLastCook = sinceLastCook.toHours();
    
    boolean withinWindow = hoursSinceLastCook <= STREAK_WINDOW_HOURS;
    target.setCookedToday(withinWindow);
    
    if (withinWindow) {
      long hoursRemaining = STREAK_WINDOW_HOURS - hoursSinceLastCook;
      target.setHoursUntilStreakBreaks((int) Math.max(0, hoursRemaining));
    } else {
      target.setHoursUntilStreakBreaks(0);
    }
  }
}
