package com.chefkix.identity.service;

import com.chefkix.identity.dto.identity.ResetPasswordParam;
import com.chefkix.identity.dto.identity.TokenExchangeParam;
import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.identity.dto.response.UserResponse;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.entity.UserActivity;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.exception.ErrorNormalizer;
import com.chefkix.identity.mapper.UserMapper;
import com.chefkix.identity.repository.UserActivityRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.identity.client.KeycloakAdminClient;
// Feign removed in monolith
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

  KeycloakAdminClient keycloakAdminClient;
  ErrorNormalizer errorNormalizer;

  UserRepository userRepository;
  UserMapper userMapper;
  KeycloakService keycloakService;
  UserActivityRepository userActivityRepository;
  UserProfileRepository profileRepository;

  @Value("${idp.client-id}")
  @NonFinal
  String clientId;

  @Value("${idp.client-secret}")
  @NonFinal
  String clientSecret;

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    log.debug(">>> [AUTH] Start authenticate for username/email={}", request.getEmailOrUsername());

    TokenExchangeResponse tokenResponse;

    // 1. Call Keycloak login
    try {
      log.debug(">>> [AUTH] Calling Keycloak login for user={}", request.getEmailOrUsername());

      // Gọi hàm login (Hàm này giờ đã ném RuntimeException chứa Raw JSON nếu lỗi)
      tokenResponse = keycloakService.login(request.getEmailOrUsername(), request.getPassword());

      // 🔥 KIỂM TRA KỸ: Đề phòng trường hợp login trả về null mà không ném lỗi (hiếm nhưng an toàn)
      if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
        throw new AppException(ErrorCode.INVALID_CREDENTIALS);
      }

      log.debug(">>> [AUTH] Keycloak login success.");

    } catch (AppException e) {
      // Re-throw AppException as-is
      throw e;
    } catch (Exception e) {
      // Parse Keycloak error to provide user-friendly message
      log.error(">>> [AUTH] Login failed. Raw Keycloak Message: {}", e.getMessage());

      String errorMessage = e.getMessage();
      if (errorMessage != null) {
        // Check for common Keycloak error patterns
        if (errorMessage.contains("invalid_grant")
            || errorMessage.contains("Invalid user credentials")) {
          throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (errorMessage.contains("Account disabled")
            || errorMessage.contains("account_disabled")) {
          throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (errorMessage.contains("not verified") || errorMessage.contains("email_not_verified")) {
          throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
      }

      // Default to invalid credentials for any auth failure
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }

    // -------------------------------------------------------------
    // ⛔ NẾU CODE CHẠY ĐẾN ĐÂY NGHĨA LÀ LOGIN THÀNH CÔNG 100% ⛔
    // -------------------------------------------------------------

    // 2. Đồng bộ local user
    log.debug(">>> [AUTH] Syncing user in local DB...");
    User user =
        userRepository
            .findByEmailOrUsername(request.getEmailOrUsername(), request.getEmailOrUsername())
            .orElseGet(
                () -> {
                  log.debug(">>> [AUTH] Creating new user in DB...");
                  User newUser = new User();
                  newUser.setUsername(request.getEmailOrUsername());
                  newUser.setEmail(request.getEmailOrUsername());
                  return userRepository.save(newUser);
                });

    // 3. Cập nhật last login
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);

    // 4. Đồng bộ UserActivity
    UserActivity activity =
        userActivityRepository.findByKeycloakId(user.getId()).orElse(new UserActivity());
    activity.setKeycloakId(user.getId());
    activity.setLastLogin(LocalDateTime.now());
    userActivityRepository.save(activity);

    // 5. Map & Build response
    UserResponse userResponse = userMapper.toUserResponse(user);

    log.debug(">>> [AUTH] Authentication process completed successfully.");

    return AuthenticationResponse.builder()
        .accessToken(tokenResponse.getAccessToken())
        .refreshToken(tokenResponse.getRefreshToken())
        .idToken(tokenResponse.getIdToken())
        .scope(tokenResponse.getScope())
        .authenticated(true)
        .lastLogin(user.getLastLogin())
        .user(userResponse)
        .build();
  }

  /**
   * Change password for authenticated user. Email is extracted from JWT token, NOT from request
   * body (security).
   */
  public void changePassword(String email, String oldPassword, String newPassword) {
    // Verify old password by attempting login
    TokenExchangeResponse tokenResponse = keycloakService.login(email, oldPassword);
    if (tokenResponse.getAccessToken() == null) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    var token =
        keycloakAdminClient.exchangeToken(
            TokenExchangeParam.builder()
                .grant_type("client_credentials")
                .client_id(clientId)
                .client_secret(clientSecret)
                .scope("openid")
                .build());
    var userProfile = profileRepository.findByEmail(email);

    if (userProfile.isEmpty() || userProfile.get().getUserId() == null) {
      throw new AppException(ErrorCode.USER_NOT_FOUND);
    }
    String userId = userProfile.get().getUserId();
    try {
      keycloakAdminClient.resetPassword(
          "Bearer " + token.getAccessToken(),
          userId,
          ResetPasswordParam.builder()
              .type("password")
              .temporary(false)
              .value(newPassword)
              .build());

      log.info("Password successfully reset for userId={}", userId);

    } catch (Exception e) {
      log.error("Failed to reset password in Keycloak for userId={}", userId, e);
      throw errorNormalizer.handleKeyCloakException(e);
    }
  }

  /** Refresh access token using Keycloak */
  public AuthenticationResponse refreshToken(String refreshToken) {
    // 1. Call Keycloak to refresh token
    TokenExchangeResponse tokenResponse = keycloakService.refreshToken(refreshToken);

    // 2. Return new access + refresh token
    return AuthenticationResponse.builder()
        .accessToken(tokenResponse.getAccessToken())
        .refreshToken(tokenResponse.getRefreshToken())
        .idToken(tokenResponse.getIdToken())
        .scope(tokenResponse.getScope())
        .authenticated(true)
        .build();
  }

  /** Logout user via Keycloak */
  public String logout(String refreshToken) {
    try {
      keycloakService.logout(refreshToken);
      return "Successfully logged out";
    } catch (Exception e) {
      log.warn("Error logging out user: {}", e.getMessage());
    }
    return "Failed to log out";
  }
}
