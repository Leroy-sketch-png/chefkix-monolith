package com.chefkix.identity.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OTP generation and HMAC utilities for the identity module.
 * <p>
 * Email delivery is handled by the notification module via Kafka (otp-delivery topic).
 * This service provides cryptographic helpers only.
 */
@Service("identityEmailService")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailService {

  @Value("${app.otp.secret}")
  String otpSecret;

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
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}
