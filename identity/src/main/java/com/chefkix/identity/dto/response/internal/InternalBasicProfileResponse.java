package com.chefkix.identity.dto.response.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal profile response for service-to-service communication.
 * Used by chat-service, post-service, etc. to get user info without full profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalBasicProfileResponse {
  private String userId;
  private String username;
  private String displayName;
  private String firstName;
  private String lastName;
  private String avatarUrl;
  private boolean verified;
}
