package com.chefkix.social.story.dto.response;

import com.chefkix.social.story.dto.request.StoryItemDto;

import java.util.List;

public record StoryResponse(
        String id,
        String userId,
        String mediaUrl,
        String mediaType,
        Double imageScale,
        Double imageRotation,
        String linkedRecipeId,
        List<StoryItemDto> items,
        String createdAt,
        String expiresAt
) {}