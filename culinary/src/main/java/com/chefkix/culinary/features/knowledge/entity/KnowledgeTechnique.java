package com.chefkix.culinary.features.knowledge.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Knowledge Graph — Cooking technique reference.
 * Migrated from hardcoded TECHNIQUE_GUIDES + COMMON_MISTAKES.
 * Spec: CHEFKIX_MASTER_PLAN.md §Engine 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "kg_techniques")
public class KnowledgeTechnique {
    @Id
    String id;

    @Indexed(unique = true)
    String canonicalName;

    @TextIndexed(weight = 10)
    String name;

    String description;

    @Indexed
    String difficulty; // beginner, intermediate, advanced, expert

    @Indexed
    String category; // heat-based, preparation, baking, preservation, asian, plating

    List<String> relatedEquipment;

    String commonMistake;

    List<String> visualCues; // "golden brown crust", "bubbling edges"

    List<String> relatedCuisines;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
