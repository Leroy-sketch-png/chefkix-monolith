// OtpController.java
package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.EmailVerificationRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.identity.service.SignupRequestService;
import com.chefkix.identity.utils.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpController {
  ProfileService profileService;
  SignupRequestService signupRequestService;

  @PostMapping("/verify-otp")
  public ApiResponse<ProfileResponse> verifyOtp(@RequestBody EmailVerificationRequest request) {
    return ApiResponse.<ProfileResponse>builder()
        .data(profileService.verifyOtpAndCreateUser(request.getEmail(), request.getOtp()))
        .build();
  }

  @PostMapping("/resend-otp")
  public ApiResponse<String> resendOtp(
      @RequestParam String email, HttpServletRequest httpServletRequest) {
    String clientIp = ClientIpUtils.getClientIpAddress(httpServletRequest);
    signupRequestService.resendOtp(email, clientIp);
    return ApiResponse.success("Successfully resent OTP");
  }
}
