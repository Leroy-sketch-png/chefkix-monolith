package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper matching the Python AI service's wrap_response() format.
 * All AI service endpoints return: { success, data, message, statusCode }.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIServiceResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Integer statusCode;
}
