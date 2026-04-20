package com.chefkix.social.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupCreationRequest {
    @NotBlank(message = "Group name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Cover image URL cannot exceed 500 characters")
    private String coverImageUrl;

    @NotBlank(message = "Privacy type is required")
    @Size(max = 20, message = "Privacy type cannot exceed 20 characters")
    private String privacyType;

    // Optional: Tags for Chefkix gamification/search (e.g., "Vegan", "Baking")
    @Size(max = 10, message = "Maximum 10 tags")
    private java.util.List<String> tags;
}