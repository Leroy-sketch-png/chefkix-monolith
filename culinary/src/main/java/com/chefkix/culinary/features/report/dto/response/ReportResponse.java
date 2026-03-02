package com.chefkix.culinary.features.report.dto.response;

import com.chefkix.culinary.common.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportResponse {
    private String reportId;
    private ReportStatus status;
}