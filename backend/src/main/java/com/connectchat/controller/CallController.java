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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CallController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CustomUserDetailsService userDetailsService;

    // Single background thread used to re-deliver CALL_OFFER after a short delay.
    // On older clients the STOMP frame collector starts ~5s after connect (after
    // initial API calls finish), so the first delivery can race and be dropped.
    // The retry arrives after the collector is guaranteed to be running.
    // onIncomingOffer() on the client guards with "if not Idle return", so a
    // duplicate offer when the call is already Incoming is safely ignored.
    private final ScheduledExecutorService retryScheduler =
            Executors.newSingleThreadScheduledExecutor();

    @MessageMapping("/call.offer")
    public void offer(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            log.warn("CALL_OFFER rejected — unauthenticated");
            return;
        }
        String targetEmail = (String) payload.get("targetEmail");
        String callerEmail = principal.getName();
        log.info("CALL_OFFER from={} to={} type={}", callerEmail, targetEmail, payload.get("callType"));
        if (targetEmail == null || targetEmail.isBlank()) {
            log.warn("CALL_OFFER rejected — targetEmail blank");
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

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "CALL_OFFER");
        msg.put("callerEmail", callerEmail);
        msg.put("callerName", callerName);
        msg.put("callerAvatar", callerAvatar);
        msg.put("conversationId", payload.getOrDefault("conversationId", ""));
        msg.put("sdp", payload.getOrDefault("sdp", ""));
        msg.put("callType", payload.getOrDefault("callType", "video"));

        // First delivery — may be dropped if callee collector isn't ready yet
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call", msg);

        // Retry after 6s — by then the collector is definitely running.
        // Duplicate is safely ignored by the client's onIncomingOffer() guard.
        final String target = targetEmail;
        final Map<String, Object> retry = new HashMap<>(msg);
        retryScheduler.schedule(
                () -> messagingTemplate.convertAndSendToUser(target, "/queue/call", retry),
                6, TimeUnit.SECONDS);
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
