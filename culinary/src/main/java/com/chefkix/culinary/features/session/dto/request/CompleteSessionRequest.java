package com.chefkix.culinary.features.session.dto.request;

import lombok.Data;

@Data
public class CompleteSessionRequest {
    private Integer rating;
    private String notes;
}