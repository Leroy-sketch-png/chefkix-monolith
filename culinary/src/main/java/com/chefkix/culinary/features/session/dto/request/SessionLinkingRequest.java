package com.chefkix.culinary.features.session.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SessionLinkingRequest {
    @NotBlank(message = "postId is required")
    @Size(max = 100)
    private String postId;
}
