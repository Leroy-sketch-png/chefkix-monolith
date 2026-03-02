package com.chefkix.social.chat.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareContactResponse {
    String conversationId;
    String displayName;
    String avatar;
    String type;
    String userId;
    boolean isOnline;
}
