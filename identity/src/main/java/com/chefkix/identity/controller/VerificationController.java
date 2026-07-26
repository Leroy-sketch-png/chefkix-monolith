package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.VerificationApplyRequest;
import com.chefkix.identity.dto.response.VerificationResponse;
import com.chefkix.identity.service.VerificationService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
public class VerificationController {

  private final VerificationService verificationService;

  @PostMapping("/apply")
  public ApiResponse<VerificationResponse> apply(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) @Valid VerificationApplyRequest request) {
    String userId = jwt.getSubject();
    VerificationResponse response = verificationService.applyForVerification(userId, request);
    return ApiResponse.<VerificationResponse>builder()
        .success(true)
        .statusCode(200)
        .data(response)
        .build();
  }

  @GetMapping("/status")
  public ApiResponse<VerificationResponse> getStatus(@AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    VerificationResponse response = verificationService.getVerificationStatus(userId);
    return ApiResponse.<VerificationResponse>builder()
        .success(true)
        .statusCode(200)
        .data(response)
        .build();
  }

  @PostMapping("/{requestId}/approve")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ApiResponse<VerificationResponse> approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String requestId,
      @Valid @RequestBody(required = false) AdminReviewRequest request) {
    String adminUserId = jwt.getSubject();
    String notes = request != null ? request.notes() : null;
    VerificationResponse response = verificationService.approveVerification(requestId, adminUserId, notes);
    return ApiResponse.<VerificationResponse>builder()
        .success(true)
        .statusCode(200)
        .data(response)
        .build();
  }

  @PostMapping("/{requestId}/reject")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ApiResponse<VerificationResponse> reject(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String requestId,
      @Valid @RequestBody(required = false) AdminReviewRequest request) {
    String adminUserId = jwt.getSubject();
    String notes = request != null ? request.notes() : null;
    VerificationResponse response = verificationService.rejectVerification(requestId, adminUserId, notes);
    return ApiResponse.<VerificationResponse>builder()
        .success(true)
        .statusCode(200)
        .data(response)
        .build();
  }

  record AdminReviewRequest(String notes) {}
}
