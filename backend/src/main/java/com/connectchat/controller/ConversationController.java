package com.connectchat.controller;

import com.connectchat.dto.request.CreateConversationRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.ConversationResponse;
import com.connectchat.dto.response.MessageResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.ConversationService;
import com.connectchat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ConversationResponse> conversations = conversationService.getConversations(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest req) {
        ConversationResponse conversation = conversationService
                .getOrCreateDirectConversation(principal.getId(), req.getParticipantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Conversation created", conversation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ConversationResponse conversation = conversationService.getConversation(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<MessageResponse> messages = messageService.getMessages(id, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        conversationService.archiveConversation(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation archive status toggled", null));
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> pinConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        conversationService.pinConversation(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation pin status toggled", null));
    }

    @PutMapping("/{id}/mute")
    public ResponseEntity<ApiResponse<Void>> muteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        conversationService.muteConversation(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation mute status toggled", null));
    }

    @GetMapping("/{id}/messages/search")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> searchMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<MessageResponse> messages = messageService.searchMessages(id, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
