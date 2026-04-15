package com.chefkix.identity.service;

import com.chefkix.identity.entity.UserActivity;
import com.chefkix.identity.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatusService {

    private final UserActivityRepository userActivityRepository;

    public void setUserOnline(String keycloakId) {
        var userActivity = userActivityRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    // If no record exists, create a new one (Optional, depends on system logic)
                    return UserActivity.builder().keycloakId(keycloakId).build();
                });

        userActivity.setIsOnline(true);
        userActivityRepository.save(userActivity);
        log.info("User {} is now ONLINE", keycloakId);
    }

    public void setUserOffline(String keycloakId) {
        var userActivity = userActivityRepository.findByKeycloakId(keycloakId).orElse(null);

        if (userActivity != null) {
            userActivity.setIsOnline(false);
            userActivity.setLastActive(LocalDateTime.now()); // Save the offline timestamp
            userActivityRepository.save(userActivity);
            log.info("User {} is now OFFLINE at {}", keycloakId, userActivity.getLastActive());
        }
    }
}