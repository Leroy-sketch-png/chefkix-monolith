package com.chefkix.social.chat.mapper;

import java.util.List;

import com.chefkix.social.chat.dto.response.ConversationResponse;
import com.chefkix.social.chat.entity.Conversation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    ConversationResponse toConversationResponse(Conversation conversation);

    List<ConversationResponse> toConversationResponseList(List<Conversation> conversations);
}
