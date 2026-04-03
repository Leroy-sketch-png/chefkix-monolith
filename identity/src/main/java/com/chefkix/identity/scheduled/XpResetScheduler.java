package com.chefkix.identity.scheduled;

import com.chefkix.identity.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to reset weekly and monthly XP counters for leaderboard.
 *
 * <p>Per spec (04-statistics.txt): - xpWeekly: Reset weekly by scheduled job (Monday 00:00) -
 * xpMonthly: Reset monthly by scheduled job (1st of month 00:00)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XpResetScheduler {

  private final MongoTemplate mongoTemplate;

  /**
   * Reset weekly XP for all users every Monday at midnight. This is used for weekly leaderboard
   * rankings.
   */
  @Scheduled(cron = "0 0 0 * * MON") // Every Monday at 00:00
  public void resetWeeklyXp() {
    log.info("Resetting weekly XP for all users...");

    try {
      Query query = new Query(Criteria.where("statistics.xpWeekly").gt(0));
      Update update =
          new Update()
              .set("statistics.xpWeekly", 0.0)
              .set("statistics.weeklyCreatorXp", 0L)
              .set("statistics.weeklyCreatorCooks", 0L);

      var result = mongoTemplate.updateMulti(query, update, UserProfile.class);

      log.info("Weekly XP reset complete. Updated {} user profiles.", result.getModifiedCount());
    } catch (Exception e) {
      log.error("Failed to reset weekly XP", e);
    }
  }

  /**
   * Reset monthly XP for all users on the 1st of each month at midnight. This is used for monthly
   * leaderboard rankings.
   */
  @Scheduled(cron = "0 0 0 1 * *") // 1st of every month at 00:00
  public void resetMonthlyXp() {
    log.info("Resetting monthly XP for all users...");

    try {
      Query query = new Query(Criteria.where("statistics.xpMonthly").gt(0));
      Update update = new Update().set("statistics.xpMonthly", 0.0);

      var result = mongoTemplate.updateMulti(query, update, UserProfile.class);

      log.info("Monthly XP reset complete. Updated {} user profiles.", result.getModifiedCount());
    } catch (Exception e) {
      log.error("Failed to reset monthly XP", e);
    }
  }

}
