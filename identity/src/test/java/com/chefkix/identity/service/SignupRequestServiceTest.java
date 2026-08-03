package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.dto.response.OtpDeliveryResponse;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.identity.repository.SignupRequestRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.utils.RedisKeyUtils;
import com.chefkix.shared.exception.AppException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SignupRequestServiceTest {

  private static final String EMAIL = "new-cook@example.com";
  private static final String IP = "203.0.113.20";

  @Mock SignupRequestRepository signupRequestRepository;
  @Mock UserProfileRepository userProfileRepository;
  @Mock EmailService emailService;
  @Mock KafkaTemplate<String, Object> kafkaTemplate;
  @Mock BaseRedisService redisService;

  private SignupRequestService service;

  @BeforeEach
  void setUp() {
    service =
        new SignupRequestService(
            signupRequestRepository,
            userProfileRepository,
            emailService,
            kafkaTemplate,
            redisService);
    ReflectionTestUtils.setField(service, "otpTtlSeconds", 600L);
    ReflectionTestUtils.setField(service, "maxResendPerHour", 3);
    ReflectionTestUtils.setField(service, "maxResendPerDay", 10);
    ReflectionTestUtils.setField(service, "maxVerifyAttempts", 5);
    ReflectionTestUtils.setField(service, "baseCooldownSeconds", 60);
  }

  @Test
  void registerReturnsServerIssuedExpiryAndCooldownTimestamps() {
    stubOtpDelivery();
    when(userProfileRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userProfileRepository.findByUsername("newcook")).thenReturn(Optional.empty());
    when(signupRequestRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    Instant before = Instant.now();

    OtpDeliveryResponse result = service.register(signupRequest(), IP);

    assertThat(result.getExpiresAt()).isBetween(before.plusSeconds(599), Instant.now().plusSeconds(601));
    assertThat(result.getResendAvailableAt())
        .isBetween(before.plusSeconds(59), Instant.now().plusSeconds(61));
    verify(redisService).set(RedisKeyUtils.getOtpCooldownKey(EMAIL), "1", 60);
  }

  @Test
  void resendReturnsTheProgressiveCooldownAndKeepsHourUnits() {
    stubOtpDelivery();
    SignupRequest request = signupRequest();
    when(signupRequestRepository.findByEmail(EMAIL)).thenReturn(Optional.of(request));
    when(redisService.get(RedisKeyUtils.getOtpHourlyLimitKey(EMAIL))).thenReturn("2");
    Instant before = Instant.now();

    OtpDeliveryResponse result = service.resendOtp(EMAIL, IP);

    assertThat(result.getExpiresAt()).isAfter(before.plusSeconds(599));
    assertThat(result.getResendAvailableAt()).isAfter(before.plusSeconds(119));
    verify(redisService).set(RedisKeyUtils.getOtpCooldownKey(EMAIL), "1", 120);
    verify(redisService).expire(RedisKeyUtils.getOtpHourlyLimitKey(EMAIL), 1, TimeUnit.HOURS);
    verify(redisService).expire(RedisKeyUtils.getOtpDailyLimitKey(EMAIL), 24, TimeUnit.HOURS);
    verify(redisService).expire(RedisKeyUtils.getOtpIpLimitKey(IP), 1, TimeUnit.HOURS);
  }

  @Test
  void cooldownFailureReportsTheActualRemainingTtl() {
    String cooldownKey = RedisKeyUtils.getOtpCooldownKey(EMAIL);
    when(redisService.exists(cooldownKey)).thenReturn(true);
    when(redisService.getExpireSeconds(cooldownKey)).thenReturn(42L);

    assertThatThrownBy(() -> service.resendOtp(EMAIL, IP))
        .isInstanceOf(AppException.class)
        .hasMessage("Please wait 42s");
  }

  private SignupRequest signupRequest() {
    return SignupRequest.builder()
        .email(EMAIL)
        .username("newcook")
        .firstName("New")
        .lastName("Cook")
        .build();
  }

  private void stubOtpDelivery() {
    when(emailService.generateOtpCode()).thenReturn("123456");
    when(emailService.hmacOtp("123456")).thenReturn("otp-hash");
    when(signupRequestRepository.save(any(SignupRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }
}
