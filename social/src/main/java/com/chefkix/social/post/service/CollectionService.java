package com.chefkix.social.post.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.dto.request.CollectionRequest;
import com.chefkix.social.post.dto.response.CollectionProgressResponse;
import com.chefkix.social.post.dto.response.CollectionResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Collection;
import com.chefkix.social.post.entity.CollectionProgress;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.social.post.repository.CollectionProgressRepository;
import com.chefkix.social.post.repository.CollectionRepository;
import com.chefkix.social.post.repository.PostRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CollectionService {

    CollectionRepository collectionRepository;
    CollectionProgressRepository collectionProgressRepository;
    PostRepository postRepository;
    PostMapper postMapper;

    private static final int MAX_COLLECTIONS_PER_USER = 50;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
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
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .isPublic(request.isPublic())
                .postIds(new ArrayList<>())
                .itemCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

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
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        // Only owner or public collections
        if (!collection.getUserId().equals(userId) && !collection.isPublic()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return toResponse(collection);
    }

    public List<PostResponse> getCollectionPosts(String collectionId) {
        String userId = getCurrentUserId();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.getUserId().equals(userId) && !collection.isPublic()) {
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

        collection.setName(request.getName().trim());
        collection.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        collection.setPublic(request.isPublic());
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
            collection.setItemCount(collection.getPostIds().size());

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
            collection.setItemCount(collection.getPostIds().size());
        }

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
