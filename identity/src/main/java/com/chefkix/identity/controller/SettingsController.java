package com.chefkix.identity.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.entity.UserSettings;
import com.chefkix.identity.service.SettingsService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 */
@RestController
@RequestMapping("/auth/settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingsController {

  SettingsService settingsService;


  @GetMapping
  public ResponseEntity<ApiResponse<UserSettings>> getAllSettings() {
    UserSettings settings = settingsService.getSettings();
    return ResponseEntity.ok(ApiResponse.success(settings));
  }


  @GetMapping("/privacy")
  public ResponseEntity<ApiResponse<UserSettings.PrivacySettings>> getPrivacySettings() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getPrivacySettings()));
  }

  @PutMapping("/privacy")
  public ResponseEntity<ApiResponse<UserSettings.PrivacySettings>> updatePrivacySettings(
      @Valid @RequestBody UserSettings.PrivacySettings privacy) {
    return ResponseEntity.ok(ApiResponse.success(settingsService.updatePrivacySettings(privacy)));
  }


  @GetMapping("/notifications")
  public ResponseEntity<ApiResponse<UserSettings.NotificationSettings>> getNotificationSettings() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getNotificationSettings()));
  }

  @PutMapping("/notifications")
  public ResponseEntity<ApiResponse<UserSettings.NotificationSettings>> updateNotificationSettings(
      @Valid @RequestBody UserSettings.NotificationSettings notifications) {
    return ResponseEntity.ok(
        ApiResponse.success(settingsService.updateNotificationSettings(notifications)));
  }


  @GetMapping("/cooking")
  public ResponseEntity<ApiResponse<UserSettings.CookingPreferences>> getCookingPreferences() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getCookingPreferences()));
  }

  @PutMapping("/cooking")
  public ResponseEntity<ApiResponse<UserSettings.CookingPreferences>> updateCookingPreferences(
      @Valid @RequestBody UserSettings.CookingPreferences cooking) {
    return ResponseEntity.ok(
        ApiResponse.success(settingsService.updateCookingPreferences(cooking)));
  }


  @GetMapping("/app")
  public ResponseEntity<ApiResponse<UserSettings.AppPreferences>> getAppPreferences() {
    return ResponseEntity.ok(ApiResponse.success(settingsService.getAppPreferences()));
  }

  @PutMapping("/app")
  public ResponseEntity<ApiResponse<UserSettings.AppPreferences>> updateAppPreferences(
      @Valid @RequestBody UserSettings.AppPreferences app) {
    return ResponseEntity.ok(ApiResponse.success(settingsService.updateAppPreferences(app)));
  }
}
