package com.connectchat.dto.request;

import com.connectchat.entity.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotNull(message = "Conversation ID must not be null")
    private UUID conversationId;

    private String content;

    private MessageType type = MessageType.TEXT;

    private UUID replyToId;
}
