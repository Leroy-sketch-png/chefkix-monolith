package com.chefkix.config;

import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypesenseCollectionInitializer {

    TypesenseService typesenseService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCollections() {
        if (!typesenseService.isHealthy()) {
            log.warn("Typesense is not reachable -- skipping collection initialization");
            return;
        }

        createRecipesCollection();
        createPostsCollection();
        createUsersCollection();
        createIngredientsCollection();
        log.info("Typesense collection initialization complete");
    }

    private void createRecipesCollection() {
        typesenseService.createCollection(Map.of(
                "name", "recipes",
                "fields", List.of(
                        Map.of("name", "id", "type", "string"),
                        Map.of("name", "title", "type", "string"),
                        Map.of("name", "description", "type", "string", "optional", true),
                        Map.of("name", "cuisine", "type", "string", "facet", true, "optional", true),
                        Map.of("name", "difficulty", "type", "string", "facet", true, "optional", true),
                        Map.of("name", "totalTime", "type", "int32", "optional", true),
                        Map.of("name", "cookCount", "type", "int32", "optional", true),
                        Map.of("name", "avgRating", "type", "float", "optional", true),
                        Map.of("name", "ingredients", "type", "string[]", "optional", true),
                        Map.of("name", "tags", "type", "string[]", "facet", true, "optional", true),
                        Map.of("name", "authorId", "type", "string", "optional", true),
                        Map.of("name", "authorName", "type", "string", "optional", true),
                        Map.of("name", "coverImageUrl", "type", "string", "optional", true),
                        Map.of("name", "createdAt", "type", "int64"),
                        Map.of("name", "embedding", "type", "float[]", "num_dim", 3072,
                                "optional", true)
                ),
                "default_sorting_field", "createdAt",
                "token_separators", List.of("-", "'")
        ));
    }

    private void createPostsCollection() {
        typesenseService.createCollection(Map.of(
                "name", "posts",
                "fields", List.of(
                        Map.of("name", "id", "type", "string"),
                        Map.of("name", "content", "type", "string", "optional", true),
                        Map.of("name", "authorId", "type", "string"),
                        Map.of("name", "authorName", "type", "string", "optional", true),
                        Map.of("name", "likeCount", "type", "int32", "optional", true),
                        Map.of("name", "commentCount", "type", "int32", "optional", true),
                        Map.of("name", "recipeTitle", "type", "string", "optional", true),
                        Map.of("name", "createdAt", "type", "int64")
                ),
                "default_sorting_field", "createdAt"
        ));
    }

    private void createUsersCollection() {
        typesenseService.createCollection(Map.of(
                "name", "users",
                "fields", List.of(
                        Map.of("name", "id", "type", "string"),
                        Map.of("name", "username", "type", "string"),
                        Map.of("name", "displayName", "type", "string", "optional", true),
                        Map.of("name", "firstName", "type", "string", "optional", true),
                        Map.of("name", "lastName", "type", "string", "optional", true),
                        Map.of("name", "bio", "type", "string", "optional", true),
                        Map.of("name", "avatarUrl", "type", "string", "optional", true),
                        Map.of("name", "followerCount", "type", "int32", "optional", true),
                        Map.of("name", "recipeCount", "type", "int32", "optional", true)
                )
        ));
    }

    private void createIngredientsCollection() {
        typesenseService.createCollection(Map.of(
                "name", "ingredients",
                "fields", List.of(
                        Map.of("name", "id", "type", "string"),
                        Map.of("name", "name", "type", "string"),
                        Map.of("name", "aliases", "type", "string[]", "optional", true),
                        Map.of("name", "category", "type", "string", "facet", true, "optional", true)
                )
        ));
    }
}
