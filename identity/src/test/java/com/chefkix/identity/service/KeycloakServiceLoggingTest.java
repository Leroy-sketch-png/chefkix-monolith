package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chefkix.shared.exception.AppException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class KeycloakServiceLoggingTest {

    @Test
    void loginDoesNotLogEmailOrKeycloakErrorBody() throws IOException {
        String email = "private-keycloak-user-42@example.test";
        String password = "private-keycloak-password-42";
        String errorBody = "private-keycloak-response-42";
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/realms/nottisn/protocol/openid-connect/token",
                exchange -> {
                    byte[] response = ("{\"error_description\":\"" + errorBody + "\"}")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(401, response.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(response);
                    }
                });
        server.start();

        Logger logger = (Logger) LoggerFactory.getLogger(KeycloakService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        try {
            KeycloakService service = new KeycloakService(
                    WebClient.builder(), new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "clientSecret", "client-secret-test");

            assertThatThrownBy(() -> service.login(email, password))
                    .isInstanceOf(AppException.class);

            String capturedLogs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(capturedLogs)
                    .contains("AppException")
                    .doesNotContain(email, password, errorBody, "client-secret-test");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            server.stop(0);
        }
    }
}
