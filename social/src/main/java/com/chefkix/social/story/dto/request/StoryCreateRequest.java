package com.chefkix.social.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 1. Request Payload để tạo Story
public record StoryCreateRequest(

        @NotBlank(message = "Media Type không được để trống")
        String mediaType, // Có thể dùng Enum, ở đây dùng String cho đơn giản

        //boolean isCloseFriendsOnly,
        Double imageScale,
        Double imageRotation,

        @Size(max = 100) String linkedRecipeId,

        // Giới hạn số lượng sticker để tránh payload quá lớn
        @Size(max = 20) List<StoryItemDto> items
) {}



