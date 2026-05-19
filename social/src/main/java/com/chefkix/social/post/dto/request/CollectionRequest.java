package com.chefkix.social.post.dto.request;

import com.chefkix.social.post.entity.DifficultyStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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

    String collectionType;

    List<String> recipeIds;

    String difficulty;

    Integer estimatedTotalMinutes;

    Integer totalXp;

    List<DifficultyStep> difficultyProgression;
}
