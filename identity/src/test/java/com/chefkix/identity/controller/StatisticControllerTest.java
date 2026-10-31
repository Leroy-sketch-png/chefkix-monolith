package com.chefkix.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.identity.dto.response.AdvancedCreatorStatsResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.service.StatisticsService;
import com.chefkix.identity.service.SubscriptionService;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class StatisticControllerTest {

    private static final String USER_ID = "creator-user-42";

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private StatisticController controller;

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
    void getMyCreatorStatsReturnsFreeTierStatsWithoutPremiumGate() {
        CreatorStatsResponse mockStats = CreatorStatsResponse.builder()
                .totalRecipesPublished(5)
                .totalCooksOfYourRecipes(120)
                .xpEarnedAsCreator(6000)
                .avgRating(4.8)
                .build();
        when(statisticsService.getMyCreatorStats(USER_ID)).thenReturn(mockStats);

        ApiResponse<CreatorStatsResponse> response = controller.getMyCreatorStats();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(mockStats);
        verify(statisticsService).getMyCreatorStats(USER_ID);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getMyAdvancedCreatorStatsSucceedsForPremiumSubscriber() {
        doNothing().when(subscriptionService).requirePremium(USER_ID);

        AdvancedCreatorStatsResponse mockAdvanced = AdvancedCreatorStatsResponse.builder()
                .baseStats(CreatorStatsResponse.builder()
                        .totalRecipesPublished(10)
                        .totalCooksOfYourRecipes(500)
                        .build())
                .highPerformingRecipes(List.of(
                        CreatorStatsResponse.TopRecipeDto.builder()
                                .id("recipe-101")
                                .title("Gourmet Pho")
                                .cookCount(250)
                                .build()
                ))
                .audienceRetentionRate(0.85)
                .totalSavesAcrossRecipes(1000L)
                .build();
        when(statisticsService.getMyAdvancedCreatorStats(USER_ID)).thenReturn(mockAdvanced);

        ApiResponse<AdvancedCreatorStatsResponse> response = controller.getMyAdvancedCreatorStats();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(mockAdvanced);
        assertThat(response.getData().getAudienceRetentionRate()).isEqualTo(0.85);
        assertThat(response.getData().getHighPerformingRecipes()).hasSize(1);
        verify(subscriptionService).requirePremium(USER_ID);
        verify(statisticsService).getMyAdvancedCreatorStats(USER_ID);
    }

    @Test
    void getMyAdvancedCreatorStatsFailsClosedWith403WhenUserIsNotPremium() {
        doThrow(new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED))
                .when(subscriptionService).requirePremium(USER_ID);

        assertThatThrownBy(() -> controller.getMyAdvancedCreatorStats())
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PREMIUM_FEATURE_REQUIRED);
                    assertThat(ex.getErrorCode().getCode()).isEqualTo(403);
                    assertThat(ex.getMessage()).isEqualTo("This feature requires a premium subscription");
                });

        verify(subscriptionService).requirePremium(USER_ID);
        verifyNoInteractions(statisticsService);
    }
}
