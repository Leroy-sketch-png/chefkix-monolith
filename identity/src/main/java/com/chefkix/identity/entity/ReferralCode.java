package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "referral_codes")
public class ReferralCode {

    @Id
    String id;

    @Indexed(unique = true)
    String userId;

    @Indexed(unique = true)
    String code;

    @Builder.Default
    Integer usageCount = 0;

    @Builder.Default
    Integer maxUses = 100;

    @Builder.Default
    Boolean active = true;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
