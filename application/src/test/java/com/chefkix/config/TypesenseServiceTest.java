package com.chefkix.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class TypesenseServiceTest {

    TypesenseService service;
    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://typesense.test");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new TypesenseService(builder.build(), new ObjectMapper());
    }

    @Test
    void primarySearchExposesInfrastructureFailure() {
        server.expect(requestTo("http://typesense.test/multi_search"))
                .andRespond(withServerError());

        AppException error = assertThrows(
                AppException.class,
                () -> service.search("recipes", Map.of("q", "chicken")));

        assertEquals(ErrorCode.SEARCH_SERVICE_UNAVAILABLE, error.getErrorCode());
        server.verify();
    }

    @Test
    void optionalSearchDegradesToNoSuggestions() {
        server.expect(requestTo("http://typesense.test/multi_search"))
                .andRespond(withServerError());

        Map<String, Object> result =
                service.searchOrEmpty("recipes", Map.of("q", "chicken"));

        assertEquals(0, result.get("found"));
        assertEquals(List.of(), result.get("hits"));
        server.verify();
    }

    @Test
    void hybridPrimarySearchExposesInfrastructureFailure() {
        server.expect(requestTo("http://typesense.test/multi_search"))
                .andRespond(withServerError());

        AppException error = assertThrows(
                AppException.class,
                () -> service.hybridSearch(
                        "recipes", "quick chicken dinner", "title,description", new float[] {0.5f}, 10));

        assertEquals(ErrorCode.SEARCH_SERVICE_UNAVAILABLE, error.getErrorCode());
        server.verify();
    }

    @Test
    void ensureCollectionFieldIsIdempotentWhenFieldExists() {
        server.expect(requestTo("http://typesense.test/collections/recipes"))
                .andRespond(withSuccess(
                        "{\"fields\":[{\"name\":\"qualityTier\",\"type\":\"string\"}]}",
                        MediaType.APPLICATION_JSON));

        assertTrue(service.ensureCollectionField(
                "recipes", Map.of("name", "qualityTier", "type", "string")));

        server.verify();
    }

    @Test
    void ensureCollectionFieldAddsMissingFieldOnce() {
        server.expect(requestTo("http://typesense.test/collections/recipes"))
                .andRespond(withSuccess("{\"fields\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://typesense.test/collections/recipes"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess());

        assertTrue(service.ensureCollectionField(
                "recipes", Map.of("name", "qualityTier", "type", "string")));

        server.verify();
    }
}
