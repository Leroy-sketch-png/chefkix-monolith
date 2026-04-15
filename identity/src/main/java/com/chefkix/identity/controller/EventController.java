package com.chefkix.identity.controller;

import com.chefkix.identity.dto.request.EventBatchRequest;
import com.chefkix.identity.service.EventTrackingService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventController {

    EventTrackingService eventTrackingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Integer>>> trackEvents(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EventBatchRequest request) {
        String userId = jwt.getSubject();
        int accepted = eventTrackingService.trackEvents(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("accepted", accepted, "total", request.getEvents().size())));
    }

    @DeleteMapping("/my-data")
    public ResponseEntity<ApiResponse<Map<String, Long>>> deleteMyEventData(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        long deleted = eventTrackingService.deleteUserEvents(userId);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("deleted", deleted)));
    }
}
