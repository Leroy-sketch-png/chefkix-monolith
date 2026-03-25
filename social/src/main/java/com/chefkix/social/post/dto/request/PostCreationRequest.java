package com.chefkix.social.post.dto.request;

import com.chefkix.social.post.enums.PostType;
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
  @Size(max = 500) String avatarUrl;

  @NotBlank(message = "Post content is required")
  @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
  String content;

  @Size(max = 10, message = "Maximum 10 photos allowed")
  List<MultipartFile> photoUrls;

  @Size(max = 500) String videoUrl;

  @Size(max = 20, message = "Maximum 20 tags allowed")
  List<String> tags;
  
  @Size(max = 100) String sessionId;
  @Builder.Default
  Boolean isPrivateRecipe = false;

  @Size(max = 10, message = "Maximum 10 tagged users")
  List<String> taggedUserIds;

  @Builder.Default
  Boolean isHidden = false;

  PostType postType; // Optional: QUICK for quick posts, POLL for polls, defaults to PERSONAL

  // Poll fields (only when postType == POLL)
  @Size(max = 500) String pollQuestion;
  @Size(max = 200) String pollOptionA;
  @Size(max = 200) String pollOptionB;
}
