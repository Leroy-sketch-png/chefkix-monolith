package com.chefkix.social.chat.mapper;

import java.util.List;

import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "sharedPostImage", ignore = true)
    @Mapping(target = "sharedPostTitle", ignore = true)
    @Mapping(target = "replyTo", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage);

    @Mapping(target = "replyToId", ignore = true)
    @Mapping(target = "replyToContent", ignore = true)
    @Mapping(target = "replyToSenderName", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ChatMessage toChatMessage(ChatMessageRequest request);

    List<ChatMessageResponse> toChatMessageResponses(List<ChatMessage> chatMessages);
}
