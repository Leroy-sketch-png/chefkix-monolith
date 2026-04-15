package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.*;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.service.*;
import com.chefkix.identity.utils.ClientIpUtils;
import com.chefkix.identity.utils.HttpOnlyCookieUtils;
import com.nimbusds.jose.JOSEException;
// Feign removed in monolith
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.text.ParseException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
  private static final int REFRESH_TOKEN_MAX_AGE_LOGIN = 7 * 24 * 60 * 60; // 7 days
  private static final int REFRESH_TOKEN_MAX_AGE_REFRESH = 7 * 24 * 60 * 60; // 7 days

  AuthenticationService authenticationService;
  SignupRequestService signupRequestService;
  ProfileService profileService;
  ResetPasswordService resetPasswordService;
  AuthRateLimitService authRateLimitService;
  UserProfileRepository userProfileRepository;

  /**
   * Check if a username is available for registration.
   * Returns { available: true/false } to support live validation on sign-up form.
   */
  @GetMapping("/check-username")
  ApiResponse<Map<String, Boolean>> checkUsernameAvailability(
      @RequestParam(value = "username") String username) {
    // Validate username format (same rules as registration)
    if (username == null || username.length() < 2 || username.length() > 30) {
      return ApiResponse.<Map<String, Boolean>>builder()
          .data(Map.of("available", false))
          .message("Username must be between 2 and 30 characters")
          .build();
    }
    
    // Check if username exists
    boolean exists = userProfileRepository.findByUsername(username).isPresent();
    return ApiResponse.<Map<String, Boolean>>builder()
        .data(Map.of("available", !exists))
        .build();
  }

  @PostMapping(path = "/register")
  ApiResponse<String> register(
      @RequestBody @Valid SignupRequest request, HttpServletRequest httpServletRequest) {
    String clientIp = ClientIpUtils.getClientIpAddress(httpServletRequest);
    signupRequestService.register(request, clientIp);
    return ApiResponse.<String>builder().data("OTP sent to email").build();
  }

  @PostMapping("/refresh-token")
  public ApiResponse<Map<String, String>> refreshToken(
      HttpServletResponse response,
      @CookieValue(name = "refresh_token", required = false) String refreshToken) {

    if (refreshToken == null || refreshToken.isEmpty()) {
      HttpOnlyCookieUtils.deleteHttpOnlyCookie(response, "refresh_token");
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    try {
      AuthenticationResponse authResponse = authenticationService.refreshToken(refreshToken);

      HttpOnlyCookieUtils.addHttpOnlyCookie(
          response,
          "refresh_token",
          authResponse.getRefreshToken(), // Get new RT from response
          REFRESH_TOKEN_MAX_AGE_REFRESH
          );

      return ApiResponse.<Map<String, String>>builder()
          .data(Map.of("accessToken", authResponse.getAccessToken()))
          .build();

    } catch (WebClientResponseException.BadRequest | WebClientResponseException.Unauthorized e) {
      HttpOnlyCookieUtils.deleteHttpOnlyCookie(response, "refresh_token");

      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  @PostMapping("/login")
  ApiResponse<AuthenticationResponse> authenticate(
      @RequestBody @Valid AuthenticationRequest request,
      HttpServletResponse response,
      HttpServletRequest httpServletRequest) { // add response
    String clientIp = ClientIpUtils.getClientIpAddress(httpServletRequest);
    authRateLimitService.assertLoginAllowed(clientIp);

    // 1. authenticate user and get token from Keycloak
    AuthenticationResponse authResponse = authenticationService.authenticate(request);

    authRateLimitService.clearLoginAttempts(clientIp);

    // 2. Store refresh token in HttpOnly cookie
    HttpOnlyCookieUtils.addHttpOnlyCookie(
        response, "refresh_token", authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE_LOGIN
        );

    // 3. Return JSON body (refreshToken no longer needed here)
    authResponse.setRefreshToken(null); // optional, prevent leaking to JS
    return ApiResponse.success(authResponse, "Successfully signed in");
  }

  @PostMapping("/google")
  ApiResponse<AuthenticationResponse> authenticateWithGoogle(
      @RequestBody @Valid GoogleAuthenticationRequest request,
      HttpServletResponse response) {
    AuthenticationResponse authResponse = authenticationService.authenticateWithGoogle(
        request.getCode(), request.getRedirectUri(), request.getCodeVerifier());

    HttpOnlyCookieUtils.addHttpOnlyCookie(
        response, "refresh_token", authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE_LOGIN);

    authResponse.setRefreshToken(null);
    return ApiResponse.success(authResponse, "Successfully signed in with Google");
  }

  @PostMapping("/forgot-password")
  ApiResponse<String> resetPassword(@RequestParam(value = "email") @NotBlank @Email String email, HttpServletRequest httpServletRequest) {
    String clientIp = ClientIpUtils.getClientIpAddress(httpServletRequest);
    authRateLimitService.assertForgotPasswordAllowed(clientIp, email);
    resetPasswordService.sendForgotPasswordOtp(email);
    return ApiResponse.<String>builder().data("Email has been sent!").build();
  }

  @PutMapping("/verify-otp-password")
  ApiResponse<String> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
    profileService.resetPassword(req.getEmail(), req.getOtp(), req.getNewPassword());
    return ApiResponse.<String>builder().data("Successfully reset password!").build();
  }

  @PutMapping("/change-password")
  ApiResponse<String> changePassword(
      Authentication authentication, @Valid @RequestBody ChangePasswordRequest req) {
    // Security: Use email from JWT, not from request body
    String email = authentication.getName();
    authenticationService.changePassword(email, req.getOldPassword(), req.getNewPassword());
    return ApiResponse.<String>builder().data("Successfully changed password!").build();
  }

  @PostMapping("/logout")
  ApiResponse<String> logout(
      // 1. Read refresh token from cookie instead of RequestBody
      @CookieValue(name = "refresh_token", required = false) String refreshToken,
      // 2. Add HttpServletResponse to delete cookie
      HttpServletResponse response)
      throws ParseException, JOSEException {

    // 3. Call service (if token exists) to revoke token at Keycloak
    if (refreshToken != null && !refreshToken.isEmpty()) {
      authenticationService.logout(refreshToken);
    }

    // 4. ALWAYS delete HttpOnly cookie on the browser side
    HttpOnlyCookieUtils.deleteHttpOnlyCookie(response, "refresh_token");

    return ApiResponse.<String>builder().data("Logged out successfully").build();
  }
}
