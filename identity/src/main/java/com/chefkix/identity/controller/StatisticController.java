package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.service.StatisticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {

  StatisticsService statisticsService;

  @PostMapping("/{userId}/add_xp")
  public ApiResponse<ProfileResponse> addXP(@RequestParam double xp, @PathVariable String userId) {
    return ApiResponse.success(statisticsService.addXp(userId, xp));
  }

  @PostMapping("/update_completion")
  public ApiResponse<RecipeCompletionResponse> updateAfterCompletion(
      @RequestBody InternalCompletionRequest request) {
    return ApiResponse.success(statisticsService.updateAfterCompletion(request));
  }

  @GetMapping("/me/creator-stats")
  public ApiResponse<CreatorStatsResponse> getMyCreatorStats() {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    return ApiResponse.success(statisticsService.getMyCreatorStats(currentUserId));
  }
}
