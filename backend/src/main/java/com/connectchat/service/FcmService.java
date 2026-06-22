package com.connectchat.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(serviceAccountPath)) {
            log.warn("Firebase service account path not configured — push notifications disabled");
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
            log.info("Firebase Admin SDK initialized");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }

    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (!initialized || !StringUtils.hasText(fcmToken)) {
            return;
        }
        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null) {
                builder.putAllData(data);
            }
            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("FCM push sent: {}", response);
        } catch (Exception e) {
            log.warn("FCM push failed for token {}: {}", fcmToken.substring(0, Math.min(10, fcmToken.length())), e.getMessage());
        }
    }
}
