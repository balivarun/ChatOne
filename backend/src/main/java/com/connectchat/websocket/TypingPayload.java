package com.connectchat.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingPayload {

    private UUID conversationId;
    private boolean isTyping;
}
