package com.chefkix.identity.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.LeaderboardResponse;
import com.chefkix.identity.service.SocialService;
import com.chefkix.identity.service.StatisticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 *
 *
 */
@RestController
@RequestMapping("/auth/leaderboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Validated
public class LeaderboardController {

  StatisticsService statisticsService;
  SocialService socialService;

  /**
   *
   *
   */
  @GetMapping
  public ApiResponse<LeaderboardResponse> getLeaderboard(
      @RequestParam(defaultValue = "global") String type,
      @RequestParam(defaultValue = "weekly") String timeframe,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String currentUserId = (auth != null && !(auth instanceof AnonymousAuthenticationToken))
        ? auth.getName() : null;

    List<String> friendIds = null;
    if ("friends".equals(type)) {
      if (currentUserId == null) {
        return ApiResponse.error(401, "Friends leaderboard requires login");
      }
      friendIds = socialService.getFriendIds(currentUserId);
    }

    LeaderboardResponse response =
        statisticsService.getLeaderboard(type, timeframe, limit, currentUserId, friendIds);

    return ApiResponse.success(response, "Leaderboard retrieved");
  }

  /**
   *
   */
  @GetMapping("/my-rank")
  public ApiResponse<LeaderboardResponse.MyRank> getMyRank(
      @RequestParam(defaultValue = "weekly") String timeframe,
      Authentication authentication) {
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      return ApiResponse.error(401, "Login required to view your rank");
    }
    String currentUserId = authentication.getName();

    LeaderboardResponse response =
        statisticsService.getLeaderboard("global", timeframe, 1, currentUserId, null);

    return ApiResponse.success(response.getMyRank(), "Your rank retrieved");
  }
}
