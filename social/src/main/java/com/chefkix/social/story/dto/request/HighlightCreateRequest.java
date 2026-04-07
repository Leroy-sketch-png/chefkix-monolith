package com.chefkix.social.story.dto.request;

import java.util.List;

public record HighlightCreateRequest(
        String title,
        String coverUrl,
        List<String> storyIds
) {}