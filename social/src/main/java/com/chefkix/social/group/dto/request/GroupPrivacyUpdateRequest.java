package com.chefkix.social.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupPrivacyUpdateRequest {
    @NotBlank(message = "Privacy type cannot be blank")
    private String privacyType;

    @NotBlank(message = "Confirmation password is required to change privacy settings")
    private String confirmationPassword;
}