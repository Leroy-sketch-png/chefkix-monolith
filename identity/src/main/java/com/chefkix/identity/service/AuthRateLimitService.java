package com.chefkix.identity.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting guard for auth-sensitive endpoints.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthRateLimitService {

  StringRedisTemplate redisTemplate;

  // Login brute-force guard: max 8 attempts per 15 minutes per IP.
  static final int LOGIN_MAX_ATTEMPTS = 8;
  static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

  // Forgot password spam guard: max 5 requests per hour per IP+email.
  static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 5;
  static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofHours(1);

  public void assertLoginAllowed(String clientIp) {
    String key = "auth:login:ip:" + normalize(clientIp);
    long attempts = incrementWithTtl(key, LOGIN_WINDOW);
    if (attempts > LOGIN_MAX_ATTEMPTS) {
      throw new AppException(ErrorCode.TOO_MANY_REQUESTS_FROM_IP);
    }
  }

  public void clearLoginAttempts(String clientIp) {
    redisTemplate.delete("auth:login:ip:" + normalize(clientIp));
  }

  public void assertForgotPasswordAllowed(String clientIp, String email) {
    String key = "auth:forgot-password:ip-email:" + normalize(clientIp) + ":" + normalize(email);
    long attempts = incrementWithTtl(key, FORGOT_PASSWORD_WINDOW);
    if (attempts > FORGOT_PASSWORD_MAX_ATTEMPTS) {
      throw new AppException(ErrorCode.TOO_MANY_REQUESTS_FROM_IP);
    }
  }

  private long incrementWithTtl(String key, Duration ttl) {
    Long value = redisTemplate.opsForValue().increment(key);
    if (value != null && value == 1L) {
      redisTemplate.expire(key, ttl);
    }
    return value == null ? 0L : value;
  }

  private String normalize(String value) {
    if (value == null) {
      return "unknown";
    }
    return value.trim().toLowerCase();
  }
}
