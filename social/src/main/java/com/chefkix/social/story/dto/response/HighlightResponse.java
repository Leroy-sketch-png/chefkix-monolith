package com.chefkix.social.story.dto.response;

public record HighlightResponse(
        String id,
        String title,
        String coverUrl,
        int storyCount // Đếm số lượng story để Frontend hiện con số
) {}