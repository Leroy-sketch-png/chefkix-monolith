package com.chefkix.culinary.features.session.dto.request;
import lombok.Data;

@Data
public class SessionNavigateRequest {
    private String action; // "next" | "previous" | "goto"
    private Integer targetStep;
}