package com.chefkix.social.post.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.request.CollectionRequest;
import com.chefkix.social.post.dto.response.CollectionProgressResponse;
import com.chefkix.social.post.dto.response.CollectionResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.service.CollectionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CollectionController {

    CollectionService collectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionResponse>> createCollection(
            @Valid @RequestBody CollectionRequest request) {
        CollectionResponse result = collectionService.createCollection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getMyCollections() {
        List<CollectionResponse> result = collectionService.getMyCollections();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponse>> getCollection(
            @PathVariable String collectionId) {
        CollectionResponse result = collectionService.getCollection(collectionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{collectionId}/posts")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getCollectionPosts(
            @PathVariable String collectionId) {
        List<PostResponse> result = collectionService.getCollectionPosts(collectionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponse>> updateCollection(
            @PathVariable String collectionId,
            @Valid @RequestBody CollectionRequest request) {
        CollectionResponse result = collectionService.updateCollection(collectionId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteCollection(@PathVariable String collectionId) {
        collectionService.deleteCollection(collectionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{collectionId}/posts/{postId}")
    public ResponseEntity<ApiResponse<CollectionResponse>> addPostToCollection(
            @PathVariable String collectionId,
            @PathVariable String postId) {
        CollectionResponse result = collectionService.addPostToCollection(collectionId, postId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{collectionId}/posts/{postId}")
    public ResponseEntity<ApiResponse<CollectionResponse>> removePostFromCollection(
            @PathVariable String collectionId,
            @PathVariable String postId) {
        CollectionResponse result = collectionService.removePostFromCollection(collectionId, postId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getUserPublicCollections(
            @PathVariable String userId) {
        List<CollectionResponse> result = collectionService.getUserPublicCollections(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ===============================================
    // FEATURED / SEASONAL COLLECTIONS
    // ===============================================

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getFeaturedCollections(
            @RequestParam(required = false) String season) {
        List<CollectionResponse> result = (season != null && !season.isBlank())
                ? collectionService.getFeaturedCollectionsBySeason(season.trim())
                : collectionService.getFeaturedCollections();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ===============================================
    // LEARNING PATH — Enrollment & Progress
    // ===============================================

    @PostMapping("/{collectionId}/enroll")
    public ResponseEntity<ApiResponse<CollectionProgressResponse>> enroll(
            @PathVariable String collectionId) {
        CollectionProgressResponse result = collectionService.enroll(collectionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
    }

    @GetMapping("/{collectionId}/progress")
    public ResponseEntity<ApiResponse<CollectionProgressResponse>> getProgress(
            @PathVariable String collectionId) {
        CollectionProgressResponse result = collectionService.getProgress(collectionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{collectionId}/progress")
    public ResponseEntity<ApiResponse<CollectionProgressResponse>> updateProgress(
            @PathVariable String collectionId,
            @RequestParam String recipeId,
            @RequestParam(defaultValue = "0") int xpEarned) {
        CollectionProgressResponse result = collectionService.updateProgress(collectionId, recipeId, xpEarned);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
