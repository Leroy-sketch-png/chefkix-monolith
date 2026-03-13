package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostUpdateRequest {
  @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
  String content;

  @Size(max = 20, message = "Maximum 20 tags allowed")
  List<String> tags;
}
