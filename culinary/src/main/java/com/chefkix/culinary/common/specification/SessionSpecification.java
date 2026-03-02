package com.chefkix.culinary.common.specification;

import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.culinary.common.enums.SessionStatus;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SessionSpecification {

    /**
     * Tạo Criteria để lọc Session History theo userId và status.
     * @param userId ID của người dùng (Bắt buộc)
     * @param query Chuỗi trạng thái ("completed", "posted", "all",...)
     * @return Criteria MongoDB hoàn chỉnh
     */
    public static Criteria getCriteria(String userId, SessionHistoryQuery query) {
        List<Criteria> criteriaList = new ArrayList<>();

        // 1. Lọc theo User ID (BẮT BUỘC)
        criteriaList.add(Criteria.where("userId").is(userId));

        // 2. Lọc theo Trạng thái (statusFilter)
        if (StringUtils.hasText(query.getStatusFilter()) && !"all".equalsIgnoreCase(query.getStatusFilter())) {
            try {
                // Chuyển chuỗi trạng thái thành Enum (Ví dụ: "completed" -> SessionStatus.COMPLETED)
                SessionStatus status = SessionStatus.valueOf(query.getStatusFilter().toUpperCase());
                criteriaList.add(Criteria.where("status").is(status));
            } catch (IllegalArgumentException e) {
                // Bỏ qua nếu giá trị không hợp lệ, hoặc ném Exception (Tùy chọn)
                // Hiện tại, ta sẽ lọc theo giá trị không tồn tại để query trả về rỗng.
                criteriaList.add(Criteria.where("status").is("INVALID_STATUS_FILTER"));
            }
        }

        // 3. Gộp tất cả điều kiện lại bằng toán tử AND
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}