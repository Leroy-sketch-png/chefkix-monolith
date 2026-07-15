package com.chefkix.culinary.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UploadImageFileImplTest {

  private final UploadImageFileImpl service = new UploadImageFileImpl(null, null);

  @Test
  void extractOptimizedImageUrlPrefersEagerSecureUrl() {
    String optimizedUrl = "https://res.cloudinary.com/demo/image/upload/c_limit,w_1600,h_1600,q_auto,f_auto/v1/photo.jpg";
    String originalUrl = "https://res.cloudinary.com/demo/image/upload/v1/photo.jpg";

    String result = service.extractOptimizedImageUrl(Map.of(
        "secure_url", originalUrl,
        "eager", List.of(Map.of("secure_url", optimizedUrl))));

    assertThat(result).isEqualTo(optimizedUrl);
  }

  @Test
  void extractOptimizedImageUrlFallsBackToOriginalSecureUrl() {
    String originalUrl = "https://res.cloudinary.com/demo/image/upload/v1/photo.jpg";

    String result = service.extractOptimizedImageUrl(Map.of("secure_url", originalUrl));

    assertThat(result).isEqualTo(originalUrl);
  }
}
