package com.chefkix.identity.service;

import com.chefkix.shared.event.EmailEvent;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.repository.SignupRequestRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.utils.RedisKeyUtils;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class SignupRequestService {

  final SignupRequestRepository signupRequestRepository;
  final UserProfileRepository userProfileRepository;
  final EmailService emailService;
  final KafkaTemplate<String, Object> kafkaTemplate;
  final BaseRedisService redisService;

  @Value("${app.otp.ttl-seconds:600}")
long otpTtlSeconds;

  @Value("${app.otp.max-resend-per-hour:3}")
  int maxResendPerHour;

  @Value("${app.otp.max-resend-per-day:10}")
  int maxResendPerDay;

  @Value("${app.otp.max-verify-attempts:5}")
  int maxVerifyAttempts;

  @Value("${app.otp.base-cooldown:60}")
  int baseCooldownSeconds;

  @Transactional
  public void register(SignupRequest request, String clientIp) {
    checkIpRateLimit(clientIp);

    if (userProfileRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn(
          "Registration attempt for existing verified email: {}", maskEmail(request.getEmail()));
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    if (userProfileRepository.findByUsername(request.getUsername()).isPresent()) {
      log.warn("Registration attempt for existing username: {}", request.getUsername());
      throw new AppException(ErrorCode.USER_EXISTED);
    }

    signupRequestRepository
        .findByEmail(request.getEmail())
        .ifPresent(
            existing -> {
              signupRequestRepository.delete(existing);
              redisService.delete(RedisKeyUtils.getOtpDailyLimitKey(request.getEmail()));
              log.info("Cleaned up existing signup request for {}", maskEmail(request.getEmail()));
            });

    SignupRequest req =
        SignupRequest.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .createdAt(Instant.now())

            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .fullName(buildFullName(request.getFirstName(), request.getLastName()))
            .phoneNumber(request.getPhoneNumber())
            .avatarUrl(request.getAvatarUrl())
            .bio(request.getBio())
            .displayName(request.getDisplayName())
            .dob(request.getDob())
            .accountType(request.getAccountType())
            .attempts(0)
            .location(request.getLocation())
            .preferences(request.getPreferences())
            .build();

    req = signupRequestRepository.save(req);

    String cooldownKey = RedisKeyUtils.getOtpCooldownKey(request.getEmail());
    redisService.set(cooldownKey, "1", baseCooldownSeconds);

    generateAndSendOtp(req);

    log.info("Registration initiated for IP: {}", clientIp);
  }

  @Transactional
  public void resendOtp(String email, String clientIp) {
    if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
      throw new AppException(ErrorCode.INVALID_EMAIL);
    }

    checkRateLimits(email, clientIp);

    SignupRequest req =
        signupRequestRepository
            .findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.SIGNUP_REQUEST_NOT_FOUND));

    invalidateOldOtp(req);

    generateAndSendOtp(req);

    incrementResendCounters(email, clientIp);
    setProgressiveCooldown(email);

    log.info("Resend OTP success for IP: {}", clientIp);
  }

  private void generateAndSendOtp(SignupRequest req) {
    String otp = emailService.generateOtpCode();
    String otpHash = emailService.hmacOtp(otp);

    req.setOtpHash(otpHash);
    req.setExpiresAt(Instant.now().plusSeconds(otpTtlSeconds));
    req.setLastOtpSentAt(Instant.now());

    signupRequestRepository.save(req);

    String name = req.getFullName() != null ? req.getFullName() : "Chef";

    String htmlContent =
        String.format(
            """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            .container { font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; max-width: 500px; }
            .header { color: #2c3e50; }
            .otp-box {
                background-color: #f8f9fa;
                padding: 15px;
                text-align: center;
                font-size: 24px;
                font-weight: bold;
                color: #d35400;
                letter-spacing: 5px;
                margin: 20px 0;
                border-radius: 5px;
            }
            .footer { font-size: 12px; color: #7f8c8d; margin-top: 20px; }
        </style>
    </head>
    <body>
        <div class="container">
            <h2 class="header">Chefkix Verification</h2>
            <p>Hello <strong>%s</strong>,</p>
            <p>You have requested to register an account at Chefkix. Here is your verification code:</p>

            <div class="otp-box">%s</div>

            <p>This code will expire in %d minutes. Do not share this code with anyone.</p>
            <div class="footer">
                Best regards,<br>The Chefkix Team
            </div>
        </div>
    </body>
    </html>
    """,
            name, otp, Math.max(1L, TimeUnit.SECONDS.toMinutes(otpTtlSeconds)));

    EmailEvent event =
        EmailEvent.builder()
            .recipientEmail(req.getEmail())
            .subject("Chefkix Verification Code")
            .body(htmlContent)
            .build();

    kafkaTemplate.send("otp-delivery", event);
    log.info("OTP Event published for {}", maskEmail(req.getEmail()));
  }


  private void checkIpRateLimit(String clientIp) {
    String ipKey = RedisKeyUtils.getOtpIpLimitKey(clientIp);
    Integer ipCount =
        redisService.get(ipKey) != null ? Integer.parseInt(redisService.get(ipKey)) : 0;

    if (ipCount >= maxResendPerHour * 2) {
      log.warn("Suspicious activity from IP: {}", clientIp);
      throw new AppException(ErrorCode.TOO_MANY_REQUESTS_FROM_IP);
    }
  }

  private void checkRateLimits(String email, String clientIp) {
    String cooldownKey = RedisKeyUtils.getOtpCooldownKey(email);
    if (redisService.exists(cooldownKey)) {
      String ttl = redisService.get(cooldownKey);
      throw new AppException(ErrorCode.OTP_RATE_LIMIT, "Please wait " + ttl + "s");
    }

    String hourlyKey = RedisKeyUtils.getOtpHourlyLimitKey(email);
    String dailyKey = RedisKeyUtils.getOtpDailyLimitKey(email);

    int hourlyCount = getRedisCount(hourlyKey);
    int dailyCount = getRedisCount(dailyKey);

    if (hourlyCount >= maxResendPerHour)
      throw new AppException(ErrorCode.OTP_HOURLY_LIMIT_EXCEEDED);
    if (dailyCount >= maxResendPerDay) throw new AppException(ErrorCode.OTP_DAILY_LIMIT_EXCEEDED);

    checkIpRateLimit(clientIp);
  }

  private void incrementResendCounters(String email, String clientIp) {
    redisService.increment(RedisKeyUtils.getOtpHourlyLimitKey(email));
    redisService.expire(RedisKeyUtils.getOtpHourlyLimitKey(email), 1, TimeUnit.HOURS);

    redisService.increment(RedisKeyUtils.getOtpDailyLimitKey(email));
    redisService.expire(RedisKeyUtils.getOtpDailyLimitKey(email), 24, TimeUnit.HOURS);

    String ipKey = RedisKeyUtils.getOtpIpLimitKey(clientIp);
    redisService.increment(ipKey);
    redisService.expire(ipKey, 1, TimeUnit.HOURS);
  }

  private void setProgressiveCooldown(String email) {
    String hourlyKey = RedisKeyUtils.getOtpHourlyLimitKey(email);
    int count = getRedisCount(hourlyKey);

    int cooldown = baseCooldownSeconds * (int) Math.pow(2, Math.max(0, count - 1));
cooldown = Math.min(cooldown, 900);

    redisService.set(RedisKeyUtils.getOtpCooldownKey(email), "1", cooldown);
  }

  private void invalidateOldOtp(SignupRequest req) {
    if (req.getOtpHash() != null) {
    }
  }

  private int getRedisCount(String key) {
    String val = redisService.get(key);
    return val == null ? 0 : Integer.parseInt(val);
  }

  private String buildFullName(String f, String l) {
    return (f == null ? "" : f) + " " + (l == null ? "" : l);
  }

  private String maskEmail(String email) {
    if (email == null || !email.contains("@")) return "***";
    int atIndex = email.indexOf("@");
    if (atIndex <= 2) return "***" + email.substring(atIndex);
    return email.substring(0, 2) + "***" + email.substring(atIndex);
  }
}
