package com.chefkix.culinary.common.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakCalculatorHelperTest {

  @Test
  void calculateUsesYesterdayAsCurrentStreakAnchorWhenUtcTodayIsMissing() {
    StreakCalculatorHelper helper = new FixedTodayStreakCalculatorHelper(LocalDate.of(2026, 4, 22));

    StreakCalculatorHelper.StreakResult result = helper.calculate(List.of(
        LocalDate.of(2026, 4, 21),
        LocalDate.of(2026, 4, 20),
        LocalDate.of(2026, 4, 18)));

    assertThat(result.getCurrentStreak()).isEqualTo(2);
    assertThat(result.getLongestStreak()).isEqualTo(2);
  }

  @Test
  void calculateResetsCurrentStreakWhenUtcTodayAndYesterdayAreBothMissing() {
    StreakCalculatorHelper helper = new FixedTodayStreakCalculatorHelper(LocalDate.of(2026, 4, 22));

    StreakCalculatorHelper.StreakResult result = helper.calculate(List.of(
        LocalDate.of(2026, 4, 20),
        LocalDate.of(2026, 4, 19),
        LocalDate.of(2026, 4, 15)));

    assertThat(result.getCurrentStreak()).isZero();
    assertThat(result.getLongestStreak()).isEqualTo(2);
  }

  private static final class FixedTodayStreakCalculatorHelper extends StreakCalculatorHelper {
    private final LocalDate today;

    private FixedTodayStreakCalculatorHelper(LocalDate today) {
      this.today = today;
    }

    @Override
    protected LocalDate utcToday() {
      return today;
    }
  }
}