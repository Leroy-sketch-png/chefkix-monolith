package com.chefkix.social.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupPrivacyUpdateRequest {
    @NotBlank(message = "Privacy type cannot be blank")
    @Size(max = 50)
    private String privacyType;

    @NotBlank(message = "Confirmation password is required to change privacy settings")
    @Size(max = 200)
    private String confirmationPassword;
}