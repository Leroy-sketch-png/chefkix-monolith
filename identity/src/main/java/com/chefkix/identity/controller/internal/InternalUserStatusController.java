package com.chefkix.identity.controller.internal;

import com.chefkix.identity.dto.request.internal.InternalUserStatusRequest;
import com.chefkix.identity.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserStatusController {

    private final UserStatusService userStatusService;

    @PostMapping("/{userId}/status")
    public void updateUserStatus(
            @PathVariable String userId,
            @RequestBody InternalUserStatusRequest request
    ) {
        if (request.isOnline()) {
            userStatusService.setUserOnline(userId);
        } else {
            userStatusService.setUserOffline(userId);
        }
    }
}