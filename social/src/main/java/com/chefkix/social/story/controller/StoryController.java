package com.chefkix.social.story.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.story.dto.request.StoryCreateRequest;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Validated
public class StoryController {
    private final StoryService storyService;

    @PostMapping
    public ApiResponse<StoryResponse> create(@Valid @RequestBody StoryCreateRequest request) {
        return ApiResponse.created(storyService.createStory(getCurrentUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        storyService.deleteStory(getCurrentUserId(), id);
        return ApiResponse.success("Story deleted");
    }

    @GetMapping("/me/active")
    public ApiResponse<List<StoryResponse>> getActive() {
        return ApiResponse.success(storyService.getMyActiveStories(getCurrentUserId()));
    }

    @GetMapping("/me/archive")
    public ApiResponse<Page<StoryResponse>> getArchive(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(storyService.getMyArchivedStories(getCurrentUserId(), page, size));
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}