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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${app.firebase.service-account-json:}")
    private String serviceAccountJson;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            InputStream stream = resolveCredentialStream();
            if (stream == null) {
                log.warn("Firebase credentials not configured — push notifications disabled");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
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

    private InputStream resolveCredentialStream() throws IOException {
        // Priority 1: raw JSON string (for cloud deployments via env var)
        if (StringUtils.hasText(serviceAccountJson)) {
            String json = serviceAccountJson.trim();
            // Support optional base64 encoding
            if (!json.startsWith("{")) {
                json = new String(Base64.getDecoder().decode(json), StandardCharsets.UTF_8);
            }
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
        // Priority 2: file path (for local development)
        if (StringUtils.hasText(serviceAccountPath)) {
            return new FileInputStream(serviceAccountPath);
        }
        return null;
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
