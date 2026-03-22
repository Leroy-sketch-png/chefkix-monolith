package com.chefkix.social.group.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinGroupResponse {
    private String groupId;
    private String membershipStatus; // "ACTIVE" or "PENDING"
    private String message;
}