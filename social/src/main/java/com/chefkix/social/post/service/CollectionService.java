package com.chefkix.social.post.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.dto.request.CollectionRequest;
import com.chefkix.social.post.dto.response.CollectionResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Collection;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.mapper.PostMapper;
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
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }
}
