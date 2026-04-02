package com.chefkix.social.story.controller;

// ... (các import)

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.dto.response.UserStoryFeedResponse;
import com.chefkix.social.story.service.StoryFeedService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.shaded.com.google.protobuf.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryFeedController {

    private final StoryFeedService storyFeedService;

    // Giả lập lấy ID từ Token
    private String getCurrentUserId() { return "user_dev_01"; }

    @GetMapping("/feed")
    public ApiResponse<List<UserStoryFeedResponse>> getFeed() {
        List<UserStoryFeedResponse> response = storyFeedService.getStoryFeed(getCurrentUserId());
        return ApiResponse.success(response);
    }

    @GetMapping("/user/{targetUserId}")
    public ApiResponse<List<StoryResponse>> getUserStories(@PathVariable("targetUserId") String targetUserId) {
        List<StoryResponse> response = storyFeedService.getUserActiveStories(getCurrentUserId(), targetUserId);
        return ApiResponse.success(response);
    }
}