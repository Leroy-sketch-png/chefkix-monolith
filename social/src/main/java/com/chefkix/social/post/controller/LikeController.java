package com.chefkix.social.post.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.request.PollVoteRequest;
import com.chefkix.social.post.dto.request.PlateRateRequest;
import com.chefkix.social.post.dto.response.PollVoteResponse;
import com.chefkix.social.post.dto.response.PlateRateResponse;
import com.chefkix.social.post.dto.response.PostLikeResponse;
import com.chefkix.social.post.dto.response.PostSaveResponse;
import com.chefkix.social.post.service.PostService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LikeController {

    PostService postService;


    /**
     */
    @PostMapping("/toggle-like/{postId}")
    public ApiResponse<PostLikeResponse> toggleLike(
            @PathVariable("postId") String postId
    ) {
        PostLikeResponse response = postService.toggleLike(postId);
        return ApiResponse.success(response);
    }


    /**
     */
    @PostMapping("/toggle-save/{postId}")
    public ApiResponse<PostSaveResponse> toggleSave(
            @PathVariable("postId") String postId
    ) {
        PostSaveResponse response = postService.toggleSave(postId);
        return ApiResponse.success(response);
    }


    /**
     */
    @PostMapping("/{postId}/vote")
    public ApiResponse<PollVoteResponse> votePoll(
            @PathVariable("postId") String postId,
            @Valid @RequestBody PollVoteRequest request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        PollVoteResponse response = postService.votePoll(postId, request.getOption(), userId);
        return ApiResponse.success(response);
    }


    /**
     */
    @PostMapping("/{postId}/rate-plate")
    public ApiResponse<PlateRateResponse> ratePlate(
            @PathVariable("postId") String postId,
            @Valid @RequestBody PlateRateRequest request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        PlateRateResponse response = postService.ratePlate(postId, request.getRating(), userId);
        return ApiResponse.success(response);
    }
}
