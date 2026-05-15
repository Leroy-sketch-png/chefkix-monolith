package com.chefkix.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

@Slf4j
@Validated
@RestController
@RequestMapping("/search")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchController {

    TypesenseService typesenseService;
    MongoTemplate mongoTemplate;
    ObjectMapper objectMapper;
    RestClient aiRestClient;

    static final int NATURAL_LANGUAGE_WORD_THRESHOLD = 3;

    public SearchController(
            TypesenseService typesenseService,
            MongoTemplate mongoTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.ai-url:http://localhost:8000}") String aiUrl,
            @Value("${app.services.ai-api-key:}") String aiApiKey) {
        this.typesenseService = typesenseService;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(aiUrl)
                .requestFactory(requestFactory);

        if (StringUtils.hasText(aiApiKey)) {
            builder.defaultHeader("X-AI-Service-Key", aiApiKey);
        }

        this.aiRestClient = builder.build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> unifiedSearch(
            @RequestParam @Size(max = 200) String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "1") @Min(1) int page) {

        Map<String, Object> results = new LinkedHashMap<>();

        if ("all".equals(type) || "recipes".equals(type)) {
            results.put("recipes", searchRecipesWithVector(q, limit, page));
        }
        if ("all".equals(type) || "posts".equals(type)) {
            results.put("posts", searchCollection("posts", q, "content,authorName,recipeTitle", limit, page));
        }
        if ("all".equals(type) || "users".equals(type)) {
            results.put("users", searchCollection("users", q, "username,displayName,firstName,lastName,bio", limit, page));
        }
        if ("all".equals(type) || "ingredients".equals(type)) {
            results.put("ingredients", searchCollection("ingredients", q, "name,aliases", limit, page));
        }

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autocomplete(
            @RequestParam @Size(max = 200) String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {

        Map<String, Object> results = new LinkedHashMap<>();

        if ("all".equals(type) || "recipes".equals(type)) {
            results.put("recipes", searchCollection("recipes", q, "title,ingredients", limit, 1));
        }
        if ("all".equals(type) || "ingredients".equals(type)) {
            results.put("ingredients", searchCollection("ingredients", q, "name,aliases", limit, 1));
        }
        if ("all".equals(type) || "users".equals(type)) {
            results.put("users", searchCollection("users", q, "username,displayName,firstName,lastName", limit, 1));
        }

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<String>>> trendingSearches(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {

        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("eventType").is("RECIPE_SEARCH")
                        .and("timestamp").gte(oneWeekAgo)
                        .and("metadata.query").exists(true)),
                Aggregation.project().and("metadata.query").as("query"),
                Aggregation.group("query").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "count")),
                Aggregation.limit(limit)
        );

        List<String> trending = mongoTemplate.aggregate(agg, "user_events", Document.class)
                .getMappedResults().stream()
                .map(doc -> doc.getString("_id"))
                .filter(q -> q != null && !q.isBlank())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(trending));
    }

    private Map<String, Object> searchCollection(
            String collection, String query, String queryBy, int perPage, int page) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("query_by", queryBy);
        params.put("per_page", String.valueOf(perPage));
        params.put("page", String.valueOf(page));
        params.put("highlight_full_fields", queryBy);
        params.put("num_typos", "2");
        params.put("typo_tokens_threshold", "1");

        return typesenseService.search(collection, params);
    }

    /**
     * Smart recipe search: uses hybrid (keyword + vector) when query looks
     * like natural language (>3 words). Falls back to keyword-only if
     * embedding generation fails or query is short/specific.
     */
    private Map<String, Object> searchRecipesWithVector(String query, int limit, int page) {
        String queryBy = "title,description,ingredients,cuisine,tags";

        // Short/specific queries -> keyword search (fast, precise)
        long wordCount = query.trim().split("\\s+").length;
        if (wordCount < NATURAL_LANGUAGE_WORD_THRESHOLD) {
            return searchCollection("recipes", query, queryBy, limit, page);
        }

        // Natural language query -> try hybrid search
        try {
            float[] embedding = generateEmbedding(query);
            if (embedding != null) {
                log.debug("Using hybrid search for query: '{}'", query);
                return typesenseService.hybridSearch("recipes", query, queryBy, embedding, limit);
            }
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to keyword: {}", e.getMessage());
        }

        // Fallback to keyword-only
        return searchCollection("recipes", query, queryBy, limit, page);
    }

    @SuppressWarnings("unchecked")
    private float[] generateEmbedding(String text) {
        try {
            String responseBody = aiRestClient.post()
                    .uri("/api/v1/embed")
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});
            if (!Boolean.TRUE.equals(response.get("success"))) {
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null || data.get("embedding") == null) {
                return null;
            }

            List<Number> embedding = (List<Number>) data.get("embedding");
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            return vector;
        } catch (Exception e) {
            log.warn("Embedding generation failed (search falls back to keyword): {}", e.getMessage());
            return null;
        }
    }
}
