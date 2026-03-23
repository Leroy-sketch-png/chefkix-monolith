package com.chefkix.identity.dto.response;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationResponse {
  String id;
  String userId;
  String status; // PENDING, APPROVED, REJECTED
  String reason;
  String adminNotes;
  Instant requestedAt;
  Instant reviewedAt;
}
