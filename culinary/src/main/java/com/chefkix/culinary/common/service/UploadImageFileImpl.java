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

    @Override
    public String uploadImageFile(MultipartFile file) {
        try {
            String publicId = generatePublicValue(file.getOriginalFilename());

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "auto"
                    ));

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Lỗi khi upload file lên Cloudinary", e);
            throw new AppException(ErrorCode.CAN_NOT_UPLOAD_IMAGE);
        }
    }

    @Override
    public List<String> uploadMultipleImageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        log.info("[UPLOAD_MULTI] Bắt đầu upload {} files song song", files.size());

        // 1. Tạo một danh sách các tác vụ upload (CompletableFuture)
        List<CompletableFuture<String>> uploadTasks = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    // Tái sử dụng logic upload 1 file của bạn
                    // Bạn nên tách logic trong hàm uploadImageFile ra
                    // một hàm private để tái sử dụng
                    log.info("Đang upload file: {}", file.getOriginalFilename());
                    return uploadImageFile(file); // Gọi lại hàm upload 1 file
                }))
                .toList();

        // 2. Chờ TẤT CẢ các tác vụ hoàn thành
        CompletableFuture<Void> allOf = CompletableFuture
                .allOf(uploadTasks.toArray(new CompletableFuture[0]));

        try {
            allOf.join(); // Chờ ở đây
        } catch (Exception e) {
            // Nếu MỘT file bị lỗi, nó sẽ ném exception ở đây
            log.error("[UPLOAD_MULTI] Có lỗi xảy ra khi upload song song", e);
            throw new AppException(ErrorCode.CAN_NOT_UPLOAD_IMAGE, "Một hoặc nhiều file bị lỗi khi upload");
        }

        log.info("[UPLOAD_MULTI] Đã upload thành công {} files", files.size());

        return uploadTasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Tạo tên file duy nhất (public_id) cho Cloudinary
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