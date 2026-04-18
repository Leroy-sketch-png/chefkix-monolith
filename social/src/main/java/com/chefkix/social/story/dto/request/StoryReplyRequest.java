package com.chefkix.social.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryReplyRequest(

        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 500, message = "Nội dung tin nhắn không được vượt quá 500 ký tự")
        String text

) {}