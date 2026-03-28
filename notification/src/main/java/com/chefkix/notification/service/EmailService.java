package com.chefkix.notification.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.chefkix.notification.dto.request.EmailRequest;
import com.chefkix.notification.dto.request.SendEmailRequest;
import com.chefkix.notification.dto.request.Sender;
import com.chefkix.notification.dto.response.EmailResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service("notificationEmailService")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

    BrevoEmailClient brevoEmailClient;

    private static final Pattern OTP_PATTERN = Pattern.compile(
            "otp-box[^>]*>\\s*(\\d{4,8})\\s*<",
            Pattern.CASE_INSENSITIVE);

    public EmailResponse sendEmail(SendEmailRequest request) {
        log.info("Preparing to send email to: {}", request.getTo().getEmail());

        // DEV BYPASS: When Brevo is not configured, print the OTP to the console.
        // This prevents infinite Kafka retries and lets developers test locally
        // without a real Brevo API key.
        if (!brevoEmailClient.isConfigured()) {
            log.warn("=========================================================");
            log.warn("[DEV EMAIL BYPASS] Brevo not configured — email NOT sent.");
            log.warn("[DEV EMAIL BYPASS] Recipient : {}", request.getTo().getEmail());
            log.warn("[DEV EMAIL BYPASS] Subject   : {}", request.getSubject());
            String extracted = extractCode(request.getHtmlContent());
            if (extracted != null) {
                log.warn("[DEV EMAIL BYPASS] *** OTP CODE: {} ***", extracted);
            }
            log.warn("=========================================================");
            log.debug("[DEV EMAIL BYPASS] Full HTML body:\n{}", request.getHtmlContent());
            return EmailResponse.builder().messageId("dev-bypass-no-brevo").build();
        }

        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("Chefkix")
                        .email("phanphutho9999@gmail.com")
                        .build())
                .to(List.of(request.getTo()))
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .build();
        try {
            EmailResponse response = brevoEmailClient.sendEmail(emailRequest);
            log.info("Email sent successfully via Brevo: {}", response);
            return response;
        } catch (RestClientException e) {
            log.error("Error sending email via Brevo: {}", e.getMessage());
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }

    /** Extracts numeric OTP/verification code from HTML content. */
    private String extractCode(String html) {
        if (html == null) return null;
        Matcher m = OTP_PATTERN.matcher(html);
        return m.find() ? m.group(1) : null;
    }
}
