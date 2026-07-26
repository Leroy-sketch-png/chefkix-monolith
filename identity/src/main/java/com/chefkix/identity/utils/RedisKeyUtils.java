package com.chefkix.identity.utils;

public class RedisKeyUtils {

  private static final String SERVICE_PREFIX = "chefkix:identity";

  public static String getOtpCooldownKey(String email) {
    return String.format("%s:otp:cooldown:%s", SERVICE_PREFIX, email);
  }

  public static String getOtpHourlyLimitKey(String email) {
    return String.format("%s:otp:limit:hourly:%s", SERVICE_PREFIX, email);
  }

  public static String getOtpDailyLimitKey(String email) {
    return String.format("%s:otp:limit:daily:%s", SERVICE_PREFIX, email);
  }

  /**
   */
  public static String getOtpIpLimitKey(String ipAddress) {
    return String.format("%s:otp:limit:ip:%s", SERVICE_PREFIX, ipAddress);
  }

  /**
   */
  public static String getOtpEmailKey(String email) {
    return String.format("%s:otp:code:%s", SERVICE_PREFIX, email);
  }
}
