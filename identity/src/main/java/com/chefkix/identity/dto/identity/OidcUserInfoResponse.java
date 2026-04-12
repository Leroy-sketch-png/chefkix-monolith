package com.chefkix.identity.dto.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OidcUserInfoResponse {
  String sub;
  String email;

  @JsonProperty("email_verified")
  Boolean emailVerified;

  @JsonProperty("preferred_username")
  String preferredUsername;

  @JsonProperty("given_name")
  String givenName;

  @JsonProperty("family_name")
  String familyName;

  String name;
  String picture;
}