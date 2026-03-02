package com.chefkix.culinary.features.report.dto.request;

import com.chefkix.culinary.common.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    @NotNull(message = "Reason is required")
    private ReportReason reason;

    @NotBlank(message = "Description is required")
    private String description;
}
