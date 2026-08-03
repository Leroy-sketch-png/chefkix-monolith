package com.chefkix.identity.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BaseRedisService {

  private final StringRedisTemplate redisTemplate;

  public void set(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }

  public void set(String key, String value, long timeoutInSeconds) {
    redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
  }

  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public boolean exists(String key) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  public void delete(String key) {
    redisTemplate.delete(key);
  }

  public void increment(String key) {
    redisTemplate.opsForValue().increment(key);
  }

  public void expire(String key, long timeout, TimeUnit timeUnit) {
    redisTemplate.expire(key, timeout, timeUnit);
  }

  public long getExpireSeconds(String key) {
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    return ttl == null ? -2L : ttl;
  }
}
