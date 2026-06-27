package com.connectchat.controller;

import com.connectchat.repository.UserRepository;
import com.connectchat.security.CustomUserDetailsService;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.FcmService;
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
    private final UserRepository userRepository;
    private final FcmService fcmService;

    // Resend CALL_OFFER after 6s for clients whose STOMP collector starts late.
    // onIncomingOffer() guards with "if not Idle return" so duplicates are safe.
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
        String callType = (String) payload.getOrDefault("callType", "video");
        log.info("CALL_OFFER from={} to={} type={}", callerEmail, targetEmail, callType);
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
        msg.put("callType", callType);

        // Deliver via STOMP (for foreground/background clients)
        messagingTemplate.convertAndSendToUser(targetEmail, "/queue/call", msg);

        // Retry after 6s for clients whose collector starts late
        final String target = targetEmail;
        final Map<String, Object> retry = new HashMap<>(msg);
        retryScheduler.schedule(
                () -> messagingTemplate.convertAndSendToUser(target, "/queue/call", retry),
                6, TimeUnit.SECONDS);

        // FCM push — wakes the device even when the app is fully closed
        final String finalCallerName = callerName;
        final String finalCallerAvatar = callerAvatar;
        try {
            userRepository.findByEmail(targetEmail).ifPresent(callee -> {
                if (callee.getFcmToken() != null) {
                    Map<String, String> fcmData = new HashMap<>();
                    fcmData.put("type", "CALL_OFFER");
                    fcmData.put("callerEmail", callerEmail);
                    fcmData.put("callerName", finalCallerName);
                    fcmData.put("callerAvatar", finalCallerAvatar);
                    fcmData.put("conversationId", (String) payload.getOrDefault("conversationId", ""));
                    fcmData.put("sdp", (String) payload.getOrDefault("sdp", ""));
                    fcmData.put("callType", callType);
                    String callNotifBody = callType.equals("video") ? "Incoming video call" : "Incoming voice call";
                    fcmService.sendNotification(callee.getFcmToken(), finalCallerName, callNotifBody, fcmData);
                    log.info("FCM call push sent to {}", targetEmail);
                }
            });
        } catch (Exception e) {
            log.warn("FCM call push failed for {}: {}", targetEmail, e.getMessage());
        }
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
