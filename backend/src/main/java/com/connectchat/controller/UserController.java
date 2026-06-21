package com.connectchat.controller;

import com.connectchat.dto.request.UpdateProfileRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.dto.response.UserResponse;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest req) {
        UserResponse user = userService.updateProfile(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", user));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        UserResponse user = userService.updateAvatar(principal.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Avatar updated", user));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserResponse> result = userService.searchUsers(query, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        userService.blockUser(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("User blocked", null));
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        userService.unblockUser(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("User unblocked", null));
    }

    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<UserResponse> blocked = userService.getBlockedUsers(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(blocked));
    }
}
