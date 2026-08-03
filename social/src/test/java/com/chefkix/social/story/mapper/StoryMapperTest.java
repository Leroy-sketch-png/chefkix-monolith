package com.chefkix.social.story.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.entity.Story;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class StoryMapperTest {

    private final StoryMapper mapper = Mappers.getMapper(StoryMapper.class);

    @Test
    void mapsStoredRecipeIdToPublicResponseContract() {
        Story story = Story.builder()
                .id("story-1")
                .userId("owner")
                .recipeId("recipe-1")
                .build();

        StoryResponse response = mapper.toStoryResponse(story);

        assertThat(response.linkedRecipeId()).isEqualTo("recipe-1");
    }
}
