package com.chefkix.culinary.common.dto.query;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 * DTO chứa các tham số lọc và phân trang cho Session History.
 * userId được lấy từ JWT, không cần có trong DTO này.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionHistoryQuery {

    // Tham số lọc theo Trạng thái (Ví dụ: "completed", "posted", "all")
    private String statusFilter;

}