package com.chefkix.identity.utils; // Hoặc package common của bạn

public class RedisKeyUtils {

  // Prefix chung để phân biệt với các service khác (Post, Chat...)
  private static final String SERVICE_PREFIX = "chefkix:identity";

  /** 1. Key Cooldown (Chờ 60s - 2p - 4p...) Ví dụ: chefkix:identity:otp:cooldown:tinvo@gmail.com */
  public static String getOtpCooldownKey(String email) {
    return String.format("%s:otp:cooldown:%s", SERVICE_PREFIX, email);
  }

  /** 2. Key đếm số lần gửi trong 1 GIỜ Ví dụ: chefkix:identity:otp:limit:hourly:tinvo@gmail.com */
  public static String getOtpHourlyLimitKey(String email) {
    return String.format("%s:otp:limit:hourly:%s", SERVICE_PREFIX, email);
  }

  /** 3. Key đếm số lần gửi trong 1 NGÀY Ví dụ: chefkix:identity:otp:limit:daily:tinvo@gmail.com */
  public static String getOtpDailyLimitKey(String email) {
    return String.format("%s:otp:limit:daily:%s", SERVICE_PREFIX, email);
  }

  /**
   * 4. Key chặn theo địa chỉ IP (Chống spam tool) Ví dụ: chefkix:identity:otp:limit:ip:192.168.1.1
   */
  public static String getOtpIpLimitKey(String ipAddress) {
    return String.format("%s:otp:limit:ip:%s", SERVICE_PREFIX, ipAddress);
  }

  /**
   * 5. (Optional) Key lưu OTP tạm (Nếu cần debug hoặc flow cũ) Ví dụ:
   * chefkix:identity:otp:code:tinvo@gmail.com
   */
  public static String getOtpEmailKey(String email) {
    return String.format("%s:otp:code:%s", SERVICE_PREFIX, email);
  }
}
