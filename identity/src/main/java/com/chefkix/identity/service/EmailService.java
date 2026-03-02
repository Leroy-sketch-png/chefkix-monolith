package com.chefkix.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailService {
  @Value("${app.otp.secret}")
  String otpSecret;

  @NonFinal
  @Value("${app.otp.ttl-seconds:600}")
  private long otpTtlSeconds;

  final JavaMailSender mailSender;

  @Async
  public void sendOtpEmail(String to, String otp) {
    // 1. Kiểm tra tính hợp lệ của địa chỉ email người nhận
    if (to == null || to.trim().isEmpty()) {
      log.error("Recipient email (to) is null or empty. Cannot send email.");
      // Ném ngoại lệ để dừng quá trình và thông báo lỗi rõ ràng hơn
      throw new IllegalArgumentException("Recipient email cannot be null or empty.");
    }

    try {
      SimpleMailMessage message = new SimpleMailMessage();

      // message.setTo(to) là nơi lỗi NullPointerException xảy ra
      // nếu 'to' là null và không được kiểm tra trước đó.
      message.setTo(to);

      message.setSubject("Your OTP Code");
      message.setText(
          "Your OTP is: " + otp + "\nThis code will expire in " + otpTtlSeconds / 60 + " minutes.");

      // Giả sử 'mailSender' đã được inject đúng cách
      mailSender.send(message);

      log.info("OTP email sent successfully to {}", to);

    } catch (Exception e) {
      // Bắt các lỗi gửi mail khác (ví dụ: lỗi kết nối SMTP, định dạng email không hợp lệ)
      log.error("Failed to send OTP email to {}", to, e);
      // Ném RuntimeException để nó được bắt và xử lý ở tầng trên (Controller/Service)
      throw new RuntimeException("Failed to send OTP email");
    }
  }

  public String generateOtpCode() {
    SecureRandom rnd = new SecureRandom();
    int code = rnd.nextInt(900_000) + 100_000; // 6-digit
    return String.valueOf(code);
  }

  String hmacOtp(String otp) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key =
          new SecretKeySpec(otpSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(key);
      byte[] raw = mac.doFinal(otp.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(raw);
    } catch (Exception e) {
      throw new RuntimeException("Failed to HMAC OTP", e);
    }
  }
}
