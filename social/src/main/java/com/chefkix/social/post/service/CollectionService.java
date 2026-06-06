package com.chefkix.social.post.service;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.dto.request.CollectionRequest;
import com.chefkix.social.post.dto.response.CollectionProgressResponse;
import com.chefkix.social.post.dto.response.CollectionResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Collection;
import com.chefkix.social.post.entity.CollectionProgress;
import com.chefkix.social.post.entity.DifficultyStep;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.social.post.repository.CollectionProgressRepository;
import com.chefkix.social.post.repository.CollectionRepository;
import com.chefkix.social.post.repository.PostRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CollectionService {

    private static final String COLLECTION_TYPE_BOOKMARK = "BOOKMARK";
    private static final String COLLECTION_TYPE_LEARNING_PATH = "LEARNING_PATH";
    private static final String COLLECTION_TYPE_SEASONAL = "SEASONAL";
    private static final int DEFAULT_RECIPE_XP = 50;

    CollectionRepository collectionRepository;
    CollectionProgressRepository collectionProgressRepository;
    PostRepository postRepository;
    PostMapper postMapper;
    RecipeProvider recipeProvider;

    private static final int MAX_COLLECTIONS_PER_USER = 50;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String name = authentication.getName();
        if (!StringUtils.hasText(name) || "anonymousUser".equalsIgnoreCase(name)) {
            return null;
        }

        return name;
    }

    @Transactional
    public CollectionResponse createCollection(CollectionRequest request) {
        String userId = getCurrentUserId();

        if (collectionRepository.countByUserId(userId) >= MAX_COLLECTIONS_PER_USER) {
            throw new AppException(ErrorCode.COLLECTION_LIMIT_EXCEEDED);
        }

        Collection collection = Collection.builder()
                .userId(userId)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .isPublic(request.isPublic())
                .postIds(new ArrayList<>())
                .recipeIds(new ArrayList<>())
                .difficultyProgression(new ArrayList<>())
                .itemCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        String collectionType = normalizeCollectionType(request.getCollectionType(), COLLECTION_TYPE_BOOKMARK);
        collection.setCollectionType(collectionType);
        if (COLLECTION_TYPE_LEARNING_PATH.equals(collectionType)) {
            applyLearningPathFields(collection, request, true);
        } else {
            applyRecipeCollectionFields(collection, request, true);
        }

        collection = collectionRepository.save(collection);
        return toResponse(collection);
    }

    public List<CollectionResponse> getMyCollections() {
        String userId = getCurrentUserId();
        return collectionRepository.findAllByUserId(userId, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(this::toResponse).toList();
    }

    public List<CollectionResponse> getUserPublicCollections(String userId) {
        return collectionRepository.findAllByUserIdAndIsPublicTrue(userId, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(this::toResponse).toList();
    }

    public CollectionResponse getCollection(String collectionId) {
        String userId = getCurrentUserIdOrNull();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        // Only owner or public collections
        if (!collection.isPublic() && !collection.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return toResponse(collection);
    }

    public List<PostResponse> getCollectionPosts(String collectionId) {
        String userId = getCurrentUserIdOrNull();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.isPublic() && !collection.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (collection.getPostIds() == null || collection.getPostIds().isEmpty()) {
            return List.of();
        }

        List<Post> posts = postRepository.findAllById(collection.getPostIds());
        return posts.stream().map(postMapper::toPostResponse).toList();
    }

    @Transactional
    public CollectionResponse updateCollection(String collectionId, CollectionRequest request) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String existingType = normalizeCollectionType(collection.getCollectionType(), COLLECTION_TYPE_BOOKMARK);
        String requestedType = normalizeCollectionType(request.getCollectionType(), existingType);
        if (!existingType.equals(requestedType)) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        collection.setName(request.getName().trim());
        collection.setDescription(trimToNull(request.getDescription()));
        collection.setPublic(request.isPublic());
        collection.setCollectionType(requestedType);
        if (COLLECTION_TYPE_LEARNING_PATH.equals(requestedType)) {
            applyLearningPathFields(collection, request, false);
        } else {
            applyRecipeCollectionFields(collection, request, false);
        }
        collection.setUpdatedAt(Instant.now());

        collection = collectionRepository.save(collection);
        return toResponse(collection);
    }

    @Transactional
    public void deleteCollection(String collectionId) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        collectionRepository.delete(collection);
    }

    @Transactional
    public CollectionResponse addPostToCollection(String collectionId, String postId) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }

        // Idempotent — don't add twice
        if (collection.getPostIds() == null) {
            collection.setPostIds(new ArrayList<>());
        }
        if (!collection.getPostIds().contains(postId)) {
            collection.getPostIds().add(postId);
            collection.setItemCount(calculateItemCount(collection));

            // Update cover image from latest added post
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null && post.getPhotoUrls() != null && !post.getPhotoUrls().isEmpty()) {
                collection.setCoverImageUrl(post.getPhotoUrls().get(0));
            }
        }

        collection.setUpdatedAt(Instant.now());
        collection = collectionRepository.save(collection);
        return toResponse(collection);
    }

    @Transactional
    public CollectionResponse removePostFromCollection(String collectionId, String postId) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (collection.getPostIds() != null) {
            collection.getPostIds().remove(postId);
        }

        collection.setItemCount(calculateItemCount(collection));

        collection.setUpdatedAt(Instant.now());
        collection = collectionRepository.save(collection);
        return toResponse(collection);
    }

    // ===============================================
    // LEARNING PATH — Enrollment & Progress
    // ===============================================

    @Transactional
    public CollectionProgressResponse enroll(String collectionId) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!"LEARNING_PATH".equals(collection.getCollectionType())) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // Idempotent — return existing progress if already enrolled
        CollectionProgress existing = collectionProgressRepository
                .findByUserIdAndCollectionId(userId, collectionId).orElse(null);
        if (existing != null) {
            return toProgressResponse(existing, collection);
        }

        CollectionProgress progress = CollectionProgress.builder()
                .userId(userId)
                .collectionId(collectionId)
                .completedRecipeIds(new ArrayList<>())
                .currentRecipeIndex(0)
                .totalXpEarned(0)
                .build();
        progress = collectionProgressRepository.save(progress);

        // Increment enrolled count
        collection.setEnrolledCount(collection.getEnrolledCount() + 1);
        collectionRepository.save(collection);

        return toProgressResponse(progress, collection);
    }

    public CollectionProgressResponse getProgress(String collectionId) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        CollectionProgress progress = collectionProgressRepository
                .findByUserIdAndCollectionId(userId, collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        return toProgressResponse(progress, collection);
    }

    @Transactional
    public CollectionProgressResponse updateProgress(String collectionId, String completedRecipeId, int xpEarned) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        CollectionProgress progress = collectionProgressRepository
                .findByUserIdAndCollectionId(userId, collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        // Idempotent — skip if already completed
        if (!progress.getCompletedRecipeIds().contains(completedRecipeId)) {
            progress.getCompletedRecipeIds().add(completedRecipeId);
            progress.setTotalXpEarned(progress.getTotalXpEarned() + xpEarned);

            // Advance index to next uncompleted recipe
            List<String> pathRecipes = collection.getRecipeIds();
            if (pathRecipes != null) {
                int nextIndex = progress.getCurrentRecipeIndex();
                while (nextIndex < pathRecipes.size()
                        && progress.getCompletedRecipeIds().contains(pathRecipes.get(nextIndex))) {
                    nextIndex++;
                }
                progress.setCurrentRecipeIndex(nextIndex);
            }
        }

        progress = collectionProgressRepository.save(progress);
        return toProgressResponse(progress, collection);
    }

    // ===============================================
    // FEATURED / SEASONAL COLLECTIONS
    // ===============================================

    /**
     * Get all featured collections (public, isFeatured=true), ordered by updatedAt DESC.
     * Used on Explore page for "Season's Best" and curated sections.
     */
    public List<CollectionResponse> getFeaturedCollections() {
        return collectionRepository.findAllByIsFeaturedTrueAndIsPublicTrue(
                Sort.by(Sort.Direction.DESC, "updatedAt")
        ).stream().map(this::toResponse).toList();
    }

    /**
     * Get featured collections matching a specific season tag.
     */
    public List<CollectionResponse> getFeaturedCollectionsBySeason(String seasonTag) {
        return collectionRepository.findAllByIsFeaturedTrueAndSeasonTag(
                seasonTag, Sort.by(Sort.Direction.DESC, "updatedAt")
        ).stream().map(this::toResponse).toList();
    }

    // ===============================================
    // MAPPING
    // ===============================================

    private void applyRecipeCollectionFields(Collection collection, CollectionRequest request, boolean isCreate) {
        if (request.getRecipeIds() == null) {
            if (isCreate) {
                collection.setRecipeIds(new ArrayList<>());
            }
            collection.setItemCount(calculateItemCount(collection));
            return;
        }

        List<String> recipeIds = normalizeRecipeIds(request.getRecipeIds());
        String coverImageUrl = recipeIds.isEmpty() ? null : validateRecipeIds(recipeIds);

        collection.setRecipeIds(new ArrayList<>(recipeIds));
        collection.setItemCount(calculateItemCount(collection));

        if (StringUtils.hasText(coverImageUrl)) {
            collection.setCoverImageUrl(coverImageUrl);
        } else if (collection.getPostIds() == null || collection.getPostIds().isEmpty()) {
            collection.setCoverImageUrl(null);
        }
    }

    private void applyLearningPathFields(Collection collection, CollectionRequest request, boolean isCreate) {
        boolean recipeStructureChanged = isCreate
                || request.getRecipeIds() != null
                || request.getDifficultyProgression() != null;

        List<DifficultyStep> stages = request.getDifficultyProgression() != null
                ? normalizeDifficultyProgression(request.getDifficultyProgression())
                : copyDifficultyProgression(collection.getDifficultyProgression());

        List<String> recipeIds;
        if (!stages.isEmpty()) {
            recipeIds = flattenRecipeIds(stages);
        } else if (request.getRecipeIds() != null) {
            recipeIds = normalizeRecipeIds(request.getRecipeIds());
        } else {
            recipeIds = copyStringList(collection.getRecipeIds());
        }

        if (recipeIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        collection.setPostIds(new ArrayList<>());
        collection.setRecipeIds(new ArrayList<>(recipeIds));
        collection.setDifficultyProgression(new ArrayList<>(stages));
        collection.setDifficulty(resolveLearningPathDifficulty(
                request.getDifficulty(),
                stages,
                recipeStructureChanged,
                collection.getDifficulty()));
        collection.setEstimatedTotalMinutes(resolveEstimatedTotalMinutes(
                request.getEstimatedTotalMinutes(),
                recipeStructureChanged,
                collection.getEstimatedTotalMinutes()));
        collection.setTotalXp(resolveTotalXp(
                request.getTotalXp(),
                recipeStructureChanged,
                collection.getTotalXp(),
                recipeIds.size()));
        collection.setItemCount(calculateItemCount(collection));

        if (recipeStructureChanged) {
            String coverImageUrl = validateRecipeIds(recipeIds);
            collection.setCoverImageUrl(StringUtils.hasText(coverImageUrl) ? coverImageUrl : null);
        }
    }

    private String normalizeCollectionType(String rawType, String defaultType) {
        String resolved = StringUtils.hasText(rawType) ? rawType.trim().toUpperCase(Locale.ROOT) : defaultType;
        if (!COLLECTION_TYPE_BOOKMARK.equals(resolved)
                && !COLLECTION_TYPE_LEARNING_PATH.equals(resolved)
                && !COLLECTION_TYPE_SEASONAL.equals(resolved)) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }
        return resolved;
    }

    private List<String> normalizeRecipeIds(List<String> recipeIds) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String recipeId : recipeIds) {
            if (StringUtils.hasText(recipeId)) {
                normalized.add(recipeId.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<DifficultyStep> normalizeDifficultyProgression(List<DifficultyStep> stages) {
        List<DifficultyStep> normalizedStages = new ArrayList<>();
        LinkedHashSet<String> seenRecipeIds = new LinkedHashSet<>();

        for (int index = 0; index < stages.size(); index++) {
            DifficultyStep stage = stages.get(index);
            List<String> stageRecipeIds = new ArrayList<>();

            if (stage != null && stage.getRecipeIds() != null) {
                for (String recipeId : normalizeRecipeIds(stage.getRecipeIds())) {
                    if (seenRecipeIds.add(recipeId)) {
                        stageRecipeIds.add(recipeId);
                    }
                }
            }

            normalizedStages.add(DifficultyStep.builder()
                    .label(StringUtils.hasText(stage != null ? stage.getLabel() : null)
                            ? stage.getLabel().trim()
                            : "Stage " + (index + 1))
                    .difficulty(StringUtils.hasText(stage != null ? stage.getDifficulty() : null)
                            ? stage.getDifficulty().trim()
                            : "Beginner")
                    .recipeIds(stageRecipeIds)
                    .order(index)
                    .build());
        }

        return normalizedStages;
    }

    private List<String> flattenRecipeIds(List<DifficultyStep> stages) {
        List<String> recipeIds = new ArrayList<>();
        for (DifficultyStep stage : stages) {
            if (stage.getRecipeIds() != null) {
                recipeIds.addAll(stage.getRecipeIds());
            }
        }
        return recipeIds;
    }

    private String validateRecipeIds(List<String> recipeIds) {
        String firstCoverImageUrl = null;
        for (String recipeId : recipeIds) {
            if (!StringUtils.hasText(recipeId) || recipeProvider.getRecipeSummary(recipeId) == null) {
                throw new AppException(ErrorCode.INVALID_OPERATION);
            }

            if (firstCoverImageUrl == null) {
                var summary = recipeProvider.getRecipeSummary(recipeId);
                if (summary != null && StringUtils.hasText(summary.getCoverImageUrl())) {
                    firstCoverImageUrl = summary.getCoverImageUrl();
                }
            }
        }
        return firstCoverImageUrl;
    }

    private String resolveLearningPathDifficulty(
            String requestedDifficulty,
            List<DifficultyStep> stages,
            boolean recipeStructureChanged,
            String existingDifficulty) {
        if (StringUtils.hasText(requestedDifficulty)) {
            return requestedDifficulty.trim();
        }
        if (!stages.isEmpty() && StringUtils.hasText(stages.get(stages.size() - 1).getDifficulty())) {
            return stages.get(stages.size() - 1).getDifficulty().trim();
        }
        return recipeStructureChanged ? null : existingDifficulty;
    }

    private Integer resolveEstimatedTotalMinutes(
            Integer requestedMinutes,
            boolean recipeStructureChanged,
            Integer existingMinutes) {
        if (requestedMinutes != null) {
            return requestedMinutes;
        }
        return recipeStructureChanged ? null : existingMinutes;
    }

    private Integer resolveTotalXp(
            Integer requestedTotalXp,
            boolean recipeStructureChanged,
            Integer existingTotalXp,
            int recipeCount) {
        if (requestedTotalXp != null) {
            return requestedTotalXp;
        }
        if (!recipeStructureChanged && existingTotalXp != null) {
            return existingTotalXp;
        }
        return recipeCount * DEFAULT_RECIPE_XP;
    }

    private List<String> copyStringList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private List<DifficultyStep> copyDifficultyProgression(List<DifficultyStep> stages) {
        return stages == null ? new ArrayList<>() : new ArrayList<>(stages);
    }

    private int calculateItemCount(Collection collection) {
        int postCount = collection.getPostIds() != null ? collection.getPostIds().size() : 0;
        int recipeCount = collection.getRecipeIds() != null ? collection.getRecipeIds().size() : 0;
        return postCount + recipeCount;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private CollectionResponse toResponse(Collection collection) {
        return CollectionResponse.builder()
                .id(collection.getId())
                .userId(collection.getUserId())
                .name(collection.getName())
                .description(collection.getDescription())
                .coverImageUrl(collection.getCoverImageUrl())
                .isPublic(collection.isPublic())
                .itemCount(collection.getItemCount())
                .postIds(collection.getPostIds())
                .collectionType(collection.getCollectionType())
                .recipeIds(collection.getRecipeIds())
                .difficulty(collection.getDifficulty())
                .estimatedTotalMinutes(collection.getEstimatedTotalMinutes())
                .totalXp(collection.getTotalXp())
                .enrolledCount(collection.getEnrolledCount())
                .completionRate(collection.getCompletionRate())
                .averageRating(collection.getAverageRating())
                .difficultyProgression(collection.getDifficultyProgression())
                .isFeatured(collection.isFeatured())
                .seasonTag(collection.getSeasonTag())
                .tagline(collection.getTagline())
                .emoji(collection.getEmoji())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }

    private CollectionProgressResponse toProgressResponse(CollectionProgress progress, Collection collection) {
        int totalRecipes = collection.getRecipeIds() != null ? collection.getRecipeIds().size() : 0;
        int completed = progress.getCompletedRecipeIds() != null ? progress.getCompletedRecipeIds().size() : 0;
        double percent = totalRecipes > 0 ? (double) completed / totalRecipes * 100.0 : 0.0;

        return CollectionProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .collectionId(progress.getCollectionId())
                .completedRecipeIds(progress.getCompletedRecipeIds())
                .currentRecipeIndex(progress.getCurrentRecipeIndex())
                .totalXpEarned(progress.getTotalXpEarned())
                .totalRecipes(totalRecipes)
                .progressPercent(Math.round(percent * 10.0) / 10.0)
                .startedAt(progress.getStartedAt())
                .lastActivityAt(progress.getLastActivityAt())
                .build();
    }
}
