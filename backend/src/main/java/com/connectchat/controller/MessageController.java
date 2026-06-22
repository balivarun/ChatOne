package com.connectchat.controller;

import com.connectchat.dto.request.EditMessageRequest;
import com.connectchat.dto.request.ForwardMessageRequest;
import com.connectchat.dto.request.MarkReadRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.MessageResponse;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest req) {
        MessageResponse message = messageService.sendMessage(principal.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent", message));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody EditMessageRequest req) {
        MessageResponse message = messageService.editMessage(id, principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Message edited", message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        messageService.deleteMessage(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Message deleted", null));
    }

    @PostMapping("/{id}/forward")
    public ResponseEntity<ApiResponse<MessageResponse>> forwardMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ForwardMessageRequest req) {
        MessageResponse message = messageService.forwardMessage(id, principal.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message forwarded", message));
    }

    @PutMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody MarkReadRequest req) {
        messageService.markMessagesRead(principal.getId(), req.getMessageIds());
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }
}
