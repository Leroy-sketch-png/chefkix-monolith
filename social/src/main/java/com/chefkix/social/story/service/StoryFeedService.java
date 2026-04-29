package com.chefkix.social.story.service;

import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.dto.response.UserStoryFeedResponse;

import java.util.List;

public interface StoryFeedService {
    List<UserStoryFeedResponse> getStoryFeed(String currentUserId);

    List<StoryResponse> getUserActiveStories(String currentUserId, String targetUserId);

    StoryResponse getStoryById(String storyId);
}
