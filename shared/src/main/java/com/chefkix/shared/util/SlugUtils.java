package com.chefkix.shared.util;

import java.text.Normalizer;

/**
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    /**
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
