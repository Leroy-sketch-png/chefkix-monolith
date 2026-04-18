package com.chefkix.social.post.controller;

import com.chefkix.social.post.dto.request.CommentRequest;
import com.chefkix.social.post.dto.request.ReplyRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.response.CommentLikeResponse;
import com.chefkix.social.post.dto.response.CommentResponse;
import com.chefkix.social.post.dto.response.ReplyLikeResponse;
import com.chefkix.social.post.dto.response.ReplyResponse;
import com.chefkix.social.post.service.CommentService;
import com.chefkix.social.post.service.ReplyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
// Import pagination and HTTP libraries

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Standard REST prefix
@RequestMapping("")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {

    CommentService commentService;
    ReplyService replyService;

    // REST-standard URL to create comment
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            Authentication authentication,
            @PathVariable("postId") String postId,
            @Valid @RequestBody CommentRequest req) {

        // Call service
        CommentResponse data = commentService.createComment(authentication, postId, req);

        // Return 201 CREATED using factory
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(data));
    }

    // REST-standard URL to create reply
    // FIXED: Added /posts prefix to match Gateway routing (/api/v1/posts/** → StripPrefix=2 → /posts/**)
    @PostMapping("/posts/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<ReplyResponse>> createReply(
            Authentication authentication, // Added authentication
            @PathVariable("commentId") String commentId,
            @Valid @RequestBody ReplyRequest req) {

        // Call service
        ReplyResponse data = replyService.createReply(req);

        // Return 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(data));
    }

    // REST-standard URL
    // Added Pageable, returns Page<T>
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAllComments(
            Authentication authentication,
            @PathVariable("postId") String postId,
            @PageableDefault(size = 20) Pageable pageable) {

        // Get current user ID for isLiked check (null-safe for anonymous)
        String currentUserId = authentication != null ? authentication.getName() : null;
        List<CommentResponse> data = commentService.getAllCommentsByPostId(postId, currentUserId);

        // Use factory
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // REST-standard URL
    // Added Pageable, returns Page<T>
    // FIXED: Added /posts prefix to match Gateway routing
    @GetMapping("/posts/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<ReplyResponse>>> getAllReplies(
            Authentication authentication,
            @PathVariable("commentId") String commentId,
            @PageableDefault(size = 20) Pageable pageable) {

        // Get current user ID for isLiked check (null-safe for anonymous)
        String currentUserId = authentication != null ? authentication.getName() : null;
        List<ReplyResponse> data = replyService.getAllRepliesByCommentId(commentId, currentUserId);

        // Use factory
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Delete a comment. Only the comment owner can delete.
     * FE: DELETE /posts/{postId}/comments/{commentId}
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            Authentication authentication,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId) {

        commentService.deleteComment(authentication, postId, commentId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Toggle like on a comment.
     * FE: POST /posts/{postId}/comments/{commentId}/like
     */
    @PostMapping("/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentLikeResponse>> toggleLikeComment(
            Authentication authentication,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId) {

        CommentLikeResponse response = commentService.toggleLike(authentication, commentId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a reply. Only the reply owner can delete.
     * FE: DELETE /posts/comments/{commentId}/replies/{replyId}
     */
    @DeleteMapping("/posts/comments/{commentId}/replies/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            Authentication authentication,
            @PathVariable("commentId") String commentId,
            @PathVariable("replyId") String replyId) {

        replyService.deleteReply(authentication, replyId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Toggle like on a reply.
     * FE: POST /posts/comments/{commentId}/replies/{replyId}/like
     */
    @PostMapping("/posts/comments/{commentId}/replies/{replyId}/like")
    public ResponseEntity<ApiResponse<ReplyLikeResponse>> toggleLikeReply(
            Authentication authentication,
            @PathVariable("commentId") String commentId,
            @PathVariable("replyId") String replyId) {

        ReplyLikeResponse response = replyService.toggleLike(authentication, replyId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}