package com.chefkix.culinary.common.enums;

public enum RecipeStatus {
    DRAFT,      // Work in progress
    PENDING,    // Awaiting review (if admin moderation exists)
    PUBLISHED,  // Publicly visible
    ARCHIVED    // Soft-deleted
}