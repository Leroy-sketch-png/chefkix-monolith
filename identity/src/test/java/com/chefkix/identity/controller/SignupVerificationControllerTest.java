package com.chefkix.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.request.EmailVerificationRequest;
import com.chefkix.identity.service.AuthenticationService;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.identity.service.SignupRequestService;
import com.chefkix.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SignupVerificationControllerTest {

  private static final String EMAIL = "new-cook@example.com";
  private static final String OTP = "123456";
  private static final String PASSWORD = "strong-password";

  @Mock ProfileService profileService;
  @Mock SignupRequestService signupRequestService;
  @Mock AuthenticationService authenticationService;

  @Test
  void currentEndpointReportsCreatedAccountWhenAutomaticSignInFails() {
    ProfileController controller = new ProfileController(profileService, authenticationService);
    when(authenticationService.authenticate(any(AuthenticationRequest.class)))
        .thenThrow(new IllegalStateException("Keycloak token exchange unavailable"));

    ApiResponse<?> result =
        controller.register(verificationRequest(), new MockHttpServletResponse());

    assertCreatedAccountFallback(result);
  }

  @Test
  void legacyEndpointReportsCreatedAccountWhenAutomaticSignInFails() {
    OtpController controller =
        new OtpController(profileService, signupRequestService, authenticationService);
    when(authenticationService.authenticate(any(AuthenticationRequest.class)))
        .thenThrow(new IllegalStateException("Keycloak token exchange unavailable"));

    ApiResponse<?> result =
        controller.verifyOtp(verificationRequest(), new MockHttpServletResponse());

    assertCreatedAccountFallback(result);
  }

  private void assertCreatedAccountFallback(ApiResponse<?> result) {
    verify(profileService).verifyOtpAndCreateUser(EMAIL, OTP, PASSWORD);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getStatusCode()).isEqualTo(200);
    assertThat(result.getData()).isNull();
    assertThat(result.getMessage())
        .isEqualTo("Email verified and account created. Please sign in to continue.");
  }

  private EmailVerificationRequest verificationRequest() {
    return EmailVerificationRequest.builder()
        .email(EMAIL)
        .otp(OTP)
        .password(PASSWORD)
        .build();
  }
}
