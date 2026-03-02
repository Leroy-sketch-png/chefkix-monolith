package com.chefkix.social.post.dto.request;

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
  String content;
  List<MultipartFile> photoUrls;
  String videoUrl;
  List<String> tags;
    String sessionId; // Optional: ID của session nấu ăn
    @Builder.Default
    Boolean isPrivateRecipe = false;
}
