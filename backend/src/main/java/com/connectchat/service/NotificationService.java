package com.connectchat.service;

import com.connectchat.dto.response.NotificationResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.dto.response.UserResponse;
import com.connectchat.entity.Notification;
import com.connectchat.entity.NotificationType;
import com.connectchat.entity.User;
import com.connectchat.exception.AccessDeniedException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.NotificationRepository;
import com.connectchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final FcmService fcmService;

    @Transactional
    public void createAndSend(UUID recipientId,
                               UUID senderId,
                               NotificationType type,
                               String title,
                               String body,
                               UUID referenceId) {
        if (recipientId.equals(senderId)) {
            return;
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));

        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .type(type)
                .title(title)
                .body(body)
                .isRead(false)
                .referenceId(referenceId)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = mapToNotificationResponse(saved);

        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/notifications",
                    response);
        } catch (Exception e) {
            log.warn("Failed to send real-time notification to user {}: {}", recipientId, e.getMessage());
        }

        if (recipient.getFcmToken() != null) {
            fcmService.sendNotification(
                    recipient.getFcmToken(),
                    notification.getTitle(),
                    notification.getBody(),
                    Map.of(
                            "type", type.name(),
                            "referenceId", referenceId != null ? referenceId.toString() : ""
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        Page<NotificationResponse> responsePage = page.map(this::mapToNotificationResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notifId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to mark this notification as read");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        UserResponse senderResponse = null;
        if (notification.getSender() != null) {
            senderResponse = userService.mapToUserResponse(notification.getSender());
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .sender(senderResponse)
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .isRead(notification.getIsRead())
                .referenceId(notification.getReferenceId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
