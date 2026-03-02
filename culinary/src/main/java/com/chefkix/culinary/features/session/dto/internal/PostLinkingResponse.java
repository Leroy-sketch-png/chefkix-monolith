package com.chefkix.culinary.features.session.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLinkingResponse {
    private String postId;
    private int photoCount;
    private String userId;
}
