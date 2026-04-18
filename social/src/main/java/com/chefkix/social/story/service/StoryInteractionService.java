package com.chefkix.social.story.service;

import com.chefkix.social.story.dto.request.StoryReplyRequest;

import java.util.List;

public interface StoryInteractionService {
    void recordView(String storyId, String userId);

    void recordReaction(String storyId, String userId, String reactionType);

    List<String> getViewerIds(String storyId, String ownerId);

    void replyToStory(String storyId, String replierId, StoryReplyRequest request);
}
