package com.chefkix.shared.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 */
public interface UploadImageFile {

    /**
     *
     */
    String uploadImageFile(MultipartFile file);

    /**
     *
     */
    List<String> uploadMultipleImageFiles(List<MultipartFile> files);
}
