package com.connectchat.dto.response;

import com.connectchat.entity.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;
    private ConversationType type;
    private UserResponse otherUser;
    private GroupResponse group;
    private MessageResponse lastMessage;
    private long unreadCount;
    private Boolean isArchived;
    private Boolean isPinned;
    private Boolean isMuted;
    private Instant updatedAt;
}
