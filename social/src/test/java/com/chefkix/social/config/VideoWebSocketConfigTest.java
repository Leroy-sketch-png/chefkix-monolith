package com.chefkix.social.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class VideoWebSocketConfigTest {

    @Test
    void extractsStandardAuthorizationBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("jwt.header.payload");

        String token = VideoWebSocketConfig.extractBearerToken(headers);

        assertThat(token).isEqualTo("jwt.header.payload");
    }

    @Test
    void extractsBrowserBearerTokenFromWebSocketSubprotocol() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", "chefkix.video, bearer.jwt.header.payload");

        String token = VideoWebSocketConfig.extractBearerToken(headers);

        assertThat(token).isEqualTo("jwt.header.payload");
    }

    @Test
    void rejectsRequestsWithoutHeaderOrProtocolToken() {
        HttpHeaders headers = new HttpHeaders();

        String token = VideoWebSocketConfig.extractBearerToken(headers);

        assertThat(token).isNull();
    }

    @Test
    void ignoresProtocolWithoutBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", VideoWebSocketConfig.VIDEO_SIGNALING_PROTOCOL);

        String token = VideoWebSocketConfig.extractBearerToken(headers);

        assertThat(token).isNull();
    }
}
