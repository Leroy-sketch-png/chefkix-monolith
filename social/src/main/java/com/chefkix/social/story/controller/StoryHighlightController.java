package com.chefkix.social.story.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.story.dto.request.HighlightCreateRequest;
import com.chefkix.social.story.dto.request.HighlightUpdateRequest;
import com.chefkix.social.story.dto.response.HighlightResponse;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.service.StoryHighlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/highlights")
@RequiredArgsConstructor
public class StoryHighlightController {

    private final StoryHighlightService highlightService;

    @PostMapping()
    public ApiResponse<HighlightResponse> createHighlight(
            @Valid @RequestBody HighlightCreateRequest request
    ) {
        HighlightResponse response = highlightService.createHighlight(getCurrentUserId(), request);
        return ApiResponse.created(response);
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<HighlightResponse>> getUserHighlights(
            @PathVariable("userId") String userId
    ) {
        return ApiResponse.success(highlightService.getUserHighlights(userId), "Highlight list retrieved");
    }

    @GetMapping("/{highlightId}/stories")
    public ApiResponse<List<StoryResponse>> getHighlightStories(
            @PathVariable("highlightId") String highlightId
    ) {
        return ApiResponse.success(highlightService.getStoriesInHighlight(highlightId));
    }

    @PutMapping("/{highlightId}")
    public ApiResponse<String> updateHighlight(
            @PathVariable("highlightId") String highlightId,
            @Valid @RequestBody HighlightUpdateRequest request) {
        highlightService.updateHighlight(highlightId, getCurrentUserId(), request);
        return ApiResponse.success("Highlight updated");
    }

    @DeleteMapping("/{highlightId}")
    public ApiResponse<String> deleteHighlight(
            @PathVariable("highlightId") String highlightId
    ) {
        highlightService.deleteHighlight(highlightId, getCurrentUserId());
        return ApiResponse.success("Highlight deleted");
    }



    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}