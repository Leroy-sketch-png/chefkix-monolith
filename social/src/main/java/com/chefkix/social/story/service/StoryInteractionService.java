package com.chefkix.social.story.service;

import java.util.List;

public interface StoryInteractionService {
    void recordView(String storyId, String userId);

    void recordReaction(String storyId, String userId, String reactionType);

    List<String> getViewerIds(String storyId, String ownerId);
}
