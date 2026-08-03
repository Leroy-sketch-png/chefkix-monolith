package com.chefkix.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class TypesenseDataSyncerTest {

    @Mock TypesenseService typesenseService;
    @Mock MongoTemplate mongoTemplate;
    @Mock KnowledgeIngredientRepository knowledgeIngredientRepository;

    TypesenseDataSyncer syncer;

    @BeforeEach
    void setUp() {
        syncer = new TypesenseDataSyncer(typesenseService, mongoTemplate, knowledgeIngredientRepository);
    }

    @Test
    void mapsMongoDocumentsAndPrunesOnlyStaleRecipeIdsAfterCompleteImport() {
        ObjectId id = new ObjectId();
        Document recipe = new Document("_id", id)
                .append("title", "Peri-Peri Chicken")
                .append("description", "Charred chile chicken")
                .append("cuisineType", "Portuguese-African")
                .append("difficulty", "INTERMEDIATE")
                .append("totalTimeMinutes", 75)
                .append("cookCount", 18)
                .append("averageRating", 4.7)
                .append("xpReward", 240)
                .append("qualityTier", "FOOLPROOF")
                .append("fullIngredientList", List.of(new Document("name", "Chicken")))
                .append("dietaryTags", List.of("gluten-free"))
                .append("userId", "seed-user")
                .append("coverImageUrl", List.of("/images/recipes/peri-peri-chicken.webp"))
                .append("createdAt", new Date(1_700_000_000_000L));
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("recipes")))
                .thenReturn(List.of(recipe));
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("user_profiles")))
                .thenReturn(List.of(new Document("userId", "seed-user")
                        .append("displayName", "Minh Tran")
                        .append("avatarUrl", "/avatars/minh.webp")));
        when(typesenseService.importDocuments(eq("recipes"), any())).thenReturn(1);
        when(typesenseService.listDocumentIds("recipes"))
                .thenReturn(Optional.of(Set.of(id.toString(), "stale-id")));
        when(typesenseService.deleteDocument("recipes", "stale-id")).thenReturn(true);

        syncer.syncRecipes();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> documents = ArgumentCaptor.forClass(List.class);
        verify(typesenseService).importDocuments(eq("recipes"), documents.capture());
        Map<String, Object> indexed = documents.getValue().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(id.toString(), indexed.get("id"));
        org.junit.jupiter.api.Assertions.assertEquals("Peri-Peri Chicken", indexed.get("title"));
        org.junit.jupiter.api.Assertions.assertEquals(List.of("Chicken"), indexed.get("ingredients"));
        org.junit.jupiter.api.Assertions.assertEquals("Foolproof", indexed.get("qualityTier"));
        org.junit.jupiter.api.Assertions.assertEquals(240, indexed.get("xpReward"));
        org.junit.jupiter.api.Assertions.assertEquals("/images/recipes/peri-peri-chicken.webp", indexed.get("coverImageUrl"));
        org.junit.jupiter.api.Assertions.assertEquals("Minh Tran", indexed.get("authorName"));
        org.junit.jupiter.api.Assertions.assertEquals("/avatars/minh.webp", indexed.get("authorAvatarUrl"));
        verify(typesenseService).deleteDocument("recipes", "stale-id");
        verify(typesenseService, never()).deleteDocument("recipes", id.toString());
    }

    @Test
    void mapsExistingPostsFromTheAuthoritativeCollectionWithIdentityAndMedia() {
        ObjectId id = new ObjectId();
        Document post = new Document("_id", id)
                .append("content", "Crispy edges, soft center")
                .append("userId", "cook-1")
                .append("displayName", "Lan Nguyen")
                .append("avatarUrl", "/avatars/lan.webp")
                .append("likes", 12)
                .append("commentCount", 3)
                .append("recipeTitle", "Banh Xeo")
                .append("photoUrls", List.of("/posts/banh-xeo.webp"))
                .append("createdAt", new Date(1_700_000_000_000L));
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("post")))
                .thenReturn(List.of(post));
        when(typesenseService.importDocuments(eq("posts"), any())).thenReturn(1);

        syncer.syncPosts();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> documents = ArgumentCaptor.forClass(List.class);
        verify(typesenseService).importDocuments(eq("posts"), documents.capture());
        Map<String, Object> indexed = documents.getValue().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(id.toString(), indexed.get("id"));
        org.junit.jupiter.api.Assertions.assertEquals("Crispy edges, soft center", indexed.get("content"));
        org.junit.jupiter.api.Assertions.assertEquals("Lan Nguyen", indexed.get("authorName"));
        org.junit.jupiter.api.Assertions.assertEquals("/avatars/lan.webp", indexed.get("authorAvatarUrl"));
        org.junit.jupiter.api.Assertions.assertEquals("/posts/banh-xeo.webp", indexed.get("photoUrl"));
        org.junit.jupiter.api.Assertions.assertEquals(12, indexed.get("likeCount"));
    }

    @Test
    void enrichesRealtimeRecipeDocumentsFromThePublicProfile() {
        Recipe recipe = Recipe.builder()
                .id("recipe-live")
                .userId("cook-live")
                .title("Ginger fish")
                .status(RecipeStatus.PUBLISHED)
                .xpReward(180)
                .build();
        when(mongoTemplate.findOne(
                any(Query.class),
                eq(Document.class),
                eq("user_profiles")
        )).thenReturn(new Document("userId", "cook-live")
                .append("displayName", "Anh Le")
                .append("avatarUrl", "/avatars/anh.webp"));

        syncer.indexRecipe(recipe);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> document = ArgumentCaptor.forClass(Map.class);
        verify(typesenseService).upsertDocument(eq("recipes"), document.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Anh Le", document.getValue().get("authorName"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "/avatars/anh.webp",
                document.getValue().get("authorAvatarUrl")
        );
        org.junit.jupiter.api.Assertions.assertEquals(180, document.getValue().get("xpReward"));
    }

    @Test
    void neverPrunesWhenAuthoritativeImportIsIncomplete() {
        Document recipe = new Document("_id", new ObjectId()).append("status", "PUBLISHED");
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("recipes")))
                .thenReturn(List.of(recipe));
        when(typesenseService.importDocuments(eq("recipes"), any())).thenReturn(0);

        syncer.syncRecipes();

        verify(typesenseService, never()).listDocumentIds("recipes");
        verify(typesenseService, never()).deleteDocument(eq("recipes"), any());
    }
}
