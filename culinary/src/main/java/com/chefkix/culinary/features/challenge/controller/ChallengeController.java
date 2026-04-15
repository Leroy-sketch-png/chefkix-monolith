package com.chefkix.culinary.features.challenge.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeHistoryResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.CommunityChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.SeasonalChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.WeeklyChallengeResponse;
import com.chefkix.culinary.features.challenge.service.ChallengeService;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class ChallengeController {
    private final ChallengeService challengeService;

    @GetMapping("/today")
    public ApiResponse<ChallengeResponse> getTodayChallenge(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        ChallengeResponse response = challengeService.getTodayChallenge(userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/history")
    public ApiResponse<ChallengeHistoryResponse> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") @Max(100) int size
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ChallengeHistoryResponse data = challengeService.getChallengeHistory(userId, page, size);

        return ApiResponse.success(data);
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyChallengeResponse> getWeeklyChallenge(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        WeeklyChallengeResponse response = challengeService.getWeeklyChallenge(userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/community")
    public ApiResponse<List<CommunityChallengeResponse>> getCommunityChallenge(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        List<CommunityChallengeResponse> response = challengeService.getActiveCommunityChallenge(userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/seasonal")
    public ApiResponse<List<SeasonalChallengeResponse>> getSeasonalChallenges(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        List<SeasonalChallengeResponse> response = challengeService.getSeasonalChallenges(userId);
        return ApiResponse.success(response);
    }
}
