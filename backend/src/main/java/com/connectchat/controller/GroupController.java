package com.connectchat.controller;

import com.connectchat.dto.request.AddGroupMembersRequest;
import com.connectchat.dto.request.CreateGroupRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.request.UpdateGroupRequest;
import com.connectchat.dto.response.*;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest req) {
        GroupResponse group = groupService.createGroup(principal.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Group created", group));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<GroupResponse> groups = groupService.getUserGroups(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<GroupResponse>>> searchGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GroupResponse> result = groupService.searchGroups(query, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        GroupResponse group = groupService.getGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody UpdateGroupRequest req) {
        GroupResponse group = groupService.updateGroup(id, principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Group updated", group));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        groupService.deleteGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Group deleted", null));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroupAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        GroupResponse group = groupService.updateGroupAvatar(id, principal.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Group avatar updated", group));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getGroupMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<MessageResponse> messages = groupService.getGroupMessages(id, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendGroupMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody SendMessageRequest req) {
        MessageResponse message = groupService.sendGroupMessage(id, principal.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent", message));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<GroupResponse>> addMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddGroupMembersRequest req) {
        GroupResponse group = groupService.addMembers(id, principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Members added", group));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        groupService.removeMember(id, principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed", null));
    }

    @PutMapping("/{id}/members/{userId}/admin")
    public ResponseEntity<ApiResponse<Void>> promoteToAdmin(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        groupService.promoteToAdmin(id, principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Member promoted to admin", null));
    }
}
