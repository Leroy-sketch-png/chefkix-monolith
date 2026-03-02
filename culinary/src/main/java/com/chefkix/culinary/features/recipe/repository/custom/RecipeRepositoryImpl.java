package com.chefkix.culinary.features.recipe.repository.custom;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.specification.RecipeSpecification; // Import class Specification
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RecipeRepositoryImpl implements RecipeRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Recipe> searchRecipes(RecipeSearchQuery queryDto, Pageable pageable) {
        // 1. Nhờ Specification xây dựng bộ lọc (Criteria)
        // Code logic lọc nằm gọn trong file Specification, giúp file này sạch sẽ
        Criteria criteria = RecipeSpecification.getCriteria(queryDto);

        // 2. Tạo Query từ Criteria và áp dụng Pageable
        Query query = new Query(criteria).with(pageable);

        // 3. Xử lý Custom Sort (Nếu có logic sort đặc biệt không có trong Pageable)
        // Ví dụ: Sort theo "Trending" (View + Like)
        applyCustomSorting(query, queryDto.getSortBy());

        // 4. Thực thi Query đếm tổng số bản ghi (để tính total pages)
        // Lưu ý: count() có thể chậm với dữ liệu lớn, có thể tối ưu bằng estimateCount() nếu cần
        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), Recipe.class);

        // 5. Thực thi Query lấy dữ liệu
        List<Recipe> recipes = mongoTemplate.find(query, Recipe.class);

        return new PageImpl<>(recipes, pageable, total);
    }

    @Override
    public void incrementViewCount(String recipeId) {
        // Dùng updateFirst để tăng atomic, không cần load object lên rồi save lại
        Query query = Query.query(Criteria.where("id").is(recipeId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Recipe.class);
    }

    // Helper method xử lý sort đặc biệt
    private void applyCustomSorting(Query query, String sortBy) {
        if ("trending".equalsIgnoreCase(sortBy)) {
            // Logic Trending: Ưu tiên View cao, sau đó đến Like cao
            query.with(Sort.by(Sort.Direction.DESC, "viewCount", "likeCount"));
        }
        else if ("xpReward".equalsIgnoreCase(sortBy)) {
            query.with(Sort.by(Sort.Direction.DESC, "xpReward"));
        }
        // Các trường hợp sort cơ bản (createdAt, title...) thì Pageable đã tự lo rồi
    }

    @Override
    public Recipe updateLikeCount(String recipeId, int amount) {
        return updateCounter(recipeId, "likeCount", amount);
    }

    @Override
    public Recipe updateSaveCount(String recipeId, int amount) {
        return updateCounter(recipeId, "saveCount", amount);
    }

    // Helper method tái sử dụng logic update
    private Recipe updateCounter(String recipeId, String fieldName, int amount) {
        Query query = Query.query(Criteria.where("id").is(recipeId));
        Update update = new Update().inc(fieldName, amount);

        // returnNew(true): Trả về object SAU KHI đã cộng
        return mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                Recipe.class
        );
    }
}