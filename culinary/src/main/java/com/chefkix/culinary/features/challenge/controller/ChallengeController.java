package com.chefkix.culinary.features.challenge.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeHistoryResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.CommunityChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.SeasonalChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.WeeklyChallengeResponse;
import com.chefkix.culinary.features.challenge.service.ChallengeService;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<ChallengeResponse> getTodayChallenge() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ChallengeResponse response = challengeService.getTodayChallenge(userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/history")
    public ApiResponse<ChallengeHistoryResponse> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ChallengeHistoryResponse data = challengeService.getChallengeHistory(userId, page, size);

        return ApiResponse.success(data);
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyChallengeResponse> getWeeklyChallenge() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        WeeklyChallengeResponse response = challengeService.getWeeklyChallenge(userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/community")
    public ApiResponse<List<CommunityChallengeResponse>> getCommunityChallenge() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<CommunityChallengeResponse> response = challengeService.getActiveCommunityChallenge(userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/seasonal")
    public ApiResponse<List<SeasonalChallengeResponse>> getSeasonalChallenges() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<SeasonalChallengeResponse> response = challengeService.getSeasonalChallenges(userId);
        return ApiResponse.success(response);
    }
}
