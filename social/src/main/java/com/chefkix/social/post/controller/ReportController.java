package com.chefkix.social.post.controller;

import com.chefkix.social.post.dto.request.ReportRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.post.dto.response.ReportResponse;
import com.chefkix.social.post.service.ReportService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 
 */
@RestController
@RequestMapping("/posts/report")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    ReportService reportService;

    /**
     * 
     * 
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            Authentication authentication,
            @Valid @RequestBody ReportRequest request) {

        ReportResponse result = reportService.createReport(authentication, request);
        ApiResponse<ReportResponse> body = ApiResponse.created(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
