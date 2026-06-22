package com.connectchat.service;

import com.connectchat.dto.request.EditMessageRequest;
import com.connectchat.dto.request.ForwardMessageRequest;
import com.connectchat.dto.request.MarkReadRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.AttachmentResponse;
import com.connectchat.dto.response.MessageResponse;
import com.connectchat.dto.response.PageResponse;
import com.connectchat.dto.response.UserResponse;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageReadRepository messageReadRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest req) {
        Conversation conversation = conversationRepository.findById(req.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", req.getConversationId()));

        participantRepository.findByConversationIdAndUserId(req.getConversationId(), senderId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Message replyTo = null;
        if (req.getReplyToId() != null) {
            replyTo = messageRepository.findById(req.getReplyToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Message", "id", req.getReplyToId()));
        }

        MessageType messageType = req.getType() != null ? req.getType() : MessageType.TEXT;

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(req.getContent())
                .type(messageType)
                .replyTo(replyTo)
                .isEdited(false)
                .isDeleted(false)
                .build();

        Message saved = messageRepository.save(message);
        MessageResponse response = mapToMessageResponse(saved);

        broadcastMessage(conversation, senderId, response);

        sendMessageNotifications(conversation, senderId, saved);

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(UUID conversationId, UUID userId, Pageable pageable) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this conversation"));

        Page<Message> messages = messageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(conversationId, pageable);

        Page<MessageResponse> responsePage = messages.map(this::mapToMessageResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public MessageResponse editMessage(UUID messageId, UUID userId, EditMessageRequest req) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }

        if (message.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted message");
        }

        message.setContent(req.getContent());
        message.setIsEdited(true);

        Message saved = messageRepository.save(message);
        MessageResponse response = mapToMessageResponse(saved);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId() + "/events",
                Map.of("type", "MESSAGE_EDITED", "data", response));

        return response;
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        message.setIsDeleted(true);
        messageRepository.save(message);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId() + "/events",
                Map.of("type", "MESSAGE_DELETED", "data", Map.of("messageId", messageId)));
    }

    @Transactional
    public MessageResponse forwardMessage(UUID messageId, UUID userId, ForwardMessageRequest req) {
        Message original = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        Conversation targetConversation = conversationRepository.findById(req.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", req.getConversationId()));

        participantRepository.findByConversationIdAndUserId(req.getConversationId(), userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in the target conversation"));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Message forwarded = Message.builder()
                .conversation(targetConversation)
                .sender(sender)
                .content(original.getContent())
                .type(original.getType())
                .isEdited(false)
                .isDeleted(false)
                .build();

        Message saved = messageRepository.save(forwarded);
        MessageResponse response = mapToMessageResponse(saved);

        broadcastMessage(targetConversation, userId, response);

        return response;
    }

    @Transactional
    public void markMessagesRead(UUID userId, List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }

        for (UUID messageId : messageIds) {
            messageReadRepository.upsertRead(messageId, userId);

            messageRepository.findById(messageId).ifPresent(message -> {
                messagingTemplate.convertAndSend(
                        "/topic/conversation/" + message.getConversation().getId() + "/events",
                        Map.of("type", "MESSAGE_READ",
                               "data", Map.of("messageId", messageId, "userId", userId)));
            });
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> searchMessages(UUID conversationId, String query, Pageable pageable) {
        Page<Message> messages = messageRepository.searchMessages(conversationId, query, pageable);
        Page<MessageResponse> responsePage = messages.map(this::mapToMessageResponse);
        return PageResponse.from(responsePage);
    }

    private void broadcastMessage(Conversation conversation, UUID senderId, MessageResponse response) {
        if (conversation.getType() == ConversationType.DIRECT) {
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
            participants.stream()
                    .filter(p -> !p.getUser().getId().equals(senderId))
                    .forEach(p -> {
                        messagingTemplate.convertAndSendToUser(
                                p.getUser().getEmail(),
                                "/queue/messages",
                                response);
                    });
        } else {
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversation.getId(),
                    response);
        }
    }

    private void sendMessageNotifications(Conversation conversation, UUID senderId, Message message) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());

        for (ConversationParticipant participant : participants) {
            UUID recipientId = participant.getUser().getId();
            if (!recipientId.equals(senderId)) {
                NotificationType notifType = conversation.getType() == ConversationType.DIRECT
                        ? NotificationType.NEW_MESSAGE
                        : NotificationType.GROUP_MESSAGE;

                String title = conversation.getType() == ConversationType.DIRECT
                        ? message.getSender().getDisplayName()
                        : (conversation.getGroup() != null ? conversation.getGroup().getName() : "Group");

                String body = message.getContent() != null
                        ? (message.getContent().length() > 100
                            ? message.getContent().substring(0, 100) + "..."
                            : message.getContent())
                        : "[Attachment]";

                notificationService.createAndSend(
                        recipientId,
                        senderId,
                        notifType,
                        title,
                        body,
                        conversation.getId());
            }
        }
    }

    public MessageResponse mapToMessageResponse(Message message) {
        MessageResponse replyToResponse = null;
        if (message.getReplyTo() != null) {
            Message replyTo = message.getReplyTo();
            replyToResponse = MessageResponse.builder()
                    .id(replyTo.getId())
                    .conversationId(replyTo.getConversation().getId())
                    .sender(userService.mapToUserResponse(replyTo.getSender()))
                    .content(replyTo.getContent())
                    .type(replyTo.getType())
                    .isEdited(replyTo.getIsEdited())
                    .isDeleted(replyTo.getIsDeleted())
                    .attachments(List.of())
                    .readBy(List.of())
                    .createdAt(replyTo.getCreatedAt())
                    .updatedAt(replyTo.getUpdatedAt())
                    .build();
        }

        List<AttachmentResponse> attachments = message.getAttachments() != null
                ? message.getAttachments().stream()
                    .map(a -> AttachmentResponse.builder()
                            .id(a.getId())
                            .url(a.getUrl())
                            .fileName(a.getFileName())
                            .fileType(a.getFileType())
                            .fileSize(a.getFileSize())
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        List<UserResponse> readBy = message.getReads() != null
                ? message.getReads().stream()
                    .map(r -> userService.mapToUserResponse(r.getUser()))
                    .collect(Collectors.toList())
                : List.of();

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(userService.mapToUserResponse(message.getSender()))
                .content(message.getContent())
                .type(message.getType())
                .replyTo(replyToResponse)
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .attachments(attachments)
                .readBy(readBy)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
