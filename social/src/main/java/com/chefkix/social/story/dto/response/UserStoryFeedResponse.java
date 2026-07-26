package com.chefkix.social.story.dto.response;

import java.util.List;

public record UserStoryFeedResponse(
        String userId,
String displayName,
String avatarUrl,
        boolean hasUnseenStory
) {}