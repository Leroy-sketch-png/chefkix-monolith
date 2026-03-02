package com.chefkix.shared.util;

import java.text.Normalizer;

/**
 * URL-safe slug generator.
 * <p>
 * Handles Unicode normalization (accented characters), lowercasing,
 * and non-alphanumeric replacement. Consolidated from identical copies
 * in chefkix-be and chefkix-post-service.
 */
public final class SlugUtils {

    private SlugUtils() {
        // utility class
    }

    /**
     * Convert an arbitrary string to a URL-safe slug.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "Xin Chào Việt Nam!"} → {@code "xin-chao-viet-nam"}</li>
     *   <li>{@code "Bún Bò Huế Recipe"} → {@code "bun-bo-hue-recipe"}</li>
     * </ul>
     */
    public static String toSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccent = normalized.replaceAll("\\p{M}", "");
        String lower = noAccent.toLowerCase();
        String slug = lower.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("(^-|-$)", "");
        return slug;
    }
}
