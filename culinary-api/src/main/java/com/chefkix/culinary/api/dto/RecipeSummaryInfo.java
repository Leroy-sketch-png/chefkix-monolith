package com.chefkix.culinary.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Lightweight recipe info for cross-module display (battle cards, review headers).
 * Only contains what's needed for rendering — not the full recipe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeSummaryInfo {
    String id;
    String title;
    String coverImageUrl;
    String authorId;
    String authorDisplayName;
}
