package com.chefkix.culinary.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Result of a video upload to Cloudinary.
 * Includes URL, thumbnail, and duration metadata.
 * Spec: vision_and_spec/20-media-lifecycle.txt §2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoUploadResult {
    String url;             // Cloudinary secure_url
    String thumbnailUrl;    // Auto-generated thumbnail
    Integer durationSec;    // Video duration in seconds
}
