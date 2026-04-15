package com.chefkix.identity.utils;

import com.chefkix.identity.entity.User;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

  private final UserRepository userRepository;

  public String getCurrentUserId(Authentication auth) {
    Authentication authentication =
        auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof Jwt jwt) {
      return jwt.getSubject(); // always returns userId as String
    }

    if (principal instanceof UserDetails ud) {
      return lookupUserIdByUsernameOrEmail(ud.getUsername());
    }

    return authentication.getName(); // final fallback
  }

  private String lookupUserIdByUsernameOrEmail(String usernameOrEmail) {
    if (usernameOrEmail == null) return null;
    return userRepository
        .findByEmail(usernameOrEmail)
        .or(() -> userRepository.findByUsername(usernameOrEmail))
        .map(User::getId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }
}
