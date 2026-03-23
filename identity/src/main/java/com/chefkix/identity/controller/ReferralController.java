package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.RedeemReferralRequest;
import com.chefkix.identity.dto.response.ReferralCodeResponse;
import com.chefkix.identity.dto.response.ReferralStatsResponse;
import com.chefkix.identity.service.ReferralService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/referrals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReferralController {

    ReferralService referralService;

    @GetMapping("/my-code")
    public ApiResponse<ReferralCodeResponse> getMyCode() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(referralService.getOrCreateMyCode(userId));
    }

    @PostMapping("/redeem")
    public ApiResponse<ReferralCodeResponse> redeemCode(
            @Valid @RequestBody RedeemReferralRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(referralService.redeemCode(userId, request.getCode()));
    }

    @GetMapping("/stats")
    public ApiResponse<ReferralStatsResponse> getMyStats() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(referralService.getMyStats(userId));
    }
}
