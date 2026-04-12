package com.chefkix.identity.dto.identity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordParam {
  @Builder.Default
  String type = "password";
  String value;
  @Builder.Default
  boolean temporary = false;
}
