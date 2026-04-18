package com.chefkix.social.story.service;

import com.chefkix.social.story.dto.request.StoryCreateRequest;
import com.chefkix.social.story.dto.response.StoryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StoryService {
    StoryResponse createStory(String userId, StoryCreateRequest request, String mediaUrl);
    void deleteStory(String userId, String storyId);

    List<StoryResponse> getMyActiveStories(String userId);

    Page<StoryResponse> getMyArchivedStories(String userId, int page, int size);

    void archiveStoryEarly(String storyId, String userId);
}