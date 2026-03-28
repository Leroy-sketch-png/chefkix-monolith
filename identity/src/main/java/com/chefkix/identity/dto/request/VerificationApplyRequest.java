package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationApplyRequest {

  /** Optional reason — why the user wants verification */
  @Size(max = 500)
  String reason;

  /** Optional payment reference for paid verification */
  @Size(max = 200)
  String paymentId;
}
