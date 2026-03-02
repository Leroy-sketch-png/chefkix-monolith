package com.chefkix.identity.service; // Hoặc package common của bạn

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BaseRedisService {

  private final StringRedisTemplate redisTemplate;

  /** 1. Set giá trị thường */
  public void set(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }

  /** 2. Set giá trị có TTL (Time To Live) Dùng cho: Lưu OTP, Lưu Cooldown */
  public void set(String key, String value, long timeoutInSeconds) {
    redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
  }

  /** 3. Lấy giá trị Dùng cho: Kiểm tra xem user đã spam bao nhiêu lần */
  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  /** 4. Kiểm tra tồn tại Dùng cho: Check xem đang bị Cooldown không */
  public boolean exists(String key) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  /** 5. Xóa key Dùng cho: Reset limit khi đăng ký lại, hoặc xóa OTP khi verify thành công */
  public void delete(String key) {
    redisTemplate.delete(key);
  }

  /**
   * 6. Tăng biến đếm (Atomic Increment) Dùng cho: Rate Limit (Đếm số lần request) Ví dụ: Gọi lần 1
   * -> lên 1, gọi lần 2 -> lên 2 (Kể cả khi nhiều request cùng lúc)
   */
  public void increment(String key) {
    redisTemplate.opsForValue().increment(key);
  }

  /**
   * 7. Set thời gian hết hạn cho key đang có Dùng cho: Gia hạn thời gian cho biến đếm Rate Limit
   */
  public void expire(String otpHourlyLimitKey, int timeoutInSeconds, TimeUnit timeUnit) {
    redisTemplate.expire(otpHourlyLimitKey, timeoutInSeconds, TimeUnit.SECONDS);
  }
}
