package com.connectchat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ForwardMessageRequest {

    @NotNull(message = "Conversation ID must not be null")
    private UUID conversationId;
}
