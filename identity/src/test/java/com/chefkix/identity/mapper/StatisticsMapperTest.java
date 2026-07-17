package com.chefkix.identity.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.chefkix.identity.dto.response.StatisticResponse;
import com.chefkix.identity.entity.Statistics;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatisticsMapperTest {

  private final StatisticsMapper mapper =
      new StatisticsMapper() {
        @Override
        public StatisticResponse toStatisticResponse(Statistics statistics) {
          StatisticResponse response = StatisticResponse.builder().build();
          computeStreakFields(statistics, response);
          return response;
        }
      };

  @Test
  void computeStreakFieldsKeepsSeventyTwoHourBoundaryActive() {
    Statistics statistics =
        Statistics.builder()
            .streakCount(3)
            .lastCookAt(Instant.now().minus(Duration.ofHours(72)))
            .build();

    StatisticResponse response = mapper.toStatisticResponse(statistics);

    assertThat(response.getCookedToday()).isTrue();
    assertThat(response.getHoursUntilStreakBreaks()).isZero();
  }

  @Test
  void computeStreakFieldsMarksStreakBrokenAfterSeventyTwoHourBucket() {
    Statistics statistics =
        Statistics.builder()
            .streakCount(3)
            .lastCookAt(Instant.now().minus(Duration.ofHours(73)))
            .build();

    StatisticResponse response = mapper.toStatisticResponse(statistics);

    assertThat(response.getCookedToday()).isFalse();
    assertThat(response.getHoursUntilStreakBreaks()).isZero();
  }
}
