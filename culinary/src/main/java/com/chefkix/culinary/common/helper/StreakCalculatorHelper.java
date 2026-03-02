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
     * Tính Current Streak và Longest Streak dựa trên danh sách ngày đã hoàn thành.
     * @param completedDates Danh sách ngày hoàn thành (cần được distinct và sorted giảm dần trước khi truyền vào)
     */
    public StreakResult calculate(List<LocalDate> completedDates) {
        if (completedDates == null || completedDates.isEmpty()) {
            return new StreakResult(0, 0);
        }

        // 1. Tính Current Streak
        // Logic: Check từ ngày hôm nay (hoặc hôm qua) lùi về trước.
        // Nếu hôm nay chưa làm, nhưng hôm qua làm -> Streak vẫn giữ.
        // Nếu hôm qua không làm -> Streak reset về 0 (trừ khi hôm nay đã làm).

        int currentStreak = 0;
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;

        // Nếu hôm nay chưa làm, check thử xem chuỗi có kết thúc ở hôm qua không
        if (!completedDates.contains(today)) {
            checkDate = today.minusDays(1);
        }

        // Bắt đầu đếm lùi
        while (completedDates.contains(checkDate)) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }

        // 2. Tính Longest Streak
        // Duyệt qua toàn bộ lịch sử để tìm chuỗi liên tiếp dài nhất
        int longestStreak = 0;
        int tempStreak = 1;

        // List đã sort giảm dần (Mới -> Cũ), nhưng để tính longest dễ hơn thì nên sort tăng dần (Cũ -> Mới)
        List<LocalDate> sortedAsc = new ArrayList<>(completedDates);
        Collections.sort(sortedAsc); // Sort cũ -> mới

        if (!sortedAsc.isEmpty()) {
            longestStreak = 1; // Ít nhất là 1 nếu list không rỗng
            for (int i = 0; i < sortedAsc.size() - 1; i++) {
                LocalDate current = sortedAsc.get(i);
                LocalDate next = sortedAsc.get(i + 1);

                if (current.plusDays(1).equals(next)) {
                    tempStreak++;
                } else {
                    tempStreak = 1; // Gãy chuỗi
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