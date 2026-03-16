package com.chefkix.culinary.features.session.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionLinkingRequest {
    @NotBlank(message = "postId is required")
    private String postId;
}
