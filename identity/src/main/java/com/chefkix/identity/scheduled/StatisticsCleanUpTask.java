package com.chefkix.identity.scheduled;

import com.chefkix.identity.entity.Statistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StatisticsCleanUpTask {

  private final MongoTemplate mongoTemplate;

  /**
   * Reset thống kê Tuần vào 00:00:00 sáng Thứ Hai hàng tuần. Cron: giây phút giờ ngày tháng thứ MON
   * = Thứ Hai
   */
  @Scheduled(cron = "0 0 0 * * MON")
  public void resetWeeklyStats() {
    log.info("Starting Weekly Stats Cleanup Job...");

    try {
      // 1. Query rỗng = Chọn tất cả bản ghi trong collection Statistics
      Query query = new Query();

      // 2. Các field cần reset về 0
      Update update =
          new Update()
              // Các chỉ số cho Leaderboard tuần
              .set("xpWeekly", 0)

              // Các chỉ số cho Creator Dashboard tuần
              .set("weeklyCreatorCooks", 0)
              .set("weeklyCreatorXp", 0);
      // .set("completionCountWeekly", 0); // Nếu có field này

      // 3. Thực thi update hàng loạt (Nhanh và hiệu quả)
      var result = mongoTemplate.updateMulti(query, update, Statistics.class);

      log.info("Weekly Cleanup Completed. Reset stats for {} users.", result.getModifiedCount());

    } catch (Exception e) {
      log.error("Failed to reset weekly stats: ", e);
      // Có thể thêm logic gửi alert (Slack/Email) ở đây nếu Job chết
    }
  }

  /** (Tùy chọn) Reset thống kê Tháng vào 00:00:00 ngày mùng 1 hàng tháng. */
  @Scheduled(cron = "0 0 0 1 * ?")
  public void resetMonthlyStats() {
    log.info("Starting Monthly Stats Cleanup Job...");

    Query query = new Query();
    Update update = new Update().set("xpMonthly", 0); // Field cho Leaderboard tháng

    var result = mongoTemplate.updateMulti(query, update, Statistics.class);
    log.info("Monthly Cleanup Completed. Reset stats for {} users.", result.getModifiedCount());
  }
}
