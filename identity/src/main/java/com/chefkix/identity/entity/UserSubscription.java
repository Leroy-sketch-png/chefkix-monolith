package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "user_subscriptions")
public class UserSubscription {

    @Id
    String id;

    @Indexed(unique = true)
    String userId;

    @Builder.Default
    SubscriptionTier tier = SubscriptionTier.FREE;

    @Builder.Default
    boolean active = false;

    String externalSubscriptionId;

    String paymentProvider;

    Instant startDate;
    Instant endDate;

    @Builder.Default
    boolean trialUsed = false;

    Instant trialStartDate;
    Instant trialEndDate;

    boolean cancelledAtPeriodEnd;
    Instant cancelledAt;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
