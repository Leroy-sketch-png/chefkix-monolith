package com.chefkix.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TypesenseCollectionInitializerTest {

    @Mock TypesenseService typesenseService;

    @Test
    void declaresAndMigratesSearchIdentityAndMediaFields() {
        when(typesenseService.isHealthy()).thenReturn(true);
        when(typesenseService.ensureCollectionField(any(), any())).thenReturn(true);

        new TypesenseCollectionInitializer(typesenseService).initializeCollections();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> schemaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(typesenseService, atLeastOnce()).createCollection(schemaCaptor.capture());

        Map<String, Object> recipeSchema = schemaNamed(schemaCaptor.getAllValues(), "recipes");
        Map<String, Object> postSchema = schemaNamed(schemaCaptor.getAllValues(), "posts");
        assertThat(fieldNames(recipeSchema)).contains("authorName", "authorAvatarUrl", "xpReward");
        assertThat(fieldNames(postSchema)).contains("authorName", "authorAvatarUrl", "photoUrl");

        verify(typesenseService).ensureCollectionField(
                eq("recipes"), fieldNamed("authorAvatarUrl"));
        verify(typesenseService).ensureCollectionField(
                eq("recipes"), fieldNamed("xpReward"));
        verify(typesenseService).ensureCollectionField(
                eq("posts"), fieldNamed("authorAvatarUrl"));
        verify(typesenseService).ensureCollectionField(
                eq("posts"), fieldNamed("photoUrl"));
    }

    private Map<String, Object> schemaNamed(
            List<Map<String, Object>> schemas,
            String name
    ) {
        return schemas.stream()
                .filter(schema -> name.equals(schema.get("name")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<String> fieldNames(Map<String, Object> schema) {
        return ((List<Map<String, Object>>) schema.get("fields")).stream()
                .map(field -> field.get("name").toString())
                .toList();
    }

    private Map<String, Object> fieldNamed(String name) {
        return org.mockito.ArgumentMatchers.argThat(
                field -> name.equals(field.get("name")));
    }
}
