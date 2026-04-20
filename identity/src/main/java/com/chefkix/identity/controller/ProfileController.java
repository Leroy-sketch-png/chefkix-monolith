package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.AuthenticationRequest;
import com.chefkix.identity.dto.request.EmailVerificationRequest;
import com.chefkix.identity.dto.request.ProfileUpdateRequest;
import com.chefkix.identity.dto.response.AuthenticationResponse;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.ProfileWithPostsResponse;
import com.chefkix.identity.service.AuthenticationService;
import com.chefkix.identity.service.ProfileService;
import com.chefkix.identity.utils.HttpOnlyCookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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

  private static final int REFRESH_TOKEN_MAX_AGE_LOGIN = 7 * 24 * 60 * 60;

  ProfileService profileService;
  AuthenticationService authenticationService;

  @PostMapping("/verify-otp-user")
  public ApiResponse<AuthenticationResponse> register(
      @RequestBody @Valid EmailVerificationRequest request,
      HttpServletResponse response) {
    String plainPassword =
        profileService.verifyOtpAndCreateUser(request.getEmail(), request.getOtp());

    AuthenticationResponse authResponse =
        authenticationService.authenticate(
            AuthenticationRequest.builder()
                .emailOrUsername(request.getEmail())
                .password(plainPassword)
                .build());

    HttpOnlyCookieUtils.addHttpOnlyCookie(
        response, "refresh_token", authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE_LOGIN);
    authResponse.setRefreshToken(null);

    return ApiResponse.created(authResponse);
  }

  /**
   * Get all profiles (legacy - limited to 100, use /profiles/paginated instead).
   * @deprecated Use /profiles/paginated for proper pagination.
   */
  @Deprecated
  @GetMapping("/profiles")
  public ApiResponse<List<ProfileResponse>> getAllProfiles() {
    return ApiResponse.success(profileService.getAllProfilesLimited());
  }

  /**
   * Get profiles with pagination support.
   * GET /api/v1/auth/profiles/paginated?page=0&size=20
   */
  @GetMapping("/profiles/paginated")
  public ApiResponse<List<ProfileResponse>> getProfilesPaginated(
      @PageableDefault(size = 20) Pageable pageable,
      @RequestParam(required = false) String search) {
    return ApiResponse.successPage(profileService.getProfilesPaginated(pageable, search));
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

  @DeleteMapping("/delete-account")
  public ApiResponse<Void> deleteAccount(Authentication authentication) {
    profileService.deleteAccount(authentication);
    return ApiResponse.<Void>builder()
        .success(true)
        .statusCode(200)
        .message("Account deleted successfully")
        .build();
  }

  @GetMapping("/export-data")
  public ApiResponse<Map<String, Object>> exportUserData(Authentication authentication) {
    return ApiResponse.success(profileService.exportUserData(authentication));
  }
}
