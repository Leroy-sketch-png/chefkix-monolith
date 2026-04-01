package com.chefkix.social.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import java.util.List;
import java.util.Map;

// 1. Request Payload để tạo Story
public record StoryCreateRequest(
        @NotBlank(message = "Media URL không được để trống")
        @URL(message = "Media URL không đúng định dạng")
        String mediaUrl,

        @NotBlank(message = "Media Type không được để trống")
        String mediaType, // Có thể dùng Enum, ở đây dùng String cho đơn giản

        boolean isCloseFriendsOnly,

        String linkedRecipeId,

        // Giới hạn số lượng sticker để tránh payload quá lớn
        List<StoryItemDto> items
) {}



