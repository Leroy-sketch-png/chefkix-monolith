package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "referral_redemptions")
@CompoundIndex(name = "referrer_referred_unique", def = "{'referrerUserId': 1, 'referredUserId': 1}", unique = true)
public class ReferralRedemption {

    @Id
    String id;

    String referrerUserId;

    @Indexed(unique = true)
    String referredUserId;

    String referralCodeId;

    @Builder.Default
    Integer xpAwarded = 100;

    @CreatedDate
    Instant redeemedAt;
}
