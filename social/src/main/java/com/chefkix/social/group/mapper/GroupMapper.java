package com.chefkix.social.group.mapper;

import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.entity.Group;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    GroupResponse toGroupResponse(Group group);

    Group toGroup(GroupCreationRequest request);
}
