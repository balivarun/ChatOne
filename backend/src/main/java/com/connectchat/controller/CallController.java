package com.connectchat.controller;

import com.connectchat.security.CustomUserDetailsService;
import com.connectchat.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CallController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CustomUserDetailsService userDetailsService;

    // @AuthenticationPrincipal does not resolve the full UserPrincipal in plain STOMP
    // context without @EnableWebSocketSecurity. Use Principal directly — getName()
    // returns the email set by the ChannelInterceptor via accessor.setUser().
    @MessageMapping("/call.offer")
    public void offer(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            log.warn("CALL_OFFER rejected — no principal (unauthenticated)");
            return;
        }
        String targetEmail = (String) payload.get("targetEmail");
        String callerEmail = principal.getName();
        log.info("CALL_OFFER from={} to={} type={}", callerEmail, targetEmail, payload.get("callType"));
        if (targetEmail == null || targetEmail.isBlank()) {
            log.warn("CALL_OFFER rejected — targetEmail is blank");
            return;
        }

        String callerName = "";
        String callerAvatar = "";
        try {
            UserPrincipal up = (UserPrincipal) userDetailsService.loadUserByUsername(callerEmail);
            callerName = up.getDisplayName() != null ? up.getDisplayName() : "";
            callerAvatar = up.getAvatarUrl() != null ? up.getAvatarUrl() : "";
        } catch (Exception e) {
            log.warn("Could not load caller details for {}: {}", callerEmail, e.getMessage());
        }

        // Use HashMap so null values don't throw (Map.of forbids nulls)
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "CALL_OFFER");
        msg.put("callerEmail", callerEmail);
        msg.put("callerName", callerName);
        msg.put("callerAvatar", callerAvatar);
        msg.put("conversationId", payload.getOrDefault("conversationId", ""));
        msg.put("sdp", payload.getOrDefault("sdp", ""));
        msg.put("callType", payload.getOrDefault("callType", "video"));
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call", msg);
    }

    @MessageMapping("/call.answer")
    public void answer(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_ANSWER",
                       "sdp", payload.getOrDefault("sdp", "")));
    }

    @MessageMapping("/call.ice")
    public void ice(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "ICE_CANDIDATE",
                       "candidate", payload.getOrDefault("candidate", ""),
                       "sdpMid", payload.getOrDefault("sdpMid", ""),
                       "sdpMLineIndex", payload.getOrDefault("sdpMLineIndex", 0)));
    }

    @MessageMapping("/call.end")
    public void end(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_ENDED"));
    }

    @MessageMapping("/call.reject")
    public void reject(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_REJECTED"));
    }
}
