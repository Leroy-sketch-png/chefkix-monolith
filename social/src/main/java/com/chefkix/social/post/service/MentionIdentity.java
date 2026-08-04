package com.chefkix.social.post.service;

import com.chefkix.identity.api.dto.BasicProfileInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

final class MentionIdentity {

    private static final String TOKEN_SUFFIX = "(?=$|[^\\p{L}\\p{N}._-])";

    private MentionIdentity() {
    }

    static List<String> reconcile(
            String content,
            List<String> requestedUserIds,
            Function<String, BasicProfileInfo> profileLookup) {
        if (content == null || content.isBlank() || requestedUserIds == null || requestedUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        return requestedUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(userId -> hasVisibleToken(content, safelyLookup(profileLookup, userId)))
                .toList();
    }

    private static BasicProfileInfo safelyLookup(
            Function<String, BasicProfileInfo> profileLookup,
            String userId) {
        try {
            return profileLookup.apply(userId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean hasVisibleToken(String content, BasicProfileInfo profile) {
        if (profile == null || profile.getUsername() == null || profile.getUsername().isBlank()) {
            return false;
        }

        Pattern token = Pattern.compile(
                "(^|\\s)@" + Pattern.quote(profile.getUsername().trim()) + TOKEN_SUFFIX,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return token.matcher(content).find();
    }
}
