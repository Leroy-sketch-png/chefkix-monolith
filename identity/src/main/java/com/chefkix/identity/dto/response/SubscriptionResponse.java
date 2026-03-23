package com.chefkix.identity.dto.response;

import com.chefkix.identity.entity.PremiumFeature;
import com.chefkix.identity.entity.SubscriptionTier;
import java.time.Instant;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionResponse {
    SubscriptionTier tier;
    boolean active;
    boolean premium;
    Instant startDate;
    Instant endDate;
    boolean trialUsed;
    boolean trialActive;
    boolean cancelledAtPeriodEnd;
    Instant cancelledAt;
    List<PremiumFeature> availableFeatures;
    Instant createdAt;
}
