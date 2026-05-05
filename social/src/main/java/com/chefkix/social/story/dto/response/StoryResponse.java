package com.chefkix.social.story.dto.response;

import com.chefkix.social.story.dto.request.StoryItemDto;

import java.util.List;

// 2. Response trả về cho Client
public record StoryResponse(
        String id,
        String userId,
        String mediaUrl,
        String mediaType,
        //boolean isCloseFriendsOnly,
        Double imageScale,
        Double imageRotation,
        String linkedRecipeId,
        List<StoryItemDto> items,
        String createdAt,
        String expiresAt
) {}