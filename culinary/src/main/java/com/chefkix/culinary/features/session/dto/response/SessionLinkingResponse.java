package com.chefkix.culinary.features.session.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionLinkingResponse {
    private String sessionId;
    private String postId;
    private List<String> badgesEarned;
    private int xpAwarded;
    private int totalXpForRecipe; // Integer for clean game system values
    private boolean creatorBonusAwarded;
}
