// OtpController.java
package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.request.EmailVerificationRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.identity.service.AuthenticationService;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.identity.service.SignupRequestService;
import com.chefkix.identity.utils.ClientIpUtils;
import com.chefkix.identity.utils.HttpOnlyCookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpController {
  private static final int REFRESH_TOKEN_MAX_AGE_LOGIN = 7 * 24 * 60 * 60; // 7 days

  ProfileService profileService;
  SignupRequestService signupRequestService;
  AuthenticationService authenticationService;

  @PostMapping("/verify-otp")
  public ApiResponse<AuthenticationResponse> verifyOtp(
      @RequestBody @Valid EmailVerificationRequest request,
      HttpServletResponse response) {
    // 1. Verify OTP, create Keycloak user + MongoDB profile, get plaintext password
    String password = profileService.verifyOtpAndCreateUser(request.getEmail(), request.getOtp());

    // 2. Auto-login: authenticate the newly created user via Keycloak
    AuthenticationResponse authResponse = authenticationService.authenticate(
        AuthenticationRequest.builder()
            .emailOrUsername(request.getEmail())
            .password(password)
            .build());

    // 3. Set refresh token as HttpOnly cookie (same pattern as login endpoint)
    HttpOnlyCookieUtils.addHttpOnlyCookie(
        response, "refresh_token", authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE_LOGIN);

    // 4. Null out refresh token from body (prevent JS access)
    authResponse.setRefreshToken(null);

    return ApiResponse.success(authResponse, "Email verified and signed in successfully");
  }

  @PostMapping("/resend-otp")
  public ApiResponse<String> resendOtp(
      @RequestParam("email") String email, HttpServletRequest httpServletRequest) {
    String clientIp = ClientIpUtils.getClientIpAddress(httpServletRequest);
    signupRequestService.resendOtp(email, clientIp);
    return ApiResponse.success("Successfully resent OTP");
  }
}
