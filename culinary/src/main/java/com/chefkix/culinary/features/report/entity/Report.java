package com.chefkix.culinary.features.report.entity;

import com.chefkix.culinary.common.enums.AppealStatus;
import com.chefkix.culinary.common.enums.ReportReason;
import com.chefkix.culinary.common.enums.ReportStatus;
import com.chefkix.culinary.common.enums.TargetType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

// Document: Report
@Document(collection = "reports")
@Data // Lombok
public class Report {
    @Id
    private String id; // MongoDB ObjectId

    private String reporterId; // ID của User báo cáo

    private TargetType targetType;
    private String targetId; // ID của post/recipe/comment

    private ReportReason reason;
    private String details;

    private ReportStatus status = ReportStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
}
