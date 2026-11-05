package com.chefkix.culinary.features.knowledge.controller;

import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Read-only visualization proxy for a hash-pinned provisional graph candidate. */
@RestController
@RequestMapping("/knowledge/graph")
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GraphCandidatePreviewController {

    AIRestClient aiRestClient;

    @GetMapping("/candidate-preview")
    public ApiResponse<Map<String, Object>> getGraphCandidatePreview(
            @RequestParam(required = false) @Size(max = 120) String q,
            @RequestParam(required = false) @Size(max = 120) String root,
            @RequestParam(defaultValue = "500") @Min(1) @Max(500) int limit) {
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .statusCode(200)
                .data(aiRestClient.getGraphCandidatePreview(q, root, limit))
                .build();
    }
}
