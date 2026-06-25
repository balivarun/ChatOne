package com.connectchat.controller;

import com.connectchat.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CallController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/call.offer")
    public void offer(@Payload Map<String, Object> payload,
                      @AuthenticationPrincipal UserPrincipal caller) {
        String targetEmail = (String) payload.get("targetEmail");
        log.info("CALL_OFFER from={} to={} type={}",
                caller != null ? caller.getEmail() : "unauthenticated", targetEmail, payload.get("callType"));
        if (caller == null) {
            log.warn("CALL_OFFER rejected — unauthenticated sender");
            return;
        }
        if (targetEmail == null || targetEmail.isBlank()) {
            log.warn("CALL_OFFER rejected — targetEmail is null/blank");
            return;
        }
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_OFFER",
                       "callerEmail", caller.getEmail(),
                       "callerName", caller.getDisplayName() != null ? caller.getDisplayName() : "",
                       "callerAvatar", caller.getAvatarUrl() != null ? caller.getAvatarUrl() : "",
                       "conversationId", payload.getOrDefault("conversationId", ""),
                       "sdp", payload.getOrDefault("sdp", ""),
                       "callType", payload.getOrDefault("callType", "video")));
    }

    @MessageMapping("/call.answer")
    public void answer(@Payload Map<String, Object> payload,
                       @AuthenticationPrincipal UserPrincipal callee) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null || callee == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_ANSWER",
                       "sdp", payload.getOrDefault("sdp", "")));
    }

    @MessageMapping("/call.ice")
    public void ice(@Payload Map<String, Object> payload,
                    @AuthenticationPrincipal UserPrincipal sender) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null || sender == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "ICE_CANDIDATE",
                       "candidate", payload.getOrDefault("candidate", ""),
                       "sdpMid", payload.getOrDefault("sdpMid", ""),
                       "sdpMLineIndex", payload.getOrDefault("sdpMLineIndex", 0)));
    }

    @MessageMapping("/call.end")
    public void end(@Payload Map<String, Object> payload,
                    @AuthenticationPrincipal UserPrincipal sender) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null || sender == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_ENDED"));
    }

    @MessageMapping("/call.reject")
    public void reject(@Payload Map<String, Object> payload,
                       @AuthenticationPrincipal UserPrincipal callee) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null || callee == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_REJECTED"));
    }
}
