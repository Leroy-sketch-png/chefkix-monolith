package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chefkix.identity.client.KeycloakAdminClient;
import com.chefkix.identity.dto.identity.ResetPasswordParam;
import com.chefkix.identity.dto.response.UserResponse;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.entity.UserActivity;
import com.chefkix.identity.entity.UserProfile;
import java.util.Optional;
import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.exception.ErrorNormalizer;
import com.chefkix.identity.mapper.UserMapper;
import com.chefkix.identity.repository.UserActivityRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

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

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "clientId", "chefkix-be");
        ReflectionTestUtils.setField(authenticationService, "clientSecret", "secret");
        ReflectionTestUtils.setField(authenticationService, "publicBaseUrl", "http://localhost:3000");
    }

    @Test
    void refreshTokenReturnsMappedAuthenticationResponse() {
        TokenExchangeResponse tokenResponse = TokenExchangeResponse.builder()
                .accessToken("access-123")
                .refreshToken("refresh-123")
                .idToken("id-123")
                .scope("openid profile")
                .build();

        when(keycloakService.refreshToken("refresh-123")).thenReturn(tokenResponse);

        var result = authenticationService.refreshToken("refresh-123");

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("access-123");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-123");
        assertThat(result.getIdToken()).isEqualTo("id-123");
        assertThat(result.getScope()).isEqualTo("openid profile");
    }

        @Test
        void refreshTokenRejectsMissingRotatedRefreshToken() {
        TokenExchangeResponse tokenResponse = TokenExchangeResponse.builder()
            .accessToken("access-123")
            .refreshToken("   ")
            .idToken("id-123")
            .scope("openid profile")
            .build();

        when(keycloakService.refreshToken("refresh-123")).thenReturn(tokenResponse);

        assertThatThrownBy(() -> authenticationService.refreshToken("refresh-123"))
            .isInstanceOf(AppException.class)
            .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED));
        }

        @Test
        void refreshTokenRejectsMissingAccessToken() {
        TokenExchangeResponse tokenResponse = TokenExchangeResponse.builder()
            .accessToken(" ")
            .refreshToken("refresh-123")
            .idToken("id-123")
            .scope("openid profile")
            .build();

        when(keycloakService.refreshToken("refresh-123")).thenReturn(tokenResponse);

        assertThatThrownBy(() -> authenticationService.refreshToken("refresh-123"))
            .isInstanceOf(AppException.class)
            .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED));
        }

    @Test
    void logoutReturnsSuccessMessageWhenKeycloakLogoutSucceeds() {
        var result = authenticationService.logout("refresh-123");

        assertThat(result).isEqualTo("Successfully logged out");
        verify(keycloakService).logout("refresh-123");
    }

    @Test
    void logoutReturnsFailureMessageWhenKeycloakLogoutThrows() {
        doThrow(new RuntimeException("keycloak down"))
            .when(keycloakService)
            .logout("refresh-123");

        var result = authenticationService.logout("refresh-123");

        assertThat(result).isEqualTo("Failed to log out");
    }

    @Test
    void authenticateMapsInvalidGrantToInvalidCredentials() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername("chefkix@example.com")
                .password("wrong-password")
                .build();

        when(keycloakService.login(request.getEmailOrUsername(), request.getPassword()))
                .thenThrow(new RuntimeException("invalid_grant: Invalid user credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void authenticateDoesNotLogCredentialsOrKeycloakErrorBody() {
        String email = "private-login-echo-91@example.test";
        String password = "private-login-password-91";
        String echoedBody = "private-keycloak-error-91";
        AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername(email)
                .password(password)
                .build();
        when(keycloakService.login(email, password))
                .thenThrow(new RuntimeException(echoedBody));

        Logger logger = (Logger) LoggerFactory.getLogger(AuthenticationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(AppException.class);

            String capturedLogs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(capturedLogs)
                    .contains("RuntimeException")
                    .doesNotContain(email, password, echoedBody);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

            @Test
            void authenticateRejectsIncompleteTokenPayloadBeforeLocalUserSync() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername("chefkix@example.com")
                .password("secret123")
                .build();

            when(keycloakService.login(request.getEmailOrUsername(), request.getPassword()))
                .thenReturn(TokenExchangeResponse.builder()
                    .accessToken("access-123")
                    .refreshToken("   ")
                    .idToken("id-123")
                    .scope("openid profile")
                    .build());

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED));

            verify(userRepository, never()).findByEmailOrUsername(any(), any());
            verify(userActivityRepository, never()).findByKeycloakId(any());
            }

    @Test
    void authenticateWithGoogleRejectsUnexpectedRedirectOrigin() {
        assertThatThrownBy(() -> authenticationService.authenticateWithGoogle(
                "code",
                "https://evil.example.com/oauth2/callback/google",
                "verifier"))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(keycloakService, never()).exchangeAuthorizationCode("code", "https://evil.example.com/oauth2/callback/google", "verifier");
    }

    @Test
    void authenticateWithGoogleRejectsIncompleteTokenPayloadBeforeUserInfoFetch() {
        when(keycloakService.exchangeAuthorizationCode(
                "code",
                "http://localhost:3000/oauth2/callback/google",
                "verifier"))
                .thenReturn(TokenExchangeResponse.builder()
                        .accessToken("google-access")
                        .refreshToken(" ")
                        .idToken("google-id")
                        .scope("openid profile email")
                        .build());

        assertThatThrownBy(() -> authenticationService.authenticateWithGoogle(
                "code",
                "http://localhost:3000/oauth2/callback/google",
                "verifier"))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(keycloakService, never()).getUserInfo(any());
        verify(userRepository, never()).findByGoogleId(any());
        verify(profileRepository, never()).findByUserId(any());
    }
    @Test
    void authenticateMapsAccountDisabledToAccountDisabledErrorCode() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername("disabled@example.com")
                .password("any-pass")
                .build();

        when(keycloakService.login(request.getEmailOrUsername(), request.getPassword()))
                .thenThrow(new RuntimeException("account_disabled: User account is disabled"));

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    @Test
    void authenticateMapsEmailNotVerifiedToAccountNotVerifiedErrorCode() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername("unverified@example.com")
                .password("any-pass")
                .build();

        when(keycloakService.login(request.getEmailOrUsername(), request.getPassword()))
                .thenThrow(new RuntimeException("email_not_verified: Please verify email first"));

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_VERIFIED));
    }

    @Test
    void authenticateHappyPathSyncsUserAndActivity() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername("chef@example.com")
                .password("valid-pass")
                .build();

        TokenExchangeResponse tokenResponse = TokenExchangeResponse.builder()
                .accessToken("access-abc")
                .refreshToken("refresh-abc")
                .idToken("id-abc")
                .scope("openid profile")
                .build();

        User existingUser = User.builder()
                .id("user-123")
                .email("chef@example.com")
                .username("chef")
                .build();

        UserActivity existingActivity = UserActivity.builder()
                .id("activity-123")
                .keycloakId("user-123")
                .build();

        UserResponse mappedResponse = new UserResponse();
        mappedResponse.setId("user-123");
        mappedResponse.setEmail("chef@example.com");

        when(keycloakService.login(request.getEmailOrUsername(), request.getPassword()))
                .thenReturn(tokenResponse);
        when(userRepository.findByEmailOrUsername("chef@example.com", "chef@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userActivityRepository.findByKeycloakId("user-123"))
                .thenReturn(Optional.of(existingActivity));
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserResponse(existingUser)).thenReturn(mappedResponse);

        var result = authenticationService.authenticate(request);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("access-abc");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-abc");
        assertThat(result.getUser().getId()).isEqualTo("user-123");
        verify(userRepository).save(existingUser);
        verify(userActivityRepository).save(existingActivity);
    }

    @Test
    void authenticateHappyPathCreatesNewUserWhenNotPresent() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .emailOrUsername("newchef@example.com")
                .password("valid-pass")
                .build();

        TokenExchangeResponse tokenResponse = TokenExchangeResponse.builder()
                .accessToken("access-new")
                .refreshToken("refresh-new")
                .idToken("id-new")
                .scope("openid profile")
                .build();

        User newUser = User.builder()
                .id("new-user-456")
                .email("newchef@example.com")
                .username("newchef@example.com")
                .build();

        UserResponse mappedResponse = new UserResponse();
        mappedResponse.setId("new-user-456");

        when(keycloakService.login(request.getEmailOrUsername(), request.getPassword()))
                .thenReturn(tokenResponse);
        when(userRepository.findByEmailOrUsername("newchef@example.com", "newchef@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userActivityRepository.findByKeycloakId("new-user-456"))
                .thenReturn(Optional.empty());
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mappedResponse);

        var result = authenticationService.authenticate(request);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("access-new");
        verify(userRepository, org.mockito.Mockito.times(2)).save(any(User.class));
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void changePasswordSuccessfullyResetsPasswordAndLogsOutUser() {
        String email = "chef@example.com";
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";

        TokenExchangeResponse loginResponse = TokenExchangeResponse.builder()
                .accessToken("user-access-token")
                .refreshToken("user-refresh-token")
                .build();

        TokenExchangeResponse adminToken = TokenExchangeResponse.builder()
                .accessToken("admin-access-token")
                .build();

        UserProfile profile = UserProfile.builder()
                .userId("keycloak-user-789")
                .email(email)
                .build();

        when(keycloakService.login(email, oldPassword)).thenReturn(loginResponse);
        when(keycloakAdminClient.exchangeToken(any())).thenReturn(adminToken);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(profile));

        authenticationService.changePassword(email, oldPassword, newPassword);

        verify(keycloakAdminClient).resetPassword(
                org.mockito.ArgumentMatchers.eq("Bearer admin-access-token"),
                org.mockito.ArgumentMatchers.eq("keycloak-user-789"),
                any(ResetPasswordParam.class));
        verify(keycloakAdminClient).logoutUser("Bearer admin-access-token", "keycloak-user-789");
    }

    @Test
    void changePasswordRejectsWhenOldPasswordInvalid() {
        String email = "chef@example.com";
        when(keycloakService.login(email, "wrongPassword"))
                .thenThrow(new RuntimeException("invalid credentials"));

        assertThatThrownBy(() -> authenticationService.changePassword(email, "wrongPassword", "newPassword456"))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(keycloakAdminClient, never()).exchangeToken(any());
        verify(keycloakAdminClient, never()).resetPassword(any(), any(), any());
    }

    @Test
    void changePasswordRejectsWhenUserProfileNotFound() {
        String email = "missing@example.com";
        TokenExchangeResponse loginResponse = TokenExchangeResponse.builder()
                .accessToken("user-access-token")
                .build();
        TokenExchangeResponse adminToken = TokenExchangeResponse.builder()
                .accessToken("admin-access-token")
                .build();

        when(keycloakService.login(email, "validPassword")).thenReturn(loginResponse);
        when(keycloakAdminClient.exchangeToken(any())).thenReturn(adminToken);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.changePassword(email, "validPassword", "newPassword456"))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(keycloakAdminClient, never()).resetPassword(any(), any(), any());
    }

    @Test
    void changePasswordPropagatesNormalizedKeycloakExceptionOnResetFailure() {
        String email = "chef@example.com";
        TokenExchangeResponse loginResponse = TokenExchangeResponse.builder()
                .accessToken("user-access-token")
                .build();
        TokenExchangeResponse adminToken = TokenExchangeResponse.builder()
                .accessToken("admin-access-token")
                .build();
        UserProfile profile = UserProfile.builder()
                .userId("keycloak-user-789")
                .email(email)
                .build();

        when(keycloakService.login(email, "validPassword")).thenReturn(loginResponse);
        when(keycloakAdminClient.exchangeToken(any())).thenReturn(adminToken);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(profile));

        RuntimeException keycloakError = new RuntimeException("reset failed in keycloak");
        doThrow(keycloakError).when(keycloakAdminClient).resetPassword(any(), any(), any());
        when(errorNormalizer.handleKeyCloakException(keycloakError))
                .thenReturn(new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        assertThatThrownBy(() -> authenticationService.changePassword(email, "validPassword", "newPassword456"))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION));

        verify(keycloakAdminClient, never()).logoutUser(any(), any());
    }
}
