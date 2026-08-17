package com.chefkix.culinary.features.knowledge.service;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.entity.KnowledgeTechnique;
import com.chefkix.culinary.features.knowledge.dto.KnowledgeGraphResponse;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeTechniqueRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeGraphService {

    KnowledgeIngredientRepository ingredientRepo;
    KnowledgeTechniqueRepository techniqueRepo;


    public List<KnowledgeIngredient> searchIngredients(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        String escaped = Pattern.quote(query.trim());
        return ingredientRepo.searchByNameOrAlias(escaped);
    }

    public Optional<KnowledgeIngredient> getIngredientByName(String canonicalName) {
        return ingredientRepo.findByCanonicalName(canonicalName.toLowerCase().trim());
    }

    public List<KnowledgeIngredient> getIngredientsByCategory(String category) {
        return ingredientRepo.findByCategory(category);
    }

    public List<KnowledgeIngredient.Substitution> getSubstitutions(String ingredientName) {
        return ingredientRepo.findByCanonicalName(ingredientName.toLowerCase().trim())
                .map(KnowledgeIngredient::getSubstitutions)
                .orElse(Collections.emptyList());
    }

    public List<KnowledgeIngredient> getAllIngredients() {
        return ingredientRepo.findAll();
    }

    public KnowledgeGraphResponse getGraph() {
        List<KnowledgeIngredient> ingredients = ingredientRepo.findAll();
        List<KnowledgeGraphResponse.Node> nodes = ingredients.stream()
                .map(ingredient -> new KnowledgeGraphResponse.Node(
                        ingredient.getCanonicalName(), ingredient.getName(), ingredient.getCategory(),
                        ingredient.getAllergenFlags() == null ? List.of() : ingredient.getAllergenFlags()))
                .toList();
        List<KnowledgeGraphResponse.Edge> edges = new ArrayList<>();
        for (KnowledgeIngredient ingredient : ingredients) {
            if (ingredient.getSubstitutions() == null) continue;
            for (KnowledgeIngredient.Substitution substitution : ingredient.getSubstitutions()) {
                ingredientRepo.findByCanonicalName(substitution.getAlternative().toLowerCase().trim())
                        .ifPresent(target -> edges.add(new KnowledgeGraphResponse.Edge(
                                ingredient.getCanonicalName(), target.getCanonicalName(), "substitution",
                                substitution.getRatio() == null ? 0.5 : Math.min(1.0, substitution.getRatio()),
                                substitution.getContext())));
            }
        }
        return new KnowledgeGraphResponse(nodes, edges);
    }


    public List<KnowledgeTechnique> searchTechniques(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        String escaped = Pattern.quote(query.trim());
        return techniqueRepo.searchByName(escaped);
    }

    public Optional<KnowledgeTechnique> getTechniqueByName(String canonicalName) {
        return techniqueRepo.findByCanonicalName(canonicalName.toLowerCase().trim());
    }

    public List<KnowledgeTechnique> getTechniquesByCategory(String category) {
        return techniqueRepo.findByCategory(category);
    }

    public List<KnowledgeTechnique> getTechniquesByDifficulty(String difficulty) {
        return techniqueRepo.findByDifficulty(difficulty);
    }

    public List<KnowledgeTechnique> getAllTechniques() {
        return techniqueRepo.findAll();
    }
}
