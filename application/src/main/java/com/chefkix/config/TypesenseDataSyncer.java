package com.chefkix.config;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.events.RecipeIndexEvent;
import com.chefkix.identity.events.UserIndexEvent;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.social.post.events.PostIndexEvent;
import com.chefkix.social.post.entity.Post;
import com.chefkix.culinary.common.enums.RecipeStatus;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypesenseDataSyncer {

    TypesenseService typesenseService;
    MongoTemplate mongoTemplate;
    KnowledgeIngredientRepository knowledgeIngredientRepo;

    @Async
    @Order(10)
    @EventListener(ApplicationReadyEvent.class)
    public void syncExistingData() {
        if (!typesenseService.isHealthy()) {
            log.warn("Typesense is not reachable -- skipping data sync");
            return;
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        syncRecipes();
        syncPosts();
        syncUsers();
        syncKnowledgeIngredients();
        log.info("Typesense data sync complete");
    }

    private void syncRecipes() {
        Query query = new Query(Criteria.where("status").is(RecipeStatus.PUBLISHED));
        List<Recipe> recipes = mongoTemplate.find(query, Recipe.class);

        if (recipes.isEmpty()) {
            log.info("No published recipes to sync");
            return;
        }

        List<Map<String, Object>> docs = recipes.stream()
                .map(this::recipeToDocument)
                .collect(Collectors.toList());

        int synced = typesenseService.importDocuments("recipes", docs);
        log.info("Synced {}/{} recipes to Typesense", synced, recipes.size());
    }

    @SuppressWarnings("unchecked")
    private void syncUsers() {
        List<Map<String, Object>> userProfiles = new ArrayList<>();

        var rawProfiles = mongoTemplate.find(new Query(),
                org.bson.Document.class, "user_profiles");

        for (var doc : rawProfiles) {
            Map<String, Object> userDoc = new LinkedHashMap<>();
            userDoc.put("id", doc.getString("userId") != null ? doc.getString("userId") : doc.getObjectId("_id").toString());
            userDoc.put("username", doc.getString("username"));
            userDoc.put("displayName", doc.getString("displayName"));
            userDoc.put("firstName", doc.getString("firstName"));
            userDoc.put("lastName", doc.getString("lastName"));
            userDoc.put("bio", doc.getString("bio"));
            userDoc.put("avatarUrl", doc.getString("avatarUrl"));
            Integer followers = doc.getInteger("followerCount");
            userDoc.put("followerCount", followers != null ? followers : 0);
            Integer recipeCount = doc.getInteger("recipesCreated");
            userDoc.put("recipeCount", recipeCount != null ? recipeCount : 0);
            userProfiles.add(userDoc);
        }

        if (!userProfiles.isEmpty()) {
            int synced = typesenseService.importDocuments("users", userProfiles);
            log.info("Synced {}/{} users to Typesense", synced, userProfiles.size());
        }
    }

    private void syncKnowledgeIngredients() {
        List<KnowledgeIngredient> ingredients = knowledgeIngredientRepo.findAll();
        if (ingredients.isEmpty()) {
            log.info("No knowledge graph ingredients to sync");
            return;
        }

        List<Map<String, Object>> docs = ingredients.stream()
                .map(ing -> {
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("id", ing.getId());
                    doc.put("name", ing.getName() != null ? ing.getName() : "");
                    doc.put("aliases", ing.getAliases() != null ? ing.getAliases() : List.of());
                    doc.put("category", ing.getCategory() != null ? ing.getCategory() : "");
                    return doc;
                })
                .collect(Collectors.toList());

        int synced = typesenseService.importDocuments("ingredients", docs);
        log.info("Synced {}/{} knowledge ingredients to Typesense", synced, ingredients.size());
    }

    private Map<String, Object> recipeToDocument(Recipe recipe) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", recipe.getId());
        doc.put("title", recipe.getTitle() != null ? recipe.getTitle() : "");
        doc.put("description", recipe.getDescription() != null ? recipe.getDescription() : "");
        doc.put("cuisine", recipe.getCuisineType() != null ? recipe.getCuisineType() : "");
        doc.put("difficulty", recipe.getDifficulty() != null ? recipe.getDifficulty().name() : "");
        doc.put("totalTime", recipe.getTotalTimeMinutes());
        doc.put("cookCount", (int) recipe.getCookCount());
        doc.put("avgRating", recipe.getAverageRating() != null ? recipe.getAverageRating() : 0.0);

        List<String> ingredientNames = new ArrayList<>();
        if (recipe.getFullIngredientList() != null) {
            recipe.getFullIngredientList().forEach(ing -> {
                if (ing.getName() != null) ingredientNames.add(ing.getName());
            });
        }
        doc.put("ingredients", ingredientNames);

        doc.put("tags", recipe.getDietaryTags() != null ? recipe.getDietaryTags() : List.of());
        doc.put("authorId", recipe.getUserId() != null ? recipe.getUserId() : "");
        doc.put("coverImageUrl",
                recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty()
                        ? recipe.getCoverImageUrl().get(0) : "");
        doc.put("createdAt",
                recipe.getCreatedAt() != null ? recipe.getCreatedAt().getEpochSecond() : 0L);

        return doc;
    }

    public void indexRecipe(Recipe recipe) {
        if (recipe.getStatus() != RecipeStatus.PUBLISHED) return;
        typesenseService.upsertDocument("recipes", recipeToDocument(recipe));
    }

    public void removeRecipe(String recipeId) {
        typesenseService.deleteDocument("recipes", recipeId);
    }

    /**
     * Real-time indexing handler. Fired synchronously by the Spring event bus
     * when DraftService publishes a recipe or RecipeService archives one.
     * Best-effort: if Typesense is down the recipe is still published,
     * and the startup sync will re-index on next boot.
     */
    @EventListener
    public void onRecipeIndexEvent(RecipeIndexEvent event) {
        if (!typesenseService.isHealthy()) {
            log.warn("Typesense unreachable -- skipping real-time index for recipe {}", event.recipeId());
            return;
        }
        try {
            if ("INDEX".equals(event.action())) {
                indexRecipe(event.recipe());
                log.info("Real-time indexed recipe {} in Typesense", event.recipeId());
            } else if ("REMOVE".equals(event.action())) {
                removeRecipe(event.recipeId());
                log.info("Real-time removed recipe {} from Typesense", event.recipeId());
            }
        } catch (Exception e) {
            log.error("Real-time Typesense index failed for recipe {}: {}", event.recipeId(), e.getMessage());
        }
    }

    // ========================================================================
    // POST INDEXING
    // ========================================================================

    private void syncPosts() {
        Query query = new Query(Criteria.where("hidden").ne(true));
        List<Post> posts = mongoTemplate.find(query, Post.class);

        if (posts.isEmpty()) {
            log.info("No posts to sync");
            return;
        }

        List<Map<String, Object>> docs = posts.stream()
                .map(this::postToDocument)
                .collect(Collectors.toList());

        int synced = typesenseService.importDocuments("posts", docs);
        log.info("Synced {}/{} posts to Typesense", synced, posts.size());
    }

    private Map<String, Object> postToDocument(Post post) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", post.getId());
        doc.put("content", post.getContent() != null ? post.getContent() : "");
        doc.put("authorId", post.getUserId() != null ? post.getUserId() : "");
        doc.put("authorName", post.getDisplayName() != null ? post.getDisplayName() : "");
        doc.put("likeCount", post.getLikes() != null ? post.getLikes() : 0);
        doc.put("commentCount", post.getCommentCount() != null ? post.getCommentCount() : 0);
        doc.put("recipeTitle", post.getRecipeTitle() != null ? post.getRecipeTitle() : "");
        doc.put("createdAt", post.getCreatedAt() != null ? post.getCreatedAt().getEpochSecond() : 0L);
        return doc;
    }

    @EventListener
    public void onPostIndexEvent(PostIndexEvent event) {
        if (!typesenseService.isHealthy()) {
            log.warn("Typesense unreachable -- skipping real-time index for post {}", event.postId());
            return;
        }
        try {
            if ("INDEX".equals(event.action())) {
                typesenseService.upsertDocument("posts", postToDocument(event.post()));
                log.info("Real-time indexed post {} in Typesense", event.postId());
            } else if ("REMOVE".equals(event.action())) {
                typesenseService.deleteDocument("posts", event.postId());
                log.info("Real-time removed post {} from Typesense", event.postId());
            }
        } catch (Exception e) {
            log.error("Real-time Typesense index failed for post {}: {}", event.postId(), e.getMessage());
        }
    }

    // ========================================================================
    // USER INDEXING (real-time)
    // ========================================================================

    private Map<String, Object> userProfileToDocument(UserProfile profile) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", profile.getUserId());
        doc.put("username", profile.getUsername());
        doc.put("displayName", profile.getDisplayName());
        doc.put("firstName", profile.getFirstName());
        doc.put("lastName", profile.getLastName());
        doc.put("bio", profile.getBio());
        doc.put("avatarUrl", profile.getAvatarUrl());
        var stats = profile.getStatistics();
        doc.put("followerCount", stats != null && stats.getFollowerCount() != null ? stats.getFollowerCount().intValue() : 0);
        doc.put("recipeCount", stats != null && stats.getTotalRecipesPublished() != null ? stats.getTotalRecipesPublished().intValue() : 0);
        return doc;
    }

    @EventListener
    public void onUserIndexEvent(UserIndexEvent event) {
        if (!typesenseService.isHealthy()) {
            log.warn("Typesense unreachable -- skipping real-time index for user {}", event.userId());
            return;
        }
        try {
            if ("INDEX".equals(event.action())) {
                typesenseService.upsertDocument("users", userProfileToDocument(event.profile()));
                log.info("Real-time indexed user {} in Typesense", event.userId());
            } else if ("REMOVE".equals(event.action())) {
                typesenseService.deleteDocument("users", event.userId());
                log.info("Real-time removed user {} from Typesense", event.userId());
            }
        } catch (Exception e) {
            log.error("Real-time Typesense index failed for user {}: {}", event.userId(), e.getMessage());
        }
    }
}
