package com.chefkix.social.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferOwnershipRequest {

    @NotBlank(message = "Target user ID is required")
    private String targetUserId;

    @NotBlank(message = "You must enter your password to confirm this action")
    private String password;
}