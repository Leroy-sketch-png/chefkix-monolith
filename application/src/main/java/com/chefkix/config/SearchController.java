package com.chefkix.config;

import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.constraints.Size;
import java.util.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchController {

    TypesenseService typesenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> unifiedSearch(
            @RequestParam @Size(max = 200) String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "1") int page) {

        Map<String, Object> results = new LinkedHashMap<>();

        if ("all".equals(type) || "recipes".equals(type)) {
            results.put("recipes", searchCollection("recipes", q, "title,description,ingredients,cuisine,tags", limit, page));
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
            @RequestParam(defaultValue = "5") int limit) {

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
}
