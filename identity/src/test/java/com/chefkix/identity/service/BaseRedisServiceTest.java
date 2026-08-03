package com.chefkix.identity.service;

import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class BaseRedisServiceTest {

  @Mock StringRedisTemplate redisTemplate;

  @Test
  void expirePreservesTheRequestedTimeUnit() {
    BaseRedisService service = new BaseRedisService(redisTemplate);

    service.expire("otp:hourly", 1, TimeUnit.HOURS);

    verify(redisTemplate).expire("otp:hourly", 1, TimeUnit.HOURS);
  }

  @Test
  void getExpireSecondsReturnsTheRedisTtl() {
    org.mockito.Mockito.when(redisTemplate.getExpire("otp:cooldown", TimeUnit.SECONDS))
        .thenReturn(42L);
    BaseRedisService service = new BaseRedisService(redisTemplate);

    org.assertj.core.api.Assertions.assertThat(service.getExpireSeconds("otp:cooldown"))
        .isEqualTo(42L);
  }
}
