package com.chefkix.culinary.common.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.util.UploadImageFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UploadImageFileImpl implements UploadImageFile {

    final Cloudinary cloudinary;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Override
    public String uploadImageFile(MultipartFile file) {
        validateImageFile(file);
        try {
            String publicId = generatePublicValue(file.getOriginalFilename());

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image"
                    ));

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Error uploading file to Cloudinary", e);
            throw new AppException(ErrorCode.CAN_NOT_UPLOAD_IMAGE);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "File is empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Image must be under 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Only JPEG, PNG, WebP, and GIF images are allowed");
        }
    }

    @Override
    public List<String> uploadMultipleImageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        log.info("[UPLOAD_MULTI] Starting parallel upload for {} files", files.size());

        List<CompletableFuture<String>> uploadTasks = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    log.info("Uploading file: {}", file.getOriginalFilename());
                    return uploadImageFile(file);
                }))
                .toList();

        // Wait for all upload tasks to complete
        CompletableFuture<Void> allOf = CompletableFuture
                .allOf(uploadTasks.toArray(new CompletableFuture[0]));

        try {
            allOf.join();
        } catch (Exception e) {
            log.error("[UPLOAD_MULTI] Error during parallel upload", e);
            throw new AppException(ErrorCode.CAN_NOT_UPLOAD_IMAGE, "One or more files failed to upload");
        }

        log.info("[UPLOAD_MULTI] Successfully uploaded {} files", files.size());

        return uploadTasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Generate a unique filename (public_id) for Cloudinary
     */
    public String generatePublicValue(String originalName) {
        String baseName = getBaseName(originalName);
        return UUID.randomUUID() + "_" + baseName;
    }

    private String getBaseName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "file";
        }
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex == -1) {
            return originalName;
        }
        return originalName.substring(0, dotIndex);
    }
}