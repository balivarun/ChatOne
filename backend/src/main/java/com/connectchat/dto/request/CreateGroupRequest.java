package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name must not be blank")
    @Size(max = 100, message = "Group name must be at most 100 characters")
    private String name;

    private String description;

    private List<UUID> memberIds;
}
