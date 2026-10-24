package com.chefkix.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chefkix.notification.service.EmailService;
import com.chefkix.shared.event.EmailEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class EmailNotificationListenerLoggingTest {

    @Test
    void otpDeliveryLogsDoNotIncludeRecipientSubjectOrBody() {
        String recipient = "private-recipient-82@example.test";
        String subject = "private-email-subject-82";
        String body = "private-email-body-82 otp=731904";
        EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
        KafkaIdempotencyService idempotencyService =
                org.mockito.Mockito.mock(KafkaIdempotencyService.class);
        when(idempotencyService.tryProcess(any())).thenReturn(true);

        Logger logger = (Logger) LoggerFactory.getLogger(EmailNotificationListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        try {
            EmailNotificationListener listener =
                    new EmailNotificationListener(emailService, idempotencyService);
            listener.listenOtpDelivery(EmailEvent.builder()
                    .eventId("event-82")
                    .recipientEmail(recipient)
                    .subject(subject)
                    .body(body)
                    .build());

            verify(emailService).sendEmail(any());
            String capturedLogs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(capturedLogs).doesNotContain(recipient, subject, body, "731904");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
