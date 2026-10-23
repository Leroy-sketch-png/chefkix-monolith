package com.chefkix.culinary.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chefkix.culinary.features.ai.dto.internal.AIModerationRequest;
import com.chefkix.shared.exception.AppException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AIRestClientModerationLoggingTest {

    @Test
    void moderationHttpErrorsDoNotWriteEchoedUserContentToLogs() throws IOException {
        String privateCaption = "private-story-caption-7fcf21";
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/api/v1/moderate",
                exchange -> {
                    byte[] response = ("{\"detail\":\"" + privateCaption + "\"}")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(503, response.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(response);
                    }
                });
        server.start();

        Logger logger = (Logger) LoggerFactory.getLogger(AIRestClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        try {
            AIRestClient client = new AIRestClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "");

            assertThatThrownBy(() -> client.moderateContent(AIModerationRequest.builder()
                            .content(privateCaption)
                            .contentType("post_caption")
                            .build()))
                    .isInstanceOf(AppException.class);

            String capturedLogs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(capturedLogs)
                    .contains("503")
                    .doesNotContain(privateCaption);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            server.stop(0);
        }
    }
}
