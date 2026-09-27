package com.chefkix.culinary.features.knowledge.controller;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.entity.KnowledgeTechnique;
import com.chefkix.culinary.features.knowledge.service.KnowledgeGraphService;
import com.chefkix.culinary.features.knowledge.dto.KnowledgeGraphResponse;
import com.chefkix.shared.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeGraphController {

    KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/graph")
    public ApiResponse<KnowledgeGraphResponse> getGraph(
            @RequestParam(required = false) @Size(max = 120) String root,
            @RequestParam(required = false) @Size(max = 120) String q,
            @RequestParam(defaultValue = "1") @Min(0) @Max(2) int depth,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return ApiResponse.<KnowledgeGraphResponse>builder()
                .success(true).statusCode(200).data(knowledgeGraphService.getGraph(root, q, depth, limit)).build();
    }


    @GetMapping("/ingredients")
    public ApiResponse<List<KnowledgeIngredient>> searchIngredients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        List<KnowledgeIngredient> results;
        if (q != null && !q.isBlank()) {
            results = knowledgeGraphService.searchIngredients(q);
        } else if (category != null && !category.isBlank()) {
            results = knowledgeGraphService.getIngredientsByCategory(category);
        } else {
            results = knowledgeGraphService.getAllIngredients();
        }
        return ApiResponse.<List<KnowledgeIngredient>>builder()
                .success(true).statusCode(200).data(results).build();
    }

    @GetMapping("/ingredients/{name}")
    public ApiResponse<KnowledgeIngredient> getIngredient(@PathVariable String name) {
        return knowledgeGraphService.getIngredientByName(name)
                .map(ing -> ApiResponse.<KnowledgeIngredient>builder()
                        .success(true).statusCode(200).data(ing).build())
                .orElse(ApiResponse.<KnowledgeIngredient>builder()
                        .success(false).statusCode(404).message("Ingredient not found").build());
    }

    @GetMapping("/ingredients/{name}/substitutions")
    public ApiResponse<List<KnowledgeIngredient.Substitution>> getSubstitutions(@PathVariable String name) {
        var subs = knowledgeGraphService.getSubstitutions(name);
        return ApiResponse.<List<KnowledgeIngredient.Substitution>>builder()
                .success(true).statusCode(200).data(subs).build();
    }


    @GetMapping("/techniques")
    public ApiResponse<List<KnowledgeTechnique>> searchTechniques(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty) {
        List<KnowledgeTechnique> results;
        if (q != null && !q.isBlank()) {
            results = knowledgeGraphService.searchTechniques(q);
        } else if (category != null && !category.isBlank()) {
            results = knowledgeGraphService.getTechniquesByCategory(category);
        } else if (difficulty != null && !difficulty.isBlank()) {
            results = knowledgeGraphService.getTechniquesByDifficulty(difficulty);
        } else {
            results = knowledgeGraphService.getAllTechniques();
        }
        return ApiResponse.<List<KnowledgeTechnique>>builder()
                .success(true).statusCode(200).data(results).build();
    }

    @GetMapping("/techniques/{name}")
    public ApiResponse<KnowledgeTechnique> getTechnique(@PathVariable String name) {
        return knowledgeGraphService.getTechniqueByName(name)
                .map(tech -> ApiResponse.<KnowledgeTechnique>builder()
                        .success(true).statusCode(200).data(tech).build())
                .orElse(ApiResponse.<KnowledgeTechnique>builder()
                        .success(false).statusCode(404).message("Technique not found").build());
    }
}
