package com.chefkix.social.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TransferOwnershipRequest {

    @NotBlank(message = "Target user ID is required")
    @Size(max = 100)
    private String targetUserId;

    @NotBlank(message = "You must enter your password to confirm this action")
    @Size(max = 200)
    private String password;
}