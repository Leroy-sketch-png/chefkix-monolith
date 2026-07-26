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

  @Size(max = 500)
  String reason;

  @Size(max = 200)
  String paymentId;
}
