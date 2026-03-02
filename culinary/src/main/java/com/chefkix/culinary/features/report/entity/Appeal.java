package com.chefkix.culinary.features.report.entity;

import com.chefkix.culinary.common.enums.AppealStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

// Document: Appeal
@Document(collection = "appeals")
@Data
public class Appeal {
    @Id
    private String id;

    private String userId;        // Người kháng cáo
    private String completionId;  // Completion bị phạt

    private String reason;
    private List<String> evidenceImages; // List URL ảnh

    private AppealStatus status = AppealStatus.PENDING;
    private LocalDateTime createdAt = LocalDateTime.now();
}