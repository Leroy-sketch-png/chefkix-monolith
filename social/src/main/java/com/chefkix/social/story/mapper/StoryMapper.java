package com.chefkix.social.story.mapper;

import com.chefkix.social.story.dto.request.StoryItemDto;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StoryMapper {
    StoryResponse toStoryResponse(Story story);
    StoryItem toStoryItem(StoryItemDto dto);
    List<StoryItem> toStoryItems(List<StoryItemDto> dtos);
}
