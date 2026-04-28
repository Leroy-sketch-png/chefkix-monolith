package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.client.KeycloakAdminClient;
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
}