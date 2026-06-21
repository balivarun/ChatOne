package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Display name must not be blank")
    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    private String bio;
}
