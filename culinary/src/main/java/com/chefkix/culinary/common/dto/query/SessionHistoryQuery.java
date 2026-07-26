package com.chefkix.culinary.common.dto.query;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionHistoryQuery {

    private String statusFilter;
    private String status;

    public String getEffectiveStatusFilter() {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return statusFilter;
        }
        return status;
    }
}