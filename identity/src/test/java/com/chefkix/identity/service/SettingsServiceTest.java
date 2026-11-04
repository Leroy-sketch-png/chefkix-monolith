package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.entity.UserSettings;
import com.chefkix.identity.repository.UserSettingsRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    private static final String USER_ID = "cook-theme-tester";

    @Mock
    private UserSettingsRepository settingsRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateAppPreferencesAllowsFreeThemesWithoutSubscriptionCheck() {
        UserSettings settings = UserSettings.builder()
                .userId(USER_ID)
                .app(UserSettings.AppPreferences.builder().theme("light").build())
                .build();
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSettings.AppPreferences update = UserSettings.AppPreferences.builder()
                .theme("dark")
                .build();

        UserSettings.AppPreferences result = settingsService.updateAppPreferences(update);

        assertThat(result.getTheme()).isEqualTo("dark");
        verify(subscriptionService, never()).requirePremium(USER_ID);
        verify(settingsRepository).save(settings);
    }

    @Test
    void updateAppPreferencesAllowsPremiumThemesForActivePremiumSubscriber() {
        UserSettings settings = UserSettings.builder()
                .userId(USER_ID)
                .app(UserSettings.AppPreferences.builder().theme("light").build())
                .build();
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(subscriptionService).requirePremium(USER_ID);

        UserSettings.AppPreferences update = UserSettings.AppPreferences.builder()
                .theme("midnight_gold")
                .build();

        UserSettings.AppPreferences result = settingsService.updateAppPreferences(update);

        assertThat(result.getTheme()).isEqualTo("midnight_gold");
        verify(subscriptionService).requirePremium(USER_ID);
        verify(settingsRepository).save(settings);
    }

    @Test
    void updateAppPreferencesRejectsPremiumThemeWith403ForFreeUser() {
        UserSettings settings = UserSettings.builder()
                .userId(USER_ID)
                .app(UserSettings.AppPreferences.builder().theme("light").build())
                .build();
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        doThrow(new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED))
                .when(subscriptionService).requirePremium(USER_ID);

        UserSettings.AppPreferences update = UserSettings.AppPreferences.builder()
                .theme("gradient_sunset")
                .build();

        assertThatThrownBy(() -> settingsService.updateAppPreferences(update))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PREMIUM_FEATURE_REQUIRED);
                });

        verify(subscriptionService).requirePremium(USER_ID);
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void updateAppPreferencesRejectsInvalidThemeWithInvalidRequest() {
        UserSettings settings = UserSettings.builder()
                .userId(USER_ID)
                .app(UserSettings.AppPreferences.builder().theme("light").build())
                .build();
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        UserSettings.AppPreferences update = UserSettings.AppPreferences.builder()
                .theme("non_existent_disco_theme")
                .build();

        assertThatThrownBy(() -> settingsService.updateAppPreferences(update))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                });

        verify(subscriptionService, never()).requirePremium(USER_ID);
        verify(settingsRepository, never()).save(any());
    }
}
