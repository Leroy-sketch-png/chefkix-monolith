package com.chefkix.identity.service;

import com.chefkix.shared.event.EmailEvent;
import com.chefkix.identity.entity.ResetPasswordRequest;
import com.chefkix.identity.repository.ResetPasswordRepository;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResetPasswordService {
  SignupRequestService signupRequestService;
  EmailService emailService;
  ResetPasswordRepository resetPasswordRepository;
  KafkaTemplate<String, EmailEvent> kafkaTemplate;

  @NonFinal
  @Value("${app.otp.ttl-seconds:600}")
  private long otpTtlSeconds;

  @Transactional
  public void sendForgotPasswordOtp(String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email cannot be null or empty");
    }

    String otp = emailService.generateOtpCode();
    String otpHash = emailService.hmacOtp(otp);
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(otpTtlSeconds);

    resetPasswordRepository.findByEmail(email).ifPresent(resetPasswordRepository::delete);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setOtpHash(otpHash);
    request.setExpiresAt(expiresAt);
    request.setEmail(email);
    request.setCreatedAt(now);

    resetPasswordRepository.save(request);

    EmailEvent notificationEmailEvent =
        EmailEvent.builder()
            .recipientEmail(request.getEmail())
            .subject("Reset password request")
            .body("Your password reset code is: " + otp)
            .build();

    // Publish message to kafka
    log.info("Sending notification event to Kafka: {}", notificationEmailEvent);
    kafkaTemplate
        .send("otp-delivery", notificationEmailEvent)
        .whenComplete(
            (res, ex) -> {
              if (ex != null) log.error("Failed to send Kafka message", ex);
              else
                log.info(
                    "Kafka message sent successfully to topic {}", res.getRecordMetadata().topic());
            });
  }
}
