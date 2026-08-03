package com.chefkix.config;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.common.enums.RecipeStatus;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
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

        syncRecipes();
        syncPosts();
        syncUsers();
        syncKnowledgeIngredients();
        log.info("Typesense data sync complete");
    }

    void syncRecipes() {
        Query query = new Query(Criteria.where("status").is(RecipeStatus.PUBLISHED));
        List<Document> recipes = mongoTemplate.find(query, Document.class, "recipes");

        if (recipes.isEmpty()) {
            log.info("No published recipes to sync");
            return;
        }

        Map<String, PublicAuthor> authorsByUserId = loadPublicAuthors();
        List<Map<String, Object>> docs = recipes.stream()
                .map(this::recipeToDocument)
                .map(doc -> withAuthor(doc, authorsByUserId.get(Objects.toString(doc.get("authorId"), ""))))
                .collect(Collectors.toList());

        int synced = typesenseService.importDocuments("recipes", docs);
        log.info("Synced {}/{} recipes to Typesense", synced, recipes.size());
        if (synced != docs.size()) {
            log.warn("Skipping stale recipe pruning because the authoritative import was incomplete");
            return;
        }

        Set<String> authoritativeIds = docs.stream()
                .map(doc -> Objects.toString(doc.get("id"), ""))
                .filter(id -> !id.isBlank())
                .collect(Collectors.toSet());
        typesenseService.listDocumentIds("recipes").ifPresent(indexedIds -> {
            Set<String> staleIds = new HashSet<>(indexedIds);
            staleIds.removeAll(authoritativeIds);
            long deleted = staleIds.stream()
                    .filter(id -> typesenseService.deleteDocument("recipes", id))
                    .count();
            if (deleted == staleIds.size()) {
                log.info("Pruned {} stale recipe documents from Typesense", deleted);
            } else {
                log.warn("Pruned {}/{} stale recipe documents from Typesense", deleted, staleIds.size());
            }
        });
    }

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

    private Map<String, Object> recipeToDocument(Object recipe) {
        if (recipe instanceof Document document) {
            return mongoRecipeToDocument(document);
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", getStringValue(recipe, "getId"));
        doc.put("title", getStringValue(recipe, "getTitle"));
        doc.put("description", getStringValue(recipe, "getDescription"));
        doc.put("cuisine", getStringValue(recipe, "getCuisineType"));

        Object difficulty = invokeNoArg(recipe, "getDifficulty");
        if (difficulty != null) {
            Object difficultyValue = invokeNoArg(difficulty, "getValue");
            doc.put("difficulty", difficultyValue != null ? difficultyValue.toString() : difficulty.toString());
        } else {
            doc.put("difficulty", "");
        }

        doc.put("totalTime", getIntValue(recipe, "getTotalTimeMinutes"));
        doc.put("cookCount", getIntValue(recipe, "getCookCount"));
        doc.put("avgRating", getDoubleValue(recipe, "getAverageRating"));
        doc.put("xpReward", getIntValue(recipe, "getXpReward"));
        doc.put("qualityTier", qualityTierValue(invokeNoArg(recipe, "getQualityTier")));

        List<String> ingredientNames = new ArrayList<>();
        Object ingredients = invokeNoArg(recipe, "getFullIngredientList");
        if (ingredients instanceof Collection<?> list) {
            for (Object item : list) {
                String ingredientName = getStringValue(item, "getName");
                if (!ingredientName.isBlank()) {
                    ingredientNames.add(ingredientName);
                }
            }
        }
        doc.put("ingredients", ingredientNames);

        Object tags = invokeNoArg(recipe, "getDietaryTags");
        doc.put("tags", tags instanceof Collection<?> ? tags : List.of());
        doc.put("authorId", getStringValue(recipe, "getUserId"));

        Object coverImages = invokeNoArg(recipe, "getCoverImageUrl");
        if (coverImages instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            doc.put("coverImageUrl", list.get(0).toString());
        } else {
            doc.put("coverImageUrl", "");
        }

        Object createdAt = invokeNoArg(recipe, "getCreatedAt");
        if (createdAt != null) {
            Object epochSecond = invokeNoArg(createdAt, "getEpochSecond");
            doc.put("createdAt", epochSecond instanceof Number number ? number.longValue() : 0L);
        } else {
            doc.put("createdAt", 0L);
        }

        return doc;
    }

    private Map<String, Object> mongoRecipeToDocument(Document recipe) {
        Map<String, Object> doc = new LinkedHashMap<>();
        Object rawId = recipe.get("_id");
        doc.put("id", rawId != null ? rawId.toString() : "");
        doc.put("title", Objects.toString(recipe.get("title"), ""));
        doc.put("description", Objects.toString(recipe.get("description"), ""));
        doc.put("cuisine", Objects.toString(recipe.get("cuisineType"), ""));
        doc.put("difficulty", Objects.toString(recipe.get("difficulty"), ""));
        doc.put("totalTime", numberValue(recipe.get("totalTimeMinutes")).intValue());
        doc.put("cookCount", numberValue(recipe.get("cookCount")).intValue());
        doc.put("avgRating", numberValue(recipe.get("averageRating")).doubleValue());
        doc.put("xpReward", numberValue(recipe.get("xpReward")).intValue());
        doc.put("qualityTier", qualityTierValue(recipe.get("qualityTier")));

        List<String> ingredientNames = new ArrayList<>();
        Object rawIngredients = recipe.get("fullIngredientList");
        if (rawIngredients instanceof Collection<?> ingredients) {
            for (Object ingredient : ingredients) {
                Object name = ingredient instanceof Map<?, ?> map ? map.get("name") : null;
                if (name != null && !name.toString().isBlank()) ingredientNames.add(name.toString());
            }
        }
        doc.put("ingredients", ingredientNames);

        Object tags = recipe.get("dietaryTags");
        doc.put("tags", tags instanceof Collection<?> ? tags : List.of());
        doc.put("authorId", Objects.toString(recipe.get("userId"), ""));

        Object coverImages = recipe.get("coverImageUrl");
        if (coverImages instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            doc.put("coverImageUrl", list.get(0).toString());
        } else {
            doc.put("coverImageUrl", "");
        }

        Object createdAt = recipe.get("createdAt");
        doc.put("createdAt", createdAt instanceof Date date ? date.toInstant().getEpochSecond() : 0L);
        return doc;
    }

    private Number numberValue(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private String qualityTierValue(Object value) {
        if (value == null) return "";
        Object displayValue = invokeNoArg(value, "getValue");
        if (displayValue != null) return displayValue.toString();
        return switch (value.toString().trim().toUpperCase(Locale.ROOT).replace(' ', '_')) {
            case "FOOLPROOF" -> "Foolproof";
            case "GOOD" -> "Good";
            case "NEEDS_WORK" -> "Needs Work";
            case "DRAFT_QUALITY" -> "Draft Quality";
            default -> value.toString();
        };
    }

    public void indexRecipe(Object recipe) {
        Object status = invokeNoArg(recipe, "getStatus");
        if (status == null || !RecipeStatus.PUBLISHED.name().equals(status.toString())) {
            return;
        }
        Map<String, Object> document = recipeToDocument(recipe);
        String authorId = Objects.toString(document.get("authorId"), "");
        typesenseService.upsertDocument("recipes", withAuthor(document, findPublicAuthor(authorId)));
    }

    public void removeRecipe(String recipeId) {
        typesenseService.deleteDocument("recipes", recipeId);
    }

    /**
     */
    @EventListener
    public void onRecipeIndexEvent(Object event) {
        if (event == null || !"com.chefkix.culinary.features.recipe.events.RecipeIndexEvent".equals(event.getClass().getName())) {
            return;
        }

        String recipeId = getStringValue(event, "recipeId");
        String action = getStringValue(event, "action");
        Object recipe = invokeNoArg(event, "recipe");

        if (!typesenseService.isHealthy()) {
            log.warn("Typesense unreachable -- skipping real-time index for recipe {}", recipeId);
            return;
        }
        try {
            if ("INDEX".equals(action)) {
                indexRecipe(recipe);
                log.info("Real-time indexed recipe {} in Typesense", recipeId);
            } else if ("REMOVE".equals(action)) {
                removeRecipe(recipeId);
                log.info("Real-time removed recipe {} from Typesense", recipeId);
            }
        } catch (Exception e) {
            log.error("Real-time Typesense index failed for recipe {}: {}", recipeId, e.getMessage());
        }
    }


    void syncPosts() {
        Query query = new Query(Criteria.where("hidden").ne(true));
        List<Document> posts = mongoTemplate.find(query, Document.class, "post");

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

    private Map<String, Object> postToDocument(Object post) {
        if (post instanceof Document document) {
            return mongoPostToDocument(document);
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", getStringValue(post, "getId"));
        doc.put("content", getStringValue(post, "getContent"));
        doc.put("authorId", getStringValue(post, "getUserId"));
        doc.put("authorName", getStringValue(post, "getDisplayName"));
        doc.put("authorAvatarUrl", getStringValue(post, "getAvatarUrl"));
        doc.put("likeCount", getIntValue(post, "getLikes"));
        doc.put("commentCount", getIntValue(post, "getCommentCount"));
        doc.put("recipeTitle", getStringValue(post, "getRecipeTitle"));
        doc.put("photoUrl", firstListValue(invokeNoArg(post, "getPhotoUrls")));

        Object createdAt = invokeNoArg(post, "getCreatedAt");
        if (createdAt != null) {
            Object epochSecond = invokeNoArg(createdAt, "getEpochSecond");
            doc.put("createdAt", epochSecond instanceof Number number ? number.longValue() : 0L);
        } else {
            doc.put("createdAt", 0L);
        }

        return doc;
    }

    private Map<String, Object> mongoPostToDocument(Document post) {
        Map<String, Object> doc = new LinkedHashMap<>();
        Object rawId = post.get("_id");
        doc.put("id", rawId != null ? rawId.toString() : "");
        doc.put("content", Objects.toString(post.get("content"), ""));
        doc.put("authorId", Objects.toString(post.get("userId"), ""));
        doc.put("authorName", Objects.toString(post.get("displayName"), ""));
        doc.put("authorAvatarUrl", Objects.toString(post.get("avatarUrl"), ""));
        doc.put("likeCount", numberValue(post.get("likes")).intValue());
        doc.put("commentCount", numberValue(post.get("commentCount")).intValue());
        doc.put("recipeTitle", Objects.toString(post.get("recipeTitle"), ""));
        doc.put("photoUrl", firstListValue(post.get("photoUrls")));

        Object createdAt = post.get("createdAt");
        doc.put("createdAt", createdAt instanceof Date date ? date.toInstant().getEpochSecond() : 0L);
        return doc;
    }

    private String firstListValue(Object value) {
        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            return list.get(0).toString();
        }
        return "";
    }

    private Map<String, PublicAuthor> loadPublicAuthors() {
        List<Document> profiles = mongoTemplate.find(new Query(), Document.class, "user_profiles");
        if (profiles == null || profiles.isEmpty()) {
            return Map.of();
        }

        return profiles.stream()
                .map(this::publicAuthorFrom)
                .filter(author -> !author.userId().isBlank())
                .collect(Collectors.toMap(PublicAuthor::userId, author -> author, (left, right) -> left));
    }

    private PublicAuthor findPublicAuthor(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        Document profile = mongoTemplate.findOne(
                Query.query(Criteria.where("userId").is(userId)),
                Document.class,
                "user_profiles"
        );
        return profile != null ? publicAuthorFrom(profile) : null;
    }

    private PublicAuthor publicAuthorFrom(Document profile) {
        String displayName = Objects.toString(profile.get("displayName"), "").trim();
        if (displayName.isBlank()) {
            displayName = Objects.toString(profile.get("username"), "").trim();
        }
        return new PublicAuthor(
                Objects.toString(profile.get("userId"), ""),
                displayName,
                Objects.toString(profile.get("avatarUrl"), "")
        );
    }

    private Map<String, Object> withAuthor(Map<String, Object> document, PublicAuthor author) {
        if (author != null) {
            document.put("authorName", author.name());
            document.put("authorAvatarUrl", author.avatarUrl());
        }
        return document;
    }

    private record PublicAuthor(String userId, String name, String avatarUrl) {}

    @EventListener
    public void onPostIndexEvent(Object event) {
        if (event == null || !"com.chefkix.social.post.events.PostIndexEvent".equals(event.getClass().getName())) {
            return;
        }

        String postId = getStringValue(event, "postId");
        String action = getStringValue(event, "action");
        Object post = invokeNoArg(event, "post");

        if (!typesenseService.isHealthy()) {
            log.warn("Typesense unreachable -- skipping real-time index for post {}", postId);
            return;
        }
        try {
            if ("INDEX".equals(action)) {
                typesenseService.upsertDocument("posts", postToDocument(post));
                log.info("Real-time indexed post {} in Typesense", postId);
            } else if ("REMOVE".equals(action)) {
                typesenseService.deleteDocument("posts", postId);
                log.info("Real-time removed post {} from Typesense", postId);
            }
        } catch (Exception e) {
            log.error("Real-time Typesense index failed for post {}: {}", postId, e.getMessage());
        }
    }


    private Map<String, Object> userProfileToDocument(Object profile) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", getStringValue(profile, "getUserId"));
        doc.put("username", getStringValue(profile, "getUsername"));
        doc.put("displayName", getStringValue(profile, "getDisplayName"));
        doc.put("firstName", getStringValue(profile, "getFirstName"));
        doc.put("lastName", getStringValue(profile, "getLastName"));
        doc.put("bio", getStringValue(profile, "getBio"));
        doc.put("avatarUrl", getStringValue(profile, "getAvatarUrl"));

        Object stats = invokeNoArg(profile, "getStatistics");
        doc.put("followerCount", getIntValue(stats, "getFollowerCount"));
        doc.put("recipeCount", getIntValue(stats, "getTotalRecipesPublished"));
        return doc;
    }

    @EventListener
    public void onUserIndexEvent(Object event) {
        if (event == null || !"com.chefkix.identity.events.UserIndexEvent".equals(event.getClass().getName())) {
            return;
        }

        String userId = getStringValue(event, "userId");
        String action = getStringValue(event, "action");
        Object profile = invokeNoArg(event, "profile");

        if (!typesenseService.isHealthy()) {
            log.warn("Typesense unreachable -- skipping real-time index for user {}", userId);
            return;
        }

        try {
            if ("INDEX".equals(action)) {
                typesenseService.upsertDocument("users", userProfileToDocument(profile));
                log.info("Real-time indexed user {} in Typesense", userId);
            } else if ("REMOVE".equals(action)) {
                typesenseService.deleteDocument("users", userId);
                log.info("Real-time removed user {} from Typesense", userId);
            }
        } catch (Exception e) {
            log.error("Real-time Typesense index failed for user {}: {}", userId, e.getMessage());
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private String getStringValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value != null ? value.toString() : "";
    }

    private int getIntValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private double getDoubleValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
