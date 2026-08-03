package com.chefkix.config;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
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

    public boolean ensureCollectionField(String collection, Map<String, Object> field) {
        try {
            String schemaBody = restClient
                    .get()
                    .uri("/collections/{collection}", collection)
                    .retrieve()
                    .body(String.class);
            Map<String, Object> schema = objectMapper.readValue(
                    schemaBody == null ? "{}" : schemaBody, new TypeReference<>() {});
            Object rawFields = schema.get("fields");
            if (rawFields instanceof List<?> fields && fields.stream().anyMatch(candidate ->
                    candidate instanceof Map<?, ?> map
                            && field.get("name").equals(map.get("name")))) {
                return true;
            }
            return updateCollectionSchema(collection, Map.of("fields", List.of(field)));
        } catch (Exception e) {
            log.error("Failed to ensure field {} on {}: {}", field.get("name"), collection, e.getMessage());
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

    public Optional<Set<String>> listDocumentIds(String collection) {
        try {
            Set<String> ids = new LinkedHashSet<>();
            int page = 1;
            int found;
            do {
                int currentPage = page;
                String result = restClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/collections/{collection}/documents/search")
                                .queryParam("q", "*")
                                .queryParam("query_by", "title")
                                .queryParam("include_fields", "id")
                                .queryParam("per_page", 250)
                                .queryParam("page", currentPage)
                                .build(collection))
                        .retrieve()
                        .body(String.class);

                Map<String, Object> response = objectMapper.readValue(result, new TypeReference<>() {});
                found = response.get("found") instanceof Number number ? number.intValue() : 0;
                Object rawHits = response.get("hits");
                if (!(rawHits instanceof List<?> hits) || hits.isEmpty()) {
                    break;
                }
                for (Object rawHit : hits) {
                    if (rawHit instanceof Map<?, ?> hit && hit.get("document") instanceof Map<?, ?> document) {
                        Object id = document.get("id");
                        if (id != null) ids.add(id.toString());
                    }
                }
                page++;
            } while (ids.size() < found);
            return Optional.of(ids);
        } catch (Exception e) {
            log.error("Failed to enumerate document IDs in {}: {}", collection, e.getMessage());
            return Optional.empty();
        }
    }

    public Map<String, Object> search(String collection, Map<String, String> searchParams) {
        return executeSearch(collection, searchParams, true);
    }

    public Map<String, Object> searchOrEmpty(String collection, Map<String, String> searchParams) {
        try {
            return executeSearch(collection, searchParams, false);
        } catch (AppException e) {
            if (e.getErrorCode() != ErrorCode.SEARCH_SERVICE_UNAVAILABLE) {
                throw e;
            }
            return Map.of("found", 0, "hits", List.of());
        }
    }

    private Map<String, Object> executeSearch(
            String collection, Map<String, String> searchParams, boolean required) {
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
            logSearchFailure(collection, required, e);
            throw new AppException(ErrorCode.SEARCH_SERVICE_UNAVAILABLE, e);
        } catch (JsonProcessingException e) {
            logSearchFailure(collection, required, e);
            throw new AppException(ErrorCode.SEARCH_SERVICE_UNAVAILABLE, e);
        }
    }

    private void logSearchFailure(String collection, boolean required, Exception error) {
        if (required) {
            log.error("Required search failed in collection {}: {}", collection, error.getMessage());
        } else {
            log.debug("Optional search unavailable for collection {}: {}", collection, error.getMessage());
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
     */
    public Map<String, Object> vectorSearch(
            String collection, float[] queryVector, int limit, String filterBy) {
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
    }

    /**
     */
    public Map<String, Object> hybridSearch(
            String collection, String query, String queryBy,
            float[] queryVector, int limit) {
        return hybridSearch(collection, query, queryBy, queryVector, limit, 1, null);
    }

    public Map<String, Object> hybridSearch(
            String collection, String query, String queryBy,
            float[] queryVector, int limit, int page, String filterBy) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("query_by", queryBy);
        params.put("per_page", String.valueOf(limit));
        params.put("page", String.valueOf(page));
        params.put("highlight_full_fields", queryBy);
        params.put("num_typos", "2");

        StringBuilder vectorStr = new StringBuilder("[");
        for (int i = 0; i < queryVector.length; i++) {
            if (i > 0) vectorStr.append(",");
            vectorStr.append(queryVector[i]);
        }
        vectorStr.append("]");
        params.put("vector_query", "embedding:(" + vectorStr + ", k:" + limit + ")");
        if (filterBy != null && !filterBy.isBlank()) {
            params.put("filter_by", filterBy);
        }

        return search(collection, params);
    }

    /**
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
