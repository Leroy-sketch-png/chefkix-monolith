package com.chefkix.culinary.features.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.culinary.features.ai.dto.internal.AIModerationRequest;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

class ContentModerationProviderLoggingTest {

    @Test
    void providerDoesNotLogExceptionTextThatMayContainSubmittedContent() {
        String privateCaption = "private-story-caption-cb83a4";
        AIRestClient aiRestClient = Mockito.mock(AIRestClient.class);
        when(aiRestClient.moderateContent(any(AIModerationRequest.class)))
                .thenThrow(new IllegalStateException(privateCaption));

        Logger logger = (Logger) LoggerFactory.getLogger(ContentModerationProviderImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        try {
            var result = new ContentModerationProviderImpl(aiRestClient)
                    .moderate(privateCaption, "post_caption");

            assertThat(result.action()).isEqualTo("flag");
            String capturedLogs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(capturedLogs)
                    .contains("IllegalStateException")
                    .doesNotContain(privateCaption);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
