package com.chefkix.social.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRequestResponse {

    private String userId;

    private String displayName;
    private String avatarUrl;

    private LocalDateTime requestedAt;
}