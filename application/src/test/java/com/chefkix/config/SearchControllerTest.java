package com.chefkix.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

class SearchControllerTest {

    @Test
    void combinedRecipeSearchAppliesEveryVisibleConstraint() {
        TypesenseService typesenseService = mock(TypesenseService.class);
        when(typesenseService.search(eq("recipes"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("found", 0, "hits", List.of()));
        SearchController controller = new SearchController(
                typesenseService, mock(MongoTemplate.class), new ObjectMapper(), "http://ai.test", "");

        controller.unifiedSearch(
                "chicken",
                "recipes",
                20,
                2,
                List.of("Beginner", "Advanced"),
                List.of("Vietnamese", "Italian"),
                List.of("gluten-free", "high-protein"),
                30,
                4.0,
                "Foolproof");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(typesenseService).search(eq("recipes"), params.capture());
        assertEquals("2", params.getValue().get("page"));
        assertEquals(
                "difficulty:=[`Beginner`, `Advanced`]"
                        + " && cuisine:=[`Vietnamese`, `Italian`]"
                        + " && tags:=`gluten-free`"
                        + " && tags:=`high-protein`"
                        + " && totalTime:<=30"
                        + " && avgRating:>=4.0"
                        + " && qualityTier:=`Foolproof`",
                params.getValue().get("filter_by"));
    }

    @Test
    void unfilteredRecipeSearchKeepsFilterParameterAbsent() {
        TypesenseService typesenseService = mock(TypesenseService.class);
        when(typesenseService.search(eq("recipes"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("found", 0, "hits", List.of()));
        SearchController controller = new SearchController(
                typesenseService, mock(MongoTemplate.class), new ObjectMapper(), "http://ai.test", "");

        controller.unifiedSearch("pho", "recipes", 10, 1, null, null, null, null, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(typesenseService).search(eq("recipes"), params.capture());
        assertFalse(params.getValue().containsKey("filter_by"));
    }
}
