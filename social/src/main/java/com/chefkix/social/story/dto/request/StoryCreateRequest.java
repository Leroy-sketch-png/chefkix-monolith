package com.chefkix.social.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record StoryCreateRequest(

        @NotBlank(message = "Media Type không được để trống")
String mediaType,

        Double imageScale,
        Double imageRotation,

        @Size(max = 100) String linkedRecipeId,

        @Size(max = 20) List<StoryItemDto> items
) {}


