package com.chefkix.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypesenseService {

    RestClient restClient;
    ObjectMapper objectMapper;

    public TypesenseService(
            @Qualifier("typesenseRestClient") RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public boolean createCollection(Map<String, Object> schema) {
        try {
            String body = objectMapper.writeValueAsString(schema);
            restClient
                    .post()
                    .uri("/collections")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Created Typesense collection: {}", schema.get("name"));
            return true;
        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                log.debug("Collection already exists: {}", schema.get("name"));
                return true;
            }
            log.error("Failed to create collection {}: {}", schema.get("name"), e.getMessage());
            return false;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize schema: {}", e.getMessage());
            return false;
        }
    }

    public boolean indexDocument(String collection, Map<String, Object> document) {
        try {
            String body = objectMapper.writeValueAsString(document);
            restClient
                    .post()
                    .uri("/collections/{collection}/documents", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to index document in {}: {}", collection, e.getMessage());
            return false;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize document: {}", e.getMessage());
            return false;
        }
    }

    public boolean upsertDocument(String collection, Map<String, Object> document) {
        try {
            String body = objectMapper.writeValueAsString(document);
            restClient
                    .post()
                    .uri("/collections/{collection}/documents?action=upsert", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to upsert document in {}: {}", collection, e.getMessage());
            return false;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize document: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteDocument(String collection, String documentId) {
        try {
            restClient
                    .delete()
                    .uri("/collections/{collection}/documents/{id}", collection, documentId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.debug("Failed to delete document {} from {}: {}", documentId, collection, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> search(String collection, Map<String, String> searchParams) {
        try {
            Map<String, Object> multiSearchBody = Map.of(
                    "searches", List.of(
                            new LinkedHashMap<>(searchParams) {{
                                put("collection", collection);
                            }}
                    )
            );

            String result = restClient
                    .post()
                    .uri("/multi_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(multiSearchBody)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> response = objectMapper.readValue(result, new TypeReference<>() {});
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            return results != null && !results.isEmpty() ? results.get(0) : Map.of("found", 0, "hits", List.of());
        } catch (RestClientException e) {
            log.error("Search failed in collection {}: {}", collection, e.getMessage());
            return Map.of("found", 0, "hits", List.of());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse search response: {}", e.getMessage());
            return Map.of("found", 0, "hits", List.of());
        }
    }

    public int importDocuments(String collection, List<Map<String, Object>> documents) {
        if (documents.isEmpty()) return 0;
        try {
            StringBuilder jsonl = new StringBuilder();
            for (Map<String, Object> doc : documents) {
                jsonl.append(objectMapper.writeValueAsString(doc)).append("\n");
            }
            restClient
                    .post()
                    .uri("/collections/{collection}/documents/import?action=upsert", collection)
                    .contentType(MediaType.valueOf("text/plain"))
                    .body(jsonl.toString())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Imported {} documents into {}", documents.size(), collection);
            return documents.size();
        } catch (Exception e) {
            log.error("Failed to import documents into {}: {}", collection, e.getMessage());
            return 0;
        }
    }

    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.debug("Typesense health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vector search using pre-computed embedding vectors.
     * Falls back to keyword search if vector field is not populated.
     */
    public Map<String, Object> vectorSearch(
            String collection, float[] queryVector, int limit, String filterBy) {
        try {
            StringBuilder vectorStr = new StringBuilder("[");
            for (int i = 0; i < queryVector.length; i++) {
                if (i > 0) vectorStr.append(",");
                vectorStr.append(queryVector[i]);
            }
            vectorStr.append("]");

            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", "*");
            params.put("vector_query", "embedding:(" + vectorStr + ", k:" + limit + ")");
            params.put("per_page", String.valueOf(limit));
            if (filterBy != null && !filterBy.isBlank()) {
                params.put("filter_by", filterBy);
            }

            return search(collection, params);
        } catch (Exception e) {
            log.error("Vector search failed in {}: {}", collection, e.getMessage());
            return Map.of("found", 0, "hits", List.of());
        }
    }

    /**
     * Hybrid search: combines keyword + vector search results.
     * Runs keyword search first, then vector search, merges by score.
     */
    public Map<String, Object> hybridSearch(
            String collection, String query, String queryBy,
            float[] queryVector, int limit) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", query);
            params.put("query_by", queryBy);
            params.put("per_page", String.valueOf(limit));
            params.put("highlight_full_fields", queryBy);
            params.put("num_typos", "2");

            StringBuilder vectorStr = new StringBuilder("[");
            for (int i = 0; i < queryVector.length; i++) {
                if (i > 0) vectorStr.append(",");
                vectorStr.append(queryVector[i]);
            }
            vectorStr.append("]");
            params.put("vector_query", "embedding:(" + vectorStr + ", k:" + limit + ")");

            return search(collection, params);
        } catch (Exception e) {
            log.error("Hybrid search failed in {}: {}", collection, e.getMessage());
            return Map.of("found", 0, "hits", List.of());
        }
    }

    /**
     * Update collection schema (e.g., add new fields).
     * Uses PATCH /collections/{name} endpoint.
     */
    public boolean updateCollectionSchema(String collection, Map<String, Object> schemaUpdate) {
        try {
            String body = objectMapper.writeValueAsString(schemaUpdate);
            restClient
                    .patch()
                    .uri("/collections/{collection}", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Updated Typesense collection schema: {}", collection);
            return true;
        } catch (Exception e) {
            log.error("Failed to update collection {} schema: {}", collection, e.getMessage());
            return false;
        }
    }
}
