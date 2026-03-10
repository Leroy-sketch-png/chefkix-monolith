package com.chefkix.notification.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase configuration for FCM push notifications.
 * 
 * <p>Requires FIREBASE_SERVICE_ACCOUNT_JSON environment variable containing
 * the service account JSON credentials.</p>
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${chefkix.push.enabled:false}")
    private boolean pushEnabled;

    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJson;

    @PostConstruct
    public void initialize() {
        if (!pushEnabled) {
            log.info("Push notifications disabled, skipping Firebase initialization");
            return;
        }

        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_JSON not set, push notifications will be disabled");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized");
            return;
        }

        try (InputStream serviceAccount = new ByteArrayInputStream(
                serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully for push notifications");

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }
}
