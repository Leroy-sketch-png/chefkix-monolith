package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivateSubscriptionRequest {
    @NotBlank(message = "Payment provider is required")
    String paymentProvider;

    @NotBlank(message = "Payment token is required")
    String paymentToken;
}
