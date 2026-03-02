package com.chefkix.culinary.features.report.controller;

import com.chefkix.culinary.features.report.dto.request.ReportRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.report.dto.response.ReportResponse;
import com.chefkix.culinary.features.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    ReportService reportService;

//    @PostMapping("/{completionId}/report")
//    @ResponseStatus(HttpStatus.CREATED)
//    public ApiResponse<ReportResponse> reportCompletion(
//            @PathVariable String completionId,
//            @RequestBody @Valid ReportRequest request
//    ) {
//        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
//        return ApiResponse.<ReportResponse>builder()
//                .data(reportService.reportCompletion(completionId, userId, request))
//                .statusCode(200)
//                .message("Report has been sent.")
//                .build();
//    }
}
