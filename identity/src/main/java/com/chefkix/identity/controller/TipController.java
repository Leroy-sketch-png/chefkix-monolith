package com.chefkix.identity.controller;

import com.chefkix.identity.entity.CreatorTipSettings;
import com.chefkix.identity.entity.Tip;
import com.chefkix.identity.service.TipService;
import com.chefkix.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;

    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
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
    public ApiResponse<CreatorTipSettings> updateSettings(@RequestBody Map<String, Object> body) {
        boolean tipsEnabled = (boolean) body.getOrDefault("tipsEnabled", false);
        int[] suggestedAmounts = null;
        if (body.containsKey("suggestedAmounts")) {
            @SuppressWarnings("unchecked")
            List<Integer> amounts = (List<Integer>) body.get("suggestedAmounts");
            suggestedAmounts = amounts.stream().mapToInt(Integer::intValue).toArray();
        }
        String thankYouMessage = (String) body.get("thankYouMessage");

        return ApiResponse.<CreatorTipSettings>builder()
                .success(true).statusCode(200)
                .data(tipService.updateSettings(userId(), tipsEnabled, suggestedAmounts, thankYouMessage))
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
    public ApiResponse<Tip> sendTip(@RequestBody Map<String, Object> body) {
        String creatorId = (String) body.get("creatorId");
        String recipeId = (String) body.get("recipeId");
        int amountCents = ((Number) body.get("amountCents")).intValue();
        String message = (String) body.get("message");

        return ApiResponse.<Tip>builder()
                .success(true).statusCode(201)
                .data(tipService.sendTip(userId(), creatorId, recipeId, amountCents, message))
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
