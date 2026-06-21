package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditMessageRequest {

    @NotBlank(message = "Content must not be blank")
    private String content;
}
