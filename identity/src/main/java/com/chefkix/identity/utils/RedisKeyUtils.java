package com.chefkix.identity.utils;

public class RedisKeyUtils {

  // Common prefix to distinguish from other services (Post, Chat...)
  private static final String SERVICE_PREFIX = "chefkix:identity";

  /** 1. Cooldown key (wait 60s - 2m - 4m...) Example: chefkix:identity:otp:cooldown:tinvo@gmail.com */
  public static String getOtpCooldownKey(String email) {
    return String.format("%s:otp:cooldown:%s", SERVICE_PREFIX, email);
  }

  /** 2. Hourly send count key. Example: chefkix:identity:otp:limit:hourly:tinvo@gmail.com */
  public static String getOtpHourlyLimitKey(String email) {
    return String.format("%s:otp:limit:hourly:%s", SERVICE_PREFIX, email);
  }

  /** 3. Daily send count key. Example: chefkix:identity:otp:limit:daily:tinvo@gmail.com */
  public static String getOtpDailyLimitKey(String email) {
    return String.format("%s:otp:limit:daily:%s", SERVICE_PREFIX, email);
  }

  /**
   * 4. IP-based rate limit key (anti-spam). Example: chefkix:identity:otp:limit:ip:192.168.1.1
   */
  public static String getOtpIpLimitKey(String ipAddress) {
    return String.format("%s:otp:limit:ip:%s", SERVICE_PREFIX, ipAddress);
  }

  /**
   * 5. (Optional) Temporary OTP storage key (for debug or legacy flow). Example:
   * chefkix:identity:otp:code:tinvo@gmail.com
   */
  public static String getOtpEmailKey(String email) {
    return String.format("%s:otp:code:%s", SERVICE_PREFIX, email);
  }
}
