package com.chefkix.identity.controller;

import com.chefkix.identity.entity.CreatorTipSettings;
import com.chefkix.identity.entity.Tip;
import com.chefkix.identity.service.TipService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;

    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ── DTOs ──────────────────────────────────────────────────────

    @Data
    public static class UpdateTipSettingsRequest {
        private boolean tipsEnabled;
        private int[] suggestedAmounts;
        private String thankYouMessage;
    }

    @Data
    public static class SendTipRequest {
        @NotBlank private String creatorId;
        private String recipeId;
        @Positive private int amountCents;
        private String message;
    }

    // ── Creator Settings ──────────────────────────────────────────

    @GetMapping("/settings")
    public ApiResponse<CreatorTipSettings> getMySettings() {
        return ApiResponse.<CreatorTipSettings>builder()
                .success(true).statusCode(200)
                .data(tipService.getOrCreateSettings(userId()))
                .build();
    }

    @PutMapping("/settings")
    public ApiResponse<CreatorTipSettings> updateSettings(@Valid @RequestBody UpdateTipSettingsRequest body) {
        return ApiResponse.<CreatorTipSettings>builder()
                .success(true).statusCode(200)
                .data(tipService.updateSettings(userId(), body.isTipsEnabled(), body.getSuggestedAmounts(), body.getThankYouMessage()))
                .build();
    }

    // ── Public: View creator's tip settings ───────────────────────

    @GetMapping("/creator/{creatorId}")
    public ApiResponse<CreatorTipSettings> getCreatorSettings(@PathVariable String creatorId) {
        CreatorTipSettings settings = tipService.getCreatorTipSettings(creatorId);
        if (settings == null || !settings.isTipsEnabled()) {
            return ApiResponse.<CreatorTipSettings>builder()
                    .success(true).statusCode(200).data(null).build();
        }
        return ApiResponse.<CreatorTipSettings>builder()
                .success(true).statusCode(200).data(settings).build();
    }

    // ── Send a Tip ────────────────────────────────────────────────

    @PostMapping("/send")
    public ApiResponse<Tip> sendTip(@Valid @RequestBody SendTipRequest body) {
        return ApiResponse.<Tip>builder()
                .success(true).statusCode(201)
                .data(tipService.sendTip(userId(), body.getCreatorId(), body.getRecipeId(), body.getAmountCents(), body.getMessage()))
                .build();
    }

    // ── Tip History ───────────────────────────────────────────────

    @GetMapping("/received")
    public ApiResponse<List<Tip>> getReceivedTips() {
        return ApiResponse.<List<Tip>>builder()
                .success(true).statusCode(200)
                .data(tipService.getReceivedTips(userId()))
                .build();
    }

    @GetMapping("/sent")
    public ApiResponse<List<Tip>> getSentTips() {
        return ApiResponse.<List<Tip>>builder()
                .success(true).statusCode(200)
                .data(tipService.getSentTips(userId()))
                .build();
    }
}
