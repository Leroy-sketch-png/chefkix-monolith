package com.chefkix.social.story.service;

import com.chefkix.social.story.dto.request.HighlightCreateRequest;
import com.chefkix.social.story.dto.request.HighlightUpdateRequest;
import com.chefkix.social.story.dto.response.HighlightResponse;
import com.chefkix.social.story.dto.response.StoryResponse;

import java.util.List;

public interface StoryHighlightService {
    HighlightResponse createHighlight(String userId, HighlightCreateRequest request);
    List<HighlightResponse> getUserHighlights(String targetUserId);
    List<StoryResponse> getStoriesInHighlight(String highlightId);
//    void updateHighlight(String highlightId, String currentUserId, HighlightUpdateRequest request);
//    void deleteHighlight(String highlightId, String currentUserId);
}