package com.chefkix.identity.dto.response;

import com.chefkix.identity.enums.RoleName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
  String id;
  RoleName name;
}
