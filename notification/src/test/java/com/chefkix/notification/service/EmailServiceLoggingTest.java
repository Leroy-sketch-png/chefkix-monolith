package com.chefkix.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chefkix.notification.dto.request.Recipient;
import com.chefkix.notification.dto.request.SendEmailRequest;
import com.chefkix.notification.dto.response.EmailResponse;
import com.chefkix.shared.exception.AppException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;

class EmailServiceLoggingTest {

    @Test
    void emailLogsDoNotIncludeRecipientOtpBodyProviderErrorOrResponseId() {
        String recipient = "private-recipient-71@example.test";
        String subject = "private-email-subject-71";
        String otp = "847261";
        String body = "<p>private-email-body-71</p><span class=\"otp-box\">847261</span>";
        String providerError = "private-provider-error-71";
        String responseId = "private-response-id-71";

        BrevoEmailClient client = org.mockito.Mockito.mock(BrevoEmailClient.class);
        when(client.isConfigured()).thenReturn(false, true, true);
        when(client.sendEmail(any()))
                .thenThrow(new ResourceAccessException(providerError))
                .thenReturn(EmailResponse.builder().messageId(responseId).build());

        Logger logger = (Logger) LoggerFactory.getLogger(EmailService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        try {
            EmailService service = new EmailService(client);
            SendEmailRequest request = SendEmailRequest.builder()
                    .to(Recipient.builder().email(recipient).build())
                    .subject(subject)
                    .htmlContent(body)
                    .build();

            service.sendEmail(request);
            assertThatThrownBy(() -> service.sendEmail(request)).isInstanceOf(AppException.class);
            service.sendEmail(request);

            String capturedLogs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(capturedLogs)
                    .contains("ResourceAccessException")
                    .doesNotContain(recipient, subject, otp, body, providerError, responseId);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
