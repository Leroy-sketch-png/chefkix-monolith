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

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {

    CommentService commentService;
    ReplyService replyService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            Authentication authentication,
            @PathVariable("postId") String postId,
            @Valid @RequestBody CommentRequest req) {

        CommentResponse data = commentService.createComment(authentication, postId, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(data));
    }

    @PostMapping("/posts/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<ReplyResponse>> createReply(
Authentication authentication,
            @PathVariable("commentId") String commentId,
            @Valid @RequestBody ReplyRequest req) {

        ReplyResponse data = replyService.createReply(req);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(data));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAllComments(
            Authentication authentication,
            @PathVariable("postId") String postId,
            @PageableDefault(size = 20) Pageable pageable) {

        String currentUserId = authentication != null ? authentication.getName() : null;
        List<CommentResponse> data = commentService.getAllCommentsByPostId(postId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/posts/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<ReplyResponse>>> getAllReplies(
            Authentication authentication,
            @PathVariable("commentId") String commentId,
            @PageableDefault(size = 20) Pageable pageable) {

        String currentUserId = authentication != null ? authentication.getName() : null;
        List<ReplyResponse> data = replyService.getAllRepliesByCommentId(commentId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
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