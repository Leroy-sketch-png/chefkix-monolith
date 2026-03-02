package com.chefkix.culinary.features.report.dto.request;

import com.chefkix.culinary.common.enums.ReportReason;
import com.chefkix.culinary.common.enums.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotNull
    private TargetType targetType; // Enum: POST, RECIPE, COMMENT

    @NotBlank
    private String targetId; // ObjectId của đối tượng bị report

    @NotNull
    private ReportReason reason;

    @Size(max = 500)
    private String details;
}