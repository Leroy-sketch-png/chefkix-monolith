package com.chefkix.social.post.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlateRateResponse {
    String userRating;
    int fireCount;
    int cringeCount;
}
