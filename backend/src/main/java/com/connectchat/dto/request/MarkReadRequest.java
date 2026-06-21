package com.connectchat.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MarkReadRequest {

    private List<UUID> messageIds;
}
