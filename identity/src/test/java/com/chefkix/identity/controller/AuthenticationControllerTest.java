package com.chefkix.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.request.GoogleAuthenticationRequest;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.service.AuthRateLimitService;
import com.chefkix.identity.service.AuthenticationService;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.identity.service.ResetPasswordService;
import com.chefkix.identity.service.SignupRequestService;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

  @Mock
  private AuthenticationService authenticationService;

  @Mock
  private SignupRequestService signupRequestService;

  @Mock
  private ProfileService profileService;

  @Mock
  private ResetPasswordService resetPasswordService;

  @Mock
  private AuthRateLimitService authRateLimitService;

  @Mock
  private UserProfileRepository userProfileRepository;

  @InjectMocks
  private AuthenticationController controller;

  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    response = new MockHttpServletResponse();
  }

  @Test
  void refreshTokenRejectsMissingCookieAndClearsRefreshCookie() {
    assertThatThrownBy(() -> controller.refreshToken(response, null))
        .isInstanceOf(AppException.class)
        .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHENTICATED));

    verifyNoInteractions(authenticationService);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=");
          assertThat(header).contains("Max-Age=0");
        });
  }

  @Test
  void refreshTokenClearsRefreshCookieWhenServiceRejectsExpiredToken() {
    when(authenticationService.refreshToken("expired-refresh"))
        .thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));

    assertThatThrownBy(() -> controller.refreshToken(response, "expired-refresh"))
        .isInstanceOf(AppException.class)
        .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHENTICATED));

    verify(authenticationService).refreshToken("expired-refresh");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=");
          assertThat(header).contains("Max-Age=0");
        });
  }

  @Test
  void refreshTokenRotatesCookieAndReturnsAccessTokenOnSuccess() {
    when(authenticationService.refreshToken("refresh-123"))
        .thenReturn(AuthenticationResponse.builder()
            .accessToken("access-456")
            .refreshToken("refresh-456")
            .build());

    var result = controller.refreshToken(response, "refresh-123");

    assertThat(result.getData()).containsEntry("accessToken", "access-456");
    verify(authenticationService).refreshToken("refresh-123");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=refresh-456");
          assertThat(header).contains("Max-Age=604800");
        });
  }

  @Test
  void authenticateSetsRefreshCookieAndScrubsRefreshTokenFromBody() {
    AuthenticationRequest request = AuthenticationRequest.builder()
        .emailOrUsername("chefkix@example.com")
        .password("secret")
        .build();
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setRemoteAddr("203.0.113.10");

    when(authenticationService.authenticate(request))
        .thenReturn(AuthenticationResponse.builder()
            .accessToken("access-123")
            .refreshToken("refresh-123")
            .authenticated(true)
            .build());

    var result = controller.authenticate(request, response, servletRequest);

    verify(authRateLimitService).assertLoginAllowed("203.0.113.10");
    verify(authRateLimitService).clearLoginAttempts("203.0.113.10");
    assertThat(result.getData().getAccessToken()).isEqualTo("access-123");
    assertThat(result.getData().getRefreshToken()).isNull();
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=refresh-123");
          assertThat(header).contains("Max-Age=604800");
        });
  }

  @Test
  void authenticateWithGoogleSetsRefreshCookieAndScrubsRefreshTokenFromBody() {
    GoogleAuthenticationRequest request = GoogleAuthenticationRequest.builder()
        .code("auth-code")
        .redirectUri("http://localhost:3000/oauth2/callback/google")
        .codeVerifier("verifier")
        .build();

    when(authenticationService.authenticateWithGoogle(
        request.getCode(),
        request.getRedirectUri(),
        request.getCodeVerifier()))
        .thenReturn(AuthenticationResponse.builder()
            .accessToken("access-789")
            .refreshToken("refresh-789")
            .authenticated(true)
            .build());

    var result = controller.authenticateWithGoogle(request, response);

    assertThat(result.getData().getAccessToken()).isEqualTo("access-789");
    assertThat(result.getData().getRefreshToken()).isNull();
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=refresh-789");
          assertThat(header).contains("Max-Age=604800");
        });
  }

  @Test
  void logoutClearsRefreshCookieWithoutCallingServiceWhenCookieMissing() throws Exception {
    var result = controller.logout(null, response);

    verify(authenticationService, never()).logout(org.mockito.ArgumentMatchers.anyString());
    assertThat(result.getData()).isEqualTo("Logged out successfully");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=");
          assertThat(header).contains("Max-Age=0");
        });
  }

  @Test
  void logoutReturnsServiceFailureMessageAndStillClearsRefreshCookie() throws Exception {
    when(authenticationService.logout("refresh-123")).thenReturn("Failed to log out");

    var result = controller.logout("refresh-123", response);

    verify(authenticationService).logout("refresh-123");
    assertThat(result.getData()).isEqualTo("Failed to log out");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
          assertThat(header).contains("refresh_token=");
          assertThat(header).contains("Max-Age=0");
        });
  }
}