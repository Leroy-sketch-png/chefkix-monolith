package com.chefkix.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.client.KeycloakAdminClient;
import com.chefkix.identity.dto.identity.OidcUserInfoResponse;
import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.request.GoogleAuthenticationRequest;
import com.chefkix.identity.dto.response.UserResponse;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.exception.ErrorNormalizer;
import com.chefkix.identity.mapper.UserMapper;
import com.chefkix.identity.repository.UserActivityRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.identity.service.AuthRateLimitService;
import com.chefkix.identity.service.AuthenticationService;
import com.chefkix.identity.service.KeycloakService;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.identity.service.ResetPasswordService;
import com.chefkix.identity.service.SignupRequestService;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@ExtendWith(MockitoExtension.class)
class AuthenticationContractTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;
    @Mock
    private ErrorNormalizer errorNormalizer;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private UserActivityRepository userActivityRepository;
    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

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

    private AuthenticationService authenticationService;
    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
    authenticationService = new AuthenticationService(
        keycloakAdminClient,
        errorNormalizer,
        userRepository,
        userMapper,
        keycloakService,
        userActivityRepository,
        profileRepository,
        eventPublisher);
    ReflectionTestUtils.setField(authenticationService, "publicBaseUrl", "http://localhost:3000");

    authenticationController = new AuthenticationController(
        authenticationService,
        signupRequestService,
        profileService,
        resetPasswordService,
        authRateLimitService,
        userProfileRepository);
    }

    @Test
    void loginControllerToServiceStoresRefreshCookieAndScrubsBodyRefreshToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationRequest loginRequest = AuthenticationRequest.builder()
        .emailOrUsername("chef@example.com")
        .password("secret123")
        .build();
    User existingUser = User.builder()
        .id("user-1")
        .email("chef@example.com")
        .username("chef")
        .enabled(true)
        .build();

    when(keycloakService.login("chef@example.com", "secret123")).thenReturn(TokenExchangeResponse.builder()
        .accessToken("access-123")
        .refreshToken("refresh-123")
        .idToken("id-123")
        .scope("openid profile")
        .build());
    when(userRepository.findByEmailOrUsername("chef@example.com", "chef@example.com"))
        .thenReturn(java.util.Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userMapper.toUserResponse(any(User.class)))
        .thenReturn(new UserResponse("user-1", "chef", "chef@example.com", true, java.util.Set.of()));

    var result = authenticationController.authenticate(loginRequest, response, request);

    assertThat(result.getData().getAccessToken()).isEqualTo("access-123");
    assertThat(result.getData().getRefreshToken()).isNull();
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=refresh-123");
            assertThat(header).contains("Max-Age=604800");
        });
    verify(authRateLimitService).assertLoginAllowed("127.0.0.1");
    verify(authRateLimitService).clearLoginAttempts("127.0.0.1");
    verify(keycloakService).login("chef@example.com", "secret123");
    }

    @Test
    void loginControllerRejectsIncompleteTokenPayloadWithoutSettingCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationRequest loginRequest = AuthenticationRequest.builder()
        .emailOrUsername("chef@example.com")
        .password("secret123")
        .build();

    when(keycloakService.login("chef@example.com", "secret123")).thenReturn(TokenExchangeResponse.builder()
        .accessToken("access-123")
        .refreshToken("   ")
        .idToken("id-123")
        .scope("openid profile")
        .build());

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.authenticate(loginRequest, response, request));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    verify(authRateLimitService).assertLoginAllowed("127.0.0.1");
    verify(authRateLimitService, never()).clearLoginAttempts("127.0.0.1");
    verify(userRepository, never()).findByEmailOrUsername(any(), any());
    verify(keycloakService).login("chef@example.com", "secret123");
    }

    @Test
    void loginControllerDoesNotSetCookieOrClearAttemptsWhenCredentialsAreInvalid() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationRequest loginRequest = AuthenticationRequest.builder()
        .emailOrUsername("chef@example.com")
        .password("wrongpass")
        .build();

    when(keycloakService.login("chef@example.com", "wrongpass"))
        .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.authenticate(loginRequest, response, request));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    verify(authRateLimitService).assertLoginAllowed("127.0.0.1");
    verify(authRateLimitService, never()).clearLoginAttempts("127.0.0.1");
    verify(keycloakService).login("chef@example.com", "wrongpass");
    }

    @Test
    void refreshTokenControllerToServiceRotatesCookieAndReturnsAccessTokenOnly() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(keycloakService.refreshToken("refresh-123")).thenReturn(TokenExchangeResponse.builder()
        .accessToken("access-456")
        .refreshToken("refresh-456")
        .idToken("id-456")
        .scope("openid profile")
        .build());

    var result = authenticationController.refreshToken(response, "refresh-123");

    assertThat(result.getData()).containsEntry("accessToken", "access-456");
    assertThat(result.getData()).hasSize(1);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=refresh-456");
            assertThat(header).contains("Max-Age=604800");
        });
    verify(keycloakService).refreshToken("refresh-123");
    }

    @Test
    void refreshTokenControllerDeletesCookieWhenRefreshCookieIsMissing() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, null));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    }

    @Test
    void refreshTokenControllerDeletesCookieWhenRefreshCookieIsBlank() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, "   "));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService, never()).refreshToken("   ");
    }

    @Test
    void refreshTokenControllerDeletesCookieAndRethrowsUnauthenticatedOnServiceFailure() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keycloakService.refreshToken("refresh-123"))
        .thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, "refresh-123"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService).refreshToken("refresh-123");
    }

    @Test
    void refreshTokenControllerPreservesCookieWhenFailureIsNotUnauthenticated() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keycloakService.refreshToken("refresh-123"))
        .thenThrow(new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, "refresh-123"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    verify(keycloakService).refreshToken("refresh-123");
    }

    @Test
    void refreshTokenControllerDeletesCookieAndNormalizesRawBadRequest() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keycloakService.refreshToken("refresh-123"))
        .thenThrow(WebClientResponseException.create(
            400,
            "Bad Request",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8));

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, "refresh-123"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService).refreshToken("refresh-123");
    }

    @Test
    void refreshTokenControllerDeletesCookieAndNormalizesRawUnauthorized() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keycloakService.refreshToken("refresh-123"))
        .thenThrow(WebClientResponseException.create(
            401,
            "Unauthorized",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8));

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, "refresh-123"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService).refreshToken("refresh-123");
    }

    @Test
    void refreshTokenControllerDeletesCookieWhenRotationReturnsBlankRefreshToken() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keycloakService.refreshToken("refresh-123")).thenReturn(TokenExchangeResponse.builder()
        .accessToken("access-456")
        .refreshToken("   ")
        .idToken("id-456")
        .scope("openid profile")
        .build());

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.refreshToken(response, "refresh-123"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService).refreshToken("refresh-123");
    }

    @Test
    void googleControllerToServiceStoresRefreshCookieAndScrubsBodyRefreshToken() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    GoogleAuthenticationRequest googleRequest = GoogleAuthenticationRequest.builder()
        .code("google-code")
        .redirectUri("http://localhost:3000/oauth2/callback/google")
        .codeVerifier("verifier-123")
        .build();

    when(keycloakService.exchangeAuthorizationCode(
        "google-code",
        "http://localhost:3000/oauth2/callback/google",
        "verifier-123"))
        .thenReturn(TokenExchangeResponse.builder()
            .accessToken("google-access")
            .refreshToken("google-refresh")
            .idToken("google-id")
            .scope("openid profile email")
            .build());
    when(keycloakService.getUserInfo("google-access")).thenReturn(OidcUserInfoResponse.builder()
        .sub("google-sub")
        .email("chef@gmail.com")
        .preferredUsername("chefg")
        .givenName("Chef")
        .familyName("Google")
        .name("Chef Google")
        .picture("https://example.com/avatar.png")
        .build());
    when(profileRepository.findByUserId("google-sub")).thenReturn(java.util.Optional.empty());
    when(profileRepository.findByEmail("chef@gmail.com")).thenReturn(java.util.Optional.empty());
    when(profileRepository.findByUsername("chefg")).thenReturn(java.util.Optional.empty());
    when(profileRepository.save(any(com.chefkix.identity.entity.UserProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.findByGoogleId("google-sub")).thenReturn(java.util.Optional.empty());
    when(userRepository.findByEmail("chef@gmail.com")).thenReturn(java.util.Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        if (user.getId() == null) {
        user.setId("google-user-1");
        }
        return user;
    });
    when(userMapper.toUserResponse(any(User.class)))
        .thenReturn(new UserResponse("google-user-1", "chefg", "chef@gmail.com", true, java.util.Set.of()));
    when(userActivityRepository.findByKeycloakId("google-user-1")).thenReturn(java.util.Optional.empty());

    var result = authenticationController.authenticateWithGoogle(googleRequest, response);

    assertThat(result.getData().getAccessToken()).isEqualTo("google-access");
    assertThat(result.getData().getRefreshToken()).isNull();
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=google-refresh");
            assertThat(header).contains("Max-Age=604800");
        });
    verify(keycloakService).exchangeAuthorizationCode(
        "google-code",
        "http://localhost:3000/oauth2/callback/google",
        "verifier-123");
    verify(keycloakService).getUserInfo("google-access");
    }

    @Test
    void googleControllerRejectsIncompleteTokenPayloadWithoutSettingCookie() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    GoogleAuthenticationRequest googleRequest = GoogleAuthenticationRequest.builder()
        .code("google-code")
        .redirectUri("http://localhost:3000/oauth2/callback/google")
        .codeVerifier("verifier-123")
        .build();

    when(keycloakService.exchangeAuthorizationCode(
        "google-code",
        "http://localhost:3000/oauth2/callback/google",
        "verifier-123"))
        .thenReturn(TokenExchangeResponse.builder()
            .accessToken("google-access")
            .refreshToken(" ")
            .idToken("google-id")
            .scope("openid profile email")
            .build());

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.authenticateWithGoogle(googleRequest, response));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    verify(keycloakService).exchangeAuthorizationCode(
        "google-code",
        "http://localhost:3000/oauth2/callback/google",
        "verifier-123");
    verify(keycloakService, never()).getUserInfo(any());
    }

    @Test
    void googleControllerRejectsInvalidRedirectWithoutSettingCookie() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    GoogleAuthenticationRequest googleRequest = GoogleAuthenticationRequest.builder()
        .code("google-code")
        .redirectUri("http://evil.example.com/oauth2/callback/google")
        .codeVerifier("verifier-123")
        .build();

    AppException thrown = assertThrows(
        AppException.class,
        () -> authenticationController.authenticateWithGoogle(googleRequest, response));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    verify(keycloakService, never()).exchangeAuthorizationCode(any(), any(), any());
    verify(keycloakService, never()).getUserInfo(any());
    }

    @Test
    void logoutControllerToServiceFailureStillClearsCookieAndPreservesFailureMessage() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    doThrow(new RuntimeException("keycloak down"))
        .when(keycloakService)
        .logout("refresh-123");

    var result = authenticationController.logout("refresh-123", response);

    assertThat(result.getData()).isEqualTo("Failed to log out");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService).logout("refresh-123");
    }

    @Test
    void logoutControllerWithoutRefreshCookieStillClearsCookieAndSkipsServiceCall() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    var result = authenticationController.logout(null, response);

    assertThat(result.getData()).isEqualTo("Logged out successfully");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService, never()).logout(any());
    }

    @Test
    void logoutControllerWithBlankRefreshCookieStillClearsCookieAndSkipsServiceCall() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    var result = authenticationController.logout("   ", response);

    assertThat(result.getData()).isEqualTo("Logged out successfully");
    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(header -> {
            assertThat(header).contains("refresh_token=");
            assertThat(header).contains("Max-Age=0");
        });
    verify(keycloakService, never()).logout(any());
    }
}