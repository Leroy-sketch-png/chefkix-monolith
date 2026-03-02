package com.chefkix.notification.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.chefkix.notification.dto.request.Recipient;
import com.chefkix.notification.dto.request.SendEmailRequest;
import com.chefkix.notification.service.EmailService;
import com.chefkix.shared.event.EmailEvent;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka listener for OTP/email delivery events.
 * Consumes flat EmailEvent messages and delegates to EmailService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailNotificationListener {

    EmailService emailService;

    @KafkaListener(
            topics = "otp-delivery",
            groupId = "notification-group",
            containerFactory = "emailEventListenerFactory")
    public void listenOtpDelivery(EmailEvent message) {
        log.info("Received OTP email event for: {}", message.getRecipientEmail());

        // Defensive: validate before sending
        if (message.getRecipientEmail() == null || message.getRecipientEmail().isBlank()) {
            log.error("❌ Cannot send email: recipientEmail is null or blank. Event: {}", message);
            return;
        }

        emailService.sendEmail(SendEmailRequest.builder()
                .to(Recipient.builder().email(message.getRecipientEmail()).build())
                .subject(message.getSubject())
                .htmlContent(message.getBody())
                .build());
    }
}
