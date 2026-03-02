package com.chefkix.culinary.common.specification;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RecipeSpecification {

    /**
     * Chuyển đổi từ DTO Search sang Criteria của MongoDB.
     * Tương đương với việc viết mệnh đề WHERE trong SQL.
     */
    public static Criteria getCriteria(RecipeSearchQuery queryDto) {
        List<Criteria> criteriaList = new ArrayList<>();

        // 1. Base Condition: Luôn chỉ lấy các bài đã Publish
        // NOTE: Entity uses 'status' enum (DRAFT, PUBLISHED, ARCHIVED), NOT boolean 'isPublished'
        criteriaList.add(Criteria.where("status").is("PUBLISHED"));

        // 2. Tìm kiếm theo Từ khóa (Title hoặc Description)
        // Sử dụng Regex để tìm gần đúng (LIKE %query%), 'i' là case-insensitive (không phân biệt hoa thường)
        if (StringUtils.hasText(queryDto.getQuery())) {
            String regex = ".*" + queryDto.getQuery() + ".*"; // Regex: Chứa chuỗi query
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i")
            ));
        }

        // 3. Lọc theo Độ khó (Exact Match)
        if (queryDto.getDifficulty() != null) {
            criteriaList.add(Criteria.where("difficulty").is(queryDto.getDifficulty()));
        }

        // 4. Lọc theo Loại ẩm thực (Exact Match)
        if (StringUtils.hasText(queryDto.getCuisineType())) {
            criteriaList.add(Criteria.where("cuisineType").is(queryDto.getCuisineType()));
        }

        // 5. Lọc theo Thời gian nấu (Less Than or Equal)
        // Tìm các món nấu nhanh hơn hoặc bằng thời gian user chọn
        if (queryDto.getMaxTimeMinutes() != null) {
            criteriaList.add(Criteria.where("totalTimeMinutes").lte(queryDto.getMaxTimeMinutes()));
        }

        // 6. Lọc theo Chế độ ăn (Dietary Tags)
        // Logic: Món ăn phải chứa TẤT CẢ các tag user chọn (AND logic)
        // Ví dụ: Chọn "Vegan" và "Keto" -> Món ăn phải có cả 2 tag này.
        if (queryDto.getDietaryTags() != null && !queryDto.getDietaryTags().isEmpty()) {
            criteriaList.add(Criteria.where("dietaryTags").all(queryDto.getDietaryTags()));
        }

        // 7. Gộp tất cả điều kiện lại bằng toán tử AND
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}