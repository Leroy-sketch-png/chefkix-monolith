package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.ActivateSubscriptionRequest;
import com.chefkix.identity.dto.response.SubscriptionResponse;
import com.chefkix.identity.service.SubscriptionService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {

    SubscriptionService subscriptionService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMySubscription() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getMySubscription()));
    }

    @GetMapping("/premium-status")
    public ResponseEntity<ApiResponse<Boolean>> isPremium() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.isPremium()));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> activate(
            @Valid @RequestBody ActivateSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.activateSubscription(request.getPaymentProvider(), request.getPaymentToken())));
    }

    @PostMapping("/trial")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> startTrial() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.startTrial()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.cancelSubscription()));
    }
}
