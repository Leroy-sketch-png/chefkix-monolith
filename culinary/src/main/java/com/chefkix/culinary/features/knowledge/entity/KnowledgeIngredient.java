package com.chefkix.culinary.features.knowledge.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Graph — Canonical ingredient reference.
 * Migrated from hardcoded INGREDIENT_SUBSTITUTIONS + enriched.
 * Spec: CHEFKIX_MASTER_PLAN.md §Engine 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "kg_ingredients")
public class KnowledgeIngredient {
    @Id
    String id;

    @Indexed(unique = true)
    String canonicalName;

    @TextIndexed(weight = 10)
    String name;

    List<String> aliases;

    @Indexed
    String category; // produce, protein, dairy, grain, spice, condiment, baking, oil, other

    List<String> commonUnits; // cup, tbsp, tsp, oz, g, ml, piece, clove, etc.

    List<String> allergenFlags; // gluten, dairy, nuts, soy, eggs, shellfish, etc.

    List<Substitution> substitutions;

    @Builder.Default
    Boolean isCommon = true;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Substitution {
        String alternative;
        String context; // "in baking", "in sautéing", "general"
        Double ratio; // 1.0 = same amount, 0.5 = half
    }
}
