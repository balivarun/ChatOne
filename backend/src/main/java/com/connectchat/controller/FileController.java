package com.connectchat.controller;

import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.AttachmentResponse;
import com.connectchat.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.connectchat.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        AttachmentResponse attachment = fileService.uploadFile(file, "uploads");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded", attachment));
    }
}
