package com.chefkix.social.post.mapper;

import com.chefkix.social.post.dto.request.ReplyRequest;
import com.chefkix.social.post.dto.response.ReplyResponse;
import com.chefkix.social.post.entity.Reply;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReplyMapper {
    ReplyResponse toResponse(Reply reply);

    Reply toReply(ReplyRequest replyRequest);
}
