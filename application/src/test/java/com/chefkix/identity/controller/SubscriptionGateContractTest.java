package com.chefkix.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.identity.api.SubscriptionProvider;
import com.chefkix.identity.dto.response.AdvancedCreatorStatsResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.service.StatisticsService;
import com.chefkix.identity.service.SubscriptionProviderImpl;
import com.chefkix.identity.service.SubscriptionService;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class SubscriptionGateContractTest {

    private static final String SUBSCRIBER_ID = "subscriber-99";
    private static final String FREE_USER_ID = "free-user-11";

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private SubscriptionService subscriptionService;

    private StatisticController controller;
    private SubscriptionProvider subscriptionProvider;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        controller = new StatisticController(statisticsService, subscriptionService);
        subscriptionProvider = new SubscriptionProviderImpl(subscriptionService);
        exceptionHandler = new GlobalExceptionHandler();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void providerContractIntegratesWithSubscriptionService() {
        when(subscriptionService.isPremium(SUBSCRIBER_ID)).thenReturn(true);
        when(subscriptionService.isPremium(FREE_USER_ID)).thenReturn(false);
        when(subscriptionService.hasFeature(SUBSCRIBER_ID, "ADVANCED_ANALYTICS")).thenReturn(true);
        when(subscriptionService.hasFeature(FREE_USER_ID, "ADVANCED_ANALYTICS")).thenReturn(false);

        assertThat(subscriptionProvider.isPremium(SUBSCRIBER_ID)).isTrue();
        assertThat(subscriptionProvider.isPremium(FREE_USER_ID)).isFalse();
        assertThat(subscriptionProvider.hasFeature(SUBSCRIBER_ID, "ADVANCED_ANALYTICS")).isTrue();
        assertThat(subscriptionProvider.hasFeature(FREE_USER_ID, "ADVANCED_ANALYTICS")).isFalse();

        doNothing().when(subscriptionService).requirePremium(SUBSCRIBER_ID);
        subscriptionProvider.requirePremium(SUBSCRIBER_ID);

        doThrow(new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED))
                .when(subscriptionService).requirePremium(FREE_USER_ID);
        assertThatThrownBy(() -> subscriptionProvider.requirePremium(FREE_USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PREMIUM_FEATURE_REQUIRED));
    }

    @Test
    void unprivilegedUserReceives403ForbiddenWhenAccessingAdvancedCreatorStats() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(FREE_USER_ID, null));

        doThrow(new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED))
                .when(subscriptionService).requirePremium(FREE_USER_ID);

        try {
            controller.getMyAdvancedCreatorStats();
        } catch (AppException ex) {
            ResponseEntity<ApiResponse<?>> response =
                    exceptionHandler.handleAppException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getStatusCode()).isEqualTo(403);
            assertThat(response.getBody().getMessage())
                    .isEqualTo("This feature requires a premium subscription");
        }

        verify(subscriptionService).requirePremium(FREE_USER_ID);
        verifyNoInteractions(statisticsService);
    }

    @Test
    void privilegedUserReceives200OkWhenAccessingAdvancedCreatorStats() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(SUBSCRIBER_ID, null));

        doNothing().when(subscriptionService).requirePremium(SUBSCRIBER_ID);

        AdvancedCreatorStatsResponse expectedData = AdvancedCreatorStatsResponse.builder()
                .baseStats(CreatorStatsResponse.builder()
                        .totalRecipesPublished(8)
                        .totalCooksOfYourRecipes(340)
                        .build())
                .highPerformingRecipes(List.of(
                        CreatorStatsResponse.TopRecipeDto.builder()
                                .id("rec-1")
                                .title("Banh Mi Masterclass")
                                .cookCount(180)
                                .build()
                ))
                .audienceRetentionRate(0.85)
                .totalSavesAcrossRecipes(680L)
                .build();

        when(statisticsService.getMyAdvancedCreatorStats(SUBSCRIBER_ID)).thenReturn(expectedData);

        ApiResponse<AdvancedCreatorStatsResponse> response = controller.getMyAdvancedCreatorStats();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getHighPerformingRecipes()).hasSize(1);
        assertThat(response.getData().getAudienceRetentionRate()).isEqualTo(0.85);
        assertThat(response.getData().getTotalSavesAcrossRecipes()).isEqualTo(680L);

        verify(subscriptionService).requirePremium(SUBSCRIBER_ID);
        verify(statisticsService).getMyAdvancedCreatorStats(SUBSCRIBER_ID);
    }
}
