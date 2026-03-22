package com.chefkix.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> search(String collection, Map<String, String> searchParams) {
        try {
            String result = restClient
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/collections/{collection}/documents/search");
                        searchParams.forEach(builder::queryParam);
                        return builder.build(collection);
                    })
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(result, new TypeReference<>() {});
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
            return false;
        }
    }
}
