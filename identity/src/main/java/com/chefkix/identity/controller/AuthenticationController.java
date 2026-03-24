package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.*;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.identity.service.*;
import com.chefkix.identity.utils.ClientIpUtils;
import com.chefkix.identity.utils.HttpOnlyCookieUtils;
import com.nimbusds.jose.JOSEException;
// Feign removed in monolith
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.text.ParseException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
  private static final int REFRESH_TOKEN_MAX_AGE_LOGIN = 7 * 24 * 60 * 60; // 7 days
  private static final int REFRESH_TOKEN_MAX_AGE_REFRESH = 30 * 24 * 60 * 60; // 30 days

  AuthenticationService authenticationService;
  SignupRequestService signupRequestService;
  ProfileService profileService;
  ResetPasswordService resetPasswordService;
  AuthRateLimitService authRateLimitService;

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
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing.");
    }

    try {
      AuthenticationResponse authResponse = authenticationService.refreshToken(refreshToken);

      HttpOnlyCookieUtils.addHttpOnlyCookie(
          response,
          "refresh_token",
          authResponse.getRefreshToken(), // Lấy RT mới từ response
          REFRESH_TOKEN_MAX_AGE_REFRESH
          );

      return ApiResponse.<Map<String, String>>builder()
          .data(Map.of("accessToken", authResponse.getAccessToken()))
          .build();

    } catch (WebClientResponseException.BadRequest | WebClientResponseException.Unauthorized e) {
      HttpOnlyCookieUtils.deleteHttpOnlyCookie(response, "refresh_token");

      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
    }
  }

  @PostMapping("/login")
  ApiResponse<AuthenticationResponse> authenticate(
      @RequestBody @Valid AuthenticationRequest request,
      HttpServletResponse response,
      HttpServletRequest httpServletRequest) { // thêm response
    String clientIp = ClientIpUtils.getClientIpAddress(httpServletRequest);
    authRateLimitService.assertLoginAllowed(clientIp);

    // 1. authenticate user và lấy token từ Keycloak
    AuthenticationResponse authResponse = authenticationService.authenticate(request);

    authRateLimitService.clearLoginAttempts(clientIp);

    // 2. Lưu refresh token vào HttpOnly cookie
    HttpOnlyCookieUtils.addHttpOnlyCookie(
        response, "refresh_token", authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE_LOGIN
        );

    // 3. Trả body JSON (không cần refreshToken nữa nếu muốn)
    authResponse.setRefreshToken(null); // optional, tránh leak vào JS
    return ApiResponse.success(authResponse, "Successfully signed in");
  }

  @PostMapping("/forgot-password")
  ApiResponse<String> resetPassword(@RequestParam(value = "email") String email, HttpServletRequest httpServletRequest) {
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
      // 1. Đọc refresh token từ cookie thay vì RequestBody
      @CookieValue(name = "refresh_token", required = false) String refreshToken,
      // 2. Thêm HttpServletResponse để xoá cookie
      HttpServletResponse response)
      throws ParseException, JOSEException {

    // 3. Gọi service (nếu có token) để revoke token ở Keycloak
    if (refreshToken != null && !refreshToken.isEmpty()) {
      authenticationService.logout(refreshToken);
    }

    // 4. LUÔN LUÔN xoá HttpOnly cookie ở phía trình duyệt
    HttpOnlyCookieUtils.deleteHttpOnlyCookie(response, "refresh_token");

    return ApiResponse.<String>builder().data("Logged out successfully").build();
  }
}
