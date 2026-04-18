package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionRequest {

    @NotBlank(message = "Collection name is required")
    @Size(max = 60, message = "Collection name must be 60 characters or less")
    String name;

    @Size(max = 200, message = "Description must be 200 characters or less")
    String description;

    boolean isPublic;
}
