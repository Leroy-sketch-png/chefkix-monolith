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
String category;

List<String> commonUnits;

List<String> allergenFlags;

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
String context;
Double ratio;
    }
}
