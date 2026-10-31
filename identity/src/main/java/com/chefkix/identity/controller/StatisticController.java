package com.chefkix.identity.controller;

import com.chefkix.identity.dto.response.AdvancedCreatorStatsResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.service.StatisticsService;
import com.chefkix.identity.service.SubscriptionService;
import com.chefkix.shared.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user creator statistics and premium creator analytics.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {

  StatisticsService statisticsService;
  SubscriptionService subscriptionService;

  @GetMapping("/me/creator-stats")
  public ApiResponse<CreatorStatsResponse> getMyCreatorStats() {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    return ApiResponse.success(statisticsService.getMyCreatorStats(currentUserId));
  }

  @GetMapping("/me/creator-stats/advanced")
  public ApiResponse<AdvancedCreatorStatsResponse> getMyAdvancedCreatorStats() {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    subscriptionService.requirePremium(currentUserId);
    return ApiResponse.success(statisticsService.getMyAdvancedCreatorStats(currentUserId));
  }
}
