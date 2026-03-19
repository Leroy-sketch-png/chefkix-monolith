package com.chefkix.identity.service;

import com.chefkix.identity.entity.UserSettings;
import com.chefkix.identity.repository.UserSettingsRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for managing user settings. Creates default settings on first access (lazy
 * initialization).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingsService {

  UserSettingsRepository settingsRepository;

  // Validation constants for enum-like String fields
  private static final Set<String> VALID_PROFILE_VISIBILITY = Set.of("public", "friends_only", "private");
  private static final Set<String> VALID_MESSAGES_FROM = Set.of("everyone", "friends", "nobody");
  private static final Set<String> VALID_SKILL_LEVELS = Set.of("beginner", "intermediate", "advanced", "expert");
  private static final Set<String> VALID_THEMES = Set.of("light", "dark", "system");
  private static final Set<String> VALID_MEASUREMENT_UNITS = Set.of("metric", "imperial");

  private void validateEnum(String value, Set<String> allowed, String fieldName) {
    if (value != null && !allowed.contains(value)) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  // ================================
  // GET OPERATIONS
  // ================================

  /** Get all settings for current user. Creates default settings if none exist. */
  public UserSettings getSettings() {
    String userId = getCurrentUserId();
    return getOrCreateSettings(userId);
  }

  /** Get settings for a specific user by ID (for cross-service lookups). */
  public UserSettings getSettingsByUserId(String userId) {
    return getOrCreateSettings(userId);
  }

  /** Get privacy settings only. */
  public UserSettings.PrivacySettings getPrivacySettings() {
    return getSettings().getPrivacy();
  }

  /** Get privacy settings for a specific user (for enforcement checks). */
  public UserSettings.PrivacySettings getPrivacySettingsByUserId(String userId) {
    return getOrCreateSettings(userId).getPrivacy();
  }

  /** Get notification settings for a specific user (for notification enforcement). */
  public UserSettings.NotificationSettings getNotificationSettingsByUserId(String userId) {
    return getOrCreateSettings(userId).getNotifications();
  }

  /** Get notification settings only. */
  public UserSettings.NotificationSettings getNotificationSettings() {
    return getSettings().getNotifications();
  }

  /** Get cooking preferences only. */
  public UserSettings.CookingPreferences getCookingPreferences() {
    return getSettings().getCooking();
  }

  /** Get app preferences only. */
  public UserSettings.AppPreferences getAppPreferences() {
    return getSettings().getApp();
  }

  // ================================
  // UPDATE OPERATIONS
  // ================================

  /** Update privacy settings. */
  public UserSettings.PrivacySettings updatePrivacySettings(UserSettings.PrivacySettings privacy) {
    String userId = getCurrentUserId();
    UserSettings settings = getOrCreateSettings(userId);

    // Validate enum-like fields
    validateEnum(privacy.getProfileVisibility(), VALID_PROFILE_VISIBILITY, "profileVisibility");
    validateEnum(privacy.getAllowMessagesFrom(), VALID_MESSAGES_FROM, "allowMessagesFrom");

    // Update only non-null fields
    if (privacy.getProfileVisibility() != null) {
      settings.getPrivacy().setProfileVisibility(privacy.getProfileVisibility());
    }
    if (privacy.getShowCookingActivity() != null) {
      settings.getPrivacy().setShowCookingActivity(privacy.getShowCookingActivity());
    }
    if (privacy.getShowOnLeaderboard() != null) {
      settings.getPrivacy().setShowOnLeaderboard(privacy.getShowOnLeaderboard());
    }
    if (privacy.getAllowFollowers() != null) {
      settings.getPrivacy().setAllowFollowers(privacy.getAllowFollowers());
    }
    if (privacy.getAllowMessagesFrom() != null) {
      settings.getPrivacy().setAllowMessagesFrom(privacy.getAllowMessagesFrom());
    }

    settingsRepository.save(settings);
    log.info("Updated privacy settings for user {}", userId);
    return settings.getPrivacy();
  }

  /** Update notification settings. */
  public UserSettings.NotificationSettings updateNotificationSettings(
      UserSettings.NotificationSettings notifications) {
    String userId = getCurrentUserId();
    UserSettings settings = getOrCreateSettings(userId);

    // Update email settings
    if (notifications.getEmail() != null) {
      UserSettings.EmailNotificationSettings email = notifications.getEmail();
      UserSettings.EmailNotificationSettings current = settings.getNotifications().getEmail();

      if (email.getWeeklyDigest() != null) current.setWeeklyDigest(email.getWeeklyDigest());
      if (email.getNewFollower() != null) current.setNewFollower(email.getNewFollower());
      if (email.getRecipeMilestone() != null)
        current.setRecipeMilestone(email.getRecipeMilestone());
    }

    // Update in-app settings
    if (notifications.getInApp() != null) {
      UserSettings.InAppNotificationSettings inApp = notifications.getInApp();
      UserSettings.InAppNotificationSettings current = settings.getNotifications().getInApp();

      if (inApp.getXpAndLevelUps() != null) current.setXpAndLevelUps(inApp.getXpAndLevelUps());
      if (inApp.getBadges() != null) current.setBadges(inApp.getBadges());
      if (inApp.getSocial() != null) current.setSocial(inApp.getSocial());
      if (inApp.getFollowers() != null) current.setFollowers(inApp.getFollowers());
      if (inApp.getPostDeadline() != null) current.setPostDeadline(inApp.getPostDeadline());
      if (inApp.getStreakWarning() != null) current.setStreakWarning(inApp.getStreakWarning());
      if (inApp.getDailyChallenge() != null) current.setDailyChallenge(inApp.getDailyChallenge());
    }

    // Update push settings
    if (notifications.getPush() != null) {
      UserSettings.PushNotificationSettings push = notifications.getPush();
      UserSettings.PushNotificationSettings current = settings.getNotifications().getPush();

      if (push.getEnabled() != null) current.setEnabled(push.getEnabled());
      if (push.getTimerAlerts() != null) current.setTimerAlerts(push.getTimerAlerts());
    }

    settingsRepository.save(settings);
    log.info("Updated notification settings for user {}", userId);
    return settings.getNotifications();
  }

  /** Update cooking preferences. */
  public UserSettings.CookingPreferences updateCookingPreferences(
      UserSettings.CookingPreferences cooking) {
    String userId = getCurrentUserId();
    UserSettings settings = getOrCreateSettings(userId);

    // Validate enum-like fields
    validateEnum(cooking.getSkillLevel(), VALID_SKILL_LEVELS, "skillLevel");
    validateEnum(cooking.getMeasurementUnits(), VALID_MEASUREMENT_UNITS, "measurementUnits");

    if (cooking.getSkillLevel() != null) {
      settings.getCooking().setSkillLevel(cooking.getSkillLevel());
    }
    if (cooking.getDietaryRestrictions() != null) {
      settings.getCooking().setDietaryRestrictions(cooking.getDietaryRestrictions());
    }
    if (cooking.getAllergies() != null) {
      settings.getCooking().setAllergies(cooking.getAllergies());
    }
    if (cooking.getDislikedIngredients() != null) {
      settings.getCooking().setDislikedIngredients(cooking.getDislikedIngredients());
    }
    if (cooking.getPreferredCuisines() != null) {
      settings.getCooking().setPreferredCuisines(cooking.getPreferredCuisines());
    }
    if (cooking.getMaxCookingTimeMinutes() != null) {
      settings.getCooking().setMaxCookingTimeMinutes(cooking.getMaxCookingTimeMinutes());
    }
    if (cooking.getDefaultServings() != null) {
      settings.getCooking().setDefaultServings(cooking.getDefaultServings());
    }
    if (cooking.getMeasurementUnits() != null) {
      settings.getCooking().setMeasurementUnits(cooking.getMeasurementUnits());
    }

    settingsRepository.save(settings);
    log.info("Updated cooking preferences for user {}", userId);
    return settings.getCooking();
  }

  /** Update app preferences. */
  public UserSettings.AppPreferences updateAppPreferences(UserSettings.AppPreferences app) {
    String userId = getCurrentUserId();
    UserSettings settings = getOrCreateSettings(userId);

    // Validate enum-like fields
    validateEnum(app.getTheme(), VALID_THEMES, "theme");

    if (app.getTheme() != null) {
      settings.getApp().setTheme(app.getTheme());
    }
    if (app.getLanguage() != null) {
      settings.getApp().setLanguage(app.getLanguage());
    }
    if (app.getAutoPlayVideos() != null) {
      settings.getApp().setAutoPlayVideos(app.getAutoPlayVideos());
    }
    if (app.getReducedMotion() != null) {
      settings.getApp().setReducedMotion(app.getReducedMotion());
    }
    if (app.getSoundEffects() != null) {
      settings.getApp().setSoundEffects(app.getSoundEffects());
    }
    if (app.getKeepScreenOn() != null) {
      settings.getApp().setKeepScreenOn(app.getKeepScreenOn());
    }

    settingsRepository.save(settings);
    log.info("Updated app preferences for user {}", userId);
    return settings.getApp();
  }

  // ================================
  // HELPER METHODS
  // ================================

  /** Get or create default settings for a user. */
  private UserSettings getOrCreateSettings(String userId) {
    return settingsRepository
        .findByUserId(userId)
        .orElseGet(
            () -> {
              log.info("Creating default settings for user {}", userId);
              UserSettings newSettings = UserSettings.builder().userId(userId).build();
              return settingsRepository.save(newSettings);
            });
  }

  private String getCurrentUserId() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
