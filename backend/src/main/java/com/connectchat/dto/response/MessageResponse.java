package com.connectchat.dto.response;

import com.connectchat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UserResponse sender;
    private String content;
    private MessageType type;
    private MessageResponse replyTo;
    private Boolean isEdited;
    private Boolean isDeleted;
    private List<AttachmentResponse> attachments;
    private List<UserResponse> readBy;
    private Instant createdAt;
    private Instant updatedAt;
}
