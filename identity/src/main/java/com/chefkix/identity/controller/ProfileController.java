package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.EmailVerificationRequest;
import com.chefkix.identity.dto.request.ProfileUpdateRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.ProfileWithPostsResponse;
import com.chefkix.identity.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileController {

  ProfileService profileService;

  @PostMapping("/verify-otp-user")
  public ApiResponse<String> register(
      @RequestBody @Valid EmailVerificationRequest request) {
    return ApiResponse.created(
        profileService.verifyOtpAndCreateUser(request.getEmail(), request.getOtp()));
  }

  /**
   * Get all profiles (legacy - returns full list).
   * Consider using /profiles/paginated for better performance.
   */
  @GetMapping("/profiles")
  public ApiResponse<List<ProfileResponse>> getAllProfiles() {
    // Uses ApiResponse.success() for standard 200 OK
    return ApiResponse.success(profileService.getAllProfiles());
  }

  /**
   * Get profiles with pagination support.
   * GET /api/v1/auth/profiles/paginated?page=0&size=20
   */
  @GetMapping("/profiles/paginated")
  public ApiResponse<Page<ProfileResponse>> getProfilesPaginated(
      @PageableDefault(size = 20) Pageable pageable,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(profileService.getProfilesPaginated(pageable, search));
  }

  @GetMapping("/me")
  public ApiResponse<ProfileResponse> getCurrentProfile(Authentication authentication) {
    // Returns ONLY profile data - no dependency on post-service
    // This prevents cascading failures: if post-service is down, login still works
    return ApiResponse.success(profileService.getCurrentProfile(authentication));
  }

  @GetMapping("/{userId}")
  public ApiResponse<ProfileWithPostsResponse> getUserProfileById(
      @PathVariable("userId") String userId,
      Authentication authentication,
      @PageableDefault(size = 5) Pageable pageable) {

    return ApiResponse.success(
        profileService.getProfileWithPostsByUserId(userId, authentication, pageable));
  }

  @GetMapping("/profile-only/{userId}")
  public ApiResponse<ProfileResponse> getUserProfileById(
      @PathVariable("userId") String userId, Authentication authentication) {

    return ApiResponse.success(profileService.getProfileByUserId(userId, authentication));
  }

  @PutMapping("/update")
  public ApiResponse<ProfileResponse> updateProfile(
      Authentication authentication, @Valid @RequestBody ProfileUpdateRequest req) {

    // You can add a custom message here easily
    return ApiResponse.success(
        profileService.updateProfile(authentication, req), "Profile updated successfully");
  }
}
