package com.connectchat.dto.response;

import com.connectchat.entity.GroupRole;
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
public class GroupResponse {

    private UUID id;
    private String name;
    private String description;
    private String avatarUrl;
    private UserResponse createdBy;
    private int memberCount;
    private GroupRole role;
    private Instant createdAt;
}
