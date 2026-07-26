package com.chefkix.social.story.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record StoryItemDto(
        @NotBlank String type,
        double x,
        double y,
        double rotation,
        double scale,
        Map<String, Object> data
) {}