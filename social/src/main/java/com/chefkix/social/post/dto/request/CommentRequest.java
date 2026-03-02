package com.chefkix.social.post.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentRequest {
  String content;
  List<String> taggedUserIds;  // Users @mentioned in the comment

}
