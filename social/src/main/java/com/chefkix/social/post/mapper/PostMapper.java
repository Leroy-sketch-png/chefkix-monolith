package com.chefkix.social.post.mapper;

import com.chefkix.social.post.dto.request.PostCreationRequest;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {
    PostResponse toPostResponse(Post post);

    @Mapping(target = "photoUrls", ignore = true)
    Post toPost(PostCreationRequest post);
}
