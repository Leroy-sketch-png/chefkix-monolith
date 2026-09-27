package com.chefkix.culinary.features.knowledge.service;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.entity.KnowledgeTechnique;
import com.chefkix.culinary.features.knowledge.dto.KnowledgeGraphResponse;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeTechniqueRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.ArrayList;
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

    public KnowledgeGraphResponse getGraph(String root, String query, int depth, int limit) {
        if (root != null && !root.isBlank() && query != null && !query.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Use either root or q");
        }

        long total = ingredientRepo.count();
        List<KnowledgeIngredient> ingredients;
        boolean hasMore;
        if (root != null && !root.isBlank()) {
            ingredients = neighborhood(root.trim().toLowerCase(Locale.ROOT), depth, limit);
            hasMore = total > ingredients.size();
        } else {
            var pageRequest = PageRequest.of(0, limit, Sort.by("canonicalName"));
            Page<KnowledgeIngredient> page = query == null || query.isBlank()
                    ? ingredientRepo.findAll(pageRequest)
                    : ingredientRepo.searchByNameOrAlias(Pattern.quote(query.trim()), pageRequest);
            ingredients = page.getContent();
            hasMore = page.hasNext();
        }

        Map<String, KnowledgeIngredient> byName = new LinkedHashMap<>();
        for (KnowledgeIngredient ingredient : ingredients) {
            byName.put(ingredient.getCanonicalName(), ingredient);
        }
        List<KnowledgeGraphResponse.Node> nodes = ingredients.stream()
                .map(ingredient -> new KnowledgeGraphResponse.Node(
                        ingredient.getCanonicalName(), ingredient.getName(), ingredient.getCategory(),
                        ingredient.getAllergenFlags() == null ? List.of() : ingredient.getAllergenFlags()))
                .toList();
        List<KnowledgeGraphResponse.Edge> edges = new ArrayList<>();
        for (KnowledgeIngredient ingredient : ingredients) {
            if (ingredient.getSubstitutions() == null) continue;
            for (KnowledgeIngredient.Substitution substitution : ingredient.getSubstitutions()) {
                if (substitution.getAlternative() == null) continue;
                KnowledgeIngredient target = byName.get(substitution.getAlternative().trim().toLowerCase(Locale.ROOT));
                if (target != null) {
                    edges.add(new KnowledgeGraphResponse.Edge(
                            ingredient.getCanonicalName(), target.getCanonicalName(), "substitution",
                            null, substitution.getContext(), substitution.getRatio()));
                }
            }
        }
        return new KnowledgeGraphResponse(nodes, edges, total, hasMore);
    }

    private List<KnowledgeIngredient> neighborhood(String root, int depth, int limit) {
        KnowledgeIngredient start = ingredientRepo.findByCanonicalName(root)
                .orElseThrow(() -> new AppException(ErrorCode.KNOWLEDGE_INGREDIENT_NOT_FOUND));
        Map<String, KnowledgeIngredient> selected = new LinkedHashMap<>();
        selected.put(start.getCanonicalName(), start);
        Collection<KnowledgeIngredient> frontier = List.of(start);
        for (int level = 0; level < depth && selected.size() < limit; level++) {
            Set<String> candidates = new LinkedHashSet<>();
            for (KnowledgeIngredient ingredient : frontier) {
                if (ingredient.getSubstitutions() == null) continue;
                for (KnowledgeIngredient.Substitution substitution : ingredient.getSubstitutions()) {
                    if (substitution.getAlternative() == null) continue;
                    String name = substitution.getAlternative().trim().toLowerCase(Locale.ROOT);
                    if (!name.isBlank() && !selected.containsKey(name)) candidates.add(name);
                }
            }
            List<String> names = new ArrayList<>(candidates);
            Collections.sort(names);
            List<KnowledgeIngredient> next = new ArrayList<>();
            for (int offset = 0; offset < names.size() && selected.size() < limit;) {
                int batchSize = Math.min(limit - selected.size(), names.size() - offset);
                List<KnowledgeIngredient> found = ingredientRepo.findByCanonicalNameIn(
                        names.subList(offset, offset + batchSize));
                found.stream().sorted(Comparator.comparing(KnowledgeIngredient::getCanonicalName))
                        .forEach(ingredient -> {
                            if (selected.size() < limit && selected.putIfAbsent(
                                    ingredient.getCanonicalName(), ingredient) == null) next.add(ingredient);
                        });
                offset += batchSize;
            }
            frontier = next;
            if (frontier.isEmpty()) break;
        }
        return new ArrayList<>(selected.values());
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
