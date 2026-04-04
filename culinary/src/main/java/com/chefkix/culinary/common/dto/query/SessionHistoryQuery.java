package com.chefkix.culinary.common.dto.query;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 * DTO containing filter and pagination parameters for Session History.
 * userId is extracted from JWT, no need to include in this DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionHistoryQuery {

    // Filter by status (e.g.: "completed", "posted", "all")
    private String statusFilter;

}