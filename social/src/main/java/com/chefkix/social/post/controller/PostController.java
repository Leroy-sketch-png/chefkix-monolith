package com.chefkix.social.post.controller;

import com.chefkix.social.post.dto.request.PostCreationRequest;
import com.chefkix.social.post.dto.request.PostUpdateRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.response.BattleVoteResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.dto.response.RecipeReviewStatsResponse;
import com.chefkix.social.post.dto.response.TasteProfileResponse;
import com.chefkix.social.post.service.PostService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {

    PostService postService;

    // Uses "POST /" (standard REST)
    // Returns ResponseEntity for 201 status
    // ========================================================================
    // 1. CREATE PERSONAL POST
    // Endpoint: POST /api/v1/posts
    // ========================================================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPersonalPost(
            @Valid @ModelAttribute PostCreationRequest request
    ) {
        // 1. Get the current logged-in user's ID
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Call service to process
        PostResponse result = postService.createPersonalPost(request, currentUserId);

        // 3. Return standard REST (201 Created)
        ApiResponse<PostResponse> body = ApiResponse.created(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }


    // ========================================================================
    // 2. CREATE GROUP POST
    // Endpoint: POST /api/v1/groups/{groupId}/posts
    // ========================================================================
    @PostMapping(value = "/groups/{groupId}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createGroupPost(
            @PathVariable("groupId") String groupId,
            @Valid @ModelAttribute PostCreationRequest request
    ) {
        // 1. Get the current logged-in user's ID
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Call service to process (pass groupId)
        PostResponse result = postService.createGroupPost(groupId, request, currentUserId);

        // 3. Return standard REST (201 Created)
        ApiResponse<PostResponse> body = ApiResponse.created(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/groups/{groupId}/posts")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getGroupPosts(
            @PathVariable("groupId") String groupId,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication
    ) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getGroupPosts(groupId, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    // Uses "PUT /{postId}" (standard REST)
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable("postId") String postId, // Get ID from Path
            @Valid @RequestBody PostUpdateRequest postUpdateRequest) {

        PostResponse result = postService.updatePost(postId, postUpdateRequest);

        // Use factory method "success"
        ApiResponse<PostResponse> body = ApiResponse.success(result, "Updated successfully");

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable("postId") String postId,
            Authentication authentication) {
        postService.deletePost(authentication, postId);
        ApiResponse<String> body = ApiResponse.success("Post deleted successfully");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getGlobalFeed(
            @RequestParam(defaultValue = "0") int mode,
            @PageableDefault(size = 5) Pageable pageable,
            Authentication authentication) {
        
        // Get current user ID from JWT (may be null if unauthenticated)
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getAllPosts(mode, pageable, currentUserId);

        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getFollowingFeed(
            @RequestParam(defaultValue = "0") int mode,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        String currentUserId = authentication.getName();
        Page<PostResponse> result = postService.getFollowingFeed(mode, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getPostsByUserId(
            @RequestParam("userId") String userId,
            @PageableDefault(size = 5) Pageable pageable,
            Authentication authentication) {
        
        // Get current user ID from JWT (may be null if unauthenticated)
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getAllPostsByUserId(userId, pageable, currentUserId);

        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    /**
     * Get all posts saved/bookmarked by the current user.
     * Returns paginated list of saved posts, most recent first.
     */
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getSavedPosts(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<PostResponse> result = postService.getSavedPosts(pageable);
        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable String postId,
            Authentication authentication) {
        
        String currentUserId = authentication != null ? authentication.getName() : null;
        PostResponse result = postService.getPostById(postId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Search posts by content, tags, or display name.
     * GET /api/v1/posts/search?q=keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PostResponse>>> searchPosts(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.searchPosts(query, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    // ========================================================================
    // RECIPE REVIEWS
    // ========================================================================

    /**
     * Get all reviews for a specific recipe, newest first.
     * GET /api/v1/posts/reviews/recipe/{recipeId}
     */
    @GetMapping("/reviews/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getReviewsForRecipe(
            @PathVariable String recipeId,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getReviewsForRecipe(recipeId, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    /**
     * Get aggregate review stats for a recipe (average rating + total count).
     * GET /api/v1/posts/reviews/recipe/{recipeId}/stats
     */
    @GetMapping("/reviews/recipe/{recipeId}/stats")
    public ResponseEntity<ApiResponse<RecipeReviewStatsResponse>> getRecipeReviewStats(
            @PathVariable String recipeId) {
        RecipeReviewStatsResponse result = postService.getRecipeReviewStats(recipeId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========================================================================
    // RECIPE BATTLES
    // ========================================================================

    /**
     * Vote in a recipe battle (toggle: same choice removes vote).
     * POST /api/v1/posts/battles/{postId}/vote?choice=A|B
     */
    @PostMapping("/battles/{postId}/vote")
    public ResponseEntity<ApiResponse<BattleVoteResponse>> voteBattle(
            @PathVariable String postId,
            @RequestParam String choice) {
        BattleVoteResponse result = postService.voteBattle(postId, choice);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get active recipe battles (not yet ended), ordered by ending soonest.
     * GET /api/v1/posts/battles/active
     */
    @GetMapping("/battles/active")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getActiveBattles(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getActiveBattles(pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.successPage(result));
    }

    /**
     * Get the authenticated user's taste profile (cuisine distribution from 5-signal analysis).
     * GET /api/v1/posts/taste-profile
     */
    @GetMapping("/taste-profile")
    public ResponseEntity<ApiResponse<TasteProfileResponse>> getTasteProfile(Authentication authentication) {
        String userId = authentication.getName();
        TasteProfileResponse profile = postService.getTasteProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}