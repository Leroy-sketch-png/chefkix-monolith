package com.chefkix.identity.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Creator tip jar settings.
 * Revenue Pillar 3 (Cooking) + Pillar 2 (Creation): Users tip creators for great recipes.
 *
 * Phase 0: Tip amounts + message storage. No payment processing.
 * Phase 1: Stripe Connect integration — payoutAccountId populated.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "creator_tip_settings")
public class CreatorTipSettings {

    @Id String id;

    @Indexed(unique = true) String userId;

    @Builder.Default boolean tipsEnabled = false;

    String payoutAccountId; // Stripe Connect account ID (null until connected)

    @Builder.Default String currency = "USD";

    // Suggested amounts to display on tip jar UI (e.g., [1, 3, 5])
    @Builder.Default int[] suggestedAmounts = new int[]{1, 3, 5};

    String thankYouMessage; // Custom message shown after a tip

    @CreatedDate Instant createdAt;

    @LastModifiedDate Instant updatedAt;
}
