package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentRequest {
  @NotBlank(message = "Comment content is required")
  @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
  String content;

  @Size(max = 10, message = "Maximum 10 users can be tagged")
  List<String> taggedUserIds;  // Users @mentioned in the comment
}
