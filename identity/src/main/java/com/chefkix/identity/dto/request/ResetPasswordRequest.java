package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  String email;

  @NotBlank(message = "OTP is required")
  @Size(min = 6, max = 6, message = "OTP must be exactly 6 characters")
  String otp;

  @NotBlank(message = "New password is required")
  @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
  String newPassword;
}
