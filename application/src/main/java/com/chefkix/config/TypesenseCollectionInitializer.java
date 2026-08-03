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
        Map<String, Object> qualityTierField = Map.of(
                "name", "qualityTier", "type", "string", "facet", true, "optional", true);
        Map<String, Object> authorAvatarField = Map.of(
                "name", "authorAvatarUrl", "type", "string", "optional", true);
        Map<String, Object> xpRewardField = Map.of(
                "name", "xpReward", "type", "int32", "optional", true);
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
                        xpRewardField,
                        qualityTierField,
                        Map.of("name", "ingredients", "type", "string[]", "optional", true),
                        Map.of("name", "tags", "type", "string[]", "facet", true, "optional", true),
                        Map.of("name", "authorId", "type", "string", "optional", true),
                        Map.of("name", "authorName", "type", "string", "optional", true),
                        authorAvatarField,
                        Map.of("name", "coverImageUrl", "type", "string", "optional", true),
                        Map.of("name", "createdAt", "type", "int64"),
                        Map.of("name", "embedding", "type", "float[]", "num_dim", 3072,
                                "optional", true)
                ),
                "default_sorting_field", "createdAt",
                "token_separators", List.of("-", "'")
        ));
        if (!typesenseService.ensureCollectionField("recipes", qualityTierField)) {
            log.error("Typesense recipes qualityTier field is not ready; Foolproof filtering is unavailable");
        }
        if (!typesenseService.ensureCollectionField("recipes", authorAvatarField)) {
            log.error("Typesense recipes authorAvatarUrl field is not ready; creator avatars will be omitted");
        }
        if (!typesenseService.ensureCollectionField("recipes", xpRewardField)) {
            log.error("Typesense recipes xpReward field is not ready; XP proof will be omitted");
        }
    }

    private void createPostsCollection() {
        Map<String, Object> authorAvatarField = Map.of(
                "name", "authorAvatarUrl", "type", "string", "optional", true);
        Map<String, Object> photoField = Map.of(
                "name", "photoUrl", "type", "string", "optional", true);
        typesenseService.createCollection(Map.of(
                "name", "posts",
                "fields", List.of(
                        Map.of("name", "id", "type", "string"),
                        Map.of("name", "content", "type", "string", "optional", true),
                        Map.of("name", "authorId", "type", "string"),
                        Map.of("name", "authorName", "type", "string", "optional", true),
                        authorAvatarField,
                        Map.of("name", "likeCount", "type", "int32", "optional", true),
                        Map.of("name", "commentCount", "type", "int32", "optional", true),
                        Map.of("name", "recipeTitle", "type", "string", "optional", true),
                        photoField,
                        Map.of("name", "createdAt", "type", "int64")
                ),
                "default_sorting_field", "createdAt"
        ));
        if (!typesenseService.ensureCollectionField("posts", authorAvatarField)) {
            log.error("Typesense posts authorAvatarUrl field is not ready; creator avatars will be omitted");
        }
        if (!typesenseService.ensureCollectionField("posts", photoField)) {
            log.error("Typesense posts photoUrl field is not ready; post images will be omitted");
        }
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
