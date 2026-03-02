package com.chefkix.notification.service;

import java.util.List;

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

    public EmailResponse sendEmail(SendEmailRequest request) {
        log.info("Preparing to send email to: {}", request.getTo().getEmail());
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("Chefkix DotCom")
                        .email("tinvo2005@gmail.com")
                        .build())
                .to(List.of(request.getTo()))
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .build();
        try {
            EmailResponse response = brevoEmailClient.sendEmail(emailRequest);
            log.info("✅ Email sent successfully via Brevo: {}", response);
            return response;
        } catch (RestClientException e) {
            log.error("❌ Error sending email via Brevo: {}", e.getMessage());
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }
}
