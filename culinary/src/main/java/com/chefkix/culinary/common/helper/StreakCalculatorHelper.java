package com.chefkix.culinary.common.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StreakCalculatorHelper {

    /**
     */
    public StreakResult calculate(List<LocalDate> completedDates) {
        if (completedDates == null || completedDates.isEmpty()) {
            return new StreakResult(0, 0);
        }


        int currentStreak = 0;
        LocalDate today = utcToday();
        LocalDate checkDate = today;

        if (!completedDates.contains(today)) {
            checkDate = today.minusDays(1);
        }

        while (completedDates.contains(checkDate)) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }

        int longestStreak = 0;
        int tempStreak = 1;

        List<LocalDate> sortedAsc = new ArrayList<>(completedDates);
Collections.sort(sortedAsc);

        if (!sortedAsc.isEmpty()) {
longestStreak = 1;
            for (int i = 0; i < sortedAsc.size() - 1; i++) {
                LocalDate current = sortedAsc.get(i);
                LocalDate next = sortedAsc.get(i + 1);

                if (current.plusDays(1).equals(next)) {
                    tempStreak++;
                } else {
tempStreak = 1;
                }
                longestStreak = Math.max(longestStreak, tempStreak);
            }
        }

        return new StreakResult(currentStreak, longestStreak);
    }

    protected LocalDate utcToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    @Data
    @AllArgsConstructor
    public static class StreakResult {
        private int currentStreak;
        private int longestStreak;
    }
}