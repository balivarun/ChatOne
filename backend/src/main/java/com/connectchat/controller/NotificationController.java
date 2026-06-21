package com.connectchat.controller;

import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.NotificationResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> notifications =
                notificationService.getNotifications(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        notificationService.markRead(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }
}
