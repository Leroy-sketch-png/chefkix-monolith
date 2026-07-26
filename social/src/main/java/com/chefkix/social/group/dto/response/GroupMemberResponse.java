package com.chefkix.social.group.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GroupMemberResponse {
    private String userId;
    private String displayName;
    private String avatarUrl;
private String role;
    private LocalDateTime joinedAt;
}