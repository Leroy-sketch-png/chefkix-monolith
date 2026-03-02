package com.chefkix.identity.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.entity.UserSettings;
import com.chefkix.identity.service.SettingsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user settings management.
 *
 * <p>All endpoints require authentication. Settings are lazily initialized with sensible defaults
 * on first access.
 */
@RestController
@RequestMapping("/auth/settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingsController {

  SettingsService settingsService;

  // ================================
  // GET ALL SETTINGS
  // ================================

  /** Get all settings for the current user. */
  @GetMapping
  public ResponseEntity<ApiResponse<UserSettings>> getAllSettings() {
    UserSettings settings = settingsService.getSettings();
    return ResponseEntity.ok(ApiResponse.success(settings));
  }

  // ================================
  // PRIVACY SETTINGS
  // ================================

  /** Get privacy settings. */
  @GetMapping("/privacy")
  public ResponseEntity<ApiResponse<UserSettings.PrivacySettings>> getPrivacySettings() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getPrivacySettings()));
  }

  /** Update privacy settings. Only non-null fields in the request body will be updated. */
  @PutMapping("/privacy")
  public ResponseEntity<ApiResponse<UserSettings.PrivacySettings>> updatePrivacySettings(
      @RequestBody UserSettings.PrivacySettings privacy) {
    return ResponseEntity.ok(ApiResponse.success(settingsService.updatePrivacySettings(privacy)));
  }

  // ================================
  // NOTIFICATION SETTINGS
  // ================================

  /** Get notification settings. */
  @GetMapping("/notifications")
  public ResponseEntity<ApiResponse<UserSettings.NotificationSettings>> getNotificationSettings() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getNotificationSettings()));
  }

  /** Update notification settings. Only non-null fields in the request body will be updated. */
  @PutMapping("/notifications")
  public ResponseEntity<ApiResponse<UserSettings.NotificationSettings>> updateNotificationSettings(
      @RequestBody UserSettings.NotificationSettings notifications) {
    return ResponseEntity.ok(
        ApiResponse.success(settingsService.updateNotificationSettings(notifications)));
  }

  // ================================
  // COOKING PREFERENCES
  // ================================

  /** Get cooking preferences. */
  @GetMapping("/cooking")
  public ResponseEntity<ApiResponse<UserSettings.CookingPreferences>> getCookingPreferences() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getCookingPreferences()));
  }

  /** Update cooking preferences. Only non-null fields in the request body will be updated. */
  @PutMapping("/cooking")
  public ResponseEntity<ApiResponse<UserSettings.CookingPreferences>> updateCookingPreferences(
      @RequestBody UserSettings.CookingPreferences cooking) {
    return ResponseEntity.ok(
        ApiResponse.success(settingsService.updateCookingPreferences(cooking)));
  }

  // ================================
  // APP PREFERENCES
  // ================================

  /** Get app preferences. */
  @GetMapping("/app")
  public ResponseEntity<ApiResponse<UserSettings.AppPreferences>> getAppPreferences() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getAppPreferences()));
  }

  /** Update app preferences. Only non-null fields in the request body will be updated. */
  @PutMapping("/app")
  public ResponseEntity<ApiResponse<UserSettings.AppPreferences>> updateAppPreferences(
      @RequestBody UserSettings.AppPreferences app) {
    return ResponseEntity.ok(ApiResponse.success(settingsService.updateAppPreferences(app)));
  }
}
