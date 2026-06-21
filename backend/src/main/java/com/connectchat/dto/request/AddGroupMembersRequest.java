package com.connectchat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AddGroupMembersRequest {

    @NotEmpty(message = "User IDs must not be empty")
    private List<UUID> userIds;
}
