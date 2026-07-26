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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpController {
private static final int REFRESH_TOKEN_MAX_AGE_LOGIN = 7 * 24 * 60 * 60;

  ProfileService profileService;
  SignupRequestService signupRequestService;
  AuthenticationService authenticationService;

  @PostMapping("/verify-otp")
  public ApiResponse<AuthenticationResponse> verifyOtp(
      @RequestBody @Valid EmailVerificationRequest request,
      HttpServletResponse response) {
    profileService.verifyOtpAndCreateUser(
        request.getEmail(), request.getOtp(), request.getPassword());

    AuthenticationResponse authResponse;
    try {
      authResponse = authenticationService.authenticate(
          AuthenticationRequest.builder()
              .emailOrUsername(request.getEmail())
              .password(request.getPassword())
              .build());
    } catch (RuntimeException exception) {
      log.warn(
          "Account created for {} but automatic sign-in failed; user can sign in manually",
          request.getEmail(),
          exception);
      return ApiResponse.success(
          null, "Email verified and account created. Please sign in to continue.");
    }

    HttpOnlyCookieUtils.addHttpOnlyCookie(
        response, "refresh_token", authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE_LOGIN);

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
