package com.chefkix.social.story.service;

public interface StoryInteractionService {
    void recordView(String storyId, String userId);

    void recordReaction(String storyId, String userId, String reactionType);
}
