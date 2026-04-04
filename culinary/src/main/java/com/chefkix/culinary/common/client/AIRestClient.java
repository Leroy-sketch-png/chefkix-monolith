package com.chefkix.culinary.common.client;

import com.chefkix.culinary.features.ai.dto.internal.*;
import com.chefkix.culinary.features.ai.dto.internal.AIMealPlanRequest;
import com.chefkix.culinary.features.ai.dto.internal.AIMealPlanResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient-based client for the external Python FastAPI AI service.
 * <p>
 * All AI service endpoints wrap responses in:
 * {@code { "success": true, "data": { ... }, "message": null, "statusCode": 200 }}
 * <p>
 * The validate and moderate methods properly unwrap this envelope.
 * Legacy methods (processRecipe, calculateMetas) use direct deserialization
 * which relies on Jackson ignoring unknown properties.
 */
@Component
@Slf4j
public class AIRestClient {

    private final WebClient webClient;

    public AIRestClient(
            @Value("${app.services.ai-url:http://localhost:8000}") String aiUrl,
            @Value("${app.services.ai-api-key:}") String aiApiKey
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(30));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(aiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        if (StringUtils.hasText(aiApiKey)) {
            builder.defaultHeader("X-AI-Service-Key", aiApiKey);
        }
        this.webClient = builder.build();
    }

    // ─── EXISTING METHODS (legacy direct deserialization) ────────────

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

    // ─── NEW METHODS (proper envelope unwrapping, fail-closed) ──────

    /**
     * Validate recipe content for safety before publishing.
     * Calls POST /api/v1/validate_recipe on the AI service.
     *
     * @throws AppException with AI_SERVICE_UNAVAILABLE if AI service is down or returns error
     */
    public AIValidationResponse validateRecipe(AIValidationRequest request) {
        log.debug("Calling AI service: POST /api/v1/validate_recipe");
        try {
            AIServiceResponse<AIValidationResponse> wrapper = webClient.post()
                    .uri("/api/v1/validate_recipe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AIServiceResponse<AIValidationResponse>>() {})
                    .block();

            if (wrapper == null || !wrapper.isSuccess() || wrapper.getData() == null) {
                log.error("AI validation returned null or unsuccessful response");
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            return wrapper.getData();
        } catch (WebClientResponseException e) {
            log.error("AI validate_recipe HTTP error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (AppException e) {
            throw e; // re-throw our own exceptions
        } catch (Exception e) {
            log.error("AI validate_recipe failed", e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Moderate recipe content before publishing.
     * Calls POST /api/v1/moderate on the AI service.
     *
     * @throws AppException with AI_SERVICE_UNAVAILABLE if AI service is down or returns error
     */
    public AIModerationResponse moderateContent(AIModerationRequest request) {
        log.debug("Calling AI service: POST /api/v1/moderate");
        try {
            AIServiceResponse<AIModerationResponse> wrapper = webClient.post()
                    .uri("/api/v1/moderate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AIServiceResponse<AIModerationResponse>>() {})
                    .block();

            if (wrapper == null || !wrapper.isSuccess() || wrapper.getData() == null) {
                log.error("AI moderation returned null or unsuccessful response");
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            return wrapper.getData();
        } catch (WebClientResponseException e) {
            log.error("AI moderate HTTP error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI moderate failed", e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Generate an AI-powered meal plan from pantry items and preferences.
     * Calls POST /api/v1/generate_meal_plan on the AI service.
     *
     * @throws AppException with AI_SERVICE_UNAVAILABLE if AI service is down or returns error
     */
    public AIMealPlanResponse generateMealPlan(AIMealPlanRequest request) {
        log.debug("Calling AI service: POST /api/v1/generate_meal_plan");
        try {
            AIServiceResponse<AIMealPlanResponse> wrapper = webClient.post()
                    .uri("/api/v1/generate_meal_plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AIServiceResponse<AIMealPlanResponse>>() {})
                    .block();

            if (wrapper == null || !wrapper.isSuccess() || wrapper.getData() == null) {
                log.error("AI meal plan returned null or unsuccessful response");
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            return wrapper.getData();
        } catch (WebClientResponseException e) {
            log.error("AI generate_meal_plan HTTP error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI generate_meal_plan failed", e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Score recipe quality (RQS) for quality tier assignment.
     * Calls POST /api/v1/score_recipe_quality on the AI service.
     * Non-blocking: returns null if AI service is unavailable (does not block publish).
     */
    public AIQualityScoreResponse scoreRecipeQuality(AIQualityScoreRequest request) {
        log.debug("Calling AI service: POST /api/v1/score_recipe_quality");
        try {
            AIServiceResponse<AIQualityScoreResponse> wrapper = webClient.post()
                    .uri("/api/v1/score_recipe_quality")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AIServiceResponse<AIQualityScoreResponse>>() {})
                    .block();

            if (wrapper == null || !wrapper.isSuccess() || wrapper.getData() == null) {
                log.warn("AI quality scoring returned null or unsuccessful response — skipping RQS");
                return null;
            }

            return wrapper.getData();
        } catch (Exception e) {
            log.warn("AI score_recipe_quality failed — skipping RQS: {}", e.getMessage());
            return null;
        }
    }
}
