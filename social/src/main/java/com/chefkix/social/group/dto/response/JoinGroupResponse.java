package com.chefkix.social.group.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinGroupResponse {
    private String groupId;
private String membershipStatus;
    private String message;
}