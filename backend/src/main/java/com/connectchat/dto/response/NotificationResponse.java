package com.connectchat.dto.response;

import com.connectchat.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UserResponse sender;
    private NotificationType type;
    private String title;
    private String body;
    private Boolean isRead;
    private UUID referenceId;
    private Instant createdAt;
}
