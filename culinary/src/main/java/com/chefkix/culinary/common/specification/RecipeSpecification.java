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

        // 1b. VISIBILITY FILTER: Respect recipe visibility settings
        // PUBLIC = everyone can see
        // PRIVATE = only the owner can see
        // FRIENDS_ONLY = owner + friends can see
        if (queryDto.getCurrentUserId() != null) {
            List<Criteria> visibilityCriteria = new ArrayList<>();
            // PUBLIC recipes are always visible
            visibilityCriteria.add(Criteria.where("recipeVisibility").is("PUBLIC"));
            // Null visibility defaults to PUBLIC (backward compatibility)
            visibilityCriteria.add(Criteria.where("recipeVisibility").is(null));
            // Owner can always see their own recipes
            visibilityCriteria.add(Criteria.where("userId").is(queryDto.getCurrentUserId()));
            // FRIENDS_ONLY: visible to friends
            if (queryDto.getFriendIds() != null && !queryDto.getFriendIds().isEmpty()) {
                visibilityCriteria.add(new Criteria().andOperator(
                        Criteria.where("recipeVisibility").is("FRIENDS_ONLY"),
                        Criteria.where("userId").in(queryDto.getFriendIds())
                ));
            }
            criteriaList.add(new Criteria().orOperator(visibilityCriteria.toArray(new Criteria[0])));
        } else {
            // No auth context: only show PUBLIC recipes
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("recipeVisibility").is("PUBLIC"),
                    Criteria.where("recipeVisibility").is(null)
            ));
        }

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