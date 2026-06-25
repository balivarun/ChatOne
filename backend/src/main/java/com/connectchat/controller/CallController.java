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

    // Caller initiates a call — sends offer SDP to callee
    @MessageMapping("/call.offer")
    public void offer(@Payload Map<String, Object> payload,
                      @AuthenticationPrincipal UserPrincipal caller) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_OFFER",
                       "callerId", caller.getId().toString(),
                       "callerName", caller.getDisplayName(),
                       "callerAvatar", caller.getAvatarUrl() != null ? caller.getAvatarUrl() : "",
                       "conversationId", payload.getOrDefault("conversationId", ""),
                       "sdp", payload.getOrDefault("sdp", ""),
                       "callType", payload.getOrDefault("callType", "video")));
    }

    // Callee answers the call — sends answer SDP back to caller
    @MessageMapping("/call.answer")
    public void answer(@Payload Map<String, Object> payload,
                       @AuthenticationPrincipal UserPrincipal callee) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_ANSWER",
                       "calleeId", callee.getId().toString(),
                       "sdp", payload.getOrDefault("sdp", "")));
    }

    // Either side sends ICE candidates to the other
    @MessageMapping("/call.ice")
    public void ice(@Payload Map<String, Object> payload,
                    @AuthenticationPrincipal UserPrincipal sender) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "ICE_CANDIDATE",
                       "senderId", sender.getId().toString(),
                       "candidate", payload.getOrDefault("candidate", ""),
                       "sdpMid", payload.getOrDefault("sdpMid", ""),
                       "sdpMLineIndex", payload.getOrDefault("sdpMLineIndex", 0)));
    }

    // Either side ends the call
    @MessageMapping("/call.end")
    public void end(@Payload Map<String, Object> payload,
                    @AuthenticationPrincipal UserPrincipal sender) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_ENDED",
                       "senderId", sender.getId().toString()));
    }

    // Callee rejects the call
    @MessageMapping("/call.reject")
    public void reject(@Payload Map<String, Object> payload,
                       @AuthenticationPrincipal UserPrincipal callee) {
        String targetEmail = (String) payload.get("targetEmail");
        if (targetEmail == null) return;
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call",
                Map.of("type", "CALL_REJECTED",
                       "calleeId", callee.getId().toString()));
    }
}
