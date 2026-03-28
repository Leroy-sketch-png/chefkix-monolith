package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Creator verification request — tracks the lifecycle of badge requests.
 * Eligible creators apply → admin reviews → approved/rejected.
 */
@Document(collection = "verification_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationRequest {

  @Id String id;

  @Indexed String userId;

  /** PENDING, APPROVED, REJECTED */
  @Builder.Default String status = "PENDING";

  /** Optional external payment reference (for paid verification) */
  String paymentId;

  /** Why the user wants verification */
  String reason;

  /** Admin notes on approval/rejection */
  String adminNotes;

  /** Admin who reviewed */
  String reviewedBy;

  @CreatedDate Instant requestedAt;

  Instant reviewedAt;
}
