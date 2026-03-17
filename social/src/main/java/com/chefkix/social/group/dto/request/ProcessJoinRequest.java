package com.chefkix.social.group.dto.request;

import com.chefkix.social.chat.enums.RequestAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessJoinRequest {

    @NotNull(message = "Action is required")
    private RequestAction action;
}