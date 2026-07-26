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
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {

  StatisticsService statisticsService;


  @GetMapping("/me/creator-stats")
  public ApiResponse<CreatorStatsResponse> getMyCreatorStats() {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    return ApiResponse.success(statisticsService.getMyCreatorStats(currentUserId));
  }
}
