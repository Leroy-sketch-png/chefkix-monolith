package com.chefkix.config;

import com.chefkix.shared.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false) @Size(max = 4)
                    List<@NotBlank @Size(max = 80) @Pattern(regexp = "^[^`\\r\\n]+$") String> difficulty,
            @RequestParam(required = false) @Size(max = 12)
                    List<@NotBlank @Size(max = 80) @Pattern(regexp = "^[^`\\r\\n]+$") String> cuisine,
            @RequestParam(required = false) @Size(max = 12)
                    List<@NotBlank @Size(max = 80) @Pattern(regexp = "^[^`\\r\\n]+$") String> dietary,
            @RequestParam(required = false) @Min(1) @Max(1440) Integer maxTime,
            @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("5.0") Double minRating,
            @RequestParam(required = false)
                    @Pattern(regexp = "(?i)Foolproof|Good|Needs Work|Draft Quality") String qualityTier) {

        Map<String, Object> results = new LinkedHashMap<>();
        String recipeFilter = buildRecipeFilter(
                difficulty, cuisine, dietary, maxTime, minRating, qualityTier);

        if ("all".equals(type) || "recipes".equals(type)) {
            results.put("recipes", searchRecipesWithVector(q, limit, page, recipeFilter));
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
            results.put("recipes", searchCollectionOrEmpty("recipes", q, "title,ingredients", limit, 1));
        }
        if ("all".equals(type) || "ingredients".equals(type)) {
            results.put("ingredients", searchCollectionOrEmpty("ingredients", q, "name,aliases", limit, 1));
        }
        if ("all".equals(type) || "users".equals(type)) {
            results.put("users", searchCollectionOrEmpty("users", q, "username,displayName,firstName,lastName", limit, 1));
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
        return typesenseService.search(collection, searchParams(query, queryBy, perPage, page));
    }

    private Map<String, Object> searchCollectionOrEmpty(
            String collection, String query, String queryBy, int perPage, int page) {
        return typesenseService.searchOrEmpty(collection, searchParams(query, queryBy, perPage, page));
    }

    private Map<String, String> searchParams(
            String query, String queryBy, int perPage, int page) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("query_by", queryBy);
        params.put("per_page", String.valueOf(perPage));
        params.put("page", String.valueOf(page));
        params.put("highlight_full_fields", queryBy);
        params.put("num_typos", "2");
        params.put("typo_tokens_threshold", "1");

        return params;
    }

    /**
     */
    private Map<String, Object> searchRecipesWithVector(
            String query, int limit, int page, String filterBy) {
        String queryBy = "title,description,ingredients,cuisine,tags";

        long wordCount = query.trim().split("\\s+").length;
        if (wordCount < NATURAL_LANGUAGE_WORD_THRESHOLD) {
            Map<String, String> params = searchParams(query, queryBy, limit, page);
            if (StringUtils.hasText(filterBy)) {
                params.put("filter_by", filterBy);
            }
            return typesenseService.search("recipes", params);
        }

        try {
            float[] embedding = generateEmbedding(query);
            if (embedding != null) {
                log.debug("Using hybrid search for query: '{}'", query);
                return typesenseService.hybridSearch(
                        "recipes", query, queryBy, embedding, limit, page, filterBy);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to keyword: {}", e.getMessage());
        }

        Map<String, String> params = searchParams(query, queryBy, limit, page);
        if (StringUtils.hasText(filterBy)) {
            params.put("filter_by", filterBy);
        }
        return typesenseService.search("recipes", params);
    }

    static String buildRecipeFilter(
            List<String> difficulties,
            List<String> cuisines,
            List<String> dietaryTags,
            Integer maxTime,
            Double minRating,
            String qualityTier) {
        List<String> filters = new ArrayList<>();
        addExactAny(filters, "difficulty", difficulties);
        addExactAny(filters, "cuisine", cuisines);
        if (dietaryTags != null) {
            dietaryTags.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .forEach(tag -> filters.add("tags:=`" + tag + "`"));
        }
        if (maxTime != null) filters.add("totalTime:<=" + maxTime);
        if (minRating != null) filters.add("avgRating:>=" + minRating);
        if (StringUtils.hasText(qualityTier)) {
            filters.add("qualityTier:=`" + qualityTier.trim() + "`");
        }
        return String.join(" && ", filters);
    }

    private static void addExactAny(
            List<String> filters, String field, List<String> rawValues) {
        if (rawValues == null) return;
        List<String> values = rawValues.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (values.isEmpty()) return;
        if (values.size() == 1) {
            filters.add(field + ":=`" + values.get(0) + "`");
            return;
        }
        filters.add(field + ":=[" + values.stream()
                .map(value -> "`" + value + "`")
                .collect(Collectors.joining(", ")) + "]");
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
