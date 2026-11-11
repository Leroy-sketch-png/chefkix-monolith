package com.chefkix.culinary.features.knowledge.service;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.entity.KnowledgeTechnique;
import com.chefkix.culinary.features.knowledge.dto.KnowledgeGraphResponse;
import com.chefkix.culinary.features.knowledge.dto.PhotoSubstitutionRequest;
import com.chefkix.culinary.features.knowledge.dto.PhotoSubstitutionResponse;
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

    public PhotoSubstitutionResponse resolvePhotoSubstitutions(PhotoSubstitutionRequest request) {
        String sourceText = request.ingredientToReplace().trim();
        String dishName = request.dishName() == null ? "" : request.dishName().trim();
        List<String> allergens = request.allergenConstraints() == null
                ? List.of() : request.allergenConstraints();
        List<String> dietaryConstraints = request.dietaryConstraints() == null
                ? List.of() : request.dietaryConstraints();

        IngredientResolution source = resolveIdentityExact(sourceText);
        if (source.ambiguous()) {
            return photoSubstitutionResult("ambiguous_source", dishName, sourceText, allergens,
                    List.of(), 0, 0);
        }

        boolean sourceAppearsInDish = request.dishIngredients().stream().anyMatch(detectedName -> {
            IngredientResolution detected = resolveIdentityExact(detectedName);
            if (source.ingredient() != null && detected.ingredient() != null) {
                return normalizeIdentity(source.ingredient().getCanonicalName())
                        .equals(normalizeIdentity(detected.ingredient().getCanonicalName()));
            }
            return normalizeIdentity(sourceText).equals(normalizeIdentity(detectedName));
        });
        if (!sourceAppearsInDish) {
            return photoSubstitutionResult("ingredient_not_in_dish", dishName, sourceText, allergens,
                    List.of(), 0, 0);
        }
        if (source.ingredient() == null) {
            return photoSubstitutionResult("source_not_in_graph", dishName, sourceText, allergens,
                    List.of(), 0, 0);
        }

        List<KnowledgeIngredient.Substitution> edges = source.ingredient().getSubstitutions() == null
                ? List.of() : source.ingredient().getSubstitutions();
        if (edges.isEmpty()) {
            return photoSubstitutionResult("no_graph_edges", dishName,
                    source.ingredient().getCanonicalName(), allergens, List.of(), 0, 0);
        }

        if (allergens.isEmpty() && dietaryConstraints.isEmpty()) {
            List<PhotoSubstitutionResponse.Candidate> candidates = edges.stream()
                    .filter(edge -> edge.getAlternative() != null && !edge.getAlternative().isBlank())
                    .map(edge -> {
                        IngredientResolution target = resolveIdentityExact(edge.getAlternative());
                        return new PhotoSubstitutionResponse.Candidate(
                                edge.getAlternative(), edge.getContext(), edge.getRatio(),
                                target.ingredient() != null);
                    })
                    .toList();
            return photoSubstitutionResult("recommendations", dishName,
                    source.ingredient().getCanonicalName(), allergens, candidates, 0, 0);
        }

        Set<String> requestedGroups = allergens.stream()
                .map(KnowledgeGraphService::allergenGroup)
                .filter(group -> !group.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        int blockedCount = 0;
        int edgeCount = 0;
        for (KnowledgeIngredient.Substitution edge : edges) {
            if (edge.getAlternative() == null || edge.getAlternative().isBlank()) continue;
            edgeCount++;
            IngredientResolution target = resolveIdentityExact(edge.getAlternative());
            if (target.ingredient() != null && hasMatchingAllergenFlag(target.ingredient(), requestedGroups)) {
                blockedCount++;
            }
        }
        // The current KG has no completeness marker for negative allergen or diet evidence.
        // Unknown candidates are withheld; an absent flag is never treated as a safe negative.
        int withheldCount = Math.max(0, edgeCount - blockedCount);
        String status = withheldCount == 0 && blockedCount > 0
                ? "no_candidates_after_filter" : "constraint_evidence_incomplete";
        return photoSubstitutionResult(status, dishName, source.ingredient().getCanonicalName(),
                allergens, List.of(), withheldCount, blockedCount);
    }

    private PhotoSubstitutionResponse photoSubstitutionResult(
            String status,
            String dishName,
            String sourceIngredient,
            List<String> allergens,
            List<PhotoSubstitutionResponse.Candidate> candidates,
            int withheldCount,
            int blockedCount) {
        return new PhotoSubstitutionResponse(status, dishName, sourceIngredient, allergens,
                candidates, withheldCount, blockedCount, "ingredient_level");
    }

    private IngredientResolution resolveIdentityExact(String identity) {
        if (identity == null || identity.isBlank()) return new IngredientResolution(null, false);
        String normalized = normalizeIdentity(identity);
        String pattern = "^" + Pattern.quote(identity.trim()) + "$";
        Map<String, KnowledgeIngredient> matches = new LinkedHashMap<>();
        for (KnowledgeIngredient ingredient : ingredientRepo.searchByNameOrAlias(pattern)) {
            boolean exact = normalizeIdentity(ingredient.getCanonicalName()).equals(normalized)
                    || normalizeIdentity(ingredient.getName()).equals(normalized)
                    || (ingredient.getAliases() != null && ingredient.getAliases().stream()
                    .anyMatch(alias -> normalizeIdentity(alias).equals(normalized)));
            if (exact) matches.put(ingredient.getCanonicalName(), ingredient);
        }
        if (matches.size() > 1) return new IngredientResolution(null, true);
        return new IngredientResolution(matches.values().stream().findFirst().orElse(null), false);
    }

    private static boolean hasMatchingAllergenFlag(KnowledgeIngredient ingredient, Set<String> requestedGroups) {
        if (ingredient.getAllergenFlags() == null) return false;
        return ingredient.getAllergenFlags().stream()
                .map(KnowledgeGraphService::allergenGroup)
                .anyMatch(requestedGroups::contains);
    }

    private static String allergenGroup(String raw) {
        String value = normalizeIdentity(raw).replace('_', ' ');
        return switch (value) {
            case "peanut", "peanuts", "groundnut", "groundnuts" -> "peanuts";
            case "tree nut", "tree nuts", "almond", "cashew", "walnut", "pecan", "pistachio",
                    "hazelnut", "macadamia", "pine nut" -> "tree_nuts";
            case "dairy", "milk", "lactose" -> "dairy";
            case "egg", "eggs" -> "eggs";
            case "gluten", "wheat" -> "wheat_gluten";
            case "soy", "soya" -> "soy";
            case "fish" -> "fish";
            case "shellfish", "crustacean", "crustaceans", "mollusc", "molluscs" -> "shellfish";
            case "sesame" -> "sesame";
            default -> "";
        };
    }

    private record IngredientResolution(KnowledgeIngredient ingredient, boolean ambiguous) {}

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
            List<KnowledgeIngredient> expanded = neighborhood(root.trim().toLowerCase(Locale.ROOT), depth, limit + 1);
            hasMore = expanded.size() > limit;
            ingredients = hasMore ? expanded.subList(0, limit) : expanded;
        } else {
            var pageRequest = PageRequest.of(0, limit, Sort.by("canonicalName"));
            Page<KnowledgeIngredient> page = query == null || query.isBlank()
                    ? ingredientRepo.findAll(pageRequest)
                    : ingredientRepo.searchByNameOrAlias(Pattern.quote(query.trim()), pageRequest);
            ingredients = page.getContent();
            hasMore = page.hasNext();
        }

        IdentityIndex byIdentity = indexIdentities(ingredients);
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
                KnowledgeIngredient target = byIdentity.resolve(substitution.getAlternative());
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
                List<String> batch = names.subList(offset, offset + batchSize);
                Map<String, KnowledgeIngredient> foundById = new LinkedHashMap<>();
                ingredientRepo.findByCanonicalNameIn(batch)
                        .forEach(ingredient -> foundById.put(ingredient.getCanonicalName(), ingredient));
                ingredientRepo.findByAliasesIn(batch)
                        .forEach(ingredient -> foundById.putIfAbsent(ingredient.getCanonicalName(), ingredient));
                IdentityIndex byIdentity = indexIdentities(foundById.values());
                for (String name : batch) {
                    KnowledgeIngredient ingredient = byIdentity.resolve(name);
                    if (ingredient != null && selected.size() < limit
                            && selected.putIfAbsent(ingredient.getCanonicalName(), ingredient) == null) {
                        next.add(ingredient);
                    }
                }
                offset += batchSize;
            }
            frontier = next;
            if (frontier.isEmpty()) break;
        }
        return new ArrayList<>(selected.values());
    }

    private static IdentityIndex indexIdentities(Collection<KnowledgeIngredient> ingredients) {
        Map<String, KnowledgeIngredient> byCanonicalName = new LinkedHashMap<>();
        Set<String> ambiguousCanonicalNames = new LinkedHashSet<>();
        for (KnowledgeIngredient ingredient : ingredients) {
            registerIdentity(byCanonicalName, ambiguousCanonicalNames,
                    ingredient.getCanonicalName(), ingredient);
        }

        Map<String, KnowledgeIngredient> byAlias = new LinkedHashMap<>();
        Set<String> ambiguousAliases = new LinkedHashSet<>();
        for (KnowledgeIngredient ingredient : ingredients) {
            if (ingredient.getAliases() != null) {
                ingredient.getAliases().forEach(alias -> {
                    String normalized = normalizeIdentity(alias);
                    if (byCanonicalName.containsKey(normalized)
                            || ambiguousCanonicalNames.contains(normalized)) return;
                    registerIdentity(byAlias, ambiguousAliases, alias, ingredient);
                });
            }
        }
        return new IdentityIndex(byCanonicalName, ambiguousCanonicalNames, byAlias, ambiguousAliases);
    }

    private static void registerIdentity(
            Map<String, KnowledgeIngredient> byIdentity,
            Set<String> ambiguous,
            String identity,
            KnowledgeIngredient ingredient) {
        String normalized = normalizeIdentity(identity);
        if (normalized.isBlank() || ambiguous.contains(normalized)) return;
        KnowledgeIngredient existing = byIdentity.putIfAbsent(normalized, ingredient);
        if (existing != null && !normalizeIdentity(existing.getCanonicalName())
                .equals(normalizeIdentity(ingredient.getCanonicalName()))) {
            byIdentity.remove(normalized);
            ambiguous.add(normalized);
        }
    }

    private static String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
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

    private record IdentityIndex(
            Map<String, KnowledgeIngredient> byCanonicalName,
            Set<String> ambiguousCanonicalNames,
            Map<String, KnowledgeIngredient> byAlias,
            Set<String> ambiguousAliases) {
        private KnowledgeIngredient resolve(String identity) {
            String normalized = normalizeIdentity(identity);
            if (normalized.isBlank() || ambiguousCanonicalNames.contains(normalized)) return null;
            KnowledgeIngredient canonical = byCanonicalName.get(normalized);
            if (canonical != null) return canonical;
            return ambiguousAliases.contains(normalized) ? null : byAlias.get(normalized);
        }
    }
}
