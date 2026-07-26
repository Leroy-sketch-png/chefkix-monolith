package com.chefkix.identity.service;

import com.chefkix.identity.api.NotificationPreferencesProvider;
import com.chefkix.identity.entity.UserSettings;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationPreferencesProviderImpl implements NotificationPreferencesProvider {

    SettingsService settingsService;

    @Override
    public boolean isNotificationEnabled(String userId, String category) {
        try {
            UserSettings.InAppNotificationSettings inApp =
                    settingsService.getNotificationSettingsByUserId(userId).getInApp();
            if (inApp == null) {
return true;
            }

            return switch (category) {
                case "social" -> !Boolean.FALSE.equals(inApp.getSocial());
                case "followers" -> !Boolean.FALSE.equals(inApp.getFollowers());
                case "xpAndLevelUps" -> !Boolean.FALSE.equals(inApp.getXpAndLevelUps());
                case "badges" -> !Boolean.FALSE.equals(inApp.getBadges());
                case "postDeadline" -> !Boolean.FALSE.equals(inApp.getPostDeadline());
                case "streakWarning" -> !Boolean.FALSE.equals(inApp.getStreakWarning());
                case "dailyChallenge" -> !Boolean.FALSE.equals(inApp.getDailyChallenge());
default -> true;
            };
        } catch (Exception e) {
            log.warn("Failed to check notification preference for user {} category {}: {}",
                    userId, category, e.getMessage());
return true;
        }
    }
}
