package com.connectchat.websocket;

import com.connectchat.dto.request.MarkReadRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.MessageResponse;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.message")
    public void sendMessage(@Payload SendMessageRequest req, Principal principal) {
        try {
            UUID senderId = extractUserId(principal);
            MessageResponse response = messageService.sendMessage(senderId, req);
            log.debug("WebSocket message sent by user {} to conversation {}", senderId, req.getConversationId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingPayload payload, Principal principal) {
        try {
            UUID senderId = extractUserId(principal);
            Map<String, Object> typingEvent = Map.of(
                    "userId", senderId.toString(),
                    "conversationId", payload.getConversationId().toString(),
                    "isTyping", payload.isTyping()
            );

            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + payload.getConversationId() + "/typing",
                    typingEvent);

            log.debug("Typing event from user {} in conversation {}: {}",
                    senderId, payload.getConversationId(), payload.isTyping());
        } catch (Exception e) {
            log.error("Failed to broadcast typing event: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload MarkReadRequest req, Principal principal) {
        try {
            UUID userId = extractUserId(principal);
            messageService.markMessagesRead(userId, req.getMessageIds());
            log.debug("Messages marked as read by user {}", userId);
        } catch (Exception e) {
            log.error("Failed to mark messages as read via WebSocket: {}", e.getMessage(), e);
        }
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getId();
            }
        }
        throw new IllegalStateException("Cannot extract user ID from principal: " + principal);
    }
}
