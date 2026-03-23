package com.chefkix.identity.dto.response;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralCodeResponse {

    String code;
    Integer usageCount;
    Integer maxUses;
    Boolean active;
    Instant createdAt;
    String shareUrl;
}
