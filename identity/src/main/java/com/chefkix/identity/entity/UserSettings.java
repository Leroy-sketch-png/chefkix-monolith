package com.chefkix.identity.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "user_settings")
public class UserSettings {

  @Id String id;

  @Indexed(unique = true)
  String userId;

  @Builder.Default PrivacySettings privacy = new PrivacySettings();

  @Builder.Default NotificationSettings notifications = new NotificationSettings();

  @Builder.Default CookingPreferences cooking = new CookingPreferences();

  @Builder.Default AppPreferences app = new AppPreferences();


  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PrivacySettings {
    @Builder.Default String profileVisibility = "public";

    @Builder.Default Boolean showCookingActivity = true;

    @Builder.Default Boolean showOnLeaderboard = true;

    @Builder.Default Boolean allowFollowers = true;

    @Builder.Default String allowMessagesFrom = "friends";
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class NotificationSettings {
    @Builder.Default EmailNotificationSettings email = new EmailNotificationSettings();

    @Builder.Default InAppNotificationSettings inApp = new InAppNotificationSettings();

    @Builder.Default PushNotificationSettings push = new PushNotificationSettings();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class EmailNotificationSettings {
    @Builder.Default Boolean weeklyDigest = true;

    @Builder.Default Boolean newFollower = false;

    @Builder.Default Boolean recipeMilestone = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class InAppNotificationSettings {
    @Builder.Default Boolean xpAndLevelUps = true;

    @Builder.Default Boolean badges = true;

    @Builder.Default Boolean social = true;

    @Builder.Default Boolean followers = true;

    @Builder.Default Boolean postDeadline = true;

    @Builder.Default Boolean streakWarning = true;

    @Builder.Default Boolean dailyChallenge = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PushNotificationSettings {
    @Builder.Default Boolean enabled = false;

    @Builder.Default Boolean timerAlerts = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CookingPreferences {
    @Builder.Default String skillLevel = "beginner";

    @Builder.Default List<String> dietaryRestrictions = new ArrayList<>();

    @Builder.Default List<String> allergies = new ArrayList<>();

    @Builder.Default List<String> dislikedIngredients = new ArrayList<>();

    @Builder.Default List<String> preferredCuisines = new ArrayList<>();

    Integer maxCookingTimeMinutes;

    @Builder.Default Integer defaultServings = 2;

    @Builder.Default String measurementUnits = "metric";
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AppPreferences {
    @Builder.Default String theme = "system";

    @Builder.Default String language = "en";

    @Builder.Default Boolean autoPlayVideos = true;

    @Builder.Default Boolean reducedMotion = false;

    @Builder.Default Boolean soundEffects = true;

    @Builder.Default KitchenAudioPreferences kitchenAudio = new KitchenAudioPreferences();

    @Builder.Default Boolean keepScreenOn = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class KitchenAudioPreferences {
    @Builder.Default Boolean spokenGuidanceEnabled = false;

    @Builder.Default Boolean timerVoiceEnabled = true;

    @Builder.Default Boolean timerChimesEnabled = true;

    @Builder.Default Boolean soundEffectsEnabled = true;
  }
}
