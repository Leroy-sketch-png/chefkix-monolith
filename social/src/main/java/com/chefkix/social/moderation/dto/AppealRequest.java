package com.chefkix.social.moderation.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Appeal request from a banned user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealRequest {
    String banId;
    String reason;
    List<String> evidenceUrls;
}
