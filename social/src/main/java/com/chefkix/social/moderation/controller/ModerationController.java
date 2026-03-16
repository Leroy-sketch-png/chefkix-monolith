package com.chefkix.social.moderation.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.moderation.dto.AppealRequest;
import com.chefkix.social.moderation.dto.BanResponse;
import com.chefkix.social.moderation.entity.Appeal;
import com.chefkix.social.moderation.service.ModerationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User-facing moderation endpoints.
 * Banned users can check ban status and submit appeals.
 *
 * Monolith path: /api/v1/moderation/*
 */
@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModerationController {

    ModerationService moderationService;

    /**
     * GET /api/v1/moderation/ban-status — Check if current user is banned.
     * Returns ban details or null if not banned.
     */
    @GetMapping("/ban-status")
    public ResponseEntity<ApiResponse<BanResponse>> getBanStatus(Authentication authentication) {
        String userId = authentication.getName();
        BanResponse ban = moderationService.getActiveBan(userId);
        return ResponseEntity.ok(ApiResponse.ok(ban));
    }

    /**
     * POST /api/v1/moderation/appeals — Submit an appeal against a ban.
     */
    @PostMapping("/appeals")
    public ResponseEntity<ApiResponse<Appeal>> createAppeal(
            Authentication authentication,
            @Valid @RequestBody AppealRequest request) {
        String userId = authentication.getName();
        Appeal appeal = moderationService.createAppeal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(appeal));
    }
}
