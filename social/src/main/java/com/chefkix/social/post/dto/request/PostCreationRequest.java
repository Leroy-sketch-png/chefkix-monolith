package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostCreationRequest {
  String avatarUrl;

  @NotBlank(message = "Post content is required")
  @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
  String content;

  @Size(max = 10, message = "Maximum 10 photos allowed")
  List<MultipartFile> photoUrls;

  String videoUrl;

  @Size(max = 20, message = "Maximum 20 tags allowed")
  List<String> tags;
  
  String sessionId; // Optional: ID của session nấu ăn
  @Builder.Default
  Boolean isPrivateRecipe = false;
}
