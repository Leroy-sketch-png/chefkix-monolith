package com.chefkix.culinary.common.client;

import com.chefkix.culinary.features.ai.dto.internal.AIMetaRequest;
import com.chefkix.culinary.features.ai.dto.internal.AIMetaResponse;
import com.chefkix.culinary.features.ai.dto.internal.AIProcessRequest;
import com.chefkix.culinary.features.ai.dto.internal.AIProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient-based replacement for the old AIClient Feign interface.
 * Calls the external Python FastAPI AI service.
 */
@Component
@Slf4j
public class AIRestClient {

    private final WebClient webClient;

    public AIRestClient(@Value("${app.services.ai-url:http://localhost:8000}") String aiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(aiUrl)
                .build();
    }

    public AIProcessResponse processRecipe(AIProcessRequest request) {
        log.debug("Calling AI service: POST /api/v1/process_recipe");
        return webClient.post()
                .uri("/api/v1/process_recipe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AIProcessResponse.class)
                .block();
    }

    public AIMetaResponse calculateMetas(AIMetaRequest request) {
        log.debug("Calling AI service: POST /api/v1/calculate_metas");
        return webClient.post()
                .uri("/api/v1/calculate_metas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AIMetaResponse.class)
                .block();
    }
}
