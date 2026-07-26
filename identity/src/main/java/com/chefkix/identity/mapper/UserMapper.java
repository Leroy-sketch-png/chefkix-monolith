package com.chefkix.identity.mapper;

import com.chefkix.identity.dto.request.UserCreationRequest;
import com.chefkix.identity.dto.response.RoleResponse;
import com.chefkix.identity.dto.response.UserResponse;
import com.chefkix.identity.entity.Role;
import com.chefkix.identity.entity.User;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  User toUser(UserCreationRequest request);

  default UserResponse toUserResponse(User user) {
    if (user == null) return null;

    UserResponse resp = new UserResponse();
    resp.setId(user.getId());
    resp.setUsername(user.getUsername());
    resp.setEmail(user.getEmail());
    resp.setEnabled(user.getEnabled());

    Set<Role> roles = user.getRoles();
    if (roles == null) {
      resp.setRoles(Collections.emptySet());
    } else {
      Set<RoleResponse> roleResponses =
          roles.stream()
              .map(
                  r -> {
                    RoleResponse rr = new RoleResponse();
                    rr.setId(r.getId());
                    rr.setName(r.getName());
                    return rr;
                  })
              .collect(Collectors.toSet());
      resp.setRoles(roleResponses);
    }

    return resp;
  }

}
