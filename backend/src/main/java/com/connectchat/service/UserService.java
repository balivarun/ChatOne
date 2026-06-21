package com.connectchat.service;

import com.connectchat.dto.request.UpdateProfileRequest;
import com.connectchat.dto.response.AttachmentResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.dto.response.UserResponse;
import com.connectchat.entity.BlockedUser;
import com.connectchat.entity.User;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.BlockedUserRepository;
import com.connectchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setDisplayName(req.getDisplayName());
        user.setBio(req.getBio());

        User saved = userRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Transactional
    public UserResponse updateAvatar(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        AttachmentResponse uploaded = fileService.uploadFile(file, "avatars");
        user.setAvatarUrl(uploaded.getUrl());

        User saved = userRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String query, UUID currentUserId, Pageable pageable) {
        List<UUID> blockedIds = blockedUserRepository.findByBlockerId(currentUserId)
                .stream()
                .map(bu -> bu.getBlocked().getId())
                .collect(Collectors.toList());

        Page<User> page = userRepository
                .findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable);

        List<UserResponse> content = page.getContent().stream()
                .filter(user -> !user.getId().equals(currentUserId) && !blockedIds.contains(user.getId()))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToUserResponse(user);
    }

    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }

        if (blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            log.info("User {} is already blocked by {}", blockedId, blockerId);
            return;
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", blockerId));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", blockedId));

        BlockedUser blockedUser = BlockedUser.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        blockedUserRepository.save(blockedUser);
        log.info("User {} blocked user {}", blockerId, blockedId);
    }

    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        BlockedUser blockedUser = blockedUserRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("Block relationship not found"));

        blockedUserRepository.delete(blockedUser);
        log.info("User {} unblocked user {}", blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getBlockedUsers(UUID userId) {
        return blockedUserRepository.findByBlockerId(userId)
                .stream()
                .map(bu -> mapToUserResponse(bu.getBlocked()))
                .collect(Collectors.toList());
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
