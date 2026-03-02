package com.chefkix.identity.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Lightweight profile DTO for cross-module calls.
 * <p>
 * Unifies the following duplicated DTOs:
 * <ul>
 *   <li>{@code chefkix-be} → InternalBasicProfileResponse (6 fields)</li>
 *   <li>{@code chefkix-chat-service} → UserProfileResponse (6 fields, @JsonAlias "avatarUrl" → "avatar")</li>
 *   <li>{@code chefkix-recipe-service} → AuthorResponse (4 fields)</li>
 * </ul>
 * <p>
 * Canonical field name is {@code avatarUrl} (not "avatar"). Chat module maps internally if needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BasicProfileInfo {

    String userId;

    String username;

    String displayName;

    String firstName;

    String lastName;

    String avatarUrl;
}
