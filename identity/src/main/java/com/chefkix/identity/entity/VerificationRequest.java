package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
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

  @Builder.Default String status = "PENDING";

  String paymentId;

  String reason;

  String adminNotes;

  String reviewedBy;

  @CreatedDate Instant requestedAt;

  Instant reviewedAt;
}
