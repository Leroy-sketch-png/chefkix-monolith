package com.chefkix.identity.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.service.StatisticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Public statistics endpoints. INTERNAL-ONLY operations (add_xp, update_completion)
 * have been removed from REST — they are called via SPI interfaces within the JVM only.
 * See: ProfileProvider.addXp(), ProfileProvider.updateAfterCompletion()
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {

  StatisticsService statisticsService;

  // REMOVED: POST /{userId}/add_xp — IDOR vulnerability. XP is awarded via Kafka xp-delivery only.
  // REMOVED: POST /update_completion — internal SPI only, not a REST endpoint.

  @GetMapping("/me/creator-stats")
  public ApiResponse<CreatorStatsResponse> getMyCreatorStats() {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    return ApiResponse.success(statisticsService.getMyCreatorStats(currentUserId));
  }
}
