package com.chefkix.social.story.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

public record HighlightCreateRequest(
        @Size(max = 100) String title,
        @Size(max = 500) String coverUrl,
        @Size(max = 50) List<String> storyIds
) {}