package com.chefkix.identity.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralStatsResponse {

    String code;
    Long totalReferrals;
    Long totalXpEarned;
    List<ReferralDetail> referrals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReferralDetail {
        String referredUsername;
        String referredAvatar;
        Integer xpAwarded;
        Instant redeemedAt;
    }
}
