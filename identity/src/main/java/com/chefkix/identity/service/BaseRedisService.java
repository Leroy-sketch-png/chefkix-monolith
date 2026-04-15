package com.chefkix.identity.service; // Or your common package

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BaseRedisService {

  private final StringRedisTemplate redisTemplate;

  /** 1. Set a regular value */
  public void set(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }

  /** 2. Set a value with TTL (Time To Live). Used for: Storing OTP, Storing Cooldown */
  public void set(String key, String value, long timeoutInSeconds) {
    redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
  }

  /** 3. Get a value. Used for: Checking how many times a user has spammed */
  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  /** 4. Check existence. Used for: Checking if currently in Cooldown */
  public boolean exists(String key) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  /** 5. Delete key. Used for: Resetting limit on re-registration, or deleting OTP on successful verification */
  public void delete(String key) {
    redisTemplate.delete(key);
  }

  /**
   * 6. Atomic Increment. Used for: Rate Limit (Counting number of requests).
   * Example: Call 1 -> goes to 1, call 2 -> goes to 2 (even with concurrent requests)
   */
  public void increment(String key) {
    redisTemplate.opsForValue().increment(key);
  }

  /**
   * 7. Set expiration time for an existing key. Used for: Extending the time for the Rate Limit counter
   */
  public void expire(String otpHourlyLimitKey, int timeoutInSeconds, TimeUnit timeUnit) {
    redisTemplate.expire(otpHourlyLimitKey, timeoutInSeconds, TimeUnit.SECONDS);
  }
}
