package com.connectchat.service;

import com.connectchat.dto.response.ConversationResponse;
import com.connectchat.dto.response.GroupResponse;
import com.connectchat.dto.response.MessageResponse;
import com.connectchat.dto.response.UserResponse;
import com.connectchat.entity.*;
import com.connectchat.exception.AccessDeniedException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findAllConversationsByUserId(userId);
        return conversations.stream()
                .map(conv -> buildConversationResponse(conv, userId))
                .sorted(Comparator.comparing(cr -> {
                    if (cr.getLastMessage() != null && cr.getLastMessage().getCreatedAt() != null) {
                        return cr.getLastMessage().getCreatedAt();
                    }
                    return Instant.EPOCH;
                }, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(UUID userId, UUID participantId) {
        if (userId.equals(participantId)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        return conversationRepository.findDirectConversationBetween(userId, participantId)
                .map(conv -> buildConversationResponse(conv, userId))
                .orElseGet(() -> createDirectConversation(userId, participantId));
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));

        return buildConversationResponse(conversation, userId);
    }

    @Transactional
    public void archiveConversation(UUID conversationId, UUID userId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));
        participant.setIsArchived(!participant.getIsArchived());
        participantRepository.save(participant);
    }

    @Transactional
    public void pinConversation(UUID conversationId, UUID userId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));
        participant.setIsPinned(!participant.getIsPinned());
        participantRepository.save(participant);
    }

    @Transactional
    public void muteConversation(UUID conversationId, UUID userId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));
        participant.setIsMuted(!participant.getIsMuted());
        participantRepository.save(participant);
    }

    private ConversationResponse createDirectConversation(UUID userId, UUID participantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User participant = userRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", participantId));

        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .participants(new ArrayList<>())
                .build();

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationParticipant userParticipant = ConversationParticipant.builder()
                .conversation(savedConversation)
                .user(user)
                .isArchived(false)
                .isPinned(false)
                .isMuted(false)
                .build();

        ConversationParticipant otherParticipant = ConversationParticipant.builder()
                .conversation(savedConversation)
                .user(participant)
                .isArchived(false)
                .isPinned(false)
                .isMuted(false)
                .build();

        participantRepository.save(userParticipant);
        participantRepository.save(otherParticipant);

        Conversation refreshed = conversationRepository.findById(savedConversation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", savedConversation.getId()));

        return buildConversationResponse(refreshed, userId);
    }

    public ConversationResponse buildConversationResponse(Conversation conv, UUID currentUserId) {
        ConversationParticipant currentParticipant = participantRepository
                .findByConversationIdAndUserId(conv.getId(), currentUserId)
                .orElse(null);

        UserResponse otherUser = null;
        GroupResponse groupResponse = null;

        if (conv.getType() == ConversationType.DIRECT) {
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conv.getId());
            otherUser = participants.stream()
                    .filter(p -> !p.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .map(p -> userService.mapToUserResponse(p.getUser()))
                    .orElse(null);
        } else if (conv.getType() == ConversationType.GROUP && conv.getGroup() != null) {
            Group group = conv.getGroup();
            GroupMember memberRole = groupMemberRepository
                    .findByGroupIdAndUserId(group.getId(), currentUserId)
                    .orElse(null);

            groupResponse = GroupResponse.builder()
                    .id(group.getId())
                    .name(group.getName())
                    .description(group.getDescription())
                    .avatarUrl(group.getAvatarUrl())
                    .createdBy(userService.mapToUserResponse(group.getCreatedBy()))
                    .memberCount(group.getMembers().size())
                    .role(memberRole != null ? memberRole.getRole() : null)
                    .createdAt(group.getCreatedAt())
                    .build();
        }

        MessageResponse lastMessage = messageRepository
                .findTopByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(conv.getId())
                .map(this::mapToMessageResponse)
                .orElse(null);

        long unreadCount = messageRepository.countUnreadMessages(conv.getId(), currentUserId);

        Instant updatedAt = lastMessage != null ? lastMessage.getCreatedAt() : conv.getCreatedAt();

        return ConversationResponse.builder()
                .id(conv.getId())
                .type(conv.getType())
                .otherUser(otherUser)
                .group(groupResponse)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .isArchived(currentParticipant != null ? currentParticipant.getIsArchived() : false)
                .isPinned(currentParticipant != null ? currentParticipant.getIsPinned() : false)
                .isMuted(currentParticipant != null ? currentParticipant.getIsMuted() : false)
                .updatedAt(updatedAt)
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(userService.mapToUserResponse(message.getSender()))
                .content(message.getContent())
                .type(message.getType())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .attachments(List.of())
                .readBy(List.of())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
