package com.connectchat.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectchat.dto.response.AttachmentResponse;
import com.connectchat.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg", "audio/wav", "audio/webm",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed"
    );

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public AttachmentResponse uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto",
                    "public_id", UUID.randomUUID().toString()
            );

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            return AttachmentResponse.builder()
                    .url(url)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted file with publicId: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file with publicId {}: {}", publicId, e.getMessage(), e);
            throw new FileUploadException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size exceeds maximum allowed size of 50MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new FileUploadException("File type not allowed: " + contentType);
        }
    }
}
