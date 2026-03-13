package com.chefkix.social.post.controller;

import com.chefkix.social.post.dto.request.PostCreationRequest;
import com.chefkix.social.post.dto.request.PostUpdateRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.response.PostResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {

    PostService postService;

    // SỬA 1: Dùng "POST /" (chuẩn REST)
    // SỬA 2: Trả về ResponseEntity để có status 201
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @ModelAttribute PostCreationRequest post) {

        PostResponse result = postService.createPost(post);

        // SỬA 3: Dùng hàm factory "created"
        ApiResponse<PostResponse> body = ApiResponse.created(result);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // SỬA 1: Dùng "PUT /{postId}" (chuẩn REST)
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable("postId") String postId, // SỬA 2: Lấy ID từ Path
            @Valid @RequestBody PostUpdateRequest postUpdateRequest) {

        PostResponse result = postService.updatePost(postId, postUpdateRequest);

        // SỬA 3: Dùng hàm factory "success"
        ApiResponse<PostResponse> body = ApiResponse.success(result, "Cập nhật thành công");

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable("postId") String postId,
            Authentication authentication) {
        postService.deletePost(authentication, postId);
        ApiResponse<String> body = ApiResponse.success("Xóa bài post thành công");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getGlobalFeed(
            @RequestParam(defaultValue = "0") int mode,
            @PageableDefault(size = 5) Pageable pageable,
            Authentication authentication) {
        
        // Get current user ID from JWT (may be null if unauthenticated)
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getAllPosts(mode, pageable, currentUserId);

        ApiResponse<Page<PostResponse>> body = ApiResponse.success(result);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getFollowingFeed(
            @RequestParam(defaultValue = "0") int mode,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        String currentUserId = authentication.getName();
        Page<PostResponse> result = postService.getFollowingFeed(mode, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPostsByUserId(
            @RequestParam("userId") String userId,
            @PageableDefault(size = 5) Pageable pageable,
            Authentication authentication) {
        
        // Get current user ID from JWT (may be null if unauthenticated)
        String currentUserId = authentication != null ? authentication.getName() : null;
        Page<PostResponse> result = postService.getAllPostsByUserId(userId, pageable, currentUserId);

        ApiResponse<Page<PostResponse>> body = ApiResponse.success(result);

        return ResponseEntity.ok(body);
    }

    /**
     * Get all posts saved/bookmarked by the current user.
     * Returns paginated list of saved posts, most recent first.
     */
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getSavedPosts(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<PostResponse> result = postService.getSavedPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable String postId,
            Authentication authentication) {
        
        String currentUserId = authentication != null ? authentication.getName() : null;
        PostResponse result = postService.getPostById(postId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}