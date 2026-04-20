package com.chefkix.identity.service;

import com.chefkix.identity.dto.identity.OidcUserInfoResponse;
import com.chefkix.identity.dto.identity.ResetPasswordParam;
import com.chefkix.identity.dto.identity.TokenExchangeParam;
import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.identity.dto.response.UserResponse;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.entity.UserActivity;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.events.UserIndexEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.exception.ErrorNormalizer;
import com.chefkix.identity.mapper.UserMapper;
import com.chefkix.identity.repository.UserActivityRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.identity.client.KeycloakAdminClient;
// Feign removed in monolith
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

  private static final String GOOGLE_CALLBACK_PATH = "/oauth2/callback/google";
  private static final Set<String> LOCALHOST_ALIASES = Set.of("localhost", "127.0.0.1");

  KeycloakAdminClient keycloakAdminClient;
  ErrorNormalizer errorNormalizer;

  UserRepository userRepository;
  UserMapper userMapper;
  KeycloakService keycloakService;
  UserActivityRepository userActivityRepository;
  UserProfileRepository profileRepository;
  ApplicationEventPublisher eventPublisher;

  @Value("${idp.client-id}")
  @NonFinal
  String clientId;

  @Value("${idp.client-secret}")
  @NonFinal
  String clientSecret;

  @Value("${app.public-base-url:http://localhost:3000}")
  @NonFinal
  String publicBaseUrl;

  @Transactional
  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    log.debug(">>> [AUTH] Start authenticate for username/email={}", request.getEmailOrUsername());

    TokenExchangeResponse tokenResponse;

    // 1. Call Keycloak login
    try {
      log.debug(">>> [AUTH] Calling Keycloak login for user={}", request.getEmailOrUsername());

      // Call login function (this now throws RuntimeException with raw JSON on error)
      tokenResponse = keycloakService.login(request.getEmailOrUsername(), request.getPassword());

      // CAREFUL CHECK: Guard against login returning null without throwing (rare but safe)
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
    // IF CODE REACHES HERE, LOGIN WAS 100% SUCCESSFUL
    // -------------------------------------------------------------

    // 2. Sync local user
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

    // 3. Update last login
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);

    // 4. Sync UserActivity
    UserActivity activity =
        userActivityRepository.findByKeycloakId(user.getId()).orElse(new UserActivity());
    activity.setKeycloakId(user.getId());
    activity.setLastLogin(LocalDateTime.now());
    userActivityRepository.save(activity);

    log.debug(">>> [AUTH] Authentication process completed successfully.");

    return buildAuthenticationResponse(tokenResponse, user);
  }

  public AuthenticationResponse authenticateWithGoogle(
      String code, String redirectUri, String codeVerifier) {
    assertAllowedGoogleRedirectUri(redirectUri);

    TokenExchangeResponse tokenResponse =
        keycloakService.exchangeAuthorizationCode(code, redirectUri, codeVerifier);

    if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
      throw new AppException(ErrorCode.UNAUTHENTICATED, "Google sign-in failed. Please try again.");
    }

    OidcUserInfoResponse userInfo = keycloakService.getUserInfo(tokenResponse.getAccessToken());
    User user = syncGoogleUser(userInfo);
    log.info("Google sign-in completed for email={}", userInfo.getEmail());

    return buildAuthenticationResponse(tokenResponse, user);
  }

  /**
   * Change password for authenticated user. Email is extracted from JWT token, NOT from request
   * body (security).
   */
  public void changePassword(String email, String oldPassword, String newPassword) {
    // Verify old password by attempting login
    TokenExchangeResponse tokenResponse;
    try {
      tokenResponse = keycloakService.login(email, oldPassword);
    } catch (Exception e) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
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

      // Revoke all existing sessions — force re-authentication on all devices
      keycloakAdminClient.logoutUser("Bearer " + token.getAccessToken(), userId);

      log.info("Password successfully changed for userId={}", userId);

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

  private AuthenticationResponse buildAuthenticationResponse(
      TokenExchangeResponse tokenResponse, User user) {
    UserResponse userResponse = userMapper.toUserResponse(user);

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

  private User syncGoogleUser(OidcUserInfoResponse userInfo) {
    if (userInfo == null || isBlank(userInfo.getSub()) || isBlank(userInfo.getEmail())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Google account did not provide a usable identity.");
    }

    UserProfile profile = upsertGoogleProfile(userInfo);

    User user = userRepository.findByGoogleId(userInfo.getSub())
      .or(() -> userRepository.findByEmail(userInfo.getEmail()))
      .orElseGet(User::new);
    user.setEmail(userInfo.getEmail());
    user.setUsername(profile.getUsername());
    user.setGoogleId(userInfo.getSub());
    user.setUserProfile(profile);
    user.setEnabled(true);
    user.setLastLogin(LocalDateTime.now());
    if (isBlank(user.getAuthProvider())) {
      user.setAuthProvider("google");
    }
    user = userRepository.save(user);

    UserActivity activity =
        userActivityRepository.findByKeycloakId(user.getId()).orElse(new UserActivity());
    activity.setKeycloakId(user.getId());
    activity.setLastLogin(user.getLastLogin());
    userActivityRepository.save(activity);

    return user;
  }

  private UserProfile upsertGoogleProfile(OidcUserInfoResponse userInfo) {
    var existingByUserId = profileRepository.findByUserId(userInfo.getSub());
    if (existingByUserId.isPresent()) {
      UserProfile profile = existingByUserId.get();
      boolean changed = applyGoogleProfileDefaults(profile, userInfo);
      if (changed) {
        profile = profileRepository.save(profile);
        eventPublisher.publishEvent(UserIndexEvent.index(profile));
      }
      return profile;
    }

    var existingByEmail = profileRepository.findByEmail(userInfo.getEmail());
    if (existingByEmail.isPresent()) {
      UserProfile profile = existingByEmail.get();
      if (!isBlank(profile.getUserId()) && !profile.getUserId().equals(userInfo.getSub())) {
        throw new AppException(
            ErrorCode.INVALID_REQUEST,
            "An account already exists for this email. Sign in with your current method first.");
      }

      profile.setUserId(userInfo.getSub());
      boolean changed = applyGoogleProfileDefaults(profile, userInfo);
      changed = true;
      profile = profileRepository.save(profile);
      if (changed) {
        eventPublisher.publishEvent(UserIndexEvent.index(profile));
      }
      return profile;
    }

    return createGoogleProfile(userInfo);
  }

  private UserProfile createGoogleProfile(OidcUserInfoResponse userInfo) {
    UserProfile profile = UserProfile.builder()
        .userId(userInfo.getSub())
        .email(userInfo.getEmail())
        .username(resolveUniqueUsername(userInfo))
        .displayName(resolveDisplayName(userInfo))
        .firstName(userInfo.getGivenName())
        .lastName(userInfo.getFamilyName())
        .fullName(resolveFullName(userInfo))
        .avatarUrl(userInfo.getPicture())
        .accountType("user")
        .statistics(Statistics.builder().build())
        .build();

    UserProfile saved = profileRepository.save(profile);
    eventPublisher.publishEvent(UserIndexEvent.index(saved));
    return saved;
  }

  private boolean applyGoogleProfileDefaults(UserProfile profile, OidcUserInfoResponse userInfo) {
    boolean changed = false;

    if (isBlank(profile.getFirstName()) && !isBlank(userInfo.getGivenName())) {
      profile.setFirstName(userInfo.getGivenName());
      changed = true;
    }
    if (isBlank(profile.getLastName()) && !isBlank(userInfo.getFamilyName())) {
      profile.setLastName(userInfo.getFamilyName());
      changed = true;
    }
    if (isBlank(profile.getFullName())) {
      profile.setFullName(resolveFullName(userInfo));
      changed = true;
    }
    if (isBlank(profile.getDisplayName())) {
      profile.setDisplayName(resolveDisplayName(userInfo));
      changed = true;
    }
    if (isBlank(profile.getAvatarUrl()) && !isBlank(userInfo.getPicture())) {
      profile.setAvatarUrl(userInfo.getPicture());
      changed = true;
    }
    if (isBlank(profile.getUsername())) {
      profile.setUsername(resolveUniqueUsername(userInfo));
      changed = true;
    }

    return changed;
  }

  private String resolveDisplayName(OidcUserInfoResponse userInfo) {
    if (!isBlank(userInfo.getName())) {
      return userInfo.getName();
    }
    if (!isBlank(userInfo.getGivenName()) || !isBlank(userInfo.getFamilyName())) {
      return resolveFullName(userInfo);
    }
    if (!isBlank(userInfo.getPreferredUsername())) {
      return userInfo.getPreferredUsername();
    }
    if (userInfo.getEmail() != null) {
      return userInfo.getEmail().split("@")[0];
    }
    return "chefkix_user";
  }

  private String resolveFullName(OidcUserInfoResponse userInfo) {
    if (!isBlank(userInfo.getGivenName()) && !isBlank(userInfo.getFamilyName())) {
      return userInfo.getGivenName() + " " + userInfo.getFamilyName();
    }
    if (!isBlank(userInfo.getGivenName())) {
      return userInfo.getGivenName();
    }
    if (!isBlank(userInfo.getFamilyName())) {
      return userInfo.getFamilyName();
    }
    return resolveDisplayName(userInfo);
  }

  private String resolveUniqueUsername(OidcUserInfoResponse userInfo) {
    String baseUsername = sanitizeUsername(userInfo.getPreferredUsername());
    if (isBlank(baseUsername)) {
      baseUsername = userInfo.getEmail() != null
          ? sanitizeUsername(userInfo.getEmail().split("@")[0])
          : "";
    }
    if (isBlank(baseUsername)) {
      baseUsername = "chefkix_user";
    }

    String candidate = baseUsername;
    int suffix = 1;
    while (profileRepository.findByUsername(candidate).isPresent()) {
      String suffixValue = String.valueOf(suffix++);
      int maxBaseLength = Math.max(3, 30 - suffixValue.length());
      String truncatedBase = baseUsername.length() > maxBaseLength
          ? baseUsername.substring(0, maxBaseLength)
          : baseUsername;
      candidate = truncatedBase + suffixValue;
    }
    return candidate;
  }

  private String sanitizeUsername(String rawValue) {
    if (isBlank(rawValue)) {
      return "";
    }

    String normalized = rawValue.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9_]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_+|_+$", "");

    if (normalized.length() > 30) {
      normalized = normalized.substring(0, 30);
    }
    if (normalized.length() < 3) {
      normalized = (normalized + "_chef").substring(0, Math.min(30, normalized.length() + 5));
    }
    return normalized;
  }

  private void assertAllowedGoogleRedirectUri(String redirectUri) {
    try {
      URI actualUri = URI.create(redirectUri);
      URI configuredUri = URI.create(publicBaseUrl);

      if (!GOOGLE_CALLBACK_PATH.equals(actualUri.getPath())) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid Google callback path.");
      }

      if (configuredUri.getHost() == null || actualUri.getHost() == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "Malformed redirect URI host.");
      }

      boolean sameHost = configuredUri.getHost().equalsIgnoreCase(actualUri.getHost())
          && configuredUri.getPort() == actualUri.getPort();
      boolean localhostAlias = LOCALHOST_ALIASES.contains(configuredUri.getHost().toLowerCase(Locale.ROOT))
          && LOCALHOST_ALIASES.contains(actualUri.getHost().toLowerCase(Locale.ROOT))
          && configuredUri.getPort() == actualUri.getPort();

      if (!(sameHost || localhostAlias)) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid Google callback origin.");
      }
    } catch (IllegalArgumentException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Malformed Google redirect URI.", e);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
