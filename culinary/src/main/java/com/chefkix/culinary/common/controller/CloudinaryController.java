package com.chefkix.culinary.common.controller;

import com.chefkix.culinary.common.dto.VideoUploadResult;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.util.UploadImageFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/recipes/uploads")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryController {

    UploadImageFile uploadImageFile;
    Cloudinary cloudinary;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final int MAX_IMAGE_COUNT = 10;

    /**
     * Upload multiple image files to Cloudinary.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<String>> uploadImages(
            @RequestParam("files") List<MultipartFile> files) {

        if (files.size() > MAX_IMAGE_COUNT) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Maximum " + MAX_IMAGE_COUNT + " images allowed per upload");
        }

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "Only JPEG, PNG, GIF, and WebP images are allowed");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "Each image must be under 10MB");
            }
        }

        List<String> imageUrls = uploadImageFile.uploadMultipleImageFiles(files);

        return ApiResponse.created(imageUrls);
    }

    /**
     * Upload a single video file to Cloudinary.
     * Returns URL, auto-generated thumbnail, and duration.
     * Spec: vision_and_spec/20-media-lifecycle.txt §2
     *
     * Constraints: mp4/webm only, max 50MB, max 60 seconds.
     */
    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VideoUploadResult> uploadVideo(
            @RequestParam("file") MultipartFile file) {

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.equals("video/mp4") && !contentType.equals("video/webm"))) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Only mp4 and webm video formats are allowed");
        }

        // Validate file size (50MB max)
        long maxSize = 50L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Video file must be under 50MB");
        }

        try {
            String publicId = "chefkix/videos/" + UUID.randomUUID();

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "video",
                            "format", "mp4",
                            "eager", "c_thumb,w_400,h_300"
                    ));

            String url = uploadResult.get("secure_url").toString();

            // Extract thumbnail from eager transformation
            String thumbnailUrl = null;
            Object eagerObj = uploadResult.get("eager");
            if (eagerObj instanceof List<?> eagerList && !eagerList.isEmpty()) {
                Object first = eagerList.get(0);
                if (first instanceof Map<?, ?> eagerMap) {
                    Object thumbUrl = eagerMap.get("secure_url");
                    if (thumbUrl != null) thumbnailUrl = thumbUrl.toString();
                }
            }
            // Fallback: generate thumbnail URL from video URL
            if (thumbnailUrl == null) {
                thumbnailUrl = url.replace("/video/upload/", "/video/upload/c_thumb,w_400,h_300/")
                        .replace(".mp4", ".jpg");
            }

            // Extract duration
            Integer durationSec = null;
            Object duration = uploadResult.get("duration");
            if (duration != null) {
                durationSec = (int) Math.round(Double.parseDouble(duration.toString()));
            }

            // Validate max duration (60 seconds)
            if (durationSec != null && durationSec > 60) {
                // Delete the uploaded video
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "Video must be 60 seconds or shorter");
            }

            VideoUploadResult result = VideoUploadResult.builder()
                    .url(url)
                    .thumbnailUrl(thumbnailUrl)
                    .durationSec(durationSec)
                    .build();

            log.info("[VIDEO_UPLOAD] Success: url={}, duration={}s", url, durationSec);
            return ApiResponse.created(result);

        } catch (AppException e) {
            throw e; // Re-throw validation errors
        } catch (IOException e) {
            log.error("[VIDEO_UPLOAD] Failed to upload video", e);
            throw new AppException(ErrorCode.CAN_NOT_UPLOAD_IMAGE, "Failed to upload video");
        }
    }
}