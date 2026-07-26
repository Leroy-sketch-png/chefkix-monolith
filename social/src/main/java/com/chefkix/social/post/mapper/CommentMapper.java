package com.chefkix.social.post.mapper;

import com.chefkix.social.post.dto.request.CommentRequest;
import com.chefkix.social.post.dto.response.CommentResponse;
import com.chefkix.social.post.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommentMapper {
  /**
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "postId", ignore = true)
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "avatarUrl", ignore = true)
  @Mapping(target = "likes", ignore = true)
  @Mapping(target = "replyCount", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Comment toComment(CommentRequest commentRequest);

  /**
   */
  @Mapping(target = "taggedUsers", ignore = true)
  CommentResponse toCommentResponse(Comment comment);
}
