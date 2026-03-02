package com.chefkix.social.post.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.response.PostLikeResponse;
import com.chefkix.social.post.dto.response.PostSaveResponse;
import com.chefkix.social.post.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for post interactions: like and save (bookmark).
 */
@RestController
@RequestMapping("/posts")  // ← FIXED: Changed from /post to /posts to match Gateway route
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LikeController {

    PostService postService;

    // ========================================================================
    // LIKE ENDPOINTS
    // ========================================================================

    /**
     * Toggle like on a post.
     * If not liked: adds like. If already liked: removes like.
     * Frontend calls this single endpoint for all like/unlike actions.
     */
    @PostMapping("/toggle-like/{postId}")
    public ApiResponse<PostLikeResponse> toggleLike(
            @PathVariable String postId
    ) {
        PostLikeResponse response = postService.toggleLike(postId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{postId}/like")
    public ApiResponse<PostLikeResponse> postLike(
            @PathVariable String postId
    ) {
        PostLikeResponse response = postService.likePost(postId);
        return ApiResponse.created(response);
    }

    @DeleteMapping("/{postId}/like")
    public ApiResponse<PostLikeResponse> postUnlike(
            @PathVariable String postId
    ) {
        PostLikeResponse response = postService.unlikePost(postId);
        return ApiResponse.success(response, "Successfully unliked post");
    }

    // ========================================================================
    // SAVE (BOOKMARK) ENDPOINTS
    // ========================================================================

    /**
     * Toggle save (bookmark) on a post.
     * If not saved: saves post. If already saved: removes save.
     * Frontend calls this single endpoint for all save/unsave actions.
     */
    @PostMapping("/toggle-save/{postId}")
    public ApiResponse<PostSaveResponse> toggleSave(
            @PathVariable String postId
    ) {
        PostSaveResponse response = postService.toggleSave(postId);
        return ApiResponse.success(response);
    }
}
