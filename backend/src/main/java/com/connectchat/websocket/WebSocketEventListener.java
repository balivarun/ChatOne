package com.connectchat.websocket;

import com.connectchat.security.UserPrincipal;
import com.connectchat.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        UserPrincipal userPrincipal = extractUserPrincipal(headerAccessor);

        if (userPrincipal != null) {
            UUID userId = userPrincipal.getId();
            onlineStatusService.setUserOnline(userId);

            broadcastStatusChange(userId, true);
            log.info("User connected via WebSocket: {} (session: {})",
                    userPrincipal.getEmail(), headerAccessor.getSessionId());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        UserPrincipal userPrincipal = extractUserPrincipal(headerAccessor);

        if (userPrincipal != null) {
            UUID userId = userPrincipal.getId();
            onlineStatusService.setUserOffline(userId);

            broadcastStatusChange(userId, false);
            log.info("User disconnected from WebSocket: {} (session: {})",
                    userPrincipal.getEmail(), headerAccessor.getSessionId());
        }
    }

    private void broadcastStatusChange(UUID userId, boolean isOnline) {
        Map<String, Object> statusEvent = Map.of(
                "type", "USER_STATUS_CHANGE",
                "userId", userId.toString(),
                "isOnline", isOnline
        );

        try {
            messagingTemplate.convertAndSend("/topic/online-status", statusEvent);
        } catch (Exception e) {
            log.warn("Failed to broadcast status change for user {}: {}", userId, e.getMessage());
        }
    }

    private UserPrincipal extractUserPrincipal(StompHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken) {
            if (authToken.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal;
            }
        }
        return null;
    }
}
