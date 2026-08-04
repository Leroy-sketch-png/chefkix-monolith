package com.chefkix.culinary.features.recipe.repository.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class RecipeRepositoryImplChallengeTest {

    @Mock private MongoTemplate mongoTemplate;

    @Test
    void challengeQueryCombinesTruthPrivacyCriteriaAndBoundedRanking() {
        Recipe recipe = Recipe.builder().id("recipe-1").build();
        when(mongoTemplate.find(any(Query.class), eq(Recipe.class))).thenReturn(List.of(recipe));
        RecipeRepositoryImpl repository = new RecipeRepositoryImpl(mongoTemplate);

        List<Recipe> result = repository.findChallengeCandidates(Map.of(
                "maxTimeMinutes", 45,
                "cuisineType", List.of("Vietnamese"),
                "ingredientContains", List.of("chili"),
                "dietaryTags", List.of("vegan"),
                "skillTags", List.of("wok"),
                "difficulty", List.of("INTERMEDIATE")), 99);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(Recipe.class));
        Query query = queryCaptor.getValue();
        String filter = query.getQueryObject().toString();

        assertThat(result).containsExactly(recipe);
        assertThat(filter)
                .contains("status=PUBLISHED")
                .contains("recipeVisibility=PUBLIC")
                .contains("recipeVisibility=null")
                .contains("totalTimeMinutes")
                .contains("$gt=0")
                .contains("$lte=45")
                .contains("cuisineType")
                .contains("fullIngredientList.name")
                .contains("dietaryTags")
                .contains("skillTags")
                .contains("difficulty");
        assertThat(query.getLimit()).isEqualTo(20);
        assertThat(query.getSortObject()).isEqualTo(new Document("cookCount", -1).append("viewCount", -1));
    }

    @Test
    void challengeQueryOmitsUndeclaredTimeCapAndRequestsOneCandidateMinimum() {
        when(mongoTemplate.find(any(Query.class), eq(Recipe.class))).thenReturn(List.of());
        RecipeRepositoryImpl repository = new RecipeRepositoryImpl(mongoTemplate);

        repository.findChallengeCandidates(Map.of(), 0);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(Recipe.class));
        assertThat(queryCaptor.getValue().getQueryObject().toString())
                .doesNotContain("totalTimeMinutes");
        assertThat(queryCaptor.getValue().getLimit()).isEqualTo(1);
    }
}
