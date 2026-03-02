package com.chefkix.identity.service;

import com.chefkix.identity.dto.request.UserCreationRequest;
// import com.chefkix.identity.dto.request.UserUpdateRequest;
import com.chefkix.identity.dto.response.UserResponse;
import com.chefkix.identity.entity.Role;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.enums.RoleName;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.mapper.UserMapper;
import com.chefkix.identity.repository.RoleRepository;
import com.chefkix.identity.repository.UserRepository;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
  UserRepository userRepository;
  RoleRepository roleRepository;
  UserMapper userMapper;
  PasswordEncoder passwordEncoder;

  public UserResponse createUser(UserCreationRequest request) {
    User user = userMapper.toUser(request);
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

    Role userRole =
        roleRepository
            .findByName(RoleName.USER)
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    user.setRoles(Set.of(userRole));
    user.setEnabled(true);

    try {
      user = userRepository.save(user);
    } catch (DataIntegrityViolationException exception) {
      throw new AppException(ErrorCode.USER_EXISTED);
    }
    return userMapper.toUserResponse(user);
  }
}
