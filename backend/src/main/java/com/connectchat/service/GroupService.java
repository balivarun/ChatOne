package com.connectchat.service;

import com.connectchat.dto.request.AddGroupMembersRequest;
import com.connectchat.dto.request.CreateGroupRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.request.UpdateGroupRequest;
import com.connectchat.dto.response.AttachmentResponse;
import com.connectchat.dto.response.GroupResponse;
import com.connectchat.dto.response.MessageResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.entity.*;
import com.connectchat.exception.AccessDeniedException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FileService fileService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public GroupResponse createGroup(UUID creatorId, CreateGroupRequest req) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        Group group = Group.builder()
                .name(req.getName())
                .description(req.getDescription())
                .createdBy(creator)
                .members(new ArrayList<>())
                .build();

        Group savedGroup = groupRepository.save(group);

        GroupMember adminMember = GroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .role(GroupRole.ADMIN)
                .build();
        groupMemberRepository.save(adminMember);

        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .group(savedGroup)
                .participants(new ArrayList<>())
                .build();
        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationParticipant creatorParticipant = ConversationParticipant.builder()
                .conversation(savedConversation)
                .user(creator)
                .isArchived(false)
                .isPinned(false)
                .isMuted(false)
                .build();
        participantRepository.save(creatorParticipant);

        if (req.getMemberIds() != null) {
            for (UUID memberId : req.getMemberIds()) {
                if (memberId.equals(creatorId)) continue;

                userRepository.findById(memberId).ifPresent(member -> {
                    if (!groupMemberRepository.existsByGroupIdAndUserId(savedGroup.getId(), memberId)) {
                        GroupMember groupMember = GroupMember.builder()
                                .group(savedGroup)
                                .user(member)
                                .role(GroupRole.MEMBER)
                                .build();
                        groupMemberRepository.save(groupMember);

                        ConversationParticipant memberParticipant = ConversationParticipant.builder()
                                .conversation(savedConversation)
                                .user(member)
                                .isArchived(false)
                                .isPinned(false)
                                .isMuted(false)
                                .build();
                        participantRepository.save(memberParticipant);

                        notificationService.createAndSend(
                                memberId, creatorId,
                                NotificationType.GROUP_INVITE,
                                "You were added to " + savedGroup.getName(),
                                creator.getDisplayName() + " added you to the group",
                                savedGroup.getId());
                    }
                });
            }
        }

        Group refreshed = groupRepository.findById(savedGroup.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", savedGroup.getId()));

        return buildGroupResponse(refreshed, creatorId);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new AccessDeniedException("You are not a member of this group");
        }

        return buildGroupResponse(group, userId);
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UUID userId, UpdateGroupRequest req) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        requireAdmin(groupId, userId);

        if (req.getName() != null && !req.getName().isBlank()) {
            group.setName(req.getName());
        }
        if (req.getDescription() != null) {
            group.setDescription(req.getDescription());
        }

        Group saved = groupRepository.save(group);
        return buildGroupResponse(saved, userId);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        requireAdmin(groupId, userId);

        conversationRepository.findAllConversationsByUserId(userId).stream()
                .filter(c -> c.getType() == ConversationType.GROUP
                        && c.getGroup() != null
                        && c.getGroup().getId().equals(groupId))
                .forEach(conversationRepository::delete);

        groupRepository.delete(group);
        log.info("Group {} deleted by user {}", groupId, userId);
    }

    @Transactional
    public GroupResponse addMembers(UUID groupId, UUID adminId, AddGroupMembersRequest req) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        requireAdmin(groupId, adminId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        Conversation groupConversation = conversationRepository.findAllConversationsByUserId(adminId).stream()
                .filter(c -> c.getType() == ConversationType.GROUP
                        && c.getGroup() != null
                        && c.getGroup().getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Group conversation not found"));

        for (UUID userId : req.getUserIds()) {
            if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
                continue;
            }

            userRepository.findById(userId).ifPresent(member -> {
                GroupMember newMember = GroupMember.builder()
                        .group(group)
                        .user(member)
                        .role(GroupRole.MEMBER)
                        .build();
                groupMemberRepository.save(newMember);

                if (participantRepository.findByConversationIdAndUserId(groupConversation.getId(), userId).isEmpty()) {
                    ConversationParticipant participant = ConversationParticipant.builder()
                            .conversation(groupConversation)
                            .user(member)
                            .isArchived(false)
                            .isPinned(false)
                            .isMuted(false)
                            .build();
                    participantRepository.save(participant);
                }

                notificationService.createAndSend(
                        userId, adminId,
                        NotificationType.MEMBER_ADDED,
                        "Added to " + group.getName(),
                        admin.getDisplayName() + " added you to the group",
                        groupId);
            });
        }

        Group refreshed = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        return buildGroupResponse(refreshed, adminId);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID adminId, UUID targetUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        requireAdmin(groupId, adminId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        groupMemberRepository.delete(member);

        conversationRepository.findAllConversationsByUserId(adminId).stream()
                .filter(c -> c.getType() == ConversationType.GROUP
                        && c.getGroup() != null
                        && c.getGroup().getId().equals(groupId))
                .findFirst()
                .ifPresent(conv -> participantRepository.findByConversationIdAndUserId(conv.getId(), targetUserId)
                        .ifPresent(participantRepository::delete));

        notificationService.createAndSend(
                targetUserId, adminId,
                NotificationType.MEMBER_REMOVED,
                "Removed from " + group.getName(),
                "You were removed from the group",
                groupId);
    }

    @Transactional
    public void promoteToAdmin(UUID groupId, UUID adminId, UUID targetUserId) {
        requireAdmin(groupId, adminId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        member.setRole(GroupRole.ADMIN);
        groupMemberRepository.save(member);
        log.info("User {} promoted to admin in group {} by {}", targetUserId, groupId, adminId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getGroupMessages(UUID groupId, UUID userId, Pageable pageable) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new AccessDeniedException("You are not a member of this group");
        }

        Conversation groupConversation = conversationRepository.findAllConversationsByUserId(userId).stream()
                .filter(c -> c.getType() == ConversationType.GROUP
                        && c.getGroup() != null
                        && c.getGroup().getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Group conversation not found"));

        Page<Message> messages = messageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(groupConversation.getId(), pageable);

        return PageResponse.from(messages.map(messageService::mapToMessageResponse));
    }

    @Transactional
    public GroupResponse updateGroupAvatar(UUID groupId, UUID userId, MultipartFile file) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        requireAdmin(groupId, userId);

        AttachmentResponse uploaded = fileService.uploadFile(file, "group-avatars");
        group.setAvatarUrl(uploaded.getUrl());

        Group saved = groupRepository.save(group);
        return buildGroupResponse(saved, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> searchGroups(String query, UUID userId, Pageable pageable) {
        Page<Group> groups = groupRepository.findByNameContainingIgnoreCase(query, pageable);
        return PageResponse.from(groups.map(g -> buildGroupResponse(g, userId)));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(UUID userId) {
        return groupRepository.findGroupsByUserId(userId).stream()
                .map(g -> buildGroupResponse(g, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendGroupMessage(UUID groupId, UUID senderId, SendMessageRequest req) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, senderId)) {
            throw new AccessDeniedException("You are not a member of this group");
        }

        Conversation groupConversation = conversationRepository.findAllConversationsByUserId(senderId).stream()
                .filter(c -> c.getType() == ConversationType.GROUP
                        && c.getGroup() != null
                        && c.getGroup().getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Group conversation not found"));

        req.setConversationId(groupConversation.getId());
        return messageService.sendMessage(senderId, req);
    }

    private GroupResponse buildGroupResponse(Group group, UUID currentUserId) {
        GroupMember currentMember = groupMemberRepository
                .findByGroupIdAndUserId(group.getId(), currentUserId)
                .orElse(null);

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .createdBy(userService.mapToUserResponse(group.getCreatedBy()))
                .memberCount(group.getMembers().size())
                .role(currentMember != null ? currentMember.getRole() : null)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private void requireAdmin(UUID groupId, UUID userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this group"));

        if (member.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
    }
}
