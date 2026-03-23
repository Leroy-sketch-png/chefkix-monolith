package com.chefkix.culinary.features.duel.controller;

import com.chefkix.culinary.features.duel.dto.request.CreateDuelRequest;
import com.chefkix.culinary.features.duel.dto.response.DuelResponse;
import com.chefkix.culinary.features.duel.service.DuelService;
import com.chefkix.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/duels")
@RequiredArgsConstructor
public class DuelController {

    private final DuelService duelService;

    private String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ApiResponse<DuelResponse> createDuel(@RequestBody CreateDuelRequest request) {
        return ApiResponse.success(duelService.createDuel(getUserId(), request));
    }

    @PostMapping("/{duelId}/accept")
    public ApiResponse<DuelResponse> acceptDuel(@PathVariable String duelId) {
        return ApiResponse.success(duelService.acceptDuel(getUserId(), duelId));
    }

    @PostMapping("/{duelId}/decline")
    public ApiResponse<DuelResponse> declineDuel(@PathVariable String duelId) {
        return ApiResponse.success(duelService.declineDuel(getUserId(), duelId));
    }

    @PostMapping("/{duelId}/cancel")
    public ApiResponse<DuelResponse> cancelDuel(@PathVariable String duelId) {
        return ApiResponse.success(duelService.cancelDuel(getUserId(), duelId));
    }

    @GetMapping("/{duelId}")
    public ApiResponse<DuelResponse> getDuel(@PathVariable String duelId) {
        return ApiResponse.success(duelService.getDuel(getUserId(), duelId));
    }

    @GetMapping("/my")
    public ApiResponse<List<DuelResponse>> getMyDuels() {
        return ApiResponse.success(duelService.getMyDuels(getUserId()));
    }

    @GetMapping("/active")
    public ApiResponse<List<DuelResponse>> getMyActiveDuels() {
        return ApiResponse.success(duelService.getMyActiveDuels(getUserId()));
    }

    @GetMapping("/invites")
    public ApiResponse<List<DuelResponse>> getPendingInvites() {
        return ApiResponse.success(duelService.getPendingInvites(getUserId()));
    }
}
