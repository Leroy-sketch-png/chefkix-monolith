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

  // --- CONFIGS (Should be in application.yml) ---
  @Value("${app.otp.ttl-seconds:600}")
  long otpTtlSeconds; // 10 minutes

  @Value("${app.otp.max-resend-per-hour:3}")
  int maxResendPerHour;

  @Value("${app.otp.max-resend-per-day:10}")
  int maxResendPerDay;

  @Value("${app.otp.max-verify-attempts:5}")
  int maxVerifyAttempts;

  @Value("${app.otp.base-cooldown:60}")
  int baseCooldownSeconds;

  // =========================================================================
  // 1. PUBLIC: REGISTER (NEW SIGNUP)
  // =========================================================================
  @Transactional
  public void register(SignupRequest request, String clientIp) {
    // 1. Security: Block IP spam from the registration step
    checkIpRateLimit(clientIp);

    // 2. CRITICAL: Check if a verified user already exists with this email
    if (userProfileRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn(
          "Registration attempt for existing verified email: {}", maskEmail(request.getEmail()));
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    // 3. Check if username already exists (verified user)
    if (userProfileRepository.findByUsername(request.getUsername()).isPresent()) {
      log.warn("Registration attempt for existing username: {}", request.getUsername());
      throw new AppException(ErrorCode.USER_EXISTED);
    }

    // 4. Clean up old request (If user re-registers but hasn't verified)
    signupRequestRepository
        .findByEmail(request.getEmail())
        .ifPresent(
            existing -> {
              signupRequestRepository.delete(existing);
              // Reset daily limit for this email to prevent user from being stuck if they spammed before
              redisService.delete(RedisKeyUtils.getOtpDailyLimitKey(request.getEmail()));
              log.info("Cleaned up existing signup request for {}", maskEmail(request.getEmail()));
            });

    // 5. Map Entity & Encode Password immediately
    SignupRequest req =
        SignupRequest.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .password(request.getPassword())
            .createdAt(Instant.now())

            // map profile info
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

    // 4. Save temporarily to DB
    req = signupRequestRepository.save(req);

    // 5. SECURITY: Set Cooldown immediately (60s)
    // To prevent user from repeatedly pressing Resend right after registering
    String cooldownKey = RedisKeyUtils.getOtpCooldownKey(request.getEmail());
    redisService.set(cooldownKey, "1", baseCooldownSeconds);

    // 6. Call shared OTP processing function
    generateAndSendOtp(req);

    log.info("Registration initiated for IP: {}", clientIp);
  }

  // =========================================================================
  // 2. PUBLIC: RESEND OTP
  // =========================================================================
  @Transactional
  public void resendOtp(String email, String clientIp) {
    // 1. Validate Email Format
    if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
      throw new AppException(ErrorCode.INVALID_EMAIL);
    }

    // 2. SECURITY: Check Rate Limits (Multi-layer Redis)
    checkRateLimits(email, clientIp);

    // 3. Get existing Entity
    SignupRequest req =
        signupRequestRepository
            .findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.SIGNUP_REQUEST_NOT_FOUND));

    // 4. SECURITY: Invalidate old OTP (Prevent Race Condition)
    invalidateOldOtp(req);

    // 5. Call shared OTP processing function
    generateAndSendOtp(req);

    // 6. SECURITY: Increment counters & Set progressive Cooldown
    incrementResendCounters(email, clientIp);
    setProgressiveCooldown(email);

    log.info("Resend OTP success for IP: {}", clientIp);
  }

  // =========================================================================
  // 3. PRIVATE CORE: SHARED LOGIC (DRY & SECURE)
  // =========================================================================
  private void generateAndSendOtp(SignupRequest req) {
    // A. Generate OTP & Hash
    String otp = emailService.generateOtpCode();
    String otpHash = emailService.hmacOtp(otp);

    // B. Update Entity
    req.setOtpHash(otpHash);
    req.setExpiresAt(Instant.now().plusSeconds(otpTtlSeconds));
    req.setLastOtpSentAt(Instant.now());

    signupRequestRepository.save(req);

    // C. Create HTML content (Hardcoded at Producer)
    // Note: Ensure req.getFullName() is not null
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

    // D. Send via Kafka
    EmailEvent event =
        EmailEvent.builder()
            .recipientEmail(req.getEmail())
            .subject("Chefkix Verification Code")
            .body(htmlContent)
            .build();

    kafkaTemplate.send("otp-delivery", event);
    log.info("OTP Event published for {}", maskEmail(req.getEmail()));
  }

  // =========================================================================
  // 4. HELPERS: SECURITY & UTILS
  // =========================================================================

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
    // Layer 1: Cooldown
    String cooldownKey = RedisKeyUtils.getOtpCooldownKey(email);
    if (redisService.exists(cooldownKey)) {
      String ttl = redisService.get(cooldownKey);
      throw new AppException(ErrorCode.OTP_RATE_LIMIT, "Please wait " + ttl + "s");
    }

    // Layer 2: Limits
    String hourlyKey = RedisKeyUtils.getOtpHourlyLimitKey(email);
    String dailyKey = RedisKeyUtils.getOtpDailyLimitKey(email);

    int hourlyCount = getRedisCount(hourlyKey);
    int dailyCount = getRedisCount(dailyKey);

    if (hourlyCount >= maxResendPerHour)
      throw new AppException(ErrorCode.OTP_HOURLY_LIMIT_EXCEEDED);
    if (dailyCount >= maxResendPerDay) throw new AppException(ErrorCode.OTP_DAILY_LIMIT_EXCEEDED);

    // Layer 3: IP Check
    checkIpRateLimit(clientIp);
  }

  private void incrementResendCounters(String email, String clientIp) {
    // Increment User Counter
    redisService.increment(RedisKeyUtils.getOtpHourlyLimitKey(email));
    redisService.expire(RedisKeyUtils.getOtpHourlyLimitKey(email), 1, TimeUnit.HOURS);

    redisService.increment(RedisKeyUtils.getOtpDailyLimitKey(email));
    redisService.expire(RedisKeyUtils.getOtpDailyLimitKey(email), 24, TimeUnit.HOURS);

    // Increment IP Counter
    String ipKey = RedisKeyUtils.getOtpIpLimitKey(clientIp);
    redisService.increment(ipKey);
    redisService.expire(ipKey, 1, TimeUnit.HOURS);
  }

  private void setProgressiveCooldown(String email) {
    String hourlyKey = RedisKeyUtils.getOtpHourlyLimitKey(email);
    int count = getRedisCount(hourlyKey);

    // Logic: 60s, 120s, 240s...
    int cooldown = baseCooldownSeconds * (int) Math.pow(2, Math.max(0, count - 1));
    cooldown = Math.min(cooldown, 900); // Max 15 minutes

    redisService.set(RedisKeyUtils.getOtpCooldownKey(email), "1", cooldown);
  }

  private void invalidateOldOtp(SignupRequest req) {
    if (req.getOtpHash() != null) {
      // Store in Redis Blacklist (Optional, or just updating to a new hash is sufficient)
      // Since we use Hash in DB, when saving a new OTP, the old OTP is automatically invalidated.
      // This function can be used for logging or stricter old OTP blocking logic if needed.
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
