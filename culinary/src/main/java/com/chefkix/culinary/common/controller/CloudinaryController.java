package com.chefkix.culinary.common.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.util.UploadImageFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryController {

    UploadImageFile uploadImageFile;

    /**
     * Endpoint để tải một file (ảnh) lên Cloudinary.
     * Nhận file từ request multipart/form-data.
     *
     * @param file Đối tượng MultipartFile được gửi từ client với key là "file"
     * @return ApiResponse chứa URL an toàn (secure_url) của file đã được upload
     */
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @ResponseStatus(HttpStatus.CREATED)
//    public ApiResponse<String> uploadImage(
//            @RequestParam("file") MultipartFile file) {
//
//        String imageUrl = uploadImageFile.uploadImageFile(file);
//
//        return ApiResponse.created(imageUrl);
//    }

    /**
     * Endpoint để tải LÊN NHIỀU file (ảnh) cùng lúc.
     *
     * @param files Danh sách các file được gửi từ client với key là "files"
     * @return ApiResponse chứa DANH SÁCH các URL an toàn (secure_url)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<String>> uploadImages(
            @RequestParam("files") List<MultipartFile> files) {

        List<String> imageUrls = uploadImageFile.uploadMultipleImageFiles(files);

        return ApiResponse.created(imageUrls);
    }
}