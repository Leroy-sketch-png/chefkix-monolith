package com.chefkix.social.story.controller;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.dto.response.UserStoryFeedResponse;
import com.chefkix.social.story.service.StoryFeedService;
import com.chefkix.social.story.service.StoryInteractionService;
import com.cloudinary.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StoryInteractionController {

    private final StoryInteractionService interactionService;

    // --- VIEW & INTERACTION ---

    @PostMapping("/stories/{storyId}/views")
    public ApiResponse<String> recordView(@PathVariable String storyId) {
        interactionService.recordView(storyId, getCurrentUserId());
        return ApiResponse.success("success");
    }

    @GetMapping("/stories/{storyId}/views")
    public ApiResponse<List<String>> getStoryViewers(@PathVariable String storyId) {
        // Trả về danh sách userId, Mobile App sẽ dùng danh sách này gọi UserService lấy tên/avatar
        return ApiResponse.success(interactionService.getViewerIds(storyId, getCurrentUserId()));
    }

    @PostMapping("/stories/{storyId}/reactions")
    public ApiResponse<String> reactToStory(
            @PathVariable String storyId,
            @RequestParam String type) { // type = "FIRE", "HEART"...
        interactionService.recordReaction(storyId, getCurrentUserId(), type);
        return ApiResponse.success("successfully reacted");
    }

    // --- HIGHLIGHTS ---

//    @PostMapping("/highlights")
//    public ResponseEntity<Void> createHighlight(@RequestBody HighlightCreateRequest request) {
//        highlightService.createHighlight(getCurrentUserId(), request);
//        return ResponseEntity.status(HttpStatus.CREATED).build();
//    }
//
//    @GetMapping("/highlights/{highlightId}/stories")
//    public ResponseEntity<List<StoryResponse>> getHighlightStories(@PathVariable String highlightId) {
//        return ResponseEntity.ok(highlightService.getStoriesInHighlight(highlightId));
//    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}