package com.chefkix.culinary.common.specification;

import static org.assertj.core.api.Assertions.assertThat;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.common.enums.QualityTier;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class RecipeSpecificationTest {

    @Test
    void appliesEveryVisibleBrowseConstraint() {
        RecipeSearchQuery query = RecipeSearchQuery.builder()
                .difficulties(List.of(Difficulty.BEGINNER, Difficulty.ADVANCED))
                .cuisineTypes(List.of("Vietnamese", "Italian"))
                .dietaryTags(List.of("gluten-free", "high-protein"))
                .maxTimeMinutes(30)
                .minRating(4.0)
                .qualityTier(QualityTier.FOOLPROOF)
                .build();

        List<Document> clauses = RecipeSpecification.getCriteria(query)
                .getCriteriaObject()
                .getList("$and", Document.class);

        assertThat(clauses).anySatisfy(clause -> assertThat(clause.get("difficulty"))
                .isEqualTo(new Document("$in", query.getDifficulties())));
        assertThat(clauses).anySatisfy(clause -> assertThat(clause.get("cuisineType"))
                .isEqualTo(new Document("$in", query.getCuisineTypes())));
        assertThat(clauses).anySatisfy(clause -> assertThat(clause.get("dietaryTags"))
                .isEqualTo(new Document("$all", query.getDietaryTags())));
        assertThat(clauses).anySatisfy(clause -> assertThat(clause.get("totalTimeMinutes"))
                .isEqualTo(new Document("$lte", 30)));
        assertThat(clauses).anySatisfy(clause -> assertThat(clause.get("averageRating"))
                .isEqualTo(new Document("$gte", 4.0)));
        assertThat(clauses).anySatisfy(clause -> assertThat(clause.get("qualityTier"))
                .isEqualTo(QualityTier.FOOLPROOF));
    }
}
