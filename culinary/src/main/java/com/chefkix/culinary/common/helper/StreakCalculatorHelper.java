package com.chefkix.culinary.common.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StreakCalculatorHelper {

    /**
     * Calculate Current Streak and Longest Streak based on a list of completed dates.
     * @param completedDates List of completed dates (should be distinct and sorted descending before passing in)
     */
    public StreakResult calculate(List<LocalDate> completedDates) {
        if (completedDates == null || completedDates.isEmpty()) {
            return new StreakResult(0, 0);
        }

        // 1. Calculate Current Streak
        // Logic: Check backwards from today (or yesterday).
        // If today is not completed but yesterday is -> Streak is preserved.
        // If yesterday is not completed -> Streak resets to 0 (unless today is completed).

        int currentStreak = 0;
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;

        // If today is not completed, check if the streak ends at yesterday
        if (!completedDates.contains(today)) {
            checkDate = today.minusDays(1);
        }

        // Start counting backwards
        while (completedDates.contains(checkDate)) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }

        // 2. Calculate Longest Streak
        // Iterate through the entire history to find the longest consecutive streak
        int longestStreak = 0;
        int tempStreak = 1;

        // List is sorted descending (Newest -> Oldest), but for longest streak it's easier to sort ascending (Oldest -> Newest)
        List<LocalDate> sortedAsc = new ArrayList<>(completedDates);
        Collections.sort(sortedAsc); // Sort oldest -> newest

        if (!sortedAsc.isEmpty()) {
            longestStreak = 1; // At least 1 if list is not empty
            for (int i = 0; i < sortedAsc.size() - 1; i++) {
                LocalDate current = sortedAsc.get(i);
                LocalDate next = sortedAsc.get(i + 1);

                if (current.plusDays(1).equals(next)) {
                    tempStreak++;
                } else {
                    tempStreak = 1; // Streak broken
                }
                longestStreak = Math.max(longestStreak, tempStreak);
            }
        }

        return new StreakResult(currentStreak, longestStreak);
    }

    @Data
    @AllArgsConstructor
    public static class StreakResult {
        private int currentStreak;
        private int longestStreak;
    }
}