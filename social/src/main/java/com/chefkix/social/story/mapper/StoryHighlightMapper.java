package com.chefkix.social.story.mapper;

import com.chefkix.social.story.dto.response.HighlightResponse;
import com.chefkix.social.story.entity.StoryHighlight;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StoryHighlightMapper {
    HighlightResponse toHighlightResponse(StoryHighlight story);
}
