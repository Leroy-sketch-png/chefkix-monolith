package com.chefkix.shared.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Image upload service contract.
 * <p>
 * Interface lives in shared so ALL modules can depend on it.
 * The implementation (Cloudinary-backed) lives in whichever module provides
 * the upload infrastructure — currently culinary or social.
 * <p>
 * Consolidated from recipe-service (single + batch) and post-service (single only).
 * The unified interface includes both single and batch upload methods.
 */
public interface UploadImageFile {

    /**
     * Upload a single image file and return its public URL.
     *
     * @param file the image multipart file
     * @return the publicly accessible URL of the uploaded image
     */
    String uploadImageFile(MultipartFile file);

    /**
     * Upload multiple image files in parallel and return their public URLs.
     * <p>
     * Returns an empty list if {@code files} is null or empty.
     *
     * @param files the image multipart files
     * @return list of publicly accessible URLs, in the same order as input
     */
    List<String> uploadMultipleImageFiles(List<MultipartFile> files);
}
