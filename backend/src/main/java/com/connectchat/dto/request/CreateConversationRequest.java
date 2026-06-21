package com.connectchat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateConversationRequest {

    @NotNull(message = "Participant ID must not be null")
    private UUID participantId;
}
